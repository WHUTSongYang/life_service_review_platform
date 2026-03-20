// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/** 附近店铺展示项：含距离（米） */
@Data
@Builder
public class NearbyShopItem {
    // 字段说明：店铺主键 ID
    private Long id;
    // 字段说明：店铺名称
    private String name;
    // 字段说明：店铺类型
    private String type;
    // 字段说明：店铺地址
    private String address;
    // 字段说明：平均评分
    private Double avgScore;
    // 字段说明：点评数量
    private Integer reviewCount;
    // 字段说明：是否促销中
    private Boolean promotion;
    // 字段说明：经度
    private Double longitude;
    // 字段说明：纬度
    private Double latitude;
    // 字段说明：与用户距离（公里）
    private Double distanceKm;
}
