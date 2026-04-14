package com.lifereview.dto;

import lombok.Builder;
import lombok.Data;

/**
 * JWT 或会话解析后的认证主体，区分普通用户与管理员并携带对应 ID。
 */
@Data
@Builder
public class AuthPrincipal {
    /** 主体类型常量：普通用户 */
    public static final String TYPE_USER = "USER";
    /** 主体类型常量：管理员 */
    public static final String TYPE_ADMIN = "ADMIN";

    /** 主体类型，取值为 {@link #TYPE_USER} 或 {@link #TYPE_ADMIN} */
    private String principalType;
    /** 通用主体主键，对用户为 userId，对管理员为 adminId */
    private Long principalId;
    /** 用户主键；当前主体为管理员时为 null */
    private Long userId;
    /** 管理员主键；当前主体为用户时为 null */
    private Long adminId;
    /** 是否为超级管理员（仅管理员主体有意义） */
    private boolean superAdmin;

    /**
     * 判断当前认证主体是否为普通用户。
     *
     * @return 若 {@code principalType} 为用户类型则返回 true，否则 false
     */
    public boolean isUser() {
        return TYPE_USER.equals(principalType);
    }

    /**
     * 判断当前认证主体是否为管理员。
     *
     * @return 若 {@code principalType} 为管理员类型则返回 true，否则 false
     */
    public boolean isAdmin() {
        return TYPE_ADMIN.equals(principalType);
    }
}
