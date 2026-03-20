// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 密码登录请求：account 为手机号或邮箱 */
@Data
public class LoginRequest {
    @NotBlank
    private String account;        // 手机号或邮箱，必填

    @NotBlank
    private String password;       // 密码，必填
}
