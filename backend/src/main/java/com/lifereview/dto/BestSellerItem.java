// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Data;

/** 畅销商品统计项：商品 id、名称、支付订单数 */
@Data
public class BestSellerItem {
    // 字段说明：商品主键 ID
    private Long productId;
    // 字段说明：商品名称
    private String productName;
    // 字段说明：已支付订单数量
    private Long orderCount;
}
