// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 热门点评展示项：用于首页/列表展示 */
@Data
@Builder
public class HotReviewItem {
    // 字段说明：点评主键 ID
    private Long id;
    // 字段说明：店铺 ID
    private Long shopId;
    // 字段说明：店铺名称
    private String shopName;
    // 字段说明：店铺类型
    private String shopType;
    // 字段说明：店铺地址
    private String shopAddress;
    // 字段说明：点评用户 ID
    private Long userId;
    // 字段说明：点评用户昵称
    private String userNickname;
    // 字段说明：点评正文内容
    private String content;
    // 字段说明：点评图片 URL 列表，逗号分隔
    private String images;
    // 字段说明：评分 1-5
    private Integer score;
    // 字段说明：点赞数
    private Integer likeCount;
    // 字段说明：创建时间
    private LocalDateTime createdAt;
}
