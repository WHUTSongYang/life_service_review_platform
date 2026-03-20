// 包声明：实体类所在包
package com.lifereview.entity;

import com.lifereview.enums.LikeTargetType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 点赞记录实体，对应 like_records 表。targetType 区分点评、博客等点赞目标 */
@Data
@TableName("like_records")
public class LikeRecord {
    @TableId
    private Long id;
    private Long userId;            // 点赞用户 ID
    private LikeTargetType targetType;  // 目标类型：REVIEW、BLOG
    private Long targetId;          // 目标 ID（点评或博客）
    private java.time.LocalDateTime createdAt;
}
