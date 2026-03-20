// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/** 用户资料展示项：id、昵称、手机号、邮箱 */
@Data
@Builder
public class UserProfileItem {
    // 字段说明：用户主键 ID
    private Long id;
    // 字段说明：用户昵称
    private String nickname;
    // 字段说明：手机号
    private String phone;
    // 字段说明：邮箱
    private String email;
}
