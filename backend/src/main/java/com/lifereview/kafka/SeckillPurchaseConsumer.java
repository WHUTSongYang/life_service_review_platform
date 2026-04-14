package com.lifereview.kafka;

import com.lifereview.dto.SeckillPurchaseMessage;
import com.lifereview.service.ShopProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀异步下单 Kafka 消费者。
 * <p>
 * 订阅秒杀主题消息，委托 {@link ShopProductService} 异步完成建单及 Redis 结果回写，
 * 与接口侧快速响应解耦。仅当 {@code app.kafka.seckill.enabled=true} 时生效。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.seckill", name = "enabled", havingValue = "true")
public class SeckillPurchaseConsumer {

    /** 商品与秒杀业务服务，处理消息中的下单逻辑 */
    private final ShopProductService shopProductService;

    /**
     * 消费秒杀购买消息：校验消息后调用业务处理。
     *
     * @param message 包含 requestId、用户、商品等字段的秒杀消息体；无效消息仅记录日志并跳过
     */
    @KafkaListener(topics = "${app.kafka.seckill.topic:seckill-purchase}")
    public void onMessage(SeckillPurchaseMessage message) {
        if (message == null || message.getRequestId() == null) {
            log.warn("Skip invalid seckill message: {}", message); // 缺少幂等键则丢弃
            return;
        }
        log.debug("Seckill message requestId={} userId={} productId={}", message.getRequestId(), message.getUserId(), message.getProductId());
        shopProductService.processSeckillPurchaseMessage(message); // 异步落单与结果写入
    }
}
