package com.lifereview.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifereview.dto.*;
import com.lifereview.entity.ProductOrder;
import com.lifereview.entity.Shop;
import com.lifereview.entity.ShopProduct;
import com.lifereview.enums.ProductOrderStatus;
import com.lifereview.repository.ProductOrderRepository;
import com.lifereview.repository.ShopProductRepository;
import com.lifereview.repository.ShopRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.CacheOpsService;
import com.lifereview.service.ShopProductService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 店铺商品服务实现类。
 * <p>负责商品 CRUD、管理端列表、商品详情缓存、秒杀（Lua + Redisson 锁，可选 Kafka 异步）、订单支付与取消；Redis 异常时秒杀可降级为纯数据库路径。</p>
 */
@Service
@RequiredArgsConstructor
public class ShopProductServiceImpl implements ShopProductService {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(ShopProductServiceImpl.class);
    /** Redis 中异步秒杀结果键前缀 */
    private static final String SECKILL_RESULT_KEY_PREFIX = "seckill:purchase:result:";

    /** 商品仓储 */
    private final ShopProductRepository shopProductRepository;
    /** 订单仓储 */
    private final ProductOrderRepository productOrderRepository;
    /** 店铺仓储 */
    private final ShopRepository shopRepository;
    /** 用户仓储 */
    private final UserRepository userRepository;
    /** Redis 字符串模板 */
    private final StringRedisTemplate stringRedisTemplate;
    /** JSON 序列化（缓存与秒杀结果） */
    private final ObjectMapper objectMapper;
    /** 缓存双删、TTL 抖动等 */
    private final CacheOpsService cacheOpsService;
    /** Redisson 客户端（分布式锁） */
    private final RedissonClient redissonClient;
    /** Kafka 模板（异步秒杀） */
    private final KafkaTemplate<String, SeckillPurchaseMessage> kafkaTemplate;
    /** 秒杀扣减库存 Lua 脚本 */
    @Qualifier("seckillDeductScript")
    private final DefaultRedisScript<Long> seckillDeductScript;
    /** 秒杀回滚 Lua 脚本 */
    @Qualifier("seckillRollbackScript")
    private final DefaultRedisScript<Long> seckillRollbackScript;
    /** 商品详情缓存键前缀 */
    @Value("${app.cache.product-detail-key-prefix:cache:product:detail:}")
    private String productDetailKeyPrefix;
    /** 商品详情缓存 TTL（秒） */
    @Value("${app.cache.product-detail-ttl-seconds:600}")
    private long productDetailTtlSeconds;
    /** 空值缓存 TTL（秒） */
    @Value("${app.cache.null-ttl-seconds:120}")
    private long nullTtlSeconds;
    /** 缓存重建分布式锁 TTL（秒） */
    @Value("${app.cache.lock-ttl-seconds:5}")
    private long cacheLockTtlSeconds;
    /** 秒杀锁最长等待时间（毫秒） */
    @Value("${app.seckill.lock-wait-ms:150}")
    private long lockWaitMs;
    /** 秒杀锁租约（毫秒） */
    @Value("${app.seckill.lock-lease-ms:3000}")
    private long lockLeaseMs;
    /** 是否启用 Kafka 异步秒杀 */
    @Value("${app.kafka.seckill.enabled:false}")
    private boolean kafkaSeckillEnabled;
    /** 秒杀消息主题 */
    @Value("${app.kafka.seckill.topic:seckill-purchase}")
    private String seckillKafkaTopic;
    /** 异步秒杀结果在 Redis 中的保留时长（小时） */
    @Value("${app.kafka.seckill.result-ttl-hours:48}")
    private long seckillResultTtlHours;

    /** 代理自身接口，保证事务方法经 Spring 代理调用 */
    private ShopProductService self;

    /**
     * 注入当前 Bean 的接口代理（延迟注入避免循环依赖）。
     *
     * @param self {@link ShopProductService} 代理实例
     */
    @Lazy
    @Autowired
    public void setSelf(ShopProductService self) {
        this.self = self;
    }

    /**
     * 启动时将数据库商品库存同步到 Redis，供秒杀 Lua 使用。
     */
    @PostConstruct
    public void preloadSeckillStock() {
        try {
            for (ShopProduct product : shopProductRepository.findAll()) {
                setSeckillStock(product.getId(), Math.max(0, product.getStock() == null ? 0 : product.getStock()));
            }
        } catch (Exception ex) {
            log.warn("Skip seckill stock preload because redis is unavailable: {}", ex.getMessage());
        }
    }

