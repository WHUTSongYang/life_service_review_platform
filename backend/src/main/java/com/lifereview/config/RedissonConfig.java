package com.lifereview.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置。
 * <p>
 * 注册单机模式 {@link RedissonClient}，供分布式锁、原子数据结构等使用（例如秒杀库存与防超卖）。
 * 连接参数与 Spring Data Redis 的 {@link RedisProperties} 保持一致。
 * </p>
 */
@Configuration
public class RedissonConfig {

    /**
     * 创建 Redisson 客户端；容器销毁时调用 {@code shutdown} 关闭连接。
     *
     * @param redisProperties Spring 注入的 Redis 主机、端口、库索引、密码等配置
     * @return 可用于业务代码的 {@link RedissonClient} 实例
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort(); // 单机地址
        SingleServerConfig single = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase());
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            single.setPassword(redisProperties.getPassword()); // 有密码则启用认证
        }
        return Redisson.create(config);
    }
}
