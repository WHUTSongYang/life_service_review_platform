package com.lifereview.service;

import com.lifereview.dto.AdminLoginRequest;
import com.lifereview.dto.AuthPrincipal;
import com.lifereview.dto.LoginRequest;
import com.lifereview.dto.RegisterRequest;
import com.lifereview.entity.User;

import java.util.Map;

/**
 * 业务职责说明：用户与管理员身份认证；覆盖注册、密码登录（含图形验证码）、Token 校验与登出。
 */
public interface AuthService {

    /**
     * 用户注册。
     *
     * @param req 注册请求体（账号、密码等）
     * @return 新建的用户实体
     */
    User register(RegisterRequest req);

    /**
     * 密码登录（含图形验证码校验）。
     *
     * @param req 登录请求体（账号、密码、验证码等）
     * @return 包含 token 与用户信息的键值映射
     */
    Map<String, Object> loginWithPassword(LoginRequest req);

    /**
     * 管理员登录。
     *
     * @param req 管理员登录请求体
     * @return 包含 token 与管理员信息的键值映射
     */
    Map<String, Object> loginAdmin(AdminLoginRequest req);

    /**
     * 校验访问令牌是否有效。
     *
     * @param token JWT 或会话 token 字符串
     * @return 有效时返回认证主体；无效或过期时返回 {@code null}
     */
    AuthPrincipal validateToken(String token);

    /**
     * 登出并使指定 token 失效。
     *
     * @param token 待失效的 token
     */
    void logout(String token);
}
