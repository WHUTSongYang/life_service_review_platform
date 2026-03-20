// 包声明：配置类所在包
package com.lifereview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置。
 * 关闭 CSRF（前后端分离场景）。放行所有 HTTP 请求，实际鉴权由 LoginInterceptor 处理。
 * 提供 BCrypt 密码编码器 Bean，供注册和登录密码校验使用。
 */
@Configuration
public class SecurityConfig {

    /** 安全过滤链：关闭 CSRF，全部放行，启用 HTTP Basic 默认配置 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 关闭 CSRF 防护（前后端分离，使用 JWT 鉴权）
        http.csrf(csrf -> csrf.disable())
                // 放行所有请求，鉴权由 LoginInterceptor 在 /api/** 上处理
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 启用 HTTP Basic 认证默认配置
                .httpBasic(Customizer.withDefaults());
        // 构建并返回过滤链
        return http.build();
    }

    /** BCrypt 密码编码器，用于用户注册加密和登录密码校验 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 返回 BCrypt 实现，支持加盐哈希
        return new BCryptPasswordEncoder();
    }
}
