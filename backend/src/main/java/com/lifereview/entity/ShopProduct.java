// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 店铺商品实体，对应 shop_products 表。含名称、价格、库存、描述、图片、启用状态 */
@Data
@TableName("shop_products")
public class ShopProduct {
    @TableId
    private Long id;
    private Long shopId;            // 所属店铺 ID
    private String name;            // 商品名称
    private java.math.BigDecimal price;  // 价格
    private Integer stock = 0;      // 库存
    private String description;     // 描述
    private String image;           // 图片 URL
    private Boolean enabled = true; // 是否上架
    private java.time.LocalDateTime createdAt;
}
