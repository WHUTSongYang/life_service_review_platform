// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 商品上下架状态更新请求：enabled */
@Data
public class ProductStatusUpdateRequest {
    // 字段说明：是否上架启用，true 上架 false 下架，必填
    @NotNull
    private Boolean enabled;
}
