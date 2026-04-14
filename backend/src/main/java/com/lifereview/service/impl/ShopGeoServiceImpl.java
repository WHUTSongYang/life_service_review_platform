package com.lifereview.service.impl;

import com.lifereview.entity.Shop;
import com.lifereview.repository.ShopRepository;
import com.lifereview.service.ShopGeoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 店铺地理位置服务实现类。
 * <p>基于 Redis GEO 维护店铺坐标索引与附近搜索；启动时从数据库全量加载；查询失败或结果异常时回退 Haversine 数据库扫描。</p>
 */
@Service
@RequiredArgsConstructor
public class ShopGeoServiceImpl implements ShopGeoService {

    /** Redis 中存储所有店铺坐标的 GEO 键名 */
    private static final String SHOP_GEO_KEY = "geo:shops";
    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(ShopGeoServiceImpl.class);
    /** Redis 模板 */
    private final StringRedisTemplate stringRedisTemplate;
    /** 店铺仓储 */
    private final ShopRepository shopRepository;

    /**
     * 启动时从数据库加载 Redis GEO 索引（Redis 不可用时跳过）。
     */
    @PostConstruct
    public void loadGeoIndex() {
        try {
            reloadGeoIndexFromDb();
        } catch (Exception e) {
            log.warn("Skip geo index preload because redis is unavailable: {}", e.getMessage());
        }
    }

    /**
     * 将单店坐标同步到 Redis GEO；无坐标则从索引中移除。
     *
     * @param shop 店铺实体（需含 id；经纬度可为空表示下线坐标）
     */
    @Override
    public void syncShopLocation(Shop shop) {
        if (shop.getId() == null) {
            return;
        }
        try {
            if (shop.getLongitude() == null || shop.getLatitude() == null) {
                stringRedisTemplate.opsForGeo().remove(SHOP_GEO_KEY, shop.getId().toString());
                return;
            }
            // 将店铺经纬度加入 GEO 索引
            stringRedisTemplate.opsForGeo().add(SHOP_GEO_KEY, new Point(shop.getLongitude(), shop.getLatitude()), shop.getId().toString());
        } catch (Exception e) {
            log.warn("Skip geo sync for shop {} because redis is unavailable: {}", shop.getId(), e.getMessage());
        }
    }

    /**
     * 按经纬度与半径查询附近店铺（千米），返回店铺 ID 与距离；优先 Redis GEO，失败则数据库回退。
     *
     * @param longitude 中心点经度
     * @param latitude  中心点纬度
     * @param radiusKm  搜索半径（千米），须大于 0
     * @param limit     最大返回条数
     * @return 按距离升序的店铺距离列表
     * @throws IllegalArgumentException 半径不大于 0 时抛出
     */
    @Override
    public List<ShopDistance> searchNearby(double longitude, double latitude, double radiusKm, long limit) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("半径必须大于0");
        }
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults;
        try {
            geoResults = doSearch(longitude, latitude, radiusKm, limit);
            if (geoResults == null || geoResults.getContent().isEmpty()) {
                log.info("Redis GEO returned empty result, trigger self-heal reload (key={})", SHOP_GEO_KEY);
                // 从数据库重新加载 GEO 索引
                reloadGeoIndexFromDb();
                geoResults = doSearch(longitude, latitude, radiusKm, limit);
            }
        } catch (Exception e) {
            log.warn("Redis geo query failed, fallback to DB distance calculation: {}", e.getMessage());
            return searchNearbyFromDb(longitude, latitude, radiusKm, limit);
        }
        List<ShopDistance> result = new ArrayList<>();
        if (geoResults == null) {
            return result;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : geoResults) {
            if (item.getContent() == null || item.getContent().getName() == null || item.getDistance() == null) {
                continue;
            }
            result.add(new ShopDistance(Long.valueOf(item.getContent().getName()), item.getDistance().getValue()));
        }
        log.debug("Redis GEO nearby query success, resultCount={}", result.size());
        return result;
    }

    /**
     * 执行 Redis GEO 半径搜索（含距离、升序、limit）。
     *
     * @param longitude 中心经度
     * @param latitude  中心纬度
     * @param radiusKm  半径千米
     * @param limit     条数上限（内部至少为 1）
     * @return Redis 查询结果，可能为 null
     */
    private GeoResults<RedisGeoCommands.GeoLocation<String>> doSearch(double longitude, double latitude, double radiusKm, long limit) {
        return stringRedisTemplate.opsForGeo().search(
                SHOP_GEO_KEY,
                GeoReference.fromCoordinate(longitude, latitude),
                new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().sortAscending().limit(Math.max(1, limit))
        );
    }

    /**
     * 遍历全表店铺并同步到 GEO 索引。
     */
    private void reloadGeoIndexFromDb() {
        int loaded = 0;
        for (Shop shop : shopRepository.findAll()) {
            syncShopLocation(shop);
            loaded++;
        }
        log.info("Redis GEO index reloaded from DB, loadedShops={}", loaded);
    }

    /**
     * 使用 Haversine 在内存中计算距离并筛选半径内店铺（Redis 不可用时的回退路径）。
     *
     * @param longitude 中心经度
     * @param latitude  中心纬度
     * @param radiusKm  半径千米
     * @param limit     最大条数
     * @return 距离升序的店铺距离列表
     */
    private List<ShopDistance> searchNearbyFromDb(double longitude, double latitude, double radiusKm, long limit) {
        double radiusMeters = radiusKm * 1000.0;
        List<ShopDistance> fallbackResult = shopRepository.findAll().stream()
                .filter(shop -> shop.getLongitude() != null && shop.getLatitude() != null)
                .map(shop -> new ShopDistance(shop.getId(), metersToKm(distanceMeters(latitude, longitude, shop.getLatitude(), shop.getLongitude()))))
                .filter(item -> item.distanceKm() * 1000.0 <= radiusMeters)
                .sorted(Comparator.comparing(ShopDistance::distanceKm))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
        log.info("DB fallback nearby query success, resultCount={}", fallbackResult.size());
        return fallbackResult;
    }

    /**
     * Haversine 公式计算两点球面距离（米）。
     *
     * @param lat1 点 1 纬度
     * @param lon1 点 1 经度
     * @param lat2 点 2 纬度
     * @param lon2 点 2 经度
     * @return 距离（米）
     */
    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * 米转千米并保留三位小数。
     *
     * @param meters 距离（米）
     * @return 距离（千米）
     */
    private double metersToKm(double meters) {
        return Math.round((meters / 1000.0) * 1000) / 1000.0;
    }
}
