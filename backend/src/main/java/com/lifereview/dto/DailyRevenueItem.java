package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 按日汇总的营业额展示项，用于管理端图表或列表。
 */
@Data
@Builder
public class DailyRevenueItem {
    /** 统计日期（格式与接口约定一致，如 yyyy-MM-dd） */
    private String date;
    /** 该日营业额金额 */
    private BigDecimal revenue;
}
