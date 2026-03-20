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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 店铺商品服务实现类。
 * 负责商品列表、秒杀（Lua 脚本 + Redisson 分布式锁）、下单、订单管理。
 */
@Service
@RequiredArgsConstructor
public class ShopProductServiceImpl implements ShopProductService {

    private static final Logger log = LoggerFactory.getLogger(ShopProductServiceImpl.class);
    private final ShopProductRepository shopProductRepository;
    private final ProductOrderRepository productOrderRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheOpsService cacheOpsService;
    private final RedissonClient redissonClient;
    @Qualifier("seckillDeductScript")
    private final DefaultRedisScript<Long> seckillDeductScript;
    @Qualifier("seckillRollbackScript")
    private final DefaultRedisScript<Long> seckillRollbackScript;
    @Value("${app.admin.user-ids:1}")
    private String adminUserIds;
    @Value("${app.cache.product-detail-key-prefix:cache:product:detail:}")
    private String productDetailKeyPrefix;
    @Value("${app.cache.product-detail-ttl-seconds:600}")
    private long productDetailTtlSeconds;
    @Value("${app.cache.null-ttl-seconds:120}")
    private long nullTtlSeconds;
    @Value("${app.cache.lock-ttl-seconds:5}")
    private long cacheLockTtlSeconds;
    @Value("${app.seckill.lock-wait-ms:150}")
    private long lockWaitMs;
    @Value("${app.seckill.lock-lease-ms:3000}")
    private long lockLeaseMs;

    /** 启动时数据预热：将所有商品库存加载到 Redis，供秒杀 Lua 脚本使用 */
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

    /** 根据店铺 id 查询该店铺下所有已启用商品，按 id 倒序 */
    @Override
    public List<ShopProductItem> listProductsByShop(Long shopId) {
        ensureShopExists(shopId);
        return shopProductRepository.findByShopIdAndEnabledTrueOrderByIdDesc(shopId).stream().map(this::mapProductItem).toList();
    }

