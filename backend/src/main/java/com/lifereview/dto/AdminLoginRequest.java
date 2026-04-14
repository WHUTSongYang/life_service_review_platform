package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录请求体，用于管理端账号密码认证。
 */
@Data
public class AdminLoginRequest {
    /** 管理员登录用户名，必填 */
    @NotBlank
    private String username;

    /** 登录密码，必填 */
    @NotBlank
    private String password;
}
