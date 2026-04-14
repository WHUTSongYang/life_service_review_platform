package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 店铺商品实体，映射数据库表 {@code shop_products}。
 * <p>归属某一店铺，包含售价、库存、图文描述及是否上架；用于商品展示与下单。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("shop_products")
public class ShopProduct {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 所属店铺 ID */
    private Long shopId;
    /** 商品名称 */
    private String name;
    /** 销售价格 */
    private java.math.BigDecimal price;
    /** 可售库存数量，默认 0 */
    private Integer stock = 0;
    /** 商品文字描述 */
    private String description;
    /** 商品主图 URL */
    private String image;
    /** 是否上架（对外可售），默认上架 */
    private Boolean enabled = true;
    /** 商品记录创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
}
