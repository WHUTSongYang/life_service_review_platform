package com.lifereview.service.impl;

import com.lifereview.service.CacheOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 缓存运维服务实现。
 * <p>
 * 负责店铺/商品/附近列表等相关 Redis 键的删除、TTL 随机抖动，以及「双删」延迟二次删除以降低主从延迟导致的不一致；
 * 维护附近店铺缓存全局版本号，用于批量失效。
 * <p>访问 Redis 的公开方法不向调用方声明受检异常；底层连接失败等可能以运行时异常形式抛出。
 */
@Service
@RequiredArgsConstructor
public class CacheOpsServiceImpl implements CacheOpsService {

    /** 附近店铺缓存逻辑版本号对应的 Redis 键 */
    private static final String NEARBY_VERSION_KEY = "cache:nearby:version";

    /** 字符串 Redis 操作模板 */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 单线程调度器：执行双删策略中的延迟删除任务。
     * <p>守护线程，避免阻止 JVM 退出。
     */
    private final ScheduledExecutorService delayedDeleteExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-delayed-delete");
        t.setDaemon(true);
        return t;
    });

    /** 在基础 TTL 上叠加的随机抖动上限（秒），配置项 {@code app.cache.ttl-jitter-seconds} */
    @Value("${app.cache.ttl-jitter-seconds:120}")
    private long ttlJitterSeconds;

    /** 双删时第二次删除相对第一次的延迟（毫秒），配置项 {@code app.cache.double-delete-delay-ms} */
    @Value("${app.cache.double-delete-delay-ms:400}")
    private long doubleDeleteDelayMs;

    /**
     * 在基础秒数上叠加 {@code [0, ttlJitterSeconds]} 的随机增量，分散缓存同时过期。
     *
     * @param baseSeconds 基础 TTL（秒）
     * @return 抖动后的 TTL；若基础或抖动非正则原样返回基础值
     */
    @Override
    public long withJitter(long baseSeconds) {
        if (baseSeconds <= 0) {
            return baseSeconds;
        }
        if (ttlJitterSeconds <= 0) {
            return baseSeconds;
        }
        // 均匀随机附加 0～ttlJitterSeconds 秒
        long randomPart = ThreadLocalRandom.current().nextLong(ttlJitterSeconds + 1);
        return baseSeconds + randomPart;
    }

    /**
     * 立即删除给定 Redis 键（忽略 null 与空白串）。
     *
     * @param keys 一个或多个 key
     * @throws org.springframework.dao.DataAccessException 与 Redis 通信失败等场景下由 Spring Data 抛出
     */
    @Override
    public void deleteNow(String... keys) {
        Arrays.stream(keys).filter(k -> k != null && !k.isBlank()).forEach(stringRedisTemplate::delete);
    }

    /**
     * 双删：先立即删键，再在延迟后重复删除一次。
     *
     * @param keys 目标 key 列表
     * @throws org.springframework.dao.DataAccessException 与 Redis 通信失败等场景下由 Spring Data 抛出
     */
    @Override
    public void deleteWithDoubleDelete(String... keys) {
        deleteNow(keys);
        // 规避主从复制延迟下旧值被回写
        delayedDeleteExecutor.schedule(() -> deleteNow(keys), Math.max(1, doubleDeleteDelayMs), TimeUnit.MILLISECONDS);
    }

    /**
     * 读取附近店铺缓存全局版本号；键不存在或解析失败时返回 0。
     *
     * @return 当前版本号（非负长整型语义）
     * @throws org.springframework.dao.DataAccessException 与 Redis 通信失败等场景下由 Spring Data 抛出
     */
    @Override
    public long getNearbyVersion() {
        String raw = stringRedisTemplate.opsForValue().get(NEARBY_VERSION_KEY);
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            // 脏数据时视为未初始化
            return 0L;
        }
    }

    /**
     * 将附近店铺缓存版本号自增 1，使依赖该版本的缓存键集体失效。
     *
     * @throws org.springframework.dao.DataAccessException 与 Redis 通信失败等场景下由 Spring Data 抛出
     */
    @Override
    public void bumpNearbyVersion() {
        stringRedisTemplate.opsForValue().increment(NEARBY_VERSION_KEY);
    }
}
