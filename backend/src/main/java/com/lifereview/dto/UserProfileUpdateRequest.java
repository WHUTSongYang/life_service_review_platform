package com.lifereview.dto;

import lombok.Data;

/**
 * 用户更新个人资料时的请求体；各字段均为可选，仅提交需要修改的项即可。
 */
@Data
public class UserProfileUpdateRequest {
    /** 用户昵称，可选 */
    private String nickname;
    /** 手机号，可选 */
    private String phone;
    /** 邮箱，可选 */
    private String email;
}
