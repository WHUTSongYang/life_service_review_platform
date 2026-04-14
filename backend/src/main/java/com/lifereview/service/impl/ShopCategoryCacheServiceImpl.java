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
 * <p>使用 Redis 缓存已启用分类名称列表，支持分布式锁防击穿、空值短 TTL 防穿透、双删刷新；启动时预置默认分类并尝试预热缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class ShopCategoryCacheServiceImpl implements ShopCategoryCacheService {

    /** 缓存中多个分类名称的分隔符 */
    private static final String DELIMITER = "||";
    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(ShopCategoryCacheServiceImpl.class);
    /** 内置默认分类（代码、展示名、排序） */
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
    /** 分类表仓储 */
    private final ShopCategoryRepository shopCategoryRepository;
    /** Redis 字符串操作 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 缓存双删等工具 */
    private final CacheOpsService cacheOpsService;
    /** 分类列表在 Redis 中的键名（可配置） */
    @Value("${app.cache.shop-categories-key:shop:categories}")
    private String shopCategoriesKey;
    /** 分类缓存 TTL（秒，可配置；0 表示不过期） */
    @Value("${app.cache.shop-categories-ttl-seconds:3600}")
    private long shopCategoriesTtlSeconds;

    /**
     * 启动时确保默认分类落库，并尝试预热 Redis 分类缓存。
     */
    @PostConstruct
    public void preloadOnStartup() {
        ensureDefaultCategories();
        try {
            refreshCache();
        } catch (Exception e) {
            log.warn("Skip category cache preload because redis is unavailable: {}", e.getMessage());
        }
    }

    /**
     * 获取已启用分类名称列表（读缓存；未命中时加锁回源 DB 并回写）。
     *
     * @return 分类展示名称有序列表
     */
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

    /**
     * 校验并规范化分类名：非空且在白名单内。
     *
     * @param rawType 原始类型字符串
     * @return 规范化后的分类名
     * @throws IllegalArgumentException 为空或不在已启用列表中时抛出
     */
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

    /**
     * 规范化分类别名（如「电影」映射为「电影院」），不做存在性校验。
     *
     * @param rawType 原始类型，可为 null
     * @return 去空白并别名映射后的字符串；输入为 null 时返回 null
     */
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

    /**
     * 双删并重建分类名称缓存。
     */
    @Override
    public void refreshCache() {
        cacheOpsService.deleteWithDoubleDelete(shopCategoriesKey);
        writeCache(loadFromDbWithFallback());
    }

    /**
     * 若数据库中不存在对应 code，则插入内置默认分类。
     */
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

    /**
     * 从数据库加载已启用分类名称（按排序字段）。
     *
     * @return 分类展示名列表
     */
    private List<String> loadFromDb() {
        List<ShopCategory> categories = shopCategoryRepository.findByEnabledTrueOrderBySortNoAscIdAsc();
        List<String> names = new ArrayList<>();
        for (ShopCategory category : categories) {
            names.add(category.getName());
        }
        return names;
    }

    /**
     * 从数据库加载分类名；若为空则回退内置默认名称列表。
     *
     * @return 非空的分类名列表（可能来自内置常量）
     */
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

    /**
     * 将分类名称列表写入 Redis，TTL 带抖动。
     *
     * @param names 分类展示名列表
     */
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

    /**
     * 写入空结果占位缓存，短 TTL 减轻缓存穿透。
     */
    private void writeNullCache() {
        try {
            stringRedisTemplate.opsForValue().set(shopCategoriesKey, CacheOpsService.NULL_MARKER, 120, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Write category null cache failed: {}", e.getMessage());
        }
    }

    /**
     * 解析缓存中的分隔字符串为名称列表。
     *
     * @param raw Redis 中存储的拼接字符串
     * @return 去空白后的名称列表
     */
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

    /**
     * 内置默认分类元数据。
     *
     * @param code   分类编码
     * @param name   展示名称
     * @param sortNo 排序号
     */
    private record DefaultCategory(String code, String name, int sortNo) {
    }
}