    /**
     * 查询某店铺下已启用商品，按 ID 倒序。
     *
     * @param shopId 店铺主键
     * @return 商品列表项
     * @throws IllegalArgumentException 店铺不存在时抛出
     */
    @Override
    public List<ShopProductItem> listProductsByShop(Long shopId) {
        ensureShopExists(shopId);
        return shopProductRepository.findByShopIdAndEnabledTrueOrderByIdDesc(shopId).stream().map(this::mapProductItem).toList();
    }

    /**
     * 查询当前用户可管理的店铺：管理员返回全部，普通用户仅返回其为店主的店铺。
     *
     * @param currentUserId 当前用户 ID
     * @return 可管理店铺摘要列表
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Override
    public List<ManageShopItem> listManageShops(Long currentUserId, boolean superAdmin) {
        if (!superAdmin) {
            ensureUserExists(currentUserId);
        }
        List<Shop> shops = superAdmin
                ? shopRepository.findAll().stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).toList()
                : shopRepository.findByOwnerUserIdOrderByIdDesc(currentUserId);
        return shops.stream().map(s -> ManageShopItem.builder().id(s.getId()).name(s.getName()).type(s.getType()).build()).toList();
    }

    /**
     * 管理端商品列表：按关键词检索并按权限过滤店铺。
     *
     * @param currentUserId 当前用户 ID
     * @param shopId        可选，限定店铺；非 null 时会校验管理权限
     * @param keyword       可选关键词
     * @return 商品列表项
     * @throws IllegalArgumentException 用户不存在或无店铺管理权限时抛出
     */
    @Override
    public List<ShopProductItem> listManageProducts(Long currentUserId, boolean superAdmin, Long shopId, String keyword) {
        if (!superAdmin) {
            ensureUserExists(currentUserId);
        }
        if (shopId != null) {
            assertCanManageShop(currentUserId, superAdmin, shopId);
        }
        Set<Long> manageableShopIds = superAdmin ? Set.of() :
                shopRepository.findByOwnerUserIdOrderByIdDesc(currentUserId).stream().map(Shop::getId).collect(Collectors.toSet());
        return shopProductRepository.searchManageProducts(shopId, keyword).stream()
                .filter(p -> superAdmin || manageableShopIds.contains(p.getShopId()))
                .map(this::mapProductItem)
                .toList();
    }

    /**
     * 创建商品并同步 Redis 秒杀库存、失效详情缓存。
     *
     * @param currentUserId 操作者用户 ID
     * @param req           创建参数
     * @return 新建商品列表项
     * @throws IllegalArgumentException 权限、名称、图片等校验失败时抛出
     */
    @Override
    @Transactional
    public ShopProductItem createProduct(Long currentUserId, boolean superAdmin, ProductManageCreateRequest req) {
        Shop shop = ensureShopExists(req.getShopId());
        assertCanManageShop(currentUserId, superAdmin, shop.getId());
        String productName = req.getName() == null ? "" : req.getName().trim();
        if (productName.isEmpty()) {
            throw new IllegalArgumentException("商品名不能为空");
        }
        String productImage = req.getImage() == null ? "" : req.getImage().trim();
        if (productImage.isEmpty()) {
            throw new IllegalArgumentException("商品图标不能为空");
        }
        ShopProduct entity = new ShopProduct();
        entity.setShopId(req.getShopId());
        entity.setName(productName);
        entity.setPrice(req.getPrice());
        entity.setStock(req.getStock());
        entity.setDescription(req.getDescription() == null ? "" : req.getDescription().trim());
        entity.setImage(productImage);
        entity.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
        ShopProduct saved = shopProductRepository.save(entity);
        setSeckillStock(saved.getId(), Math.max(0, saved.getStock() == null ? 0 : saved.getStock()));
        invalidateProductCache(saved.getId());
        return mapProductItem(saved);
    }

