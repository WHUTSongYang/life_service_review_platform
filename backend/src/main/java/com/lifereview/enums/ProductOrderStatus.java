package com.lifereview.enums;

/**
 * 商品订单生命周期状态枚举。
 * <p>表示从待支付到已支付或取消等阶段，与 {@link com.lifereview.entity.ProductOrder} 的 {@code status} 字段对应。</p>
 */
public enum ProductOrderStatus {
    /** 待支付 */
    PENDING,
    /** 已支付 */
    PAID,
    /** 已取消 */
    CANCELLED
}
