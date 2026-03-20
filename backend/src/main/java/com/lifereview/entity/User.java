// 包声明：实体类所在包
package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 用户实体，对应 users 表。支持手机号、邮箱、密码、昵称 */
@Data
@TableName("users")
public class User {
    @TableId
    private Long id;
    private String phone;           // 手机号，用于登录
    private String email;           // 邮箱，可选
    private String password;       // BCrypt 加密后的密码
    private String nickname;       // 昵称，展示用
    private java.time.LocalDateTime createdAt;  // 创建时间
}
