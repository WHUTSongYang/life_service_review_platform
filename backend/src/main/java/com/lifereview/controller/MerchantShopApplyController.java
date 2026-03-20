package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.ShopApplyCreateRequest;
import com.lifereview.dto.ShopApplyItem;
import com.lifereview.service.ShopApplyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家店铺入驻申请控制器。
 * 提供提交申请、查询我的申请接口，需登录。
 */
@RestController
@RequestMapping("/api/merchant/shops/apply")
@RequiredArgsConstructor
public class MerchantShopApplyController {

    private final ShopApplyService shopApplyService;

    // 提交店铺入驻申请
    @PostMapping
    public ApiResponse<ShopApplyItem> submitApply(@Valid @RequestBody ShopApplyCreateRequest req, HttpServletRequest request) {
        return ApiResponse.ok(shopApplyService.submitApply(currentUserId(request), req));
    }

    // 查询当前用户的所有申请列表
    @GetMapping("/mine")
    public ApiResponse<List<ShopApplyItem>> listMine(HttpServletRequest request) {
        return ApiResponse.ok(shopApplyService.listMine(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