    /** 查询当前用户可管理的店铺列表。管理员查全部，普通用户查自己为店主的店铺，按 id 倒序 */
    @Override
    public List<ManageShopItem> listManageShops(Long currentUserId) {
        ensureUserExists(currentUserId);
        List<Shop> shops = isAdmin(currentUserId)
                ? shopRepository.findAll().stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).toList()
                : shopRepository.findByOwnerUserIdOrderByIdDesc(currentUserId);
        return shops.stream().map(s -> ManageShopItem.builder().id(s.getId()).name(s.getName()).type(s.getType()).build()).toList();
    }

    @Override
    public List<ShopProductItem> listManageProducts(Long currentUserId, Long shopId, String keyword) {
        ensureUserExists(currentUserId);
        boolean admin = isAdmin(currentUserId);
        if (shopId != null) {
            assertCanManageShop(currentUserId, shopId);
        }
        Set<Long> manageableShopIds = admin ? Set.of() :
                shopRepository.findByOwnerUserIdOrderByIdDesc(currentUserId).stream().map(Shop::getId).collect(Collectors.toSet());
        return shopProductRepository.searchManageProducts(shopId, keyword).stream()
                .filter(p -> admin || manageableShopIds.contains(p.getShopId()))
                .map(this::mapProductItem)
                .toList();
    }

    @Override
    @Transactional
    public ShopProductItem createProduct(Long currentUserId, ProductManageCreateRequest req) {
        Shop shop = ensureShopExists(req.getShopId());
        assertCanManageShop(currentUserId, shop.getId());
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

    /** 更新商品库存。需有店铺管理权限，更新后同步 Redis 秒杀库存并失效缓存 */
    @Override
    @Transactional
    public ShopProductItem updateStock(Long currentUserId, Long productId, ProductStockUpdateRequest req) {
        ensureUserExists(currentUserId);
        ShopProduct entity = shopProductRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        assertCanManageShop(currentUserId, entity.getShopId());
        entity.setStock(req.getStock());
        ShopProduct saved = shopProductRepository.save(entity);
        setSeckillStock(saved.getId(), Math.max(0, saved.getStock() == null ? 0 : saved.getStock()));
        invalidateProductCache(saved.getId());
        return mapProductItem(saved);
    }

    /** 更新商品上下架状态。需有店铺管理权限 */
    @Override
    @Transactional
    public ShopProductItem updateStatus(Long currentUserId, Long productId, ProductStatusUpdateRequest req) {
        ensureUserExists(currentUserId);
        ShopProduct entity = shopProductRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        assertCanManageShop(currentUserId, entity.getShopId());
        entity.setEnabled(req.getEnabled());
        ShopProduct saved = shopProductRepository.save(entity);
        invalidateProductCache(saved.getId());
        return mapProductItem(saved);
    }

    /** 编辑商品信息（名称、价格、图片等）。需有店铺管理权限 */
    @Override
    @Transactional
    public ShopProductItem editProduct(Long currentUserId, Long productId, ProductEditRequest req) {
        ensureUserExists(currentUserId);
        ShopProduct entity = shopProductRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        assertCanManageShop(currentUserId, entity.getShopId());
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
     * 秒杀购买商品。每人限购一件。流程：Redisson 分布式锁 -> Lua 脚本扣 Redis 库存 -> DB 扣库存并创建订单。
     * Redis 不可用时降级为纯 DB 购买。脚本返回 0 成功，1 库存不足，2 已购买过。
     */
    @Override
    @Transactional
    public ProductOrderItem purchase(Long currentUserId, Long productId) {
        ensureUserExists(currentUserId);
        if (productOrderRepository.existsByUserIdAndProductId(currentUserId, productId)) {
            throw new IllegalArgumentException("同一商品仅限购买一次");
        }
        ShopProduct product = getProductByIdCached(productId);
        if (!Boolean.TRUE.equals(product.getEnabled())) {
            throw new IllegalArgumentException("商品已下架");
        }
        RLock lock = redissonClient.getLock(lockKey(productId, currentUserId));
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
                scriptCode = executeDeductScript(product, currentUserId);
            } catch (Exception redisEx) {
                log.warn("Redis seckill script failed, fallback to DB purchase. productId={}, userId={}, err={}", productId, currentUserId, redisEx.getMessage());
                return purchaseFallbackFromDb(currentUserId, product);
            }
            if (scriptCode == null || scriptCode == -1L) {        //2026.3.16 redis中不存在该商品的库存
                setSeckillStock(productId, Math.max(0, product.getStock() == null ? 0 : product.getStock()));
                scriptCode = executeDeductScript(product, currentUserId);
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
                    rollbackRedisReservation(productId, currentUserId);
                    throw new IllegalArgumentException("商品已抢光");
                }
                //创建订单
                ProductOrder order = new ProductOrder();
                order.setProductId(product.getId());
                order.setShopId(product.getShopId());
                order.setUserId(currentUserId);
                order.setQuantity(1);
                order.setAmount(product.getPrice());
                order.setStatus(ProductOrderStatus.PENDING);
                ProductOrder saved = productOrderRepository.save(order);
                invalidateProductCache(productId);
                return mapOrderItem(saved, Map.of(product.getId(), product), Map.of(product.getShopId(), ensureShopExists(product.getShopId())));
            } catch (DataIntegrityViolationException ex) {
                rollbackRedisReservation(productId, currentUserId);
                throw new IllegalArgumentException("同一商品仅限购买一次");
            } catch (RuntimeException ex) {
                rollbackRedisReservation(productId, currentUserId);
                throw ex;
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

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

    @Override
    @Transactional
    public ProductOrderItem payOrder(Long currentUserId, Long orderId) {
        ProductOrder order = productOrderRepository.findByIdAndUserId(orderId, currentUserId).orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != ProductOrderStatus.PENDING) {
            throw new IllegalArgumentException("当前订单状态不可支付");
        }
        order.setStatus(ProductOrderStatus.PAID);
        ProductOrder saved = productOrderRepository.save(order);
        ShopProduct product = shopProductRepository.findById(saved.getProductId()).orElse(null);
        Shop shop = shopRepository.findById(saved.getShopId()).orElse(null);
        return mapOrderItem(saved, product == null ? Map.of() : Map.of(product.getId(), product), shop == null ? Map.of() : Map.of(shop.getId(), shop));
    }

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

    private ShopProductItem mapProductItem(ShopProduct item) {
        return ShopProductItem.builder().id(item.getId()).shopId(item.getShopId()).name(item.getName()).price(item.getPrice()).stock(item.getStock()).enabled(item.getEnabled()).description(item.getDescription()).image(item.getImage()).build();
    }

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

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private Shop ensureShopExists(Long shopId) {
        return shopRepository.findById(shopId).orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
    }

    private void assertCanManageShop(Long userId, Long shopId) {
        if (isAdmin(userId)) {
            return;
        }
        Shop shop = ensureShopExists(shopId);
        if (shop.getOwnerUserId() == null || !shop.getOwnerUserId().equals(userId)) {
            throw new IllegalArgumentException("仅可管理自己店铺的商品");
        }
    }

    private boolean isAdmin(Long userId) {
        Set<Long> admins = Arrays.stream(adminUserIds.split(",")).map(String::trim).filter(v -> !v.isEmpty()).map(Long::valueOf).collect(Collectors.toSet());
        return admins.contains(userId);
    }

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

    private Long executeDeductScript(ShopProduct product, Long userId) {
        return stringRedisTemplate.execute(seckillDeductScript, List.of(stockKey(product.getId()), buyerSetKey(product.getId())), String.valueOf(userId));
    }

    private void rollbackRedisReservation(Long productId, Long userId) {
        try {
            stringRedisTemplate.execute(seckillRollbackScript, List.of(stockKey(productId), buyerSetKey(productId)), String.valueOf(userId));
        } catch (Exception ex) {
            log.warn("Rollback redis seckill reservation failed, productId={}, userId={}, err={}", productId, userId, ex.getMessage());
        }
    }

    private void setSeckillStock(Long productId, int stock) {
        try {
            stringRedisTemplate.opsForValue().set(stockKey(productId), String.valueOf(Math.max(0, stock)));
        } catch (Exception ex) {
            log.warn("Set redis seckill stock failed, productId={}, err={}", productId, ex.getMessage());
        }
    }

    private void invalidateProductCache(Long productId) {
        cacheOpsService.deleteWithDoubleDelete(productCacheKey(productId));
    }

    private String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    private String buyerSetKey(Long productId) {
        return "seckill:buyers:" + productId;
    }

    private String lockKey(Long productId, Long userId) {
        return "lock:seckill:product:" + productId + ":user:" + userId;
    }

    private String productCacheKey(Long productId) {
        return productDetailKeyPrefix + productId;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            throw new IllegalStateException("缓存序列化失败", ex);
        }
    }

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
