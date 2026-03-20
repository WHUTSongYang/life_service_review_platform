// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 日营业额统计项：日期、营业额金额 */
@Data
@Builder
public class DailyRevenueItem {
    // 字段说明：日期
    private String date;
    // 字段说明：当日营业额金额
    private BigDecimal revenue;
}
