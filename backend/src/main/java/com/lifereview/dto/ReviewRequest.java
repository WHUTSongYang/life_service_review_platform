// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 发表点评请求：内容、图片、评分（1-5） */
@Data
public class ReviewRequest {
    // 字段说明：点评正文内容，必填
    @NotBlank
    private String content;

    // 字段说明：点评图片 URL 列表，逗号分隔，可选
    private String images;

    // 字段说明：评分 1-5，必填
    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;
}
