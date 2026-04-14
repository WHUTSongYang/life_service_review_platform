package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户签到功能的状态展示项，用于首页或签到页展示进度。
 */
@Data
@Builder
public class SignInStatusItem {
    /** 当日是否已完成签到 */
    private Boolean signedToday;
    /** 当前自然月内已累计签到天数 */
    private Integer monthSignedDays;
    /** 连续签到天数 */
    private Integer continuousSignedDays;
    /** 服务端认定的“当前日期”字符串，便于前端展示对齐 */
    private String date;
}
