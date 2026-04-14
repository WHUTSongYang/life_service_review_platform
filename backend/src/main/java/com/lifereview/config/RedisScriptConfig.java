package com.lifereview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis Lua 脚本 Bean 配置。
 * <p>
 * 加载秒杀扣减与回滚脚本，供业务通过 {@link org.springframework.data.redis.core.RedisTemplate}
 * 以原子方式执行库存校验与变更。
 * </p>
 */
@Configuration
public class RedisScriptConfig {

    /**
     * 秒杀扣减库存脚本：校验库存、扣减并返回剩余库存等业务逻辑由 Lua 保证原子性。
     *
     * @return 指向 {@code classpath:scripts/seckill_deduct.lua}、返回类型为 {@link Long} 的脚本定义
     */
    @Bean(name = "seckillDeductScript")
    public DefaultRedisScript<Long> seckillDeductScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/seckill_deduct.lua"));
        script.setResultType(Long.class); // 与 Lua 返回值一致
        return script;
    }

    /**
     * 秒杀库存回滚脚本：用于订单取消或超时未支付等场景恢复 Redis 中的可售库存。
     *
     * @return 指向 {@code classpath:scripts/seckill_rollback.lua}、返回类型为 {@link Long} 的脚本定义
     */
    @Bean(name = "seckillRollbackScript")
    public DefaultRedisScript<Long> seckillRollbackScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/seckill_rollback.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
