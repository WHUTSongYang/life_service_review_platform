package com.lifereview.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家编辑商品时的请求体，用于更新名称、价格与图片等可编辑字段。
 */
@Data
public class ProductEditRequest {
    /** 商品名称，必填 */
    @NotBlank
    private String name;

    /** 商品单价，必填，不得小于 0.01 */
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    /** 商品图片 URL，可选 */
    private String image;
}
