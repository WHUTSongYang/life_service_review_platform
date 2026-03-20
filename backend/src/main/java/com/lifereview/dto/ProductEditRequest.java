// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 商品编辑请求：名称、价格、库存、描述、图片、启用状态 */
@Data
public class ProductEditRequest {
    // 字段说明：商品名称，必填
    @NotBlank
    private String name;

    // 字段说明：商品单价，必填，不小于 0.01
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    // 字段说明：商品图片 URL，可选
    private String image;
}
