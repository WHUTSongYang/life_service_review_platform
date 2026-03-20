package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.AdminLoginRequest;
import com.lifereview.dto.CodeLoginRequest;
import com.lifereview.dto.LoginRequest;
import com.lifereview.dto.RegisterRequest;
import com.lifereview.entity.User;
import com.lifereview.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器。
 * 提供用户注册、密码登录、验证码登录、管理员登录、登出接口。所有接口无需登录即可访问。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 用户注册。需提供手机号或邮箱至少一个、密码、确认密码、昵称 */
    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    /** 密码登录。account 为手机号或邮箱，返回 token、userId、nickname、principalType */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.loginWithPassword(req));
    }

    /** 验证码登录。account 为手机号，code 为验证码，返回格式同密码登录 */
    @PostMapping("/code-login")
    public ApiResponse<Map<String, Object>> codeLogin(@Valid @RequestBody CodeLoginRequest req) {
        return ApiResponse.ok(authService.loginWithCode(req));
    }

    /** 管理员登录。需提供 username、password，返回 token、adminId、nickname、principalType、isSuperAdmin */
    @PostMapping("/admin/login")
    public ApiResponse<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return ApiResponse.ok(authService.loginAdmin(req));
    }

    /** 登出。从 Authorization 头解析 Bearer token 并清除 Redis 会话 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ApiResponse.ok(null);
    }
}
