package com.lifereview.service;

/**
 * 业务职责说明：与业务相关的 Redis 缓存键管理，包括过期抖动、立即删除、双删策略及附近店铺缓存版本控制。
 */
public interface CacheOpsService {

    /** 缓存空值占位符，用于降低缓存穿透风险。 */
    String NULL_MARKER = "__NULL__";

    /**
     * 在基础 TTL（秒）上叠加随机抖动，使大量键的过期时间分散。
     *
     * @param baseSeconds 基准过期秒数
     * @return 加入抖动后的过期秒数
     */
    long withJitter(long baseSeconds);

    /**
     * 立即删除给定的一个或多个缓存键。
     *
     * @param keys 缓存键，可变参数
     */
    void deleteNow(String... keys);

    /**
     * 双删策略：先删除缓存，再在短延迟后再次删除，以缩小与数据库不一致的可见窗口。
     *
     * @param keys 缓存键，可变参数
     */
    void deleteWithDoubleDelete(String... keys);

    /**
     * 读取「附近店铺」相关缓存的全局版本号（用于客户端或逻辑判断缓存是否失效）。
     *
     * @return 当前版本号数值
     */
    long getNearbyVersion();

    /**
     * 递增附近店铺缓存版本号，使依赖该版本的前端或二级缓存整体失效。
     */
    void bumpNearbyVersion();
}
