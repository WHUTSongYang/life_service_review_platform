// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 商品管理-新增商品请求：名称、价格、库存、描述、图片 */
@Data
public class ProductManageCreateRequest {
    // 字段说明：所属店铺 ID，必填
    @NotNull
    private Long shopId;

    // 字段说明：商品名称，必填
    @NotBlank
    private String name;

    // 字段说明：商品单价，必填，不小于 0.01
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    // 字段说明：库存数量，必填，不小于 0
    @NotNull
    @Min(0)
    private Integer stock;

    // 字段说明：商品描述，可选
    private String description;
    // 字段说明：商品图片 URL，必填
    @NotBlank
    private String image;
    // 字段说明：是否上架启用，可选
    private Boolean enabled;
}
