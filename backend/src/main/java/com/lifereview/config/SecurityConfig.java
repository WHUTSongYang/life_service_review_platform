package com.lifereview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 基础配置。
 * <p>
 * 关闭 CSRF（前后端分离 + JWT 由拦截器校验）；默认放行所有 HTTP 请求，细粒度鉴权由
 * {@link LoginInterceptor} 完成。同时提供 {@link BCryptPasswordEncoder} 供注册与登录哈希密码。
 * </p>
 */
@Configuration
public class SecurityConfig {

    /**
     * 构建安全过滤器链：禁用 CSRF、全部请求 permitAll，并启用 HTTP Basic 默认行为。
     *
     * @param http {@link HttpSecurity} 构建器
     * @return 装配完成的 {@link SecurityFilterChain}
     * @throws Exception Spring Security 配置过程中的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // 前后端分离场景关闭表单 CSRF
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // 业务鉴权在 LoginInterceptor
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * 密码编码器 Bean，使用 BCrypt 加盐哈希。
     *
     * @return 可用于 {@code encode}/{@code matches} 的 {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
