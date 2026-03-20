package com.lifereview.service;

import com.lifereview.dto.ShopApplyCreateRequest;
import com.lifereview.dto.ShopApplyItem;

import java.util.List;

/**
 * 店铺入驻申请服务接口。
 * 负责商户提交申请、查看我的申请、管理员审核（通过/驳回）。
 */
public interface ShopApplyService {

    // 提交店铺入驻申请，返回申请项
    ShopApplyItem submitApply(Long userId, ShopApplyCreateRequest req);

    // 查询当前用户的所有申请列表
    List<ShopApplyItem> listMine(Long userId);

    // 查询待审核申请列表（仅超级管理员可操作）
    List<ShopApplyItem> listPending(Long operatorId, boolean superAdmin);

    // 通过指定申请，reviewNote 为审核备注
    ShopApplyItem approve(Long operatorId, boolean superAdmin, Long applyId, String reviewNote);

    // 驳回指定申请，reviewNote 为驳回原因
    ShopApplyItem reject(Long operatorId, boolean superAdmin, Long applyId, String reviewNote);
}
