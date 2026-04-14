package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户发布博客或动态的请求体，包含标题、正文与可选配图。
 */
@Data
public class BlogRequest {
    /** 博客或动态标题，必填 */
    @NotBlank
    private String title;

    /** 正文内容，必填 */
    @NotBlank
    private String content;

    /** 配图地址，多个 URL 可用逗号等方式分隔；可选 */
    private String images;
}
