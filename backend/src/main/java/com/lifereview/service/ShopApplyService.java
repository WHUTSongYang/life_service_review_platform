package com.lifereview.service;

import com.lifereview.dto.ShopApplyCreateRequest;
import com.lifereview.dto.ShopApplyItem;

import java.util.List;

/**
 * 业务职责说明：商户店铺入驻申请的提交、查询及管理员审核（通过/驳回）。
 */
public interface ShopApplyService {

    /**
     * 提交一条新的店铺入驻申请。
     *
     * @param userId 当前商户用户 ID
     * @param req    申请内容请求体
     * @return 申请项 DTO
     */
    ShopApplyItem submitApply(Long userId, ShopApplyCreateRequest req);

    /**
     * 查询当前登录用户提交的全部申请记录。
     *
     * @param userId 用户 ID
     * @return 申请项列表
     */
    List<ShopApplyItem> listMine(Long userId);

    /**
     * 查询待审核的入驻申请列表（通常仅超级管理员可调用）。
     *
     * @param operatorId  当前操作者用户 ID
     * @param superAdmin  是否为超级管理员
     * @return 待审核申请项列表
     */
    List<ShopApplyItem> listPending(Long operatorId, boolean superAdmin);

    /**
     * 审核通过指定申请。
     *
     * @param operatorId  当前操作者用户 ID
     * @param superAdmin  是否为超级管理员
     * @param applyId     申请 ID
     * @param reviewNote  审核备注
     * @return 更新后的申请项 DTO
     */
    ShopApplyItem approve(Long operatorId, boolean superAdmin, Long applyId, String reviewNote);

    /**
     * 驳回指定申请。
     *
     * @param operatorId  当前操作者用户 ID
     * @param superAdmin  是否为超级管理员
     * @param applyId     申请 ID
     * @param reviewNote  驳回原因说明
     * @return 更新后的申请项 DTO
     */
    ShopApplyItem reject(Long operatorId, boolean superAdmin, Long applyId, String reviewNote);
}
