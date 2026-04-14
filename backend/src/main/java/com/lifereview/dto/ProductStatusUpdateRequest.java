package com.lifereview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新商品上下架状态的请求体，通过布尔值控制是否对客展示。
 */
@Data
public class ProductStatusUpdateRequest {
    /** 是否上架启用：{@code true} 上架，{@code false} 下架，必填 */
    @NotNull
    private Boolean enabled;
}
