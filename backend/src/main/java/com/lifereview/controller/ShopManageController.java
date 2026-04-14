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
 * 店铺信息管理控制器（商家/超管）。
 * <p>URL 前缀：{@code /api/shops/manage}。超级管理员可管理全部店铺；普通用户仅能管理自己有权限的店铺。</p>
 */
@RestController
@RequestMapping("/api/shops/manage")
@RequiredArgsConstructor
public class ShopManageController {

    /** 店铺与点评领域服务（含可管理店铺列表与更新） */
    private final ShopReviewService shopReviewService;

    /**
     * 获取当前用户可管理的店铺列表。
     *
     * @param request 当前 HTTP 请求（含 currentUserId、isSuperAdmin）
     * @return 店铺实体列表
     */
    @GetMapping
    public ApiResponse<List<Shop>> listManageShops(HttpServletRequest request) {
        return ApiResponse.ok(shopReviewService.listManageShops(currentUserId(request), isSuperAdmin(request)));
    }

    /**
     * 在权限范围内更新店铺资料。
     *
     * @param shopId  店铺主键
     * @param req     可更新字段
     * @param request 当前 HTTP 请求
     * @return 更新后的店铺实体
     * @throws IllegalArgumentException 无权限或业务校验失败
     */
    @PutMapping("/{shopId}")
    public ApiResponse<Shop> updateShop(
            @PathVariable("shopId") Long shopId,
            @Valid @RequestBody ShopManageUpdateRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(shopReviewService.updateManageShop(currentUserId(request), isSuperAdmin(request), shopId, req));
    }

    /** 从请求属性解析当前用户 ID，未设置则返回 null。 */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        return userId == null ? null : (Long) userId;
    }

    /** 判断当前请求是否来自超级管理员。 */
    private boolean isSuperAdmin(HttpServletRequest request) {
        Object value = request.getAttribute("isSuperAdmin");
        return Boolean.TRUE.equals(value);
    }
}
