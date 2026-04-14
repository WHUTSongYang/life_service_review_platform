package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单条点评的详情展示项，包含店铺摘要、作者、正文、评分与互动数据等。
 */
@Data
@Builder
public class ReviewDetailItem {
    /** 点评主键 ID */
    private Long id;
    /** 店铺 ID */
    private Long shopId;
    /** 店铺名称 */
    private String shopName;
    /** 店铺类型 */
    private String shopType;
    /** 店铺地址 */
    private String shopAddress;
    /** 点评作者用户 ID */
    private Long userId;
    /** 点评作者昵称 */
    private String userNickname;
    /** 点评正文内容 */
    private String content;
    /** 点评配图 URL 列表，多为逗号分隔的字符串 */
    private String images;
    /** 评分，取值范围 1–5 */
    private Integer score;
    /** 点赞数 */
    private Integer likeCount;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
