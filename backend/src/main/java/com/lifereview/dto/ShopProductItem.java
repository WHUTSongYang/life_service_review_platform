// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 店铺商品展示项：名称、价格、库存、图片等 */
@Data
@Builder
public class ShopProductItem {
    // 字段说明：商品主键 ID
    private Long id;
    // 字段说明：所属店铺 ID
    private Long shopId;
    // 字段说明：商品名称
    private String name;
    // 字段说明：商品单价
    private BigDecimal price;
    // 字段说明：库存数量
    private Integer stock;
    // 字段说明：是否上架启用
    private Boolean enabled;
    // 字段说明：商品描述
    private String description;
    // 字段说明：商品图片 URL
    private String image;
}
