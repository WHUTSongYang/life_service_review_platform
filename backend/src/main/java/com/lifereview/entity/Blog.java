package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 博客/动态实体，映射数据库表 {@code blogs}。
 * <p>表示用户发布的图文动态，含标题、正文、配图与点赞计数等。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("blogs")
public class Blog {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 发布者用户 ID */
    private Long userId;
    /** 动态标题 */
    private String title;
    /** 正文内容 */
    private String content;
    /** 配图 URL 列表，多个地址以逗号分隔 */
    private String images;
    /** 点赞数，默认 0 */
    private Integer likeCount = 0;
    /** 创建时间 */
    private java.time.LocalDateTime createdAt;
    /** 最后更新时间 */
    private java.time.LocalDateTime updatedAt;
}
