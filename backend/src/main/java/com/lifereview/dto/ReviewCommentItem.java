// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 点评评论展示项：评论 id、点评 id、用户、内容、时间 */
@Data
@Builder
public class ReviewCommentItem {
    // 字段说明：评论主键 ID
    private Long id;
    // 字段说明：所属点评 ID
    private Long reviewId;
    // 字段说明：评论用户 ID
    private Long userId;
    // 字段说明：评论用户昵称
    private String userNickname;
    // 字段说明：评论内容
    private String content;
    // 字段说明：评论创建时间
    private LocalDateTime createdAt;
}
