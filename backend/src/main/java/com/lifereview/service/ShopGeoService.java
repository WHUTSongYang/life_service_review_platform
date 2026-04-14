package com.lifereview.service;

import com.lifereview.entity.Shop;

import java.util.List;

/**
 * 业务职责说明：店铺经纬度与 Redis GEO 索引的维护，以及按地理位置检索附近店铺。
 */
public interface ShopGeoService {

    /**
     * 将店铺的经纬度写入或更新到 Redis GEO 空间索引。
     *
     * @param shop 含有效经纬度等字段的店铺实体
     */
    void syncShopLocation(Shop shop);

    /**
     * 以给定坐标为圆心、指定半径检索附近店铺，并返回与圆心的距离。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param radiusKm  搜索半径（公里）
     * @param limit     返回条数上限
     * @return 店铺 ID 与距离（公里）的列表
     */
    List<ShopDistance> searchNearby(double longitude, double latitude, double radiusKm, long limit);

    /**
     * 单次附近检索结果：店铺标识与到查询点的距离。
     *
     * @param shopId      店铺 ID
     * @param distanceKm  与查询点之间的距离（公里）
     */
    record ShopDistance(Long shopId, Double distanceKm) {
    }
}
