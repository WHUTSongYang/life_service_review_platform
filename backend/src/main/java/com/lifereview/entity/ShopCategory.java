package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 店铺分类实体，映射数据库表 {@code shop_categories}。
 * <p>维护店铺业务分类的稳定编码、展示名称、排序及是否启用，供前台筛选与后台配置使用。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("shop_categories")
public class ShopCategory {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 分类唯一编码，程序中引用 */
    private String code;
    /** 分类展示名称 */
    private String name;
    /** 排序号，数值越小越靠前，默认 0 */
    private Integer sortNo = 0;
    /** 是否在前台/逻辑中启用该分类 */
    private Boolean enabled = true;
    /** 记录创建时间 */
    private java.time.LocalDateTime createdAt;
}
