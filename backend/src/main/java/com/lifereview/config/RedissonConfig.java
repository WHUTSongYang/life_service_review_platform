// 包声明：配置类所在包
package com.lifereview.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Redisson 配置：单机 Redis 客户端，用于分布式锁（如秒杀防超卖） */
@Configuration
public class RedissonConfig {

    /** 创建 Redisson 客户端 Bean，从 Spring Redis 配置读取连接信息 */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        // 拼接 Redis 地址：redis://host:port
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
        // 配置单机模式
        SingleServerConfig single = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase());
        // 若有密码则设置
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            single.setPassword(redisProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
