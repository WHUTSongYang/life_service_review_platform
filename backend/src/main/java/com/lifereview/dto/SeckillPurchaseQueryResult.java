package com.lifereview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端轮询秒杀异步处理结果时的响应载体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillPurchaseQueryResult {
    /** 处理中：订单尚未最终确定 */
    public static final String STATUS_PENDING = "PENDING";
    /** 成功：下单完成，可返回订单信息 */
    public static final String STATUS_SUCCESS = "SUCCESS";
    /** 失败：库存不足、重复提交等业务失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 状态码，取值见 {@link #STATUS_PENDING} 等常量 */
    private String status;
    /** 人类可读说明或错误信息 */
    private String message;
    /** 成功时携带的订单摘要；处理中或失败时可能为空 */
    private ProductOrderItem order;
}
