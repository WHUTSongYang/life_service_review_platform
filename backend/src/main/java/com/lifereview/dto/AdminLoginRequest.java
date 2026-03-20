// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理员登录请求：用户名、密码 */
@Data
public class AdminLoginRequest {
    @NotBlank
    private String username;       // 管理员用户名，必填

    @NotBlank
    private String password;      // 密码，必填
}
