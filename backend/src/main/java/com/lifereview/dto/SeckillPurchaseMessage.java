package com.lifereview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 通过 Kafka 投递的秒杀下单消息体，用于异步削峰与订单处理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillPurchaseMessage {
    /** 客户端或服务端生成的请求唯一标识，用于关联异步结果 */
    private String requestId;
    /** 下单用户 ID */
    private Long userId;
    /** 秒杀商品 ID */
    private Long productId;
    /** 请求发起时间（UTC） */
    private Instant requestedAt;
}
