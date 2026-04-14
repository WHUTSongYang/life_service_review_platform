package com.lifereview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储在 Redis 等介质中的秒杀处理结果快照，通常以 JSON 序列化。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillPurchaseResultPayload {
    /** 用户 ID */
    private long userId;
    /** 商品 ID */
    private long productId;
    /** 处理状态，语义与轮询接口中的 status 一致 */
    private String status;
    /** 说明或错误信息 */
    private String message;
    /** 成功时的订单展示项 */
    private ProductOrderItem order;
}
