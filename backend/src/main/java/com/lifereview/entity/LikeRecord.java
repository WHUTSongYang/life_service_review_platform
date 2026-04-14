package com.lifereview.entity;

import com.lifereview.enums.LikeTargetType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 点赞记录实体，映射数据库表 {@code like_records}。
 * <p>记录用户对某类内容（如点评、博客）的点赞；{@link LikeTargetType} 区分目标类型，{@code targetId} 为对应业务主键。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("like_records")
public class LikeRecord {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 点赞用户 ID */
    private Long userId;
    /** 点赞目标类型（如博客、点评） */
    private LikeTargetType targetType;
    /** 被点赞目标的业务主键 ID */
    private Long targetId;
    /** 点赞时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
}
