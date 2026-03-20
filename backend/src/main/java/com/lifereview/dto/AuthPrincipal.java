// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/** 认证主体：区分用户/管理员，含 principalType、principalId、userId/adminId、superAdmin */
@Data
@Builder
public class AuthPrincipal {
    public static final String TYPE_USER = "USER";   // 普通用户类型标识
    public static final String TYPE_ADMIN = "ADMIN"; // 管理员类型标识

    private String principalType;  // 主体类型：USER 或 ADMIN
    private Long principalId;      // 通用主体 ID（userId 或 adminId）
    private Long userId;           // 用户 ID，管理员时为 null
    private Long adminId;          // 管理员 ID，用户时为 null
    private boolean superAdmin;    // 是否超级管理员

    public boolean isUser() {
        return TYPE_USER.equals(principalType);
    }

    public boolean isAdmin() {
        return TYPE_ADMIN.equals(principalType);
    }
}
