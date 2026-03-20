// 包声明：工具类所在包
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
 * JWT 令牌生成与解析。
 * 支持 USER 和 ADMIN 两种主体类型，payload 含 principalType、principalId、superAdmin。
 * 使用 HMAC-SHA 签名，密钥和过期时间从 app.jwt 配置读取。
 */
@Component
public class JwtTokenProvider {

    /** JWT 签名密钥 */
    @Value("${app.jwt.secret}")
    private String secret;

    /** 令牌过期秒数
     * -- GETTER --
     * 返回配置的 token 过期秒数，供 Redis 会话 TTL 使用
     */
    @Getter
    @Value("${app.jwt.expire-seconds}")
    private long expireSeconds;

    private SecretKey key;

    /** 初始化时根据 secret 生成 HMAC 密钥 */
    @PostConstruct
    public void init() {
        //HMAC（Hash-based Message Authentication Code，基于哈希的消息认证码）密钥，是用于 HMAC 算法的 “专用秘钥” —— 你可以把它理解成：
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 兼容旧接口，等同于 createUserToken */
    public String createToken(Long userId) {
        return createUserToken(userId);
    }

    /** 创建用户登录令牌 */
    public String createUserToken(Long userId) {
        return createToken(AuthPrincipal.TYPE_USER, userId, false);
    }

    /** 创建管理员登录令牌 */
    public String createAdminToken(Long adminId, boolean superAdmin) {
        return createToken(AuthPrincipal.TYPE_ADMIN, adminId, superAdmin);
    }

    /** 内部签发 token。subject 为 principalType:principalId，claims 含 principalType、principalId、superAdmin */
    private String createToken(String principalType, Long principalId, boolean superAdmin) {
        Instant now = Instant.now();
        String subject = principalType + ":" + principalId;         //value
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
     * 解析JWT Token，提取身份信息并封装为AuthPrincipal对象
     * @param token 待解析的JWT Token字符串
     * @return 封装好的身份信息对象AuthPrincipal
     * @throws IllegalArgumentException 当Token的subject格式非法时抛出
     */
    public AuthPrincipal parsePrincipal(String token) {
        // 1. 解析JWT Token并验证签名（核心步骤）
        // Jwts.parser()：创建JWT解析器
        // verifyWith(key)：使用指定的密钥（如HMAC密钥）验证Token签名，确保Token未被篡改
        // build()：构建解析器实例
        // parseSignedClaims(token)：解析带签名的Token，若签名无效/Token过期会直接抛出异常
        // getPayload()：获取Token的载荷部分（存放自定义数据的地方）
        Claims payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 2. 优先从Payload中直接读取身份核心字段
        // principalType：身份类型（如USER/ADMIN，区分是用户还是管理员）
        String principalType = payload.get("principalType", String.class);
        // principalId：身份ID（用户ID/管理员ID，Number类型兼容整数/长整数）
        Number principalIdNum = payload.get("principalId", Number.class);
        // superAdmin：是否为超级管理员（默认null时取false）
        boolean superAdmin = Boolean.TRUE.equals(payload.get("superAdmin", Boolean.class));

        // 3. 若Payload中未直接存储principalType/principalId，则从Subject字段解析
        if (principalType == null || principalIdNum == null) {
            // 获取JWT的Subject字段（通常用于存储主体标识）
            String subject = payload.getSubject();
            // 校验Subject是否为空，或是否符合「类型:ID」的格式
            if (subject == null || !subject.contains(":")) {
                throw new IllegalArgumentException("token subject invalid");
            }
            // 按冒号分割Subject，最多分割为2部分（避免ID中包含冒号）
            String[] parts = subject.split(":", 2);
            // 分割后第一部分为身份类型
            principalType = parts[0];
            // 分割后第二部分转换为Long类型的身份ID
            principalIdNum = Long.valueOf(parts[1]);
        }

        // 4. 将Number类型的principalId转为Long（统一ID类型）
        Long principalId = principalIdNum.longValue();

        // 5. 构建并返回AuthPrincipal对象
        // 核心逻辑：根据身份类型，区分填充userId/adminId（非对应类型则为null）
        return AuthPrincipal.builder()
                .principalType(principalType) // 身份类型（USER/ADMIN）
                .principalId(principalId)     // 统一的身份ID
                // 若为用户类型，userId=principalId；否则为null
                .userId(AuthPrincipal.TYPE_USER.equals(principalType) ? principalId : null)
                // 若为管理员类型，adminId=principalId；否则为null
                .adminId(AuthPrincipal.TYPE_ADMIN.equals(principalType) ? principalId : null)
                .superAdmin(superAdmin)       // 是否超级管理员
                .build();
    }

}
