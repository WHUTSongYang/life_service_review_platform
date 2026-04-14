package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点评下某条评论的展示项，用于列表或详情中的嵌套展示。
 */
@Data
@Builder
public class ReviewCommentItem {
    /** 评论主键 ID */
    private Long id;
    /** 所属点评 ID */
    private Long reviewId;
    /** 评论发表用户 ID */
    private Long userId;
    /** 评论发表用户昵称 */
    private String userNickname;
    /** 评论正文内容 */
    private String content;
    /** 评论创建时间 */
    private LocalDateTime createdAt;
}
