package com.lifereview.entity;

import com.lifereview.enums.ProductOrderStatus;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 店铺商品订单实体，映射数据库表 {@code product_orders}。
 * <p>关联商品、店铺与下单用户，包含数量、应付金额及订单状态（待支付、已支付、已取消等）。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("product_orders")
public class ProductOrder {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 所购商品 ID */
    private Long productId;
    /** 商品所属店铺 ID */
    private Long shopId;
    /** 下单用户 ID */
    private Long userId;
    /** 购买数量，默认 1 */
    private Integer quantity = 1;
    /** 订单金额 */
    private java.math.BigDecimal amount;
    /** 订单状态，默认待支付 */
    private ProductOrderStatus status = ProductOrderStatus.PENDING;
    /** 下单时间 */
    private java.time.LocalDateTime createdAt;
    /** 订单信息最后更新时间 */
    private java.time.LocalDateTime updatedAt;
}
