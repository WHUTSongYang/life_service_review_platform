package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.entity.User;
import com.lifereview.service.UserRelationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户关系控制器。
 * 提供关注、取消关注、关注列表、粉丝列表接口，需登录。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRelationController {

    private final UserRelationService userRelationService;

    // 关注指定用户
    @PostMapping("/{targetUserId}/follow")
    public ApiResponse<Void> follow(@PathVariable("targetUserId") Long targetUserId, HttpServletRequest request) {
        userRelationService.follow(currentUserId(request), targetUserId);
        return ApiResponse.ok(null);
    }

    // 取消关注指定用户
    @DeleteMapping("/{targetUserId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable("targetUserId") Long targetUserId, HttpServletRequest request) {
        userRelationService.unfollow(currentUserId(request), targetUserId);
        return ApiResponse.ok(null);
    }

    // 获取当前用户关注的用户列表
    @GetMapping("/me/following")
    public ApiResponse<List<User>> following(HttpServletRequest request) {
        return ApiResponse.ok(userRelationService.listFollowing(currentUserId(request)));
    }

    // 获取当前用户的粉丝列表
    @GetMapping("/me/followers")
    public ApiResponse<List<User>> followers(HttpServletRequest request) {
        return ApiResponse.ok(userRelationService.listFollowers(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
