// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 发布博客/动态请求：标题、内容、图片 */
@Data
public class BlogRequest {
    // 字段说明：博客标题，必填
    @NotBlank
    private String title;

    // 字段说明：博客正文内容，必填
    @NotBlank
    private String content;

    // 字段说明：图片 URL 列表，逗号分隔，可选
    private String images;
}
