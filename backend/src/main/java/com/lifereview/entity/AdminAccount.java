// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 管理员账号实体，对应 admin_accounts 表。superAdmin 为 true 表示超级管理员 */
@Data
@TableName("admin_accounts")
public class AdminAccount {
    @TableId
    private Long id;
    private String username;        // 登录用户名
    private String password;       // BCrypt 加密密码
    private String nickname;       // 昵称
    private Boolean superAdmin = false;  // 是否超级管理员
    private Boolean enabled = true;  // 是否启用
    private java.time.LocalDateTime createdAt;
}
