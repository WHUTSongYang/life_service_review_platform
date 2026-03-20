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
 * 缓存操作服务实现类。
 * 管理店铺/商品详情、附近店铺等 Redis 缓存，支持双删策略防止缓存穿透。
 */
@Service
@RequiredArgsConstructor
public class CacheOpsServiceImpl implements CacheOpsService {

    // 附近店铺缓存版本号 Redis 键
    private static final String NEARBY_VERSION_KEY = "cache:nearby:version";

    private final StringRedisTemplate stringRedisTemplate;
    // 延迟删除线程池，用于双删策略的第二次删除
    private final ScheduledExecutorService delayedDeleteExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-delayed-delete");
        t.setDaemon(true);
        return t;
    });

    // TTL 随机抖动秒数，用于分散缓存过期时间
    @Value("${app.cache.ttl-jitter-seconds:120}")
    private long ttlJitterSeconds;

    // 双删策略中第二次删除的延迟毫秒数
    @Value("${app.cache.double-delete-delay-ms:400}")
    private long doubleDeleteDelayMs;

    @Override
    public long withJitter(long baseSeconds) {
        if (baseSeconds <= 0) {
            return baseSeconds;
        }
        if (ttlJitterSeconds <= 0) {
            return baseSeconds;
        }
        // 在基础秒数上叠加 0~ttlJitterSeconds 的随机值
        long randomPart = ThreadLocalRandom.current().nextLong(ttlJitterSeconds + 1);
        return baseSeconds + randomPart;
    }

    @Override
    public void deleteNow(String... keys) {
        Arrays.stream(keys).filter(k -> k != null && !k.isBlank()).forEach(stringRedisTemplate::delete);
    }

    @Override
    public void deleteWithDoubleDelete(String... keys) {
        deleteNow(keys);
        // 延迟指定毫秒后再次删除，避免主从延迟导致缓存不一致
        delayedDeleteExecutor.schedule(() -> deleteNow(keys), Math.max(1, doubleDeleteDelayMs), TimeUnit.MILLISECONDS);
    }

    @Override
    public long getNearbyVersion() {
        String raw = stringRedisTemplate.opsForValue().get(NEARBY_VERSION_KEY);
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    @Override
    public void bumpNearbyVersion() {
        stringRedisTemplate.opsForValue().increment(NEARBY_VERSION_KEY);
    }
}
