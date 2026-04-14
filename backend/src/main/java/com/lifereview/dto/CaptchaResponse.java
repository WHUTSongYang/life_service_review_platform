package com.lifereview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图形验证码下发给前端的响应，包含会话标识与 Base64 图片数据。
 */
@Data
@AllArgsConstructor
public class CaptchaResponse {
    /** 本次验证码会话 ID，登录时需原样回传 */
    private String captchaId;
    /** PNG 图片的 Base64 编码（不含前缀时可由前端拼接 data:image/png;base64,） */
    private String imageBase64;
}
