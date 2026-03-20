// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 验证码登录请求：account 为手机号，code 为验证码 */
@Data
public class CodeLoginRequest {
    @NotBlank
    private String account;        // 手机号，必填

    @NotBlank
    private String code;          // 短信验证码，必填
}
