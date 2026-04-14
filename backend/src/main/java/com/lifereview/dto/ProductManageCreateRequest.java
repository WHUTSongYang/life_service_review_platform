package com.lifereview.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品管理场景下新增商品的请求体，包含所属店铺、定价、库存与展示信息等。
 */
@Data
public class ProductManageCreateRequest {
    /** 所属店铺 ID，必填 */
    @NotNull
    private Long shopId;

    /** 商品名称，必填 */
    @NotBlank
    private String name;

    /** 商品单价，必填，不得小于 0.01 */
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    /** 库存数量，必填，不得小于 0 */
    @NotNull
    @Min(0)
    private Integer stock;

    /** 商品描述，可选 */
    private String description;
    /** 商品图片 URL，必填 */
    @NotBlank
    private String image;
    /** 是否上架启用，可选；未传时由业务默认处理 */
    private Boolean enabled;
}
