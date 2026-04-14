package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 管理员账号实体，映射数据库表 {@code admin_accounts}。
 * <p>用于后台登录与权限识别；{@code superAdmin} 为 {@code true} 时表示超级管理员。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("admin_accounts")
public class AdminAccount {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 登录用户名 */
    private String username;
    /** 经 BCrypt 加密后的登录密码 */
    private String password;
    /** 展示用昵称 */
    private String nickname;
    /** 是否为超级管理员 */
    private Boolean superAdmin = false;
    /** 账号是否启用，{@code false} 时不可登录 */
    private Boolean enabled = true;
    /** 记录创建时间 */
    private java.time.LocalDateTime createdAt;
}
