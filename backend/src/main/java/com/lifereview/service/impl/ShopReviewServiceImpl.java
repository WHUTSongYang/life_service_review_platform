package com.lifereview.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifereview.dto.HotReviewItem;
import com.lifereview.dto.NearbyShopItem;
import com.lifereview.dto.ReviewDetailItem;
import com.lifereview.dto.ReviewRequest;
import com.lifereview.dto.ShopManageUpdateRequest;
import com.lifereview.entity.LikeRecord;
import com.lifereview.entity.Review;
import com.lifereview.entity.Shop;
import com.lifereview.entity.User;
import com.lifereview.enums.LikeTargetType;
import com.lifereview.repository.LikeRecordRepository;
import com.lifereview.repository.ReviewCommentRepository;
import com.lifereview.repository.ReviewRepository;
import com.lifereview.repository.ShopRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.CacheOpsService;
import com.lifereview.service.ShopCategoryCacheService;
import com.lifereview.service.ShopGeoService;
import com.lifereview.service.ShopReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 店铺与点评服务实现类。
 * <p>负责店铺列表与搜索、详情缓存、创建/更新店铺、点评 CRUD、点赞、热门/最新点评、附近店铺（GEO + 版本化缓存）、管理端店铺维护等。</p>
 */
@Service
@RequiredArgsConstructor
public class ShopReviewServiceImpl implements ShopReviewService {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(ShopReviewServiceImpl.class);
    /** 点评仓储 */
    private final ReviewRepository reviewRepository;
    /** 点评评论仓储 */
    private final ReviewCommentRepository reviewCommentRepository;
    /** 店铺仓储 */
    private final ShopRepository shopRepository;
    /** 点赞记录仓储 */
    private final LikeRecordRepository likeRecordRepository;
    /** 用户仓储 */
    private final UserRepository userRepository;
    /** 店铺地理位置服务 */
    private final ShopGeoService shopGeoService;
    /** 店铺分类缓存与校验 */
    private final ShopCategoryCacheService shopCategoryCacheService;
    /** Redis 字符串模板 */
    private final StringRedisTemplate stringRedisTemplate;
    /** JSON 序列化 */
    private final ObjectMapper objectMapper;
    /** 缓存操作（双删、附近列表全局版本等） */
    private final CacheOpsService cacheOpsService;
    /** 店铺详情缓存键前缀 */
    @Value("${app.cache.shop-detail-key-prefix:cache:shop:detail:}")
    private String shopDetailKeyPrefix;
    /** 店铺详情缓存 TTL（秒） */
    @Value("${app.cache.shop-detail-ttl-seconds:600}")
    private long shopDetailTtlSeconds;
    /** 附近店铺列表缓存键前缀 */
    @Value("${app.cache.nearby-key-prefix:cache:nearby:}")
    private String nearbyKeyPrefix;
    /** 附近列表缓存 TTL（秒） */
    @Value("${app.cache.nearby-ttl-seconds:90}")
    private long nearbyTtlSeconds;
    /** 空值缓存 TTL（秒） */
    @Value("${app.cache.null-ttl-seconds:120}")
    private long nullTtlSeconds;
    /** 缓存防击穿锁 TTL（秒） */
    @Value("${app.cache.lock-ttl-seconds:5}")
    private long cacheLockTtlSeconds;

    /**
     * 查询全部店铺。
     *
     * @return 店铺实体列表
     */
    @Override
    public List<Shop> listShops() {
        return shopRepository.findAll();
    }

