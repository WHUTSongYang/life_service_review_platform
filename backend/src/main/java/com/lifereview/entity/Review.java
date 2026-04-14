package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 店铺点评实体，映射数据库表 {@code reviews}。
 * <p>用户对店铺的文字评价，可含配图、星级评分及点赞计数。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("reviews")
public class Review {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 被点评的店铺 ID */
    private Long shopId;
    /** 发表点评的用户 ID */
    private Long userId;
    /** 点评正文 */
    private String content;
    /** 配图 URL 列表，多个地址以逗号分隔 */
    private String images;
    /** 评分，一般为 1～5 星 */
    private Integer score;
    /** 点赞数，默认 0 */
    private Integer likeCount = 0;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
    /** 最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updatedAt;
}
