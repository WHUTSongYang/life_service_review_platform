package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户使用账号密码登录的请求体，需配合图形验证码防刷。
 */
@Data
public class LoginRequest {
    /** 登录账号，支持手机号或邮箱 */
    @NotBlank
    private String account;

    /** 登录密码 */
    @NotBlank
    private String password;

    /** 图形验证码会话 ID，与获取验证码接口返回一致 */
    @NotBlank
    private String captchaId;

    /** 用户输入的验证码内容（如 4 位数字或字符） */
    @NotBlank
    private String captchaCode;
}
