// 包声明：实体类所在包
package com.lifereview.entity;

import com.lifereview.enums.ProductOrderStatus;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 商品订单实体，对应 product_orders 表。含商品、数量、金额、状态（待支付/已支付/已完成/已取消） */
@Data
@TableName("product_orders")
public class ProductOrder {
    @TableId
    private Long id;
    private Long productId;         // 商品 ID
    private Long shopId;            // 店铺 ID
    private Long userId;            // 下单用户 ID
    private Integer quantity = 1;   // 数量
    private java.math.BigDecimal amount;  // 订单金额
    private ProductOrderStatus status = ProductOrderStatus.PENDING;  // 订单状态
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
