// 包声明：配置类所在包
package com.lifereview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** Redis Lua 脚本配置：秒杀扣减与回滚脚本，供秒杀业务原子操作使用 */
@Configuration
public class RedisScriptConfig {

    /** 秒杀扣减库存脚本 Bean，原子执行：校验库存、扣减、返回剩余库存 */
    @Bean(name = "seckillDeductScript")
    public DefaultRedisScript<Long> seckillDeductScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // 从 classpath:scripts/seckill_deduct.lua 加载脚本
        script.setLocation(new ClassPathResource("scripts/seckill_deduct.lua"));
        // 脚本返回值为 Long 类型（剩余库存）
        script.setResultType(Long.class);
        return script;
    }

    /** 秒杀回滚脚本 Bean，订单取消或超时未支付时恢复 Redis 库存 */
    @Bean(name = "seckillRollbackScript")
    public DefaultRedisScript<Long> seckillRollbackScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // 从 classpath:scripts/seckill_rollback.lua 加载脚本
        script.setLocation(new ClassPathResource("scripts/seckill_rollback.lua"));
        // 脚本返回值为 Long 类型
        script.setResultType(Long.class);
        return script;
    }
}
