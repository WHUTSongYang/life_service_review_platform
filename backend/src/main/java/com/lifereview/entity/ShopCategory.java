// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 店铺分类实体，对应 shop_categories 表。含 code、name、排序号 */
@Data
@TableName("shop_categories")
public class ShopCategory {
    @TableId
    private Long id;
    private String code;            // 分类编码
    private String name;            // 分类名称
    private Integer sortNo = 0;     // 排序号，越小越靠前
    private Boolean enabled = true; // 是否启用
    private java.time.LocalDateTime createdAt;
}
