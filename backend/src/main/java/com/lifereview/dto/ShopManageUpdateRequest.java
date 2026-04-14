package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商家在店铺管理后台更新店铺资料时的请求体。
 */
@Data
public class ShopManageUpdateRequest {
    /** 店铺名称，必填 */
    @NotBlank
    private String name;
    /** 店铺类型，必填 */
    @NotBlank
    private String type;
    /** 店铺封面图 URL，必填 */
    @NotBlank
    private String image;
    /** 店铺地址，必填 */
    @NotBlank
    private String address;
    /** 经度，可选 */
    private Double longitude;
    /** 纬度，可选 */
    private Double latitude;
}
