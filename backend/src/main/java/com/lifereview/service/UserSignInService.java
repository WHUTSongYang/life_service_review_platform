package com.lifereview.service;

import com.lifereview.dto.SignInStatusItem;

/**
 * 用户签到服务接口。
 * 负责每日签到及连续签到天数统计。
 */
public interface UserSignInService {

    // 获取当前用户签到状态（今日是否已签、连续天数等）
    SignInStatusItem getStatus(Long userId);

    // 执行今日签到，返回更新后的签到状态
    SignInStatusItem signToday(Long userId);
}
