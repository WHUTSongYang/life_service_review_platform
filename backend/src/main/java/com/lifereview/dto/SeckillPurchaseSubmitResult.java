package com.lifereview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交秒杀请求的即时响应：开启 Kafka 时为异步受理（带 requestId）；关闭时为同步返回订单。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillPurchaseSubmitResult {
    /** 是否为异步模式：{@code true} 表示仅已受理，需轮询结果 */
    private boolean async;
    /** 异步模式下的请求 ID，用于查询 {@link SeckillPurchaseQueryResult} */
    private String requestId;
    /** 同步模式下直接返回的订单信息；异步时通常为空 */
    private ProductOrderItem order;
}
