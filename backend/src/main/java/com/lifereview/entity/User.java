package com.lifereview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 平台用户实体，映射数据库表 {@code users}。
 * <p>C 端账号信息：支持手机号、邮箱登录标识，密码经加密存储，以及展示昵称与注册时间。
 * 字段的 getter/setter 等由 Lombok {@code @Data} 生成。</p>
 */
@Data
@TableName("users")
public class User {
    /** 主键 ID */
    @TableId
    private Long id;
    /** 手机号，常用作登录账号 */
    private String phone;
    /** 邮箱，可选，可作为补充登录或联系方式 */
    private String email;
    /** 经 BCrypt 等算法加密后的登录密码 */
    private String password;
    /** 用户昵称，用于界面展示 */
    private String nickname;
    /** 账号注册/创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
}
