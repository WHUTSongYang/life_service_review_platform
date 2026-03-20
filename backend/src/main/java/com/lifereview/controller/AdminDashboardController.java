package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.BestSellerItem;
import com.lifereview.dto.DailyRevenueItem;
import com.lifereview.service.DashboardStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端数据可视化控制器。
 * 提供日营业额、畅销商品等统计接口，需管理员登录。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardStatsService dashboardStatsService;

    // 获取指定天数内的日营业额列表
    @GetMapping("/daily-revenue")
    public ApiResponse<List<DailyRevenueItem>> dailyRevenue(
            @RequestParam(name = "days", defaultValue = "7") Integer days,
            @RequestParam(name = "shopId", required = false) Long shopId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(dashboardStatsService.getDailyRevenue(currentUserId(request), isSuperAdmin(request), shopId, days));
    }

    // 获取畅销商品 TopN 列表
    @GetMapping("/best-sellers")
    public ApiResponse<List<BestSellerItem>> bestSellers(
            @RequestParam(name = "days", defaultValue = "7") Integer days,
            @RequestParam(name = "shopId", required = false) Long shopId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(dashboardStatsService.getBestSellers(currentUserId(request), isSuperAdmin(request), shopId, days, limit));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        return userId == null ? null : (Long) userId;
    }

    private boolean isSuperAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute("isSuperAdmin"));
    }
}
