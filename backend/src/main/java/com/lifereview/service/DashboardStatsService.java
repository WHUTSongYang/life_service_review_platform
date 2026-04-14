package com.lifereview.service;

import com.lifereview.dto.BestSellerItem;
import com.lifereview.dto.DailyRevenueItem;

import java.util.List;

/**
 * 业务职责说明：管理端经营数据统计，如日营业额曲线、畅销商品排行等。
 */
public interface DashboardStatsService {

    /**
     * 查询最近若干天内的日营业额序列。
     *
     * @param userId      当前操作者用户 ID
     * @param superAdmin  是否为超级管理员
     * @param shopId      店铺 ID；超级管理员可指定，普通商户通常仅看自己店铺
     * @param days        统计天数
     * @return 按日聚合的营业额项列表
     */
    List<DailyRevenueItem> getDailyRevenue(Long userId, boolean superAdmin, Long shopId, int days);

    /**
     * 查询畅销商品 TopN。
     *
     * @param userId      当前操作者用户 ID
     * @param superAdmin  是否为超级管理员
     * @param shopId      店铺 ID
     * @param days        统计时间窗口（天）
     * @param limit       返回条数上限
     * @return 畅销商品项列表
     */
    List<BestSellerItem> getBestSellers(Long userId, boolean superAdmin, Long shopId, int days, int limit);
}
