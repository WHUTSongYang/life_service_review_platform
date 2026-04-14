package com.lifereview.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 日营业额 SQL 查询结果行，供 MyBatis 等映射为业务 DTO。
 */
@Data
public class DailyRevenueQueryRow {
    /** 统计日期 */
    private String date;
    /** 该日营业额金额 */
    private BigDecimal revenue;
}
