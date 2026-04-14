package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户对某条点评发表评论时的请求体。
 */
@Data
public class ReviewCommentCreateRequest {
    /** 评论正文，必填 */
    @NotBlank(message = "留言内容不能为空")
    private String content;
}
