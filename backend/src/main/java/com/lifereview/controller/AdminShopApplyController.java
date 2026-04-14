package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.ShopApplyDecisionRequest;
import com.lifereview.dto.ShopApplyItem;
import com.lifereview.service.ShopApplyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端店铺入驻审核控制器。
 * <p>URL 前缀：{@code /api/admin/shops/apply}。仅超级管理员可操作入驻审核。</p>
 */
@RestController
@RequestMapping("/api/admin/shops/apply")
@RequiredArgsConstructor
public class AdminShopApplyController {

    /** 店铺入驻申请与审核业务服务 */
    private final ShopApplyService shopApplyService;

    /**
     * 获取待审核的店铺入驻申请列表。
     *
     * @param request 当前 HTTP 请求（解析操作者与超管身份）
     * @return 待审核申请列表
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/pending")
    public ApiResponse<List<ShopApplyItem>> listPending(HttpServletRequest request) {
        return ApiResponse.ok(shopApplyService.listPending(currentOperatorId(request), isSuperAdmin(request)));
    }

    /**
     * 通过指定入驻申请。
     *
     * @param applyId 申请主键
     * @param req     可选，审核备注等
     * @param request 当前 HTTP 请求
     * @return 更新后的申请信息
     * @throws IllegalArgumentException 未登录或业务校验失败
     */
    @PostMapping("/{applyId}/approve")
    public ApiResponse<ShopApplyItem> approve(
            @PathVariable("applyId") Long applyId,
            @RequestBody(required = false) ShopApplyDecisionRequest req,
            HttpServletRequest request
    ) {
        String reviewNote = req == null ? null : req.getReviewNote();
        return ApiResponse.ok(shopApplyService.approve(currentOperatorId(request), isSuperAdmin(request), applyId, reviewNote));
    }

    /**
     * 驳回指定入驻申请。
     *
     * @param applyId 申请主键
     * @param req     可选，驳回原因等
     * @param request 当前 HTTP 请求
     * @return 更新后的申请信息
     * @throws IllegalArgumentException 未登录或业务校验失败
     */
    @PostMapping("/{applyId}/reject")
    public ApiResponse<ShopApplyItem> reject(
            @PathVariable("applyId") Long applyId,
            @RequestBody(required = false) ShopApplyDecisionRequest req,
            HttpServletRequest request
    ) {
        String reviewNote = req == null ? null : req.getReviewNote();
        return ApiResponse.ok(shopApplyService.reject(currentOperatorId(request), isSuperAdmin(request), applyId, reviewNote));
    }

    /** 从请求属性解析当前操作者 ID，未登录则抛出异常。 */
    private Long currentOperatorId(HttpServletRequest request) {
        Object principalId = request.getAttribute("principalId");
        if (principalId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) principalId;
    }

    /** 判断当前请求是否来自超级管理员。 */
    private boolean isSuperAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute("isSuperAdmin"));
    }
}
