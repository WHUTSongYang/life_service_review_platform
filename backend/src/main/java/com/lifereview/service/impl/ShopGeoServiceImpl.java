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
 * 基于 Redis GEO 实现附近店铺搜索，启动时从数据库加载 GEO 索引。
 */
@Service
@RequiredArgsConstructor
public class ShopGeoServiceImpl implements ShopGeoService {

    // Redis GEO 键
    private static final String SHOP_GEO_KEY = "geo:shops";
    private static final Logger log = LoggerFactory.getLogger(ShopGeoServiceImpl.class);
    private final StringRedisTemplate stringRedisTemplate;
    private final ShopRepository shopRepository;

    @PostConstruct
    public void loadGeoIndex() {
        try {
            reloadGeoIndexFromDb();
        } catch (Exception e) {
            log.warn("Skip geo index preload because redis is unavailable: {}", e.getMessage());
        }
    }

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

    private GeoResults<RedisGeoCommands.GeoLocation<String>> doSearch(double longitude, double latitude, double radiusKm, long limit) {
        return stringRedisTemplate.opsForGeo().search(
                SHOP_GEO_KEY,
                GeoReference.fromCoordinate(longitude, latitude),
                new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().sortAscending().limit(Math.max(1, limit))
        );
    }

    private void reloadGeoIndexFromDb() {
        int loaded = 0;
        for (Shop shop : shopRepository.findAll()) {
            syncShopLocation(shop);
            loaded++;
        }
        log.info("Redis GEO index reloaded from DB, loadedShops={}", loaded);
    }

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

    // 使用 Haversine 公式计算两点间距离（米）
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

    private double metersToKm(double meters) {
        return Math.round((meters / 1000.0) * 1000) / 1000.0;
    }
}
