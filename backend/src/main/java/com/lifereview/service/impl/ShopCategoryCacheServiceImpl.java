package com.lifereview.service.impl;

import com.lifereview.entity.ShopCategory;
import com.lifereview.repository.ShopCategoryRepository;
import com.lifereview.service.CacheOpsService;
import com.lifereview.service.ShopCategoryCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 店铺分类缓存服务实现类。
 * 使用 Redis 缓存分类列表，支持双删策略，启动时预加载默认分类。
 */
@Service
@RequiredArgsConstructor
public class ShopCategoryCacheServiceImpl implements ShopCategoryCacheService {

    // 缓存中分类名称的分隔符
    private static final String DELIMITER = "||";
    private static final Logger log = LoggerFactory.getLogger(ShopCategoryCacheServiceImpl.class);
    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory("FOOD", "美食", 1),
            new DefaultCategory("HOTEL", "酒店", 2),
            new DefaultCategory("ENTERTAINMENT", "娱乐", 3),
            new DefaultCategory("MASSAGE", "按摩", 4),
            new DefaultCategory("CINEMA", "电影院", 5),
            new DefaultCategory("FOOT_BATH", "足疗", 6),
            new DefaultCategory("BEAUTY", "丽人", 7),
            new DefaultCategory("SPORTS", "运动", 8)
    );
    private final ShopCategoryRepository shopCategoryRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheOpsService cacheOpsService;
    // 分类 Redis 缓存键
    @Value("${app.cache.shop-categories-key:shop:categories}")
    private String shopCategoriesKey;
    // 分类缓存 TTL 秒数
    @Value("${app.cache.shop-categories-ttl-seconds:3600}")
    private long shopCategoriesTtlSeconds;

    @PostConstruct
    public void preloadOnStartup() {
        ensureDefaultCategories();
        try {
            refreshCache();
        } catch (Exception e) {
            log.warn("Skip category cache preload because redis is unavailable: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getEnabledCategoryNames() {
        String lockKey = shopCategoriesKey + ":lock";
        try {
            // 尝试从缓存读取
            String raw = stringRedisTemplate.opsForValue().get(shopCategoriesKey);
            if (raw != null && !raw.isBlank()) {
                if (CacheOpsService.NULL_MARKER.equals(raw)) {
                    return List.of();
                }
                List<String> cached = parse(raw);
                if (!cached.isEmpty()) {
                    return cached;
                }
            }
        } catch (Exception e) {
            log.warn("Read category cache failed, fallback to DB: {}", e.getMessage());
        }
        // 缓存未命中时加锁，防止缓存击穿
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (!Boolean.TRUE.equals(locked)) {
            return loadFromDbWithFallback();
        }
        try {
            List<String> names = loadFromDbWithFallback();
            if (names.isEmpty()) {
                writeNullCache();
            } else {
                writeCache(names);
            }
            return names;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public String validateAndNormalizeCategory(String rawType) {
        String normalized = normalizeCategoryName(rawType);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("商铺类型不能为空");
        }
        if (!getEnabledCategoryNames().contains(normalized)) {
            throw new IllegalArgumentException("不支持的商铺类型: " + normalized);
        }
        return normalized;
    }

    @Override
    public String normalizeCategoryName(String rawType) {
        if (rawType == null) {
            return null;
        }
        String trimmed = rawType.trim();
        if ("电影".equals(trimmed)) {
            return "电影院";
        }
        return trimmed;
    }

    @Override
    public void refreshCache() {
        cacheOpsService.deleteWithDoubleDelete(shopCategoriesKey);
        writeCache(loadFromDbWithFallback());
    }

    private void ensureDefaultCategories() {
        for (DefaultCategory item : DEFAULT_CATEGORIES) {
            if (shopCategoryRepository.existsByCode(item.code())) {
                continue;
            }
            ShopCategory category = new ShopCategory();
            category.setCode(item.code());
            category.setName(item.name());
            category.setSortNo(item.sortNo());
            category.setEnabled(true);
            shopCategoryRepository.save(category);
        }
    }

    private List<String> loadFromDb() {
        List<ShopCategory> categories = shopCategoryRepository.findByEnabledTrueOrderBySortNoAscIdAsc();
        List<String> names = new ArrayList<>();
        for (ShopCategory category : categories) {
            names.add(category.getName());
        }
        return names;
    }

    private List<String> loadFromDbWithFallback() {
        List<String> names = loadFromDb();
        if (!names.isEmpty()) {
            return names;
        }
        List<String> fallback = new ArrayList<>();
        for (DefaultCategory item : DEFAULT_CATEGORIES) {
            fallback.add(item.name());
        }
        return fallback;
    }

    private void writeCache(List<String> names) {
        try {
            String payload = String.join(DELIMITER, names);
            if (shopCategoriesTtlSeconds > 0) {
                stringRedisTemplate.opsForValue().set(shopCategoriesKey, payload, cacheOpsService.withJitter(shopCategoriesTtlSeconds), TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(shopCategoriesKey, payload);
            }
        } catch (Exception e) {
            log.warn("Write category cache failed, continue with DB data: {}", e.getMessage());
        }
    }

    private void writeNullCache() {
        try {
            stringRedisTemplate.opsForValue().set(shopCategoriesKey, CacheOpsService.NULL_MARKER, 120, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write category null cache failed: {}", e.getMessage());
        }
    }

    private List<String> parse(String raw) {
        String[] items = raw.split("\\|\\|");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String value = item.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private record DefaultCategory(String code, String name, int sortNo) {
    }
}
