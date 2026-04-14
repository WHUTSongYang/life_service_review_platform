package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注册请求体：凭证（手机或邮箱）、密码与昵称等。
 */
@Data
public class RegisterRequest {
    /**
     * 手机号；与 {@link #email} 二选一。前端通常根据是否包含“@”写入手机或邮箱字段。
     */
    private String phone;
    /**
     * 邮箱；与 {@link #phone} 二选一。
     */
    private String email;

    /** 登录密码，必填 */
    @NotBlank
    private String password;
    /** 确认密码，须与 {@link #password} 一致，必填 */
    @NotBlank
    private String confirmPassword;

    /** 用户昵称，必填 */
    @NotBlank
    private String nickname;
}
