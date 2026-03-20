// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/** 签到状态展示项：今日是否已签到、连续签到天数等 */
@Data
@Builder
public class SignInStatusItem {
    // 字段说明：今日是否已签到
    private Boolean signedToday;
    // 字段说明：本月已签到天数
    private Integer monthSignedDays;
    // 字段说明：连续签到天数
    private Integer continuousSignedDays;
    // 字段说明：当前日期
    private String date;
}
