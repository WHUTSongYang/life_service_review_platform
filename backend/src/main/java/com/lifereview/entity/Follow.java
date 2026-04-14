package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户关注关系实体，映射数据库表 {@code follows}。
 * <p>表示「用户 A 关注用户 B」：{@code userId} 为关注者，{@code followUserId} 为被关注者。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("follows")
public class Follow {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 关注者用户 ID */
    private Long userId;
    /** 被关注者用户 ID */
    private Long followUserId;
    /** 关注关系建立时间 */
    private java.time.LocalDateTime createdAt;
}
