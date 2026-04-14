package com.lifereview.dto;

import com.lifereview.enums.ShopApplyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺入驻申请在管理端或列表中的展示项，含申请人、店铺信息与审核进度。
 */
@Data
@Builder
public class ShopApplyItem {
    /** 申请主键 ID */
    private Long id;
    /** 申请人用户 ID */
    private Long applicantUserId;
    /** 申请人昵称 */
    private String applicantNickname;
    /** 申请中的店铺名称 */
    private String name;
    /** 申请中的店铺类型 */
    private String type;
    /** 店铺封面图 URL */
    private String image;
    /** 店铺地址 */
    private String address;
    /** 经度 */
    private Double longitude;
    /** 纬度 */
    private Double latitude;
    /** 当前审核状态 */
    private ShopApplyStatus status;
    /** 审核人用户 ID */
    private Long reviewerUserId;
    /** 审核人昵称 */
    private String reviewerNickname;
    /** 审核备注 */
    private String reviewNote;
    /** 申请提交时间 */
    private LocalDateTime createdAt;
    /** 审核完成时间 */
    private LocalDateTime reviewedAt;
}
