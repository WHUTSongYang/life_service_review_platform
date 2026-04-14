package com.lifereview.service;

import com.lifereview.dto.SignInStatusItem;

/**
 * 业务职责说明：用户每日签到及连续签到天数等签到状态查询与更新。
 */
public interface UserSignInService {

    /**
     * 查询用户签到状态（如今日是否已签、连续签到天数等）。
     *
     * @param userId 用户 ID
     * @return 签到状态 DTO
     */
    SignInStatusItem getStatus(Long userId);

    /**
     * 执行当日签到并持久化。
     *
     * @param userId 用户 ID
     * @return 签到完成后的最新状态
     */
    SignInStatusItem signToday(Long userId);
}
