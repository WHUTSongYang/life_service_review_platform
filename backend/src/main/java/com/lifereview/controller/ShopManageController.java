package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.ShopManageUpdateRequest;
import com.lifereview.entity.Shop;
import com.lifereview.service.ShopReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店铺管理控制器。
 * 提供可管理店铺列表、更新店铺信息接口，超级管理员可管理全部，普通用户仅可管理自己的店铺。
 */
@RestController
@RequestMapping("/api/shops/manage")
@RequiredArgsConstructor
public class ShopManageController {

    private final ShopReviewService shopReviewService;

    // 获取当前用户可管理的店铺列表
    @GetMapping
    public ApiResponse<List<Shop>> listManageShops(HttpServletRequest request) {
        return ApiResponse.ok(shopReviewService.listManageShops(currentUserId(request), isSuperAdmin(request)));
    }

    // 更新店铺信息
    @PutMapping("/{shopId}")
    public ApiResponse<Shop> updateShop(
            @PathVariable("shopId") Long shopId,
            @Valid @RequestBody ShopManageUpdateRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(shopReviewService.updateManageShop(currentUserId(request), isSuperAdmin(request), shopId, req));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        return userId == null ? null : (Long) userId;
    }

    private boolean isSuperAdmin(HttpServletRequest request) {
        Object value = request.getAttribute("isSuperAdmin");
        return Boolean.TRUE.equals(value);
    }
}