    /**
     * 分页搜索店铺（页码、每页条数会做安全裁剪）。
     *
     * @param page    页码（从 0 起）
     * @param size    每页大小（最大 50）
     * @param keyword 关键词，可为空
     * @param type    类型筛选，可为空
     * @return 当前页数据与总条数
     */
    @Override
    public ShopSearchResult searchShops(int page, int size, String keyword, String type) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Page<Shop> shopPage = shopRepository.searchShops(keyword, type, PageRequest.of(safePage, safeSize));
        return new ShopSearchResult(shopPage.getContent(), shopPage.getTotalElements());
    }

    /**
     * 店铺详情（Redis 缓存 + 空值防穿透 + 分布式锁防击穿）。
     *
     * @param shopId 店铺主键
     * @return 店铺实体
     * @throws IllegalArgumentException 店铺不存在时抛出
     * @throws IllegalStateException    缓存序列化异常且无法回源时可能抛出
     */
    @Override
    public Shop getShopDetail(Long shopId) {
        String cacheKey = shopDetailCacheKey(shopId);
        String raw = stringRedisTemplate.opsForValue().get(cacheKey);
        if (raw != null) {
            if (CacheOpsService.NULL_MARKER.equals(raw)) {
                throw new IllegalArgumentException("店铺不存在");
            }
            try {
                return objectMapper.readValue(raw, Shop.class);
            } catch (Exception ex) {
                log.warn("Parse shop detail cache failed, fallback DB. shopId={}, err={}", shopId, ex.getMessage());
            }
        }
        String lockKey = cacheKey + ":lock";
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(Math.max(1, cacheLockTtlSeconds)));
        if (!Boolean.TRUE.equals(locked)) {
            // 未抢到锁：尽量读已写入的缓存，否则直查 DB
            String retryRaw = stringRedisTemplate.opsForValue().get(cacheKey);
            if (retryRaw != null) {
                if (CacheOpsService.NULL_MARKER.equals(retryRaw)) {
                    throw new IllegalArgumentException("店铺不存在");
                }
                try {
                    return objectMapper.readValue(retryRaw, Shop.class);
                } catch (Exception ignore) {
                }
            }
            return ensureShopExists(shopId);
        }
        try {
            Shop shop = shopRepository.findById(shopId).orElse(null);
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, CacheOpsService.NULL_MARKER, Duration.ofSeconds(Math.max(30, nullTtlSeconds)));
                throw new IllegalArgumentException("店铺不存在");
            }
            stringRedisTemplate.opsForValue().set(cacheKey, toJson(shop), Duration.ofSeconds(cacheOpsService.withJitter(shopDetailTtlSeconds)));
            return shop;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 创建店铺：校验分类与图片，同步 GEO，失效相关缓存。
     *
     * @param shop 待持久化店铺（含类型、坐标等）
     * @return 保存后的店铺
     * @throws IllegalArgumentException 分类或图片不合法时抛出
     */
    @Override
    public Shop createShop(Shop shop) {
        String normalizedType = shopCategoryCacheService.validateAndNormalizeCategory(shop.getType());
        shop.setType(normalizedType);
        validateShopImage(shop.getImage());
        shop.setPromotion(Boolean.TRUE.equals(shop.getPromotion()));
        Shop saved = shopRepository.save(shop);
        shopGeoService.syncShopLocation(saved);
        invalidateShopRelatedCache(saved.getId());
        return saved;
    }

    /**
     * 更新店铺信息并同步 GEO 与缓存。
     *
     * @param shopId 店铺主键
     * @param req    新字段
     * @return 更新后的店铺
     * @throws IllegalArgumentException 店铺不存在或参数不合法时抛出
     */
    @Override
    public Shop updateShop(Long shopId, Shop req) {
        Shop shop = ensureShopExists(shopId);
        String normalizedType = shopCategoryCacheService.validateAndNormalizeCategory(req.getType());
        shop.setName(req.getName());
        shop.setType(normalizedType);
        validateShopImage(req.getImage());
        shop.setImage(req.getImage());
        shop.setPromotion(Boolean.TRUE.equals(req.getPromotion()));
        shop.setAddress(req.getAddress());
        shop.setLongitude(req.getLongitude());
        shop.setLatitude(req.getLatitude());
        Shop saved = shopRepository.save(shop);
        shopGeoService.syncShopLocation(saved);
        invalidateShopRelatedCache(saved.getId());
        return saved;
    }

    /**
     * 按店铺查询点评列表（按创建时间倒序）。
     *
     * @param shopId 店铺主键
     * @return 点评实体列表
     */
    @Override
    public List<Review> listReviews(Long shopId) {
        return reviewRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    /**
     * 热门点评分页（关键词与店铺类型可筛选）。
     *
     * @param page     页码
     * @param size     每页条数（最大 50）
     * @param keyword  关键词
     * @param shopType 店铺类型
     * @return 热门点评展示项列表
     */
    @Override
    public List<HotReviewItem> listHotReviews(int page, int size, String keyword, String shopType) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Page<Review> reviewPage = reviewRepository.searchHotReviews(keyword, shopType, PageRequest.of(safePage, safeSize));
        return buildHotReviewItems(reviewPage.getContent());
    }

    /**
     * 最新点评分页。
     *
     * @param page     页码
     * @param size     每页条数（最大 50）
     * @param keyword  关键词
     * @param shopType 店铺类型
     * @return 点评展示项列表
     */
    @Override
    public List<HotReviewItem> listLatestReviews(int page, int size, String keyword, String shopType) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Page<Review> reviewPage = reviewRepository.searchLatestReviews(keyword, shopType, PageRequest.of(safePage, safeSize));
        return buildHotReviewItems(reviewPage.getContent());
    }

    /**
     * 已启用店铺类型（分类）名称列表。
     *
     * @return 分类展示名列表
     */
    @Override
    public List<String> listShopTypes() {
        return shopCategoryCacheService.getEnabledCategoryNames();
    }

    /**
     * 管理端可见店铺：超级管理员返回全部，否则仅返回当前用户为店主的店铺。
     *
     * @param userId      用户 ID
     * @param superAdmin  是否为超级管理员
     * @return 店铺列表
     * @throws IllegalArgumentException 非超管且用户不存在时抛出
     */
    @Override
    public List<Shop> listManageShops(Long userId, boolean superAdmin) {
        if (superAdmin) {
            return shopRepository.findAll().stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).toList();
        }
        ensureUserExists(userId);
        return shopRepository.findByOwnerUserIdOrderByIdDesc(userId);
    }

    /**
     * 店主或超级管理员更新店铺资料（含分类、图片、经纬度校验）。
     *
     * @param userId      操作者用户 ID
     * @param superAdmin  是否为超级管理员
     * @param shopId      店铺主键
     * @param req         更新请求体
     * @return 更新后的店铺
     * @throws IllegalArgumentException 权限不足或参数非法时抛出
     */
    @Override
    @Transactional
    public Shop updateManageShop(Long userId, boolean superAdmin, Long shopId, ShopManageUpdateRequest req) {
        Shop shop = ensureShopExists(shopId);
        if (!superAdmin) {
            ensureUserExists(userId);
            if (shop.getOwnerUserId() == null || !shop.getOwnerUserId().equals(userId)) {
                throw new IllegalArgumentException("仅可管理自己店铺");
            }
        }
        String normalizedType = shopCategoryCacheService.validateAndNormalizeCategory(req.getType());
        validateShopImage(req.getImage());
        validateLocation(req.getLongitude(), req.getLatitude());
        shop.setName(req.getName().trim());
        shop.setType(normalizedType);
        shop.setImage(req.getImage().trim());
        shop.setAddress(req.getAddress().trim());
        shop.setLongitude(req.getLongitude());
        shop.setLatitude(req.getLatitude());
        Shop saved = shopRepository.save(shop);
        shopGeoService.syncShopLocation(saved);
        invalidateShopRelatedCache(saved.getId());
        return saved;
    }

    /**
     * 点评详情（含店铺信息与作者昵称）。
     *
     * @param reviewId 点评主键
     * @return 详情 DTO
     * @throws IllegalArgumentException 点评或店铺不存在时抛出
     */
    @Override
    public ReviewDetailItem getReviewDetail(Long reviewId) {
        Review review = findReview(reviewId);
        Shop shop = ensureShopExists(review.getShopId());
        User user = userRepository.findById(review.getUserId()).orElse(null);
        return ReviewDetailItem.builder()
                .id(review.getId())
                .shopId(review.getShopId())
                .shopName(shop.getName())
                .shopType(shop.getType())
                .shopAddress(shop.getAddress())
                .userId(review.getUserId())
                .userNickname(user == null ? "匿名用户" : user.getNickname())
                .content(review.getContent())
                .images(review.getImages())
                .score(review.getScore())
                .likeCount(review.getLikeCount())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * 附近店铺列表：读缓存或根据 GEO 计算距离后写入缓存（含全局版本号）。
     *
     * @param longitude 中心经度
     * @param latitude  中心纬度
     * @param radiusKm  半径（千米）
     * @param limit     最大条数（最大 100）
     * @return 带距离的附近店铺项
     */
    @Override
    public List<NearbyShopItem> listNearbyShops(double longitude, double latitude, double radiusKm, long limit) {
        long safeLimit = Math.min(Math.max(1, limit), 100);
        String cacheKey = nearbyCacheKey(longitude, latitude, radiusKm, safeLimit);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<NearbyShopItem>>() {
                });
            } catch (Exception ex) {
                log.warn("Parse nearby cache failed, fallback recompute. key={}, err={}", cacheKey, ex.getMessage());
            }
        }
        List<ShopGeoService.ShopDistance> distances = shopGeoService.searchNearby(longitude, latitude, radiusKm, safeLimit);
        if (distances.isEmpty()) {
            stringRedisTemplate.opsForValue().set(cacheKey, "[]", Duration.ofSeconds(cacheOpsService.withJitter(nearbyTtlSeconds)));
            return List.of();
        }
        List<Long> shopIds = distances.stream().map(ShopGeoService.ShopDistance::shopId).toList();
        Map<Long, Shop> shopMap = new LinkedHashMap<>();
        for (Shop shop : shopRepository.findAllById(shopIds)) {
            shopMap.put(shop.getId(), shop);
        }
        List<NearbyShopItem> result = new ArrayList<>();
        for (ShopGeoService.ShopDistance distance : distances) {
            Shop shop = shopMap.get(distance.shopId());
            if (shop == null) {
                continue;
            }
            result.add(NearbyShopItem.builder()
                    .id(shop.getId())
                    .name(shop.getName())
                    .type(shop.getType())
                    .address(shop.getAddress())
                    .avgScore(shop.getAvgScore())
                    .reviewCount(shop.getReviewCount())
                    .promotion(Boolean.TRUE.equals(shop.getPromotion()))
                    .longitude(shop.getLongitude())
                    .latitude(shop.getLatitude())
                    .distanceKm(Math.round(distance.distanceKm() * 1000) / 1000.0)
                    .build());
        }
        result.sort(Comparator.comparing(NearbyShopItem::getDistanceKm));
        stringRedisTemplate.opsForValue().set(cacheKey, toJson(result), Duration.ofSeconds(cacheOpsService.withJitter(nearbyTtlSeconds)));
        return result;
    }

    /**
     * 按评分与点评数取 TOP 店铺。
     *
     * @param limit 条数上限（最大 50）
     * @return 店铺列表
     */
    @Override
    public List<Shop> listTopShopsByAvgScore(int limit) {
        int safe = Math.min(Math.max(1, limit), 50);
        return shopRepository.findTopByAvgScoreDesc(safe);
    }

    /**
     * 用户发表点评并刷新店铺均分与点评数。
     *
     * @param userId 用户 ID
     * @param shopId 店铺 ID
     * @param req    点评内容、图片、评分
     * @return 保存后的点评
     * @throws IllegalArgumentException 用户或店铺不存在时抛出
     */
    @Override
    @Transactional
    public Review createReview(Long userId, Long shopId, ReviewRequest req) {
        ensureUserExists(userId);
        ensureShopExists(shopId);
        Review review = new Review();
        review.setShopId(shopId);
        review.setUserId(userId);
        review.setContent(req.getContent());
        review.setImages(req.getImages());
        review.setScore(req.getScore());
        Review saved = reviewRepository.save(review);
        refreshShopScore(shopId);
        return saved;
    }

    /**
     * 修改当前用户自己的点评。
     *
     * @param userId   用户 ID
     * @param reviewId 点评主键
     * @param req      新内容
     * @return 更新后的点评
     * @throws IllegalArgumentException 点评不存在或非本人时抛出
     */
    @Override
    @Transactional
    public Review updateReview(Long userId, Long reviewId, ReviewRequest req) {
        Review review = findReview(reviewId);
        assertOwner(review.getUserId(), userId);
        review.setContent(req.getContent());
        review.setImages(req.getImages());
        review.setScore(req.getScore());
        Review saved = reviewRepository.save(review);
        refreshShopScore(saved.getShopId());
        return saved;
    }

    /**
     * 删除点评及其下评论，并刷新店铺均分。
     *
     * @param userId   用户 ID
     * @param reviewId 点评主键
     * @throws IllegalArgumentException 点评不存在或非本人时抛出
     */
    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = findReview(reviewId);
        assertOwner(review.getUserId(), userId);
        Long shopId = review.getShopId();
        reviewCommentRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
        refreshShopScore(shopId);
    }

    /**
     * 切换当前用户对某点评的点赞状态，返回最新点赞数。
     *
     * @param userId   用户 ID
     * @param reviewId 点评主键
     * @return 更新后的点赞总数
     * @throws IllegalArgumentException 用户或点评不存在时抛出
     */
    @Override
    public Integer toggleReviewLike(Long userId, Long reviewId) {
        ensureUserExists(userId);
        Review review = findReview(reviewId);
        LikeRecord like = likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, LikeTargetType.REVIEW, reviewId).orElse(null);
        if (like == null) {
            LikeRecord entity = new LikeRecord();
            entity.setUserId(userId);
            entity.setTargetType(LikeTargetType.REVIEW);
            entity.setTargetId(reviewId);
            likeRecordRepository.save(entity);
            review.setLikeCount(review.getLikeCount() + 1);
        } else {
            likeRecordRepository.delete(like);
            review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
        }
        return reviewRepository.save(review).getLikeCount();
    }

    /**
     * 将点评实体列表组装为热门列表项（批量查店铺与用户）。
     *
     * @param reviews 点评实体列表
     * @return 展示项列表
     */
    private List<HotReviewItem> buildHotReviewItems(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        Set<Long> shopIds = reviews.stream().map(Review::getShopId).collect(Collectors.toSet());
        Set<Long> userIds = reviews.stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<Long, Shop> shopMap = shopRepository.findAllById(shopIds).stream().collect(Collectors.toMap(Shop::getId, s -> s));
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        List<HotReviewItem> result = new ArrayList<>();
        for (Review review : reviews) {
            Shop shop = shopMap.get(review.getShopId());
            User user = userMap.get(review.getUserId());
            result.add(HotReviewItem.builder()
                    .id(review.getId())
                    .shopId(review.getShopId())
                    .shopName(shop != null ? shop.getName() : "未知店铺")
                    .shopType(shop != null ? shop.getType() : "")
                    .shopAddress(shop != null ? shop.getAddress() : "")
                    .userId(review.getUserId())
                    .userNickname(user != null ? user.getNickname() : "匿名用户")
                    .content(review.getContent())
                    .images(review.getImages())
                    .score(review.getScore())
                    .likeCount(review.getLikeCount())
                    .createdAt(review.getCreatedAt())
                    .build());
        }
        return result;
    }

    /**
     * 根据点评统计重算店铺均分与点评数，并失效店铺相关缓存。
     *
     * @param shopId 店铺主键
     * @throws IllegalArgumentException 店铺不存在时抛出
     */
    private void refreshShopScore(Long shopId) {
        Shop shop = ensureShopExists(shopId);
        Double avg = reviewRepository.calcAvgScoreByShopId(shopId);
        long count = reviewRepository.countByShopId(shopId);
        shop.setAvgScore(Math.round(avg * 10) / 10.0);
        shop.setReviewCount((int) count);
        shopRepository.save(shop);
        invalidateShopRelatedCache(shopId);
    }

    /**
     * 按 ID 查店铺，不存在抛异常。
     *
     * @param shopId 店铺主键
     * @return 店铺实体
     * @throws IllegalArgumentException 不存在时抛出
     */
    private Shop ensureShopExists(Long shopId) {
        return shopRepository.findById(shopId).orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
    }

    /**
     * 店铺详情 Redis 键。
     *
     * @param shopId 店铺 ID
     * @return 完整键名
     */
    private String shopDetailCacheKey(Long shopId) {
        return shopDetailKeyPrefix + shopId;
    }

    /**
     * 附近列表缓存键：包含全局版本与坐标/半径粒度化，避免缓存错位。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param radiusKm  半径
     * @param limit     条数
     * @return 缓存键
     */
    private String nearbyCacheKey(double longitude, double latitude, double radiusKm, long limit) {
        long version = cacheOpsService.getNearbyVersion();
        double lng = round4(longitude);
        double lat = round4(latitude);
        return nearbyKeyPrefix + "v:" + version + ":" + lng + ":" + lat + ":" + round2(radiusKm) + ":" + limit;
    }

    /**
     * 失效店铺详情缓存并递增附近列表全局版本。
     *
     * @param shopId 店铺 ID
     */
    private void invalidateShopRelatedCache(Long shopId) {
        cacheOpsService.deleteWithDoubleDelete(shopDetailCacheKey(shopId));
        cacheOpsService.bumpNearbyVersion();
    }

    /**
     * 经纬度保留四位小数（用于缓存键归一）。
     *
     * @param value 原始值
     * @return 归一后值
     */
    private double round4(double value) {
        return Math.round(value * 10000D) / 10000D;
    }

    /**
     * 半径保留两位小数。
     *
     * @param value 原始半径
     * @return 归一后值
     */
    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    /**
     * 对象转 JSON 字符串（写入 Redis）。
     *
     * @param value 任意可序列化对象
     * @return JSON
     * @throws IllegalStateException 序列化失败时抛出
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("缓存序列化失败", ex);
        }
    }

    /**
     * 校验店铺主图非空。
     *
     * @param image 图片 URL 或路径
     * @throws IllegalArgumentException 为空时抛出
     */
    private void validateShopImage(String image) {
        if (image == null || image.trim().isEmpty()) {
            throw new IllegalArgumentException("店铺图片不能为空");
        }
    }

    /**
     * 校验店铺经纬度非空且在合法范围。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @throws IllegalArgumentException 不合法时抛出
     */
    private void validateLocation(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("店铺经纬度不能为空");
        }
        if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("经纬度不合法");
        }
    }

    /**
     * 按 ID 查点评，不存在抛异常。
     *
     * @param reviewId 点评主键
     * @return 点评实体
     * @throws IllegalArgumentException 不存在时抛出
     */
    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> new IllegalArgumentException("评价不存在"));
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
     * 校验点评作者与当前用户一致。
     *
     * @param ownerId 点评作者用户 ID
     * @param userId  当前用户 ID
     * @throws IllegalArgumentException 不一致时抛出
     */
    private void assertOwner(Long ownerId, Long userId) {
        if (!ownerId.equals(userId)) {
            throw new IllegalArgumentException("无权限操作他人评价");
        }
    }
}
