// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 店铺入驻申请请求：店铺名称、类型、地址、经纬度、图片 */
@Data
public class ShopApplyCreateRequest {
    // 字段说明：店铺名称，必填
    @NotBlank
    private String name;

    // 字段说明：店铺类型，必填
    @NotBlank
    private String type;

    // 字段说明：店铺封面图 URL，可选
    private String image;

    // 字段说明：店铺地址，必填
    @NotBlank
    private String address;

    // 字段说明：经度
    private Double longitude;
    // 字段说明：纬度
    private Double latitude;
}