    /**
     * 更新商品库存并同步 Redis、失效缓存。
     *
     * @param currentUserId 操作者用户 ID
     * @param productId     商品主键
     * @param req           新库存
     * @return 更新后的商品列表项
     * @throws IllegalArgumentException 用户/商品不存在或无管理权限时抛出
     */
    @Override
    @Transactional
    public ShopProductItem updateStock(Long currentUserId, boolean superAdmin, Long productId, ProductStockUpdateRequest req) {
        if (!superAdmin) {
            ensureUserExists(currentUserId);
        }
        ShopProduct entity = shopProductRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        assertCanManageShop(currentUserId, superAdmin, entity.getShopId());
        entity.setStock(req.getStock());
        ShopProduct saved = shopProductRepository.save(entity);
        setSeckillStock(saved.getId(), Math.max(0, saved.getStock() == null ? 0 : saved.getStock()));
        invalidateProductCache(saved.getId());
        return mapProductItem(saved);
    }

    /**
     * 更新商品上下架状态。
     *
     * @param currentUserId 操作者用户 ID
     * @param productId     商品主键
     * @param req           是否启用
     * @return 更新后的商品列表项
     * @throws IllegalArgumentException 用户/商品不存在或无管理权限时抛出
     */
    @Override
    @Transactional
    public ShopProductItem updateStatus(Long currentUserId, boolean superAdmin, Long productId, ProductStatusUpdateRequest req) {
        if (!superAdmin) {
            ensureUserExists(currentUserId);
        }
        ShopProduct entity = shopProductRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        assertCanManageShop(currentUserId, superAdmin, entity.getShopId());
        entity.setEnabled(req.getEnabled());
        ShopProduct saved = shopProductRepository.save(entity);
        invalidateProductCache(saved.getId());
        return mapProductItem(saved);
    }

    /**
     * 编辑商品基本信息（名称、价格、图片等）。
     *
     * @param currentUserId 操作者用户 ID
     * @param productId     商品主键
     * @param req           编辑内容
     * @return 更新后的商品列表项
     * @throws IllegalArgumentException 校验失败或无权限时抛出
     */
    @Override
    @Transactional
    public ShopProductItem editProduct(Long currentUserId, boolean superAdmin, Long productId, ProductEditRequest req) {
        if (!superAdmin) {
            ensureUserExists(currentUserId);
        }
        ShopProduct entity = shopProductRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        assertCanManageShop(currentUserId, superAdmin, entity.getShopId());
        String productName = req.getName() == null ? "" : req.getName().trim();
        if (productName.isEmpty()) {
            throw new IllegalArgumentException("商品名不能为空");
        }
        entity.setName(productName);
        entity.setPrice(req.getPrice());
        entity.setImage(req.getImage() == null ? "" : req.getImage().trim());
        ShopProduct saved = shopProductRepository.save(entity);
        invalidateProductCache(saved.getId());
        return mapProductItem(saved);
    }

