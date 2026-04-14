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
 * <p>URL 前缀：{@code /api/admin/dashboard}。需管理员登录，由拦截器注入 {@code currentUserId}、{@code isSuperAdmin}。</p>
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    /** 仪表盘统计（日营业额、畅销商品等）业务服务 */
    private final DashboardStatsService dashboardStatsService;

    /**
     * 获取指定天数内的日营业额列表。
     *
     * @param days   统计天数，默认 7
     * @param shopId 可选，限定某店铺；超管可传，普通管理员通常为自己的店铺上下文
     * @param request 当前 HTTP 请求（解析登录态与超管标记）
     * @return 日营业额条目列表
     */
    @GetMapping("/daily-revenue")
    public ApiResponse<List<DailyRevenueItem>> dailyRevenue(
            @RequestParam(name = "days", defaultValue = "7") Integer days,
            @RequestParam(name = "shopId", required = false) Long shopId,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(dashboardStatsService.getDailyRevenue(currentUserId(request), isSuperAdmin(request), shopId, days));
    }

    /**
     * 获取畅销商品 TopN 列表。
     *
     * @param days   统计天数，默认 7
     * @param shopId 可选，限定某店铺
     * @param limit  返回条数上限，默认 10
     * @param request 当前 HTTP 请求
     * @return 畅销商品条目列表
     */
    @GetMapping("/best-sellers")
    public ApiResponse<List<BestSellerItem>> bestSellers(
            @RequestParam(name = "days", defaultValue = "7") Integer days,
            @RequestParam(name = "shopId", required = false) Long shopId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(dashboardStatsService.getBestSellers(currentUserId(request), isSuperAdmin(request), shopId, days, limit));
    }

    /** 从请求属性解析当前用户 ID，未设置则返回 null。 */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        return userId == null ? null : (Long) userId;
    }

    /** 判断当前请求是否来自超级管理员。 */
    private boolean isSuperAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute("isSuperAdmin"));
    }
}
