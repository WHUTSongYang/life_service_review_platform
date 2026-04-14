package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 店铺实体，映射数据库表 {@code shops}。
 * <p>描述线下/线上店铺的基础信息：名称、类型、地址、经纬度（用于附近检索）、封面图、店主、平均评分与点评数量等。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("shops")
public class Shop {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 店铺名称 */
    private String name;
    /** 店铺类型（如餐饮、美发等业务分类） */
    private String type;
    /** 店主用户 ID */
    private Long ownerUserId;
    /** 店铺封面图 URL */
    private String image;
    /** 是否参与推广展示 */
    private Boolean promotion = false;
    /** 店铺地址描述 */
    private String address;
    /** 经度，用于地理位置与「附近店铺」 */
    private Double longitude;
    /** 纬度，用于地理位置与「附近店铺」 */
    private Double latitude;
    /** 平均评分，默认 0 */
    private Double avgScore = 0.0;
    /** 点评条数，默认 0 */
    private Integer reviewCount = 0;
}
