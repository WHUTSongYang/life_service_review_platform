package com.lifereview.service;

import com.lifereview.dto.BestSellerItem;
import com.lifereview.dto.DailyRevenueItem;

import java.util.List;

/**
 * 管理端数据统计服务接口。
 * 提供日营业额、畅销商品等统计报表。
 */
public interface DashboardStatsService {

    // 获取指定天数内的日营业额列表
    List<DailyRevenueItem> getDailyRevenue(Long userId, boolean superAdmin, Long shopId, int days);

    // 获取畅销商品 TopN 列表
    List<BestSellerItem> getBestSellers(Long userId, boolean superAdmin, Long shopId, int days, int limit);
}
