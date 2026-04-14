package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户个人资料在接口中的展示项，脱敏或完整视业务而定。
 */
@Data
@Builder
public class UserProfileItem {
    /** 用户主键 ID */
    private Long id;
    /** 用户昵称 */
    private String nickname;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
}
