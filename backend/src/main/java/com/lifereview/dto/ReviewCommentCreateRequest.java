// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建点评评论请求：content 必填 */
@Data
public class ReviewCommentCreateRequest {
    // 字段说明：评论内容，必填
    @NotBlank(message = "留言内容不能为空")
    private String content;
}
