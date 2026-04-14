package com.lifereview.dto;

import lombok.Data;

/**
 * 管理端或统计接口中的畅销商品条目，汇总销量维度数据。
 */
@Data
public class BestSellerItem {
    /** 商品主键 ID */
    private Long productId;
    /** 商品展示名称 */
    private String productName;
    /** 已支付订单数量（或等价销量统计口径） */
    private Long orderCount;
}
