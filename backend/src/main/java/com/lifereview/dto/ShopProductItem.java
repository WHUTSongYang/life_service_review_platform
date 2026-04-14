package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 店铺下某件商品的列表或详情展示项。
 */
@Data
@Builder
public class ShopProductItem {
    /** 商品主键 ID */
    private Long id;
    /** 所属店铺 ID */
    private Long shopId;
    /** 商品名称 */
    private String name;
    /** 商品单价 */
    private BigDecimal price;
    /** 库存数量 */
    private Integer stock;
    /** 是否上架启用 */
    private Boolean enabled;
    /** 商品描述 */
    private String description;
    /** 商品图片 URL */
    private String image;
}
