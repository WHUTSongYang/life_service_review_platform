package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.HotReviewItem;
import com.lifereview.dto.SignInStatusItem;
import com.lifereview.dto.UserProfileItem;
import com.lifereview.dto.UserProfileUpdateRequest;
import com.lifereview.service.UserSignInService;
import com.lifereview.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户资料控制器。
 * 提供个人资料查询、更新、我的点评、签到状态、每日签到接口，需登录。
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserSignInService userSignInService;

    // 获取当前用户资料
    @GetMapping
    public ApiResponse<UserProfileItem> getProfile(HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.getProfile(currentUserId(request)));
    }

    // 更新当前用户资料
    @PutMapping
    public ApiResponse<UserProfileItem> updateProfile(@Valid @RequestBody UserProfileUpdateRequest req, HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.updateProfile(currentUserId(request), req));
    }

    // 获取当前用户发表的点评列表
    @GetMapping("/reviews")
    public ApiResponse<List<HotReviewItem>> listMyReviews(HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.listMyReviews(currentUserId(request)));
    }

    // 获取签到状态（今日是否已签、连续天数等）
    @GetMapping("/sign-in/status")
    public ApiResponse<SignInStatusItem> signInStatus(HttpServletRequest request) {
        return ApiResponse.ok(userSignInService.getStatus(currentUserId(request)));
    }

    // 执行今日签到
    @PostMapping("/sign-in")
    public ApiResponse<SignInStatusItem> signInToday(HttpServletRequest request) {
        return ApiResponse.ok(userSignInService.signToday(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
