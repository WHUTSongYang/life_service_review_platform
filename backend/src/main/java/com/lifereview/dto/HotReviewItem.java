package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 热门或推荐点评的列表展示项，聚合店铺与作者等摘要信息。
 */
@Data
@Builder
public class HotReviewItem {
    /** 点评主键 ID */
    private Long id;
    /** 所属店铺 ID */
    private Long shopId;
    /** 店铺名称 */
    private String shopName;
    /** 店铺类型或业态 */
    private String shopType;
    /** 店铺地址摘要 */
    private String shopAddress;
    /** 发表点评的用户 ID */
    private Long userId;
    /** 用户昵称 */
    private String userNickname;
    /** 点评正文 */
    private String content;
    /** 配图 URL，多个时逗号分隔 */
    private String images;
    /** 评分，通常为 1～5 */
    private Integer score;
    /** 点赞数量 */
    private Integer likeCount;
    /** 点评创建时间 */
    private LocalDateTime createdAt;
}
