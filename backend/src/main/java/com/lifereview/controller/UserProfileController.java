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
 * 当前用户个人中心控制器。
 * <p>URL 前缀：{@code /api/users/me}。查询、修改资料、我的点评、签到均需登录。</p>
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    /** 用户资料查询与更新、我的点评列表 */
    private final UserProfileService userProfileService;

    /** 每日签到与连续签到状态 */
    private final UserSignInService userSignInService;

    /**
     * 获取当前登录用户的资料卡片。
     *
     * @param request 当前 HTTP 请求
     * @return 用户资料 DTO
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping
    public ApiResponse<UserProfileItem> getProfile(HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.getProfile(currentUserId(request)));
    }

    /**
     * 更新当前用户资料（昵称、头像等）。
     *
     * @param req     可更新字段
     * @param request 当前 HTTP 请求
     * @return 更新后的资料 DTO
     * @throws IllegalArgumentException 未登录或校验失败
     */
    @PutMapping
    public ApiResponse<UserProfileItem> updateProfile(@Valid @RequestBody UserProfileUpdateRequest req, HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.updateProfile(currentUserId(request), req));
    }

    /**
     * 当前用户发表的点评列表。
     *
     * @param request 当前 HTTP 请求
     * @return 与热门流一致的点评条目
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/reviews")
    public ApiResponse<List<HotReviewItem>> listMyReviews(HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.listMyReviews(currentUserId(request)));
    }

    /**
     * 查询今日签到状态（是否已签、连续天数等）。
     *
     * @param request 当前 HTTP 请求
     * @return 签到状态 DTO
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/sign-in/status")
    public ApiResponse<SignInStatusItem> signInStatus(HttpServletRequest request) {
        return ApiResponse.ok(userSignInService.getStatus(currentUserId(request)));
    }

    /**
     * 执行当日签到。
     *
     * @param request 当前 HTTP 请求
     * @return 更新后的签到状态
     * @throws IllegalArgumentException 未登录或重复签到
     */
    @PostMapping("/sign-in")
    public ApiResponse<SignInStatusItem> signInToday(HttpServletRequest request) {
        return ApiResponse.ok(userSignInService.signToday(currentUserId(request)));
    }

    /** 从请求属性解析当前登录用户 ID，未登录则抛出异常。 */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
