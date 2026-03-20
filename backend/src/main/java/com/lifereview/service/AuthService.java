package com.lifereview.service;

import com.lifereview.dto.AdminLoginRequest;
import com.lifereview.dto.AuthPrincipal;
import com.lifereview.dto.CodeLoginRequest;
import com.lifereview.dto.LoginRequest;
import com.lifereview.dto.RegisterRequest;
import com.lifereview.entity.User;

import java.util.Map;

/**
 * 认证服务接口。
 * 负责用户注册、密码/验证码登录、管理员登录、Token 校验及登出。
 */
public interface AuthService {

    // 用户注册，返回注册成功的用户实体
    User register(RegisterRequest req);

    // 密码登录，返回包含 token 和用户信息的 Map
    Map<String, Object> loginWithPassword(LoginRequest req);

    // 验证码登录，返回包含 token 和用户信息的 Map
    Map<String, Object> loginWithCode(CodeLoginRequest req);

    // 管理员登录，返回包含 token 和管理员信息的 Map
    Map<String, Object> loginAdmin(AdminLoginRequest req);

    // 校验 token 有效性，返回认证主体信息，无效则返回 null
    AuthPrincipal validateToken(String token);

    // 登出，使指定 token 失效
    void logout(String token);
}
