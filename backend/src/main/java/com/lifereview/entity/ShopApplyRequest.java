package com.lifereview.entity;

import com.lifereview.enums.ShopApplyStatus;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 店铺入驻申请实体，映射数据库表 {@code shop_apply_requests}。
 * <p>用户提交开店申请时填写店铺信息与坐标，管理员审核后更新状态、审核人与备注。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("shop_apply_requests")
public class ShopApplyRequest {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 申请人用户 ID */
    private Long applicantUserId;
    /** 申请中的店铺名称 */
    private String name;
    /** 店铺类型 */
    private String type;
    /** 店铺展示图 URL */
    private String image;
    /** 店铺地址 */
    private String address;
    /** 经度 */
    private Double longitude;
    /** 纬度 */
    private Double latitude;
    /** 审核状态，默认待审核 */
    private ShopApplyStatus status = ShopApplyStatus.PENDING;
    /** 审核管理员用户 ID，未审核时可为空 */
    private Long reviewerUserId;
    /** 审核备注（通过/拒绝说明等） */
    private String reviewNote;
    /** 申请提交时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
    /** 审核完成时间 */
    private java.time.LocalDateTime reviewedAt;
}
