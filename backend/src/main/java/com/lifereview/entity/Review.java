// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 点评实体，对应 reviews 表。用户对店铺的评论，含内容、图片、评分、点赞数 */
@Data
@TableName("reviews")
public class Review {
    @TableId
    private Long id;
    private Long shopId;            // 所属店铺 ID
    private Long userId;            // 点评用户 ID
    private String content;         // 点评正文
    private String images;          // 图片 URL 列表，逗号分隔
    private Integer score;          // 评分 1-5
    private Integer likeCount = 0;  // 点赞数
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
