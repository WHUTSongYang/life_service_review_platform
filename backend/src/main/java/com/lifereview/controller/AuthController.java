package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.AdminLoginRequest;
import com.lifereview.dto.CaptchaResponse;
import com.lifereview.dto.LoginRequest;
import com.lifereview.dto.RegisterRequest;
import com.lifereview.entity.User;
import com.lifereview.service.AuthService;
import com.lifereview.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证与登录控制器。
 * <p>URL 前缀：{@code /api/auth}。注册、登录、验证码、管理员登录、登出均无需事先登录。</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** 用户注册、密码登录、管理员登录、登出与会话 */
    private final AuthService authService;

    /** 图形验证码生成（如 PNG Base64） */
    private final CaptchaService captchaService;

    /**
     * 用户注册。
     *
     * @param req 手机号或邮箱、密码、确认密码、昵称等
     * @return 注册成功后的用户实体
     * @throws IllegalArgumentException 参数或业务校验失败时由全局异常处理
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    /**
     * 密码登录（含图形验证码校验）。
     *
     * @param req 账号（手机或邮箱）、密码、验证码 ID 与码
     * @return 含 token、userId、nickname、principalType 等的 Map
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.loginWithPassword(req));
    }

    /**
     * 获取登录用图形验证码。
     *
     * @return captchaId 与图片 Base64 等
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.ok(captchaService.generate());
    }

    /**
     * 管理后台账号登录。
     *
     * @param req 管理员用户名与密码
     * @return 含 token、adminId、nickname、principalType、isSuperAdmin 等
     */
    @PostMapping("/admin/login")
    public ApiResponse<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return ApiResponse.ok(authService.loginAdmin(req));
    }

    /**
     * 登出：清除服务端会话（如 Redis 中的 token）。
     *
     * @param authHeader 可选，Authorization 头，格式为 Bearer 加访问令牌
     * @return 空数据成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ApiResponse.ok(null);
    }
}
