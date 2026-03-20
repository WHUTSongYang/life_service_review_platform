package com.lifereview.service;

import com.lifereview.entity.Shop;

import java.util.List;

/**
 * 店铺地理位置服务接口。
 * 基于 Redis GEO 实现附近店铺搜索与位置同步。
 */
public interface ShopGeoService {

    // 同步店铺经纬度到 Redis GEO 索引
    void syncShopLocation(Shop shop);

    // 按经纬度与半径搜索附近店铺，返回店铺 ID 及距离（公里）
    List<ShopDistance> searchNearby(double longitude, double latitude, double radiusKm, long limit);

    // 店铺距离记录：店铺 ID 与距离（公里）
    record ShopDistance(Long shopId, Double distanceKm) {
    }
}
