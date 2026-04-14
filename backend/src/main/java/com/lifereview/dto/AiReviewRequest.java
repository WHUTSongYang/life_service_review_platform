package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 辅助生成点评文案的请求体，根据店铺名称与类型生成合适用语。
 */
@Data
public class AiReviewRequest {

    /** 目标店铺名称，例如餐饮、美发等门店名 */
    @NotBlank
    private String shopName;

    /** 店铺业态或类型，例如餐饮、美发、酒店、KTV 等，用于文风与要点 */
    @NotBlank
    private String shopType;
}
