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
 * 基于 Redis Bitmap 实现每日签到及连续签到天数统计。
 */
@Service
@RequiredArgsConstructor
public class UserSignInServiceImpl implements UserSignInService {

    // 月份格式化，用于 Redis 键
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    @Override
    public SignInStatusItem getStatus(Long userId) {
        ensureUserExists(userId);
        LocalDate today = LocalDate.now();
        return buildStatus(userId, today);
    }

    @Override
    public SignInStatusItem signToday(Long userId) {
        ensureUserExists(userId);
        LocalDate today = LocalDate.now();
        String key = signKey(userId, today);
        int offset = today.getDayOfMonth() - 1;
        // 使用 Bitmap 标记当日已签到
        stringRedisTemplate.opsForValue().setBit(key, offset, true);
        return buildStatus(userId, today);
    }

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

    // 从今天往前统计连续签到天数
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

    private boolean isSigned(Long userId, LocalDate date) {
        String key = signKey(userId, date);
        int offset = date.getDayOfMonth() - 1;
        Boolean bit = stringRedisTemplate.opsForValue().getBit(key, offset);
        return Boolean.TRUE.equals(bit);
    }

    private String signKey(Long userId, LocalDate date) {
        return "sign:" + userId + ":" + date.format(MONTH_FMT);
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }
}
