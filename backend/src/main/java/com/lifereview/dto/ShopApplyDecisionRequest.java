package com.lifereview.dto;

import lombok.Data;

/**
 * 管理员对店铺入驻申请作出通过或拒绝决策时的请求体。
 */
@Data
public class ShopApplyDecisionRequest {
    /** 审核备注，通过或拒绝时均可填写，供申请人查看 */
    private String reviewNote;
}
