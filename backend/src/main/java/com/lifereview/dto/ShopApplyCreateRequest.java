package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商户提交店铺入驻申请时的请求体，包含店铺基本信息与地理位置。
 */
@Data
public class ShopApplyCreateRequest {
    /** 店铺名称，必填 */
    @NotBlank
    private String name;

    /** 店铺类型，必填 */
    @NotBlank
    private String type;

    /** 店铺封面图 URL，可选 */
    private String image;

    /** 店铺地址，必填 */
    @NotBlank
    private String address;

    /** 经度，可选 */
    private Double longitude;
    /** 纬度，可选 */
    private Double latitude;
}
