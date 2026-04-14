package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.entity.User;
import com.lifereview.service.UserRelationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户关注关系控制器。
 * <p>URL 前缀：{@code /api/users}。关注、取关、关注列表、粉丝列表均需登录。</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRelationController {

    /** 关注与粉丝列表业务服务 */
    private final UserRelationService userRelationService;

    /**
     * 关注指定用户。
     *
     * @param targetUserId 被关注用户 ID
     * @param request      当前 HTTP 请求
     * @return 空数据成功响应
     * @throws IllegalArgumentException 未登录或业务不允许（如关注自己）
     */
    @PostMapping("/{targetUserId}/follow")
    public ApiResponse<Void> follow(@PathVariable("targetUserId") Long targetUserId, HttpServletRequest request) {
        userRelationService.follow(currentUserId(request), targetUserId);
        return ApiResponse.ok(null);
    }

    /**
     * 取消关注指定用户。
     *
     * @param targetUserId 被取消关注的用户 ID
     * @param request      当前 HTTP 请求
     * @return 空数据成功响应
     * @throws IllegalArgumentException 未登录
     */
    @DeleteMapping("/{targetUserId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable("targetUserId") Long targetUserId, HttpServletRequest request) {
        userRelationService.unfollow(currentUserId(request), targetUserId);
        return ApiResponse.ok(null);
    }

    /**
     * 当前登录用户关注的用户列表。
     *
     * @param request 当前 HTTP 请求
     * @return 用户实体列表
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/me/following")
    public ApiResponse<List<User>> following(HttpServletRequest request) {
        return ApiResponse.ok(userRelationService.listFollowing(currentUserId(request)));
    }

    /**
     * 当前登录用户的粉丝列表。
     *
     * @param request 当前 HTTP 请求
     * @return 用户实体列表
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/me/followers")
    public ApiResponse<List<User>> followers(HttpServletRequest request) {
        return ApiResponse.ok(userRelationService.listFollowers(currentUserId(request)));
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
