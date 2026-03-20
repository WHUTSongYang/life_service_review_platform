// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 店铺实体，对应 shops 表。含名称、类型、经纬度、评分、地址、店主等 */
@Data
@TableName("shops")
public class Shop {
    @TableId
    private Long id;
    private String name;            // 店铺名称
    private String type;            // 店铺类型（餐饮、美发等）
    private Long ownerUserId;       // 店主用户 ID
    private String image;           // 店铺封面图 URL
    private Boolean promotion = false;  // 是否推广
    private String address;         // 地址
    private Double longitude;       // 经度，用于附近店铺
    private Double latitude;        // 纬度
    private Double avgScore = 0.0;  // 平均评分
    private Integer reviewCount = 0;  // 点评数量
}
