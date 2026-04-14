package com.lifereview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新商品库存数量的请求体。
 */
@Data
public class ProductStockUpdateRequest {
    /** 库存数量，必填，不得小于 0 */
    @NotNull
    @Min(0)
    private Integer stock;
}
