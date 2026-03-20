// 包声明：DTO 所在包
package com.lifereview.dto;

import com.lifereview.enums.ShopApplyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 店铺入驻申请展示项：申请人、店铺信息、审核状态等 */
@Data
@Builder
public class ShopApplyItem {
    // 字段说明：申请主键 ID
    private Long id;
    // 字段说明：申请人用户 ID
    private Long applicantUserId;
    // 字段说明：申请人昵称
    private String applicantNickname;
    // 字段说明：申请店铺名称
    private String name;
    // 字段说明：申请店铺类型
    private String type;
    // 字段说明：店铺封面图 URL
    private String image;
    // 字段说明：店铺地址
    private String address;
    // 字段说明：经度
    private Double longitude;
    // 字段说明：纬度
    private Double latitude;
    // 字段说明：审核状态
    private ShopApplyStatus status;
    // 字段说明：审核人用户 ID
    private Long reviewerUserId;
    // 字段说明：审核人昵称
    private String reviewerNickname;
    // 字段说明：审核备注
    private String reviewNote;
    // 字段说明：申请创建时间
    private LocalDateTime createdAt;
    // 字段说明：审核完成时间
    private LocalDateTime reviewedAt;
}
