package com.lifereview.repository;

import com.lifereview.dto.BestSellerItem;
import com.lifereview.dto.DailyRevenueQueryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端数据统计 Mapper：日营业额、畅销商品 */
@Mapper
public interface DashboardStatsRepository {
    List<DailyRevenueQueryRow> queryDailyRevenue(
            @Param("shopIds") List<Long> shopIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    List<BestSellerItem> queryBestSellers(
            @Param("shopIds") List<Long> shopIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit
    );
}
