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
 * 负责店铺列表、搜索、详情、点评 CRUD、热门点评、附近店铺等。
 */
@Service
@RequiredArgsConstructor
public class ShopReviewServiceImpl implements ShopReviewService {

    private static final Logger log = LoggerFactory.getLogger(ShopReviewServiceImpl.class);
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ShopRepository shopRepository;
    private final LikeRecordRepository likeRecordRepository;
    private final UserRepository userRepository;
    private final ShopGeoService shopGeoService;
    private final ShopCategoryCacheService shopCategoryCacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheOpsService cacheOpsService;
    @Value("${app.cache.shop-detail-key-prefix:cache:shop:detail:}")
    private String shopDetailKeyPrefix;
    @Value("${app.cache.shop-detail-ttl-seconds:600}")
    private long shopDetailTtlSeconds;
    @Value("${app.cache.nearby-key-prefix:cache:nearby:}")
    private String nearbyKeyPrefix;
    @Value("${app.cache.nearby-ttl-seconds:90}")
    private long nearbyTtlSeconds;
    @Value("${app.cache.null-ttl-seconds:120}")
    private long nullTtlSeconds;
    @Value("${app.cache.lock-ttl-seconds:5}")
    private long cacheLockTtlSeconds;

    @Override
    public List<Shop> listShops() {
        return shopRepository.findAll();
    }

    @Override
    public ShopSearchResult searchShops(int page, int size, String keyword, String type) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Page<Shop> shopPage = shopRepository.searchShops(keyword, type, PageRequest.of(safePage, safeSize));
        return new ShopSearchResult(shopPage.getContent(), shopPage.getTotalElements());
    }

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

    @Override
    public List<Review> listReviews(Long shopId) {
        return reviewRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    @Override
    public List<HotReviewItem> listHotReviews(int page, int size, String keyword, String shopType) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Page<Review> reviewPage = reviewRepository.searchHotReviews(keyword, shopType, PageRequest.of(safePage, safeSize));
        return buildHotReviewItems(reviewPage.getContent());
    }

    @Override
    public List<HotReviewItem> listLatestReviews(int page, int size, String keyword, String shopType) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Page<Review> reviewPage = reviewRepository.searchLatestReviews(keyword, shopType, PageRequest.of(safePage, safeSize));
        return buildHotReviewItems(reviewPage.getContent());
    }

    @Override
    public List<String> listShopTypes() {
        return shopCategoryCacheService.getEnabledCategoryNames();
    }

    @Override
    public List<Shop> listManageShops(Long userId, boolean superAdmin) {
        if (superAdmin) {
            return shopRepository.findAll().stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).toList();
        }
        ensureUserExists(userId);
        return shopRepository.findByOwnerUserIdOrderByIdDesc(userId);
    }

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

    private void refreshShopScore(Long shopId) {
        Shop shop = ensureShopExists(shopId);
        Double avg = reviewRepository.calcAvgScoreByShopId(shopId);
        long count = reviewRepository.countByShopId(shopId);
        shop.setAvgScore(Math.round(avg * 10) / 10.0);
        shop.setReviewCount((int) count);
        shopRepository.save(shop);
        invalidateShopRelatedCache(shopId);
    }

    private Shop ensureShopExists(Long shopId) {
        return shopRepository.findById(shopId).orElseThrow(() -> new IllegalArgumentException("店铺不存在"));
    }

    private String shopDetailCacheKey(Long shopId) {
        return shopDetailKeyPrefix + shopId;
    }

    private String nearbyCacheKey(double longitude, double latitude, double radiusKm, long limit) {
        long version = cacheOpsService.getNearbyVersion();
        double lng = round4(longitude);
        double lat = round4(latitude);
        return nearbyKeyPrefix + "v:" + version + ":" + lng + ":" + lat + ":" + round2(radiusKm) + ":" + limit;
    }

    private void invalidateShopRelatedCache(Long shopId) {
        cacheOpsService.deleteWithDoubleDelete(shopDetailCacheKey(shopId));
        cacheOpsService.bumpNearbyVersion();
    }

    private double round4(double value) {
        return Math.round(value * 10000D) / 10000D;
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("缓存序列化失败", ex);
        }
    }

    private void validateShopImage(String image) {
        if (image == null || image.trim().isEmpty()) {
            throw new IllegalArgumentException("店铺图片不能为空");
        }
    }

    private void validateLocation(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("店铺经纬度不能为空");
        }
        if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("经纬度不合法");
        }
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> new IllegalArgumentException("评价不存在"));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private void assertOwner(Long ownerId, Long userId) {
        if (!ownerId.equals(userId)) {
            throw new IllegalArgumentException("无权限操作他人评价");
        }
    }
}
