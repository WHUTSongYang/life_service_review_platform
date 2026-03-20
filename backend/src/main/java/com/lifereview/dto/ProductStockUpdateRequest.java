// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 商品库存更新请求：stock */
@Data
public class ProductStockUpdateRequest {
    // 字段说明：库存数量，必填，不小于 0
    @NotNull
    @Min(0)
    private Integer stock;
}
