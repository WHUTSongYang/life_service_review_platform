// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 关注关系实体，对应 follows 表。userId 关注 followUserId */
@Data
@TableName("follows")
public class Follow {
    @TableId
    private Long id;
    private Long userId;            // 关注者用户 ID
    private Long followUserId;      // 被关注者用户 ID
    private java.time.LocalDateTime createdAt;
}
