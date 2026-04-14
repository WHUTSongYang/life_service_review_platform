package com.lifereview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户发表店铺点评时的请求体：正文、配图与星级评分。
 */
@Data
public class ReviewRequest {
    /** 点评正文，必填 */
    @NotBlank
    private String content;

    /** 点评配图 URL 列表，逗号分隔，可选 */
    private String images;

    /** 评分，必填，取值 1–5 */
    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;
}
