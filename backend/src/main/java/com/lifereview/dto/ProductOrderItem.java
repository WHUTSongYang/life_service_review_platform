// 包声明：DTO 所在包
package com.lifereview.dto;

import com.lifereview.enums.ProductOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品订单展示项：订单详情，含商品、店铺、状态等 */
@Data
@Builder
public class ProductOrderItem {
    // 字段说明：订单主键 ID
    private Long id;
    // 字段说明：商品 ID
    private Long productId;
    // 字段说明：商品名称
    private String productName;
    // 字段说明：店铺 ID
    private Long shopId;
    // 字段说明：店铺名称
    private String shopName;
    // 字段说明：购买数量
    private Integer quantity;
    // 字段说明：订单金额
    private BigDecimal amount;
    // 字段说明：订单状态
    private ProductOrderStatus status;
    // 字段说明：创建时间
    private LocalDateTime createdAt;
    // 字段说明：更新时间
    private LocalDateTime updatedAt;
}
