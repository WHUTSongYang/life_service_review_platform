// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Data;

/** 用户资料更新请求：昵称、手机号、邮箱（均为可选） */
@Data
public class UserProfileUpdateRequest {
    // 字段说明：用户昵称，可选
    private String nickname;
    // 字段说明：手机号，可选
    private String phone;
    // 字段说明：邮箱，可选
    private String email;
}
