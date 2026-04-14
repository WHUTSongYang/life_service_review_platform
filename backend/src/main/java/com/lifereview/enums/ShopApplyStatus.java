package com.lifereview.enums;

/**
 * 店铺入驻申请的审核状态枚举。
 * <p>与 {@link com.lifereview.entity.ShopApplyRequest} 的 {@code status} 字段对应，驱动管理员审核流程展示与业务分支。</p>
 */
public enum ShopApplyStatus {
    /** 待管理员审核 */
    PENDING,
    /** 审核通过 */
    APPROVED,
    /** 审核拒绝 */
    REJECTED
}
