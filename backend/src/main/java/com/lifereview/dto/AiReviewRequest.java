// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** AI 帮写点评请求：shopName、shopType 必填，AI 据此生成评价文案 */
@Data
public class AiReviewRequest {

    /** 店铺名称，如"海底捞火锅"、"优剪理发" */
    @NotBlank
    private String shopName;

    /** 店铺类型，如"餐饮"、"美发"、"酒店"、"KTV" */
    @NotBlank
    private String shopType;
}
