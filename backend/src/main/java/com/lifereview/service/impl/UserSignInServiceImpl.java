package com.lifereview.service.impl;

import com.lifereview.dto.SignInStatusItem;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.UserSignInService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用户签到服务实现类。
 * <p>基于 Redis 按用户、按月的 Bitmap 记录每日签到，支持查询今日是否已签、当月累计天数与连续签到天数。</p>
 */
@Service
@RequiredArgsConstructor
public class UserSignInServiceImpl implements UserSignInService {

    /** 月份格式化，用于构造 Redis 键（yyyyMM） */
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    /** Redis 字符串模板（Bitmap 位操作） */
    private final StringRedisTemplate stringRedisTemplate;
    /** 用户仓储（存在性校验） */
    private final UserRepository userRepository;

    /**
     * 查询签到状态：今日是否已签、本月已签天数、连续天数。
     *
     * @param userId 用户主键
     * @return 签到状态 DTO
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Override
    public SignInStatusItem getStatus(Long userId) {
        ensureUserExists(userId);
        LocalDate today = LocalDate.now();
        return buildStatus(userId, today);
    }

    /**
     * 将今日标记为已签到并返回最新状态（重复签到会覆盖为已签，不额外报错）。
     *
     * @param userId 用户主键
     * @return 最新签到状态
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    @Override
    public SignInStatusItem signToday(Long userId) {
        ensureUserExists(userId);
        LocalDate today = LocalDate.now();
        String key = signKey(userId, today);
        int offset = today.getDayOfMonth() - 1;
        // Bitmap：当月第 offset 位标记为已签到
        stringRedisTemplate.opsForValue().setBit(key, offset, true);
        return buildStatus(userId, today);
    }

    /**
     * 组装签到状态 DTO。
     *
     * @param userId 用户 ID
     * @param today  基准日期（通常为当天）
     * @return 状态项
     */
    private SignInStatusItem buildStatus(Long userId, LocalDate today) {
        boolean signedToday = isSigned(userId, today);
        int monthSignedDays = countMonthSignedDays(userId, today);
        int continuousSignedDays = countContinuousSignedDays(userId, today);
        return SignInStatusItem.builder()
                .signedToday(signedToday)
                .monthSignedDays(monthSignedDays)
                .continuousSignedDays(continuousSignedDays)
                .date(today.toString())
                .build();
    }

    /**
     * 统计指定自然月内已签到天数。
     *
     * @param userId 用户 ID
     * @param date   该月内任意一天（用于确定年月）
     * @return 已签天数
     */
    private int countMonthSignedDays(Long userId, LocalDate date) {
        int days = date.lengthOfMonth();
        int count = 0;
        String key = signKey(userId, date);
        for (int i = 0; i < days; i++) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, i);
            if (Boolean.TRUE.equals(bit)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 从指定日期向前连续统计签到天数（最多回溯 366 天）。
     *
     * @param userId 用户 ID
     * @param today  起始日（含）
     * @return 连续签到天数
     */
    private int countContinuousSignedDays(Long userId, LocalDate today) {
        int streak = 0;
        LocalDate cursor = today;
        for (int i = 0; i < 366; i++) {
            if (isSigned(userId, cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * 判断某日是否已签到。
     *
     * @param userId 用户 ID
     * @param date   日期
     * @return 是否已签
     */
    private boolean isSigned(Long userId, LocalDate date) {
        String key = signKey(userId, date);
        int offset = date.getDayOfMonth() - 1;
        Boolean bit = stringRedisTemplate.opsForValue().getBit(key, offset);
        return Boolean.TRUE.equals(bit);
    }

    /**
     * 生成用户某自然月的签到 Bitmap Redis 键。
     *
     * @param userId 用户 ID
     * @param date   该月内任意一天
     * @return 键名
     */
    private String signKey(Long userId, LocalDate date) {
        return "sign:" + userId + ":" + date.format(MONTH_FMT);
    }

    /**
     * 校验用户存在。
     *
     * @param userId 用户主键
     * @throws IllegalArgumentException 不存在时抛出
     */
    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }
}
