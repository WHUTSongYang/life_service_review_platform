// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Data;

/** 店铺入驻审核决策请求：通过/拒绝，可带审核备注 */
@Data
public class ShopApplyDecisionRequest {
    // 字段说明：审核备注，通过或拒绝时的说明
    private String reviewNote;
}
