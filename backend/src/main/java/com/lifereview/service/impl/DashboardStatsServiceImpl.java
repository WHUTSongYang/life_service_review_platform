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
 * 管理端看板统计服务实现。
 * <p>
 * 按管理员可管辖店铺范围聚合「每日营业额」与「畅销商品」；超管可查看全部管辖店，普通管理员仅自己的店，
 * 若传入 {@code shopId} 则校验该店是否在权限范围内。
 */
@Service
@RequiredArgsConstructor
public class DashboardStatsServiceImpl implements DashboardStatsService {

    /** 图表横轴日期格式 {@code yyyy-MM-dd} */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DashboardStatsRepository dashboardStatsRepository;
    private final ShopReviewService shopReviewService;

    /**
     * 查询最近若干天内每日营业额，缺失日期补零。
     *
     * @param userId     当前管理员用户 id
     * @param superAdmin 是否超级管理员
     * @param shopId     可选，限定单店；{@code null} 表示权限内全部店
     * @param days       统计天数（会被限制在 1～90）
     * @return 按日期升序的营业额列表；无权限范围内店铺时返回空列表
     * @throws IllegalArgumentException 指定 {@code shopId} 但无权限查看该店
     */
    @Override
    public List<DailyRevenueItem> getDailyRevenue(Long userId, boolean superAdmin, Long shopId, int days) {
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
        // 将 SQL 行转为 date -> revenue 映射
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        for (DailyRevenueQueryRow row : rows) {
            revenueMap.put(row.getDate(), row.getRevenue() == null ? BigDecimal.ZERO : row.getRevenue());
        }
        // 连续日历补齐，无数据日期为 0
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

    /**
     * 查询指定时间窗口内畅销商品 TOP 列表。
     *
     * @param userId     当前管理员用户 id
     * @param superAdmin 是否超级管理员
     * @param shopId     可选单店过滤
     * @param days       统计天数（1～90）
     * @param limit      返回条数上限（1～50）
     * @return 畅销商品 DTO 列表；无店铺范围时为空列表
     * @throws IllegalArgumentException 指定 {@code shopId} 但无权限
     */
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

    /**
     * 解析统计所允许的店铺 id 列表：先取管辖店铺，再按 {@code shopId} 过滤。
     *
     * @param userId     管理员 id
     * @param superAdmin 是否超管
     * @param shopId     可选，非空时必须属于管辖列表
     * @return 参与 SQL 的店铺 id 列表
     * @throws IllegalArgumentException {@code shopId} 不在管辖范围内
     */
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
