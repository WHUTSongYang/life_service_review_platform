// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 点评评论实体，对应 review_comments 表。对某条点评的回复 */
@Data
@TableName("review_comments")
public class ReviewComment {
    @TableId
    private Long id;
    private Long reviewId;          // 所属点评 ID
    private Long userId;            // 评论用户 ID
    private String content;         // 评论内容
    private java.time.LocalDateTime createdAt;
}
