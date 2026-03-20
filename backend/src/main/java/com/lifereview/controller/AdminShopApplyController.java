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
 * 仅超级管理员可操作，提供待审核列表、通过、驳回接口。
 */
@RestController
@RequestMapping("/api/admin/shops/apply")
@RequiredArgsConstructor
public class AdminShopApplyController {

    private final ShopApplyService shopApplyService;

    // 获取待审核的店铺入驻申请列表
    @GetMapping("/pending")
    public ApiResponse<List<ShopApplyItem>> listPending(HttpServletRequest request) {
        return ApiResponse.ok(shopApplyService.listPending(currentOperatorId(request), isSuperAdmin(request)));
    }

    // 通过指定申请
    @PostMapping("/{applyId}/approve")
    public ApiResponse<ShopApplyItem> approve(
            @PathVariable("applyId") Long applyId,
            @RequestBody(required = false) ShopApplyDecisionRequest req,
            HttpServletRequest request
    ) {
        String reviewNote = req == null ? null : req.getReviewNote();
        return ApiResponse.ok(shopApplyService.approve(currentOperatorId(request), isSuperAdmin(request), applyId, reviewNote));
    }

    // 驳回指定申请
    @PostMapping("/{applyId}/reject")
    public ApiResponse<ShopApplyItem> reject(
            @PathVariable("applyId") Long applyId,
            @RequestBody(required = false) ShopApplyDecisionRequest req,
            HttpServletRequest request
    ) {
        String reviewNote = req == null ? null : req.getReviewNote();
        return ApiResponse.ok(shopApplyService.reject(currentOperatorId(request), isSuperAdmin(request), applyId, reviewNote));
    }

    private Long currentOperatorId(HttpServletRequest request) {
        Object principalId = request.getAttribute("principalId");
        if (principalId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) principalId;
    }

    private boolean isSuperAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute("isSuperAdmin"));
    }
}
