// 包声明：实体类所在包
package com.lifereview.entity;

import com.lifereview.enums.ShopApplyStatus;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 店铺入驻申请实体，对应 shop_apply_requests 表。申请人、店铺信息、审核状态、审核人 */
@Data
@TableName("shop_apply_requests")
public class ShopApplyRequest {
    @TableId
    private Long id;
    private Long applicantUserId;   // 申请人用户 ID
    private String name;            // 申请店铺名称
    private String type;            // 店铺类型
    private String image;           // 店铺图片
    private String address;         // 地址
    private Double longitude;       // 经度
    private Double latitude;        // 纬度
    private ShopApplyStatus status = ShopApplyStatus.PENDING;  // 审核状态
    private Long reviewerUserId;    // 审核人 ID（管理员）
    private String reviewNote;      // 审核备注
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime reviewedAt;
}
