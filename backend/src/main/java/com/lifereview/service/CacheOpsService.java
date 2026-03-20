package com.lifereview.service;

/**
 * 缓存操作服务接口。
 * 管理店铺详情、商品详情、附近店铺等 Redis 缓存的读写与失效。
 */
public interface CacheOpsService {

    // 缓存空值占位符，用于防止缓存穿透
    String NULL_MARKER = "__NULL__";

    // 在基础秒数上叠加随机抖动，用于缓存过期时间分散
    long withJitter(long baseSeconds);

    // 立即删除指定缓存键
    void deleteNow(String... keys);

    // 双删策略删除缓存，先删再延迟删，减少缓存不一致
    void deleteWithDoubleDelete(String... keys);

    // 获取附近店铺缓存的版本号
    long getNearbyVersion();

    // 递增附近店铺缓存版本号，触发缓存失效
    void bumpNearbyVersion();
}