    /**
     * 同步秒杀购买：分布式锁 + Lua 扣 Redis 库存 + 条件更新 DB 库存并建单；Redis 失败时走数据库扣减。
     * <p>脚本返回码：0 成功，1 库存不足，2 已购买过；-1 或 null 表示需预热库存后重试。</p>
     *
     * @param userId    购买用户 ID
     * @param productId 商品主键
     * @return 订单列表项
     * @throws IllegalArgumentException 限购、下架、抢光、锁竞争等业务异常
     */
    @Override
    @Transactional
    public ProductOrderItem executeSeckillPurchase(Long userId, Long productId) {
        ensureUserExists(userId);
        //从数据库的product_orders表中根据用户id和商品id进行查询
        if (productOrderRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new IllegalArgumentException("同一商品仅限购买一次");
        }
        ShopProduct product = getProductByIdCached(productId);
        if (!Boolean.TRUE.equals(product.getEnabled())) {
            throw new IllegalArgumentException("商品已下架");
        }
        RLock lock = redissonClient.getLock(lockKey(productId, userId));
        boolean locked;
        try {
            locked = lock.tryLock(lockWaitMs, lockLeaseMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("系统繁忙，请稍后重试");
        }
        if (!locked) {
            throw new IllegalArgumentException("抢购火爆，请稍后重试");
        }
        try {
            Long scriptCode;
            try {
                scriptCode = executeDeductScript(product, userId);
            } catch (Exception redisEx) {
                log.warn("Redis seckill script failed, fallback to DB purchase. productId={}, userId={}, err={}", productId, userId, redisEx.getMessage());
                return purchaseFallbackFromDb(userId, product);
            }
            // Redis 中不存在该商品库存键时，从 DB 同步后再执行脚本
            if (scriptCode == null || scriptCode == -1L) {
                setSeckillStock(productId, Math.max(0, product.getStock() == null ? 0 : product.getStock()));
                scriptCode = executeDeductScript(product, userId);
            }
            if (scriptCode == null) {
                throw new IllegalArgumentException("系统繁忙，请稍后重试");
            }
            if (scriptCode == 1L) {
                throw new IllegalArgumentException("商品已抢光");
            }
            if (scriptCode == 2L) {
                throw new IllegalArgumentException("同一商品仅限购买一次");
            }
            if (scriptCode != 0L) {
                throw new IllegalArgumentException("抢购失败，请重试");
            }
            try {
                int updatedRows = shopProductRepository.decreaseStockIfAvailable(productId);
                if (updatedRows != 1) {
                    rollbackRedisReservation(productId, userId);
                    throw new IllegalArgumentException("商品已抢光");
                }
                // 创建待支付订单
                ProductOrder order = new ProductOrder();
                order.setProductId(product.getId());
                order.setShopId(product.getShopId());
                order.setUserId(userId);
                order.setQuantity(1);
                order.setAmount(product.getPrice());
                order.setStatus(ProductOrderStatus.PENDING);
                ProductOrder saved = productOrderRepository.save(order);
                invalidateProductCache(productId);
                return mapOrderItem(saved, Map.of(product.getId(), product), Map.of(product.getShopId(), ensureShopExists(product.getShopId())));
            } catch (DataIntegrityViolationException ex) {
                rollbackRedisReservation(productId, userId);
                throw new IllegalArgumentException("同一商品仅限购买一次");
            } catch (RuntimeException ex) {
                rollbackRedisReservation(productId, userId);
                throw ex;
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 提交秒杀：未启用 Kafka 时同步执行；启用时发消息并返回 requestId。
     *
     * @param currentUserId 当前用户 ID
     * @param productId     商品主键
     * @return 同步订单或异步 requestId
     * @throws IllegalArgumentException 业务校验失败或 Kafka 发送失败时抛出
     */
    @Override
    public SeckillPurchaseSubmitResult submitSeckillPurchase(Long currentUserId, Long productId) {
        // 未启用 Kafka：走同步秒杀
        if (!kafkaSeckillEnabled) {
            return SeckillPurchaseSubmitResult.builder()
                    .async(false)
                    .order(self.executeSeckillPurchase(currentUserId, productId))
                    .build();
        }
        //启用了kafka，走异步秒杀
        ensureUserExists(currentUserId);
        if (productOrderRepository.existsByUserIdAndProductId(currentUserId, productId)) {
            throw new IllegalArgumentException("同一商品仅限购买一次");
        }
        ShopProduct product = getProductByIdCached(productId);
        if (!Boolean.TRUE.equals(product.getEnabled())) {
            throw new IllegalArgumentException("商品已下架");
        }
        String requestId = UUID.randomUUID().toString();
        writeResultPending(requestId, currentUserId, productId);
        SeckillPurchaseMessage msg = new SeckillPurchaseMessage(requestId, currentUserId, productId, Instant.now());
        try {
            kafkaTemplate.send(seckillKafkaTopic, String.valueOf(productId), msg).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Kafka send seckill failed requestId={}", requestId, e);
            stringRedisTemplate.delete(SECKILL_RESULT_KEY_PREFIX + requestId);
            throw new IllegalArgumentException("抢购队列繁忙，请稍后重试");
        }
        return SeckillPurchaseSubmitResult.builder().async(true).requestId(requestId).build();
    }

    /**
     * 根据 requestId 查询异步秒杀结果。
     *
     * @param currentUserId 当前用户 ID
     * @param requestId     提交时返回的请求 ID
     * @return 状态、提示文案及成功时的订单
     * @throws IllegalArgumentException requestId 无效或越权查看时抛出
     * @throws IllegalStateException    结果 JSON 解析失败时抛出
     */
    @Override
    public SeckillPurchaseQueryResult getSeckillPurchaseResult(Long currentUserId, String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId 无效");
        }
        String raw = stringRedisTemplate.opsForValue().get(SECKILL_RESULT_KEY_PREFIX + requestId);
        if (raw == null) {
            return SeckillPurchaseQueryResult.builder()
                    .status(SeckillPurchaseQueryResult.STATUS_FAILED)
                    .message("请求不存在或已过期")
                    .build();
        }
        try {
            SeckillPurchaseResultPayload p = objectMapper.readValue(raw, SeckillPurchaseResultPayload.class);
            if (p.getUserId() != currentUserId) {
                throw new IllegalArgumentException("无权查看该请求");
            }
            return SeckillPurchaseQueryResult.builder()
                    .status(p.getStatus())
                    .message(p.getMessage())
                    .order(p.getOrder())
                    .build();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析秒杀结果失败", e);
        }
    }

    /**
     * Kafka 消费者调用：执行秒杀并写入 Redis 结果（成功/失败/幂等兼容）。
     *
     * @param msg 秒杀消息体
     */
    @Override
    public void processSeckillPurchaseMessage(SeckillPurchaseMessage msg) {
        String requestId = msg.getRequestId();
        try {
            ProductOrderItem order = self.executeSeckillPurchase(msg.getUserId(), msg.getProductId());
            writeResultSuccess(requestId, msg.getUserId(), msg.getProductId(), order);
        } catch (IllegalArgumentException e) {
            String m = e.getMessage();
            if (m != null && (m.contains("仅限购买一次") || m.contains("已购买"))) {
                productOrderRepository.findByUserIdAndProductId(msg.getUserId(), msg.getProductId()).ifPresentOrElse(
                        order -> writeResultSuccess(requestId, msg.getUserId(), msg.getProductId(), mapOrderItemFromEntity(order)),
                        () -> writeResultFailed(requestId, msg.getUserId(), msg.getProductId(), m)
                );
            } else {
                writeResultFailed(requestId, msg.getUserId(), msg.getProductId(), m != null ? m : "抢购失败");
            }
        } catch (Exception e) {
            log.error("Seckill purchase consumer failed requestId={}", requestId, e);
            writeResultFailed(requestId, msg.getUserId(), msg.getProductId(), "系统繁忙，请稍后重试");
        }
    }

    /**
     * 将秒杀结果标记为处理中并写入 Redis。
     *
     * @param requestId 请求 ID
     * @param userId    用户 ID
     * @param productId 商品 ID
     */
    private void writeResultPending(String requestId, long userId, long productId) {
        SeckillPurchaseResultPayload p = new SeckillPurchaseResultPayload();
        p.setUserId(userId);
        p.setProductId(productId);
        p.setStatus(SeckillPurchaseQueryResult.STATUS_PENDING);
        storeSeckillResult(requestId, p);
    }

    /**
     * 写入秒杀成功结果（含订单快照）。
     *
     * @param requestId 请求 ID
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param order     订单展示项
     */
    private void writeResultSuccess(String requestId, long userId, long productId, ProductOrderItem order) {
        SeckillPurchaseResultPayload p = new SeckillPurchaseResultPayload();
        p.setUserId(userId);
        p.setProductId(productId);
        p.setStatus(SeckillPurchaseQueryResult.STATUS_SUCCESS);
        p.setOrder(order);
        storeSeckillResult(requestId, p);
    }

    /**
     * 写入秒杀失败结果与原因。
     *
     * @param requestId 请求 ID
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param message   失败说明
     */
    private void writeResultFailed(String requestId, long userId, long productId, String message) {
        SeckillPurchaseResultPayload p = new SeckillPurchaseResultPayload();
        p.setUserId(userId);
        p.setProductId(productId);
        p.setStatus(SeckillPurchaseQueryResult.STATUS_FAILED);
        p.setMessage(message);
        storeSeckillResult(requestId, p);
    }

    /**
     * 序列化并设置 Redis 键 TTL。
     *
     * @param requestId 请求 ID
     * @param p           结果负载
     * @throws IllegalStateException 序列化或写入 Redis 失败时抛出
     */
    private void storeSeckillResult(String requestId, SeckillPurchaseResultPayload p) {
        try {
            stringRedisTemplate.opsForValue().set(
                    SECKILL_RESULT_KEY_PREFIX + requestId,
                    objectMapper.writeValueAsString(p),
                    Duration.ofHours(Math.max(1, seckillResultTtlHours)));
        } catch (Exception e) {
            throw new IllegalStateException("写入秒杀结果失败", e);
        }
    }

    /**
     * 订单实体转展示项（补查商品与店铺）。
     *
     * @param order 订单实体
     * @return 订单列表项
     */
    private ProductOrderItem mapOrderItemFromEntity(ProductOrder order) {
        ShopProduct product = shopProductRepository.findById(order.getProductId()).orElse(null);
        Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
        return mapOrderItem(order,
                product == null ? Map.of() : Map.of(product.getId(), product),
                shop == null ? Map.of() : Map.of(shop.getId(), shop));
    }

    /**
     * 当前用户订单列表（按创建时间倒序，批量填充商品与店铺名）。
     *
     * @param currentUserId 用户 ID
     * @return 订单列表项
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Override
    public List<ProductOrderItem> listMyOrders(Long currentUserId) {
        ensureUserExists(currentUserId);
        List<ProductOrder> orders = productOrderRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        if (orders.isEmpty()) {
            return List.of();
        }
        Set<Long> productIds = orders.stream().map(ProductOrder::getProductId).collect(Collectors.toSet());
        Set<Long> shopIds = orders.stream().map(ProductOrder::getShopId).collect(Collectors.toSet());
        Map<Long, ShopProduct> productMap = shopProductRepository.findAllById(productIds).stream().collect(Collectors.toMap(ShopProduct::getId, p -> p));
        Map<Long, Shop> shopMap = shopRepository.findAllById(shopIds).stream().collect(Collectors.toMap(Shop::getId, s -> s));
        return orders.stream().map(order -> mapOrderItem(order, productMap, shopMap)).toList();
    }

    /**
     * 将待支付订单标记为已支付。
     *
     * @param currentUserId 用户 ID
     * @param orderId       订单主键
     * @return 更新后的订单项
     * @throws IllegalArgumentException 订单不存在、非本人或状态不可支付时抛出
     */
    @Override
    @Transactional
    public ProductOrderItem payOrder(Long currentUserId, Long orderId) {
        ProductOrder order = productOrderRepository.findByIdAndUserId(orderId, currentUserId).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != ProductOrderStatus.PENDING) {
            throw new IllegalArgumentException("当前订单状态不可支付");
        }
        order.setStatus(ProductOrderStatus.PAID);
        //2026.4.14 sy修改 订单表中不修改时间的bug
        order.setUpdatedAt(LocalDateTime.now());
        //更新订单表
        ProductOrder saved = productOrderRepository.save(order);
        ShopProduct product = shopProductRepository.findById(saved.getProductId()).orElse(null);
        Shop shop = shopRepository.findById(saved.getShopId()).orElse(null);
        return mapOrderItem(saved, product == null ? Map.of() : Map.of(product.getId(), product), shop == null ? Map.of() : Map.of(shop.getId(), shop));
    }

    /**
     * 取消待支付订单：回滚 DB 库存并尽量递增 Redis 秒杀库存字符串。
     *
     * @param currentUserId 用户 ID
     * @param orderId       订单主键
     * @return 更新后的订单项
     * @throws IllegalArgumentException 订单不存在或状态不可取消时抛出
     */
    @Override
    @Transactional
    public ProductOrderItem cancelOrder(Long currentUserId, Long orderId) {
        ProductOrder order = productOrderRepository.findByIdAndUserId(orderId, currentUserId).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != ProductOrderStatus.PENDING) {
            throw new IllegalArgumentException("当前订单状态不可取消");
        }
        order.setStatus(ProductOrderStatus.CANCELLED);
        ProductOrder saved = productOrderRepository.save(order);
        shopProductRepository.increaseStock(saved.getProductId(), 1);
        try {
            stringRedisTemplate.opsForValue().increment(stockKey(saved.getProductId()));
        } catch (Exception ex) {
            log.warn("Increment redis seckill stock failed, productId={}, err={}", saved.getProductId(), ex.getMessage());
        }
        ShopProduct product = shopProductRepository.findById(saved.getProductId()).orElse(null);
        invalidateProductCache(saved.getProductId());
        Shop shop = shopRepository.findById(saved.getShopId()).orElse(null);
        return mapOrderItem(saved, product == null ? Map.of() : Map.of(product.getId(), product), shop == null ? Map.of() : Map.of(shop.getId(), shop));
    }

    /**
     * 商品实体转列表 DTO。
     *
     * @param item 商品实体
     * @return 列表项
     */
    private ShopProductItem mapProductItem(ShopProduct item) {
        return ShopProductItem.builder().id(item.getId()).shopId(item.getShopId()).name(item.getName()).price(item.getPrice()).stock(item.getStock()).enabled(item.getEnabled()).description(item.getDescription()).image(item.getImage()).build();
    }

    /**
     * 订单实体转展示 DTO。
     *
     * @param order     订单
     * @param productMap 商品 ID 到实体的映射
     * @param shopMap    店铺 ID 到实体的映射
     * @return 订单列表项
     */
    private ProductOrderItem mapOrderItem(ProductOrder order, Map<Long, ShopProduct> productMap, Map<Long, Shop> shopMap) {
        ShopProduct product = productMap.get(order.getProductId());
        Shop shop = shopMap.get(order.getShopId());
        return ProductOrderItem.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .productName(product == null ? "商品已删除" : product.getName())
                .shopId(order.getShopId())
                .shopName(shop == null ? "店铺已删除" : shop.getName())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * 校验用户存在。
     *
     * @param userId 用户主键
     * @throws IllegalArgumentException 不存在时抛出
     */
    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    /**
     * 按 ID 查询店铺，不存在则抛异常。
     *
     * @param shopId 店铺主键
     * @return 店铺实体
     * @throws IllegalArgumentException 店铺不存在时抛出
     */
    private Shop ensureShopExists(Long shopId) {
        return shopRepository.findById(shopId).orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
    }

    /**
     * 校验当前用户可管理该店铺（管理员或店主）。
     *
     * @param userId 用户 ID
     * @param shopId 店铺 ID
     * @throws IllegalArgumentException 非店主且非管理员时抛出
     */
    private void assertCanManageShop(Long userId, boolean superAdmin, Long shopId) {
        if (superAdmin) {
            return;
        }
        Shop shop = ensureShopExists(shopId);
        if (shop.getOwnerUserId() == null || !shop.getOwnerUserId().equals(userId)) {
            throw new IllegalArgumentException("仅可管理自己店铺的商品");
        }
    }

    /**
     * 带缓存读取商品详情；缓存穿透用空值短 TTL，重建用分布式锁。
     *
     * sy：先根据商品缓存的key在redis中进行读取，如果查到了value，如果是__NULL__则说明缓存的空值来防止缓存穿透，如果是其他的则将value映射为商品返回。
     *     如果没查到，则先根据商品id尝试获取锁，如果获取锁失败，则从数据库中查询商品，如果在数据库中也没查到，则返回商品不存在的异常。  如果在数据库中查到了，返回商品。
     *                                    如果获取锁成功，则在数据库中查询上货品，如果在数据库中也没查到，构建值为__NULL__的缓存，返回商品不存在的异常。释放锁
     *                                                                     如果在数据库中查到了，构建商品在redis的缓存，并将TTL设置为抖动的，抖动区间在配置文件里设置。返回商品。释放锁
     * @param productId 商品主键
     * @return 商品实体
     * @throws IllegalArgumentException 商品不存在时抛出
     */
    private ShopProduct getProductByIdCached(Long productId) {
        String cacheKey = productCacheKey(productId);
        String raw = stringRedisTemplate.opsForValue().get(cacheKey);
        if (raw != null) {
            if (CacheOpsService.NULL_MARKER.equals(raw)) {
                throw new IllegalArgumentException("商品不存在");
            }
            try {
                return objectMapper.readValue(raw, ShopProduct.class);
            } catch (Exception ignore) {
            }
        }
        String lockKey = cacheKey + ":lock";
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(Math.max(1, cacheLockTtlSeconds)));
        if (!Boolean.TRUE.equals(locked)) {
            ShopProduct fallback = shopProductRepository.findById(productId).orElse(null);
            if (fallback == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            return fallback;
        }
        try {
            ShopProduct product = shopProductRepository.findById(productId).orElse(null);
            if (product == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, CacheOpsService.NULL_MARKER, Duration.ofSeconds(Math.max(30, nullTtlSeconds)));
                throw new IllegalArgumentException("商品不存在");
            }
            stringRedisTemplate.opsForValue().set(cacheKey, toJson(product), Duration.ofSeconds(cacheOpsService.withJitter(productDetailTtlSeconds)));
            return product;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 执行秒杀扣减 Lua 脚本。
     *
     * @param product 商品
     * @param userId  用户 ID
     * @return 脚本返回码
     */
    private Long executeDeductScript(ShopProduct product, Long userId) {
        return stringRedisTemplate.execute(seckillDeductScript, List.of(stockKey(product.getId()), buyerSetKey(product.getId())), String.valueOf(userId));
    }

    /**
     * 回滚 Redis 中对某用户在该商品上的秒杀预占。
     *
     * @param productId 商品 ID
     * @param userId    用户 ID
     */
    private void rollbackRedisReservation(Long productId, Long userId) {
        try {
            stringRedisTemplate.execute(seckillRollbackScript, List.of(stockKey(productId), buyerSetKey(productId)), String.valueOf(userId));
        } catch (Exception ex) {
            log.warn("Rollback redis seckill reservation failed, productId={}, userId={}, err={}", productId, userId, ex.getMessage());
        }
    }

    /**
     * 将秒杀可用库存写入 Redis 字符串键。
     *
     * @param productId 商品 ID
     * @param stock     库存数量（非负）
     */
    private void setSeckillStock(Long productId, int stock) {
        try {
            stringRedisTemplate.opsForValue().set(stockKey(productId), String.valueOf(Math.max(0, stock)));
        } catch (Exception ex) {
            log.warn("Set redis seckill stock failed, productId={}, err={}", productId, ex.getMessage());
        }
    }

    /**
     * 双删商品详情缓存。
     *
     * @param productId 商品 ID
     */
    private void invalidateProductCache(Long productId) {
        cacheOpsService.deleteWithDoubleDelete(productCacheKey(productId));
    }

    /**
     * Redis 秒杀库存键。
     *
     * @param productId 商品 ID
     * @return 键名
     */
    private String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    /**
     * Redis 已购用户集合键（Lua 去重）。
     *
     * @param productId 商品 ID
     * @return 键名
     */
    private String buyerSetKey(Long productId) {
        return "seckill:buyers:" + productId;
    }

    /**
     * Redisson 秒杀分布式锁键（按用户+商品粒度）。
     *
     * @param productId 商品 ID
     * @param userId    用户 ID
     * @return 锁键
     */
    private String lockKey(Long productId, Long userId) {
        return "lock:seckill:product:" + productId + ":user:" + userId;
    }

    /**
     * 商品详情缓存完整键。
     *
     * @param productId 商品 ID
     * @return 缓存键
     */
    private String productCacheKey(Long productId) {
        return productDetailKeyPrefix + productId;
    }

    /**
     * 对象序列化为 JSON 字符串（用于 Redis）。
     *
     * @param obj 待序列化对象
     * @return JSON 字符串
     * @throws IllegalStateException 序列化失败时抛出
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            throw new IllegalStateException("缓存序列化失败", ex);
        }
    }

    /**
     * Redis 不可用时的纯数据库秒杀下单路径。
     *
     * @param currentUserId 用户 ID
     * @param product       商品实体
     * @return 新建订单项
     * @throws IllegalArgumentException 库存不足或唯一约束冲突（重复购买）时抛出
     */
    private ProductOrderItem purchaseFallbackFromDb(Long currentUserId, ShopProduct product) {
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new IllegalArgumentException("商品已抢光");
        }
        try {
            int updatedRows = shopProductRepository.decreaseStockIfAvailable(product.getId());
            if (updatedRows != 1) {
                throw new IllegalArgumentException("商品已抢光");
            }
            ProductOrder order = new ProductOrder();
            order.setProductId(product.getId());
            order.setShopId(product.getShopId());
            order.setUserId(currentUserId);
            order.setQuantity(1);
            order.setAmount(product.getPrice());
            order.setStatus(ProductOrderStatus.PENDING);
            ProductOrder saved = productOrderRepository.save(order);
            invalidateProductCache(product.getId());
            return mapOrderItem(saved, Map.of(product.getId(), product), Map.of(product.getShopId(), ensureShopExists(product.getShopId())));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("同一商品仅限购买一次");
        }
    }
}
