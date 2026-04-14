package com.lifereview.util;

import com.lifereview.dto.AuthPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 访问令牌的创建与解析工具。
 * <p>
 * 使用 HMAC-SHA 对称签名；载荷包含 {@code principalType}、{@code principalId}、{@code superAdmin}，
 * 支持 {@link AuthPrincipal#TYPE_USER} 与 {@link AuthPrincipal#TYPE_ADMIN}。密钥与有效期来自 {@code app.jwt} 配置。
 * </p>
 */
@Component
public class JwtTokenProvider {

    /** HMAC 签名用密钥字符串（配置长度需满足算法要求） */
    @Value("${app.jwt.secret}")
    private String secret;

    /** 访问令牌有效时长（秒），也可被会话 TTL 等逻辑复用 */
    @Getter
    @Value("${app.jwt.expire-seconds}")
    private long expireSeconds;

    /** 由 {@link #secret} 派生的 {@link SecretKey}，用于签名与验签 */
    private SecretKey key;

    /**
     * 初始化：根据配置密钥构建 HMAC-SHA 密钥实例。
     */
    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // jjwt 要求的密钥派生
    }

    /**
     * 兼容旧接口：等同于 {@link #createUserToken(Long)}。
     *
     * @param userId 用户主键
     * @return 已签名的 JWT 字符串
     */
    public String createToken(Long userId) {
        return createUserToken(userId);
    }

    /**
     * 为用户签发 JWT，主体类型为 {@link AuthPrincipal#TYPE_USER}。
     *
     * @param userId 用户主键
     * @return 已签名的 JWT 字符串
     */
    public String createUserToken(Long userId) {
        return createToken(AuthPrincipal.TYPE_USER, userId, false);
    }

    /**
     * 为管理员签发 JWT，可标记是否超级管理员。
     *
     * @param adminId   管理员主键
     * @param superAdmin 是否超级管理员
     * @return 已签名的 JWT 字符串
     */
    public String createAdminToken(Long adminId, boolean superAdmin) {
        return createToken(AuthPrincipal.TYPE_ADMIN, adminId, superAdmin);
    }

    /**
     * 按主体类型与 ID 构建 JWT：subject 为 {@code type:id}，并写入标准声明与自定义声明。
     *
     * @param principalType 主体类型（用户/管理员）
     * @param principalId   主体 ID
     * @param superAdmin    是否超级管理员（仅管理员语义有效）
     * @return 紧凑序列化后的 Token
     */
    private String createToken(String principalType, Long principalId, boolean superAdmin) {
        Instant now = Instant.now();
        String subject = principalType + ":" + principalId;
        return Jwts.builder()
                .subject(subject)
                .claim("principalType", principalType)
                .claim("principalId", principalId)
                .claim("superAdmin", superAdmin)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 校验签名与有效期，解析载荷并组装为 {@link AuthPrincipal}。
     * <p>若载荷中缺少类型与 ID，则回退解析 {@code subject}（格式须为 {@code type:id}）。</p>
     *
     * @param token 不带 {@code Bearer} 前缀的原始 JWT
     * @return 解析成功后的身份主体
     * @throws io.jsonwebtoken.JwtException 签名无效、过期或格式错误时由 jjwt 抛出
     * @throws IllegalArgumentException 当 subject 无法解析出合法身份时抛出
     */
    public AuthPrincipal parsePrincipal(String token) {
        Claims payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload(); // 验签并取载荷

        String principalType = payload.get("principalType", String.class);
        Number principalIdNum = payload.get("principalId", Number.class);
        boolean superAdmin = Boolean.TRUE.equals(payload.get("superAdmin", Boolean.class));

        if (principalType == null || principalIdNum == null) {
            String subject = payload.getSubject();
            if (subject == null || !subject.contains(":")) {
                throw new IllegalArgumentException("token subject invalid"); // 无法从 subject 恢复身份
            }
            String[] parts = subject.split(":", 2);
            principalType = parts[0];
            principalIdNum = Long.valueOf(parts[1]);
        }

        Long principalId = principalIdNum.longValue();

        return AuthPrincipal.builder()
                .principalType(principalType)
                .principalId(principalId)
                .userId(AuthPrincipal.TYPE_USER.equals(principalType) ? principalId : null)
                .adminId(AuthPrincipal.TYPE_ADMIN.equals(principalType) ? principalId : null)
                .superAdmin(superAdmin)
                .build();
    }

}
