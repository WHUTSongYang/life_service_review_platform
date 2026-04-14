package com.lifereview.service;

import com.lifereview.entity.ProductOrder;
import com.lifereview.repository.ProductOrderRepository;
import com.lifereview.repository.ShopProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消任务：扫描超时未支付订单并回补库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelScheduler {

    private final ProductOrderRepository productOrderRepository;
    private final ShopProductRepository shopProductRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheOpsService cacheOpsService;

    /** 商品详情缓存键前缀（与商品服务保持一致） */
    @Value("${app.cache.product-detail-key-prefix:cache:product:detail:}")
    private String productDetailKeyPrefix;

    /** 订单超时分钟数 */
    @Value("${app.order.auto-cancel.timeout-minutes:15}")
    private long timeoutMinutes;

    /**
     * 定时扫描待支付订单，超过超时时间后自动取消并回补库存。
     */
    @Scheduled(
            fixedDelayString = "${app.order.auto-cancel.scan-interval-ms:60000}",
            initialDelayString = "${app.order.auto-cancel.initial-delay-ms:30000}"
    )
    @Transactional
    public void cancelExpiredPendingOrders() {
        if (timeoutMinutes <= 0) {
            return;
        }
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ProductOrder> expiredOrders = productOrderRepository.findPendingCreatedBefore(deadline);
        if (expiredOrders.isEmpty()) {
            return;
        }
        int cancelled = 0;
        for (ProductOrder order : expiredOrders) {
            if (order.getId() == null || order.getProductId() == null) {
                continue;
            }
            // 条件更新，避免与支付并发时误取消已支付订单
            if (productOrderRepository.cancelIfPending(order.getId()) != 1) {
                continue;
            }
            cancelled++;
            restoreStock(order.getProductId());
        }
        if (cancelled > 0) {
            log.info("Auto-cancelled {} expired pending orders, timeoutMinutes={}", cancelled, timeoutMinutes);
        }
    }

    private void restoreStock(Long productId) {
        shopProductRepository.increaseStock(productId, 1);
        cacheOpsService.deleteWithDoubleDelete(productDetailKeyPrefix + productId);
        try {
            stringRedisTemplate.opsForValue().increment("seckill:stock:" + productId);
        } catch (Exception ex) {
            log.warn("Increment redis seckill stock failed in auto-cancel, productId={}, err={}", productId, ex.getMessage());
        }
    }
}
