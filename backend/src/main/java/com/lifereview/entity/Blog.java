// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 博客/动态实体，对应 blogs 表。用户发布的图文内容 */
@Data
@TableName("blogs")
public class Blog {
    @TableId
    private Long id;
    private Long userId;            // 发布用户 ID
    private String title;           // 标题
    private String content;         // 正文
    private String images;          // 图片 URL 列表，逗号分隔
    private Integer likeCount = 0;  // 点赞数
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
