package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 点评下的评论实体，映射数据库表 {@code review_comments}。
 * <p>表示用户对某条店铺点评的回复（楼中楼评论）。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("review_comments")
public class ReviewComment {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 所属点评 ID */
    private Long reviewId;
    /** 发表评论的用户 ID */
    private Long userId;
    /** 评论正文 */
    private String content;
    /** 评论发表时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
}
