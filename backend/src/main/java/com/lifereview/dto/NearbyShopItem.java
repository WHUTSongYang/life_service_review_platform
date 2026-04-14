package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 附近店铺展示项，包含与用户距离等列表展示所需字段。
 */
@Data
@Builder
public class NearbyShopItem {
    /** 店铺主键 ID */
    private Long id;
    /** 店铺名称 */
    private String name;
    /** 店铺类型 */
    private String type;
    /** 店铺地址 */
    private String address;
    /** 平均评分 */
    private Double avgScore;
    /** 点评数量 */
    private Integer reviewCount;
    /** 是否处于促销中 */
    private Boolean promotion;
    /** 经度 */
    private Double longitude;
    /** 纬度 */
    private Double latitude;
    /** 与用户距离（公里） */
    private Double distanceKm;
}
