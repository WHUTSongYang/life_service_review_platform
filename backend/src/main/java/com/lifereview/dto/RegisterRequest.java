// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 用户注册请求：手机号/邮箱、密码、确认密码、昵称 */
@Data
public class RegisterRequest {
    private String phone;           // 手机号，与 email 二选一
    private String email;          // 邮箱，与 phone 二选一

    @NotBlank
    private String password;       // 密码，必填

    @NotBlank
    private String confirmPassword;  // 确认密码，需与 password 一致

    @NotBlank
    private String nickname;       // 昵称，必填
}
