package com.lifereview.service.impl;

import com.lifereview.dto.BestSellerItem;
import com.lifereview.dto.DailyRevenueItem;
import com.lifereview.dto.DailyRevenueQueryRow;
import com.lifereview.entity.Shop;
import com.lifereview.repository.DashboardStatsRepository;
import com.lifereview.service.DashboardStatsService;
import com.lifereview.service.ShopReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计服务实现类。
 * 提供日营业额、畅销商品等统计，按管理员权限过滤可查看的店铺范围。
 */
@Service
@RequiredArgsConstructor
public class DashboardStatsServiceImpl implements DashboardStatsService {

    // 日期格式化器
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DashboardStatsRepository dashboardStatsRepository;
    private final ShopReviewService shopReviewService;

    @Override
    public List<DailyRevenueItem> getDailyRevenue(Long userId, boolean superAdmin, Long shopId, int days) {
        // 限制天数在 1~90 之间
        int safeDays = Math.min(Math.max(1, days), 90);
        List<Long> scopeShopIds = resolveScopeShopIds(userId, superAdmin, shopId);
        if (scopeShopIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(safeDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();
        List<DailyRevenueQueryRow> rows = dashboardStatsRepository.queryDailyRevenue(scopeShopIds, startTime, endTime);
        // 按日期聚合营业额
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        for (DailyRevenueQueryRow row : rows) {
            revenueMap.put(row.getDate(), row.getRevenue() == null ? BigDecimal.ZERO : row.getRevenue());
        }
        // 补齐每一天的营业额（无数据则为 0）
        List<DailyRevenueItem> result = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateText = date.format(DATE_FMT);
            result.add(DailyRevenueItem.builder()
                    .date(dateText)
                    .revenue(revenueMap.getOrDefault(dateText, BigDecimal.ZERO))
                    .build());
        }
        return result;
    }

    @Override
    public List<BestSellerItem> getBestSellers(Long userId, boolean superAdmin, Long shopId, int days, int limit) {
        int safeDays = Math.min(Math.max(1, days), 90);
        int safeLimit = Math.min(Math.max(1, limit), 50);
        List<Long> scopeShopIds = resolveScopeShopIds(userId, superAdmin, shopId);
        if (scopeShopIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(safeDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();
        return dashboardStatsRepository.queryBestSellers(scopeShopIds, startTime, endTime, safeLimit);
    }

    // 解析统计范围内的店铺 ID 列表，超级管理员可见全部，普通管理员仅可管理自己的店铺
    private List<Long> resolveScopeShopIds(Long userId, boolean superAdmin, Long shopId) {
        List<Shop> scopeShops = shopReviewService.listManageShops(userId, superAdmin);
        List<Long> allShopIds = scopeShops.stream().map(Shop::getId).toList();
        if (shopId == null) {
            return allShopIds;
        }
        if (!allShopIds.contains(shopId)) {
            throw new IllegalArgumentException("无权限查看该店铺数据");
        }
        return List.of(shopId);
    }
}
