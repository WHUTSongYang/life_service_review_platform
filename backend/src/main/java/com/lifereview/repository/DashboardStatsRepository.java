package com.lifereview.repository;

import com.lifereview.dto.BestSellerItem;
import com.lifereview.dto.DailyRevenueQueryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端看板统计数据访问：不对应单张业务宽表，通过 SQL 聚合订单等数据，提供按店铺范围与时间维度的日营业额、畅销商品排行查询。
 */
@Mapper
public interface DashboardStatsRepository {
    /**
     * 在指定店铺集合与时间范围内，按日聚合营业额等指标（行结构见 {@link DailyRevenueQueryRow}）。
     *
     * @param shopIds   店铺 id 列表，通常为主账号可管理的店铺范围
     * @param startTime 统计起始时间（含）
     * @param endTime   统计结束时间（含）
     * @return 每日聚合结果列表
     */
    List<DailyRevenueQueryRow> queryDailyRevenue(
            @Param("shopIds") List<Long> shopIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 在指定店铺集合与时间范围内，查询销售额或销量最高的商品 TOP N（见 {@link BestSellerItem}）。
     *
     * @param shopIds   店铺 id 列表
     * @param startTime 统计起始时间（含）
     * @param endTime   统计结束时间（含）
     * @param limit     返回条数上限
     * @return 畅销商品列表
     */
    List<BestSellerItem> queryBestSellers(
            @Param("shopIds") List<Long> shopIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit
    );
}
