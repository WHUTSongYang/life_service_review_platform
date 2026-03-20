// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 日营业额查询原始行：date、revenue，供 Mapper 映射 */
@Data
public class DailyRevenueQueryRow {
    // 字段说明：日期
    private String date;
    // 字段说明：当日营业额金额
    private BigDecimal revenue;
}
