package com.lifereview.dto;

import com.lifereview.enums.ProductOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品订单列表或详情中的展示项，聚合订单、商品与店铺等摘要信息。
 */
@Data
@Builder
public class ProductOrderItem {
    /** 订单主键 ID */
    private Long id;
    /** 商品 ID */
    private Long productId;
    /** 商品名称 */
    private String productName;
    /** 店铺 ID */
    private Long shopId;
    /** 店铺名称 */
    private String shopName;
    /** 购买数量 */
    private Integer quantity;
    /** 订单金额 */
    private BigDecimal amount;
    /** 订单状态 */
    private ProductOrderStatus status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
