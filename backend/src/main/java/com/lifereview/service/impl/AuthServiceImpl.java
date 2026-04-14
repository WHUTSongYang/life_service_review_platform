package com.lifereview.service.impl;

import com.lifereview.dto.AdminLoginRequest;
import com.lifereview.dto.AuthPrincipal;
import com.lifereview.dto.LoginRequest;
import com.lifereview.dto.RegisterRequest;
import com.lifereview.entity.AdminAccount;
import com.lifereview.entity.User;
import com.lifereview.repository.AdminAccountRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.AuthService;
import com.lifereview.service.CaptchaService;
import com.lifereview.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现。
 * <p>
 * 支持普通用户注册、密码登录（含图形验证码校验）、管理员登录、JWT 签发与 Redis 会话存储、
 * token 校验及可选滑动过期、登出删除会话。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaService captchaService;

    /** Redis 中会话 token 的 key 前缀，如 {@code login:token:}，配置项 {@code app.session.redis-prefix} */
    @Value("${app.session.redis-prefix}")
    private String tokenPrefix;

    /** 是否在每次校验 token 成功时顺延 Redis key 过期时间（滑动过期），配置项 {@code app.session.sliding-expire} */
    @Value("${app.session.sliding-expire:true}")
    private boolean slidingExpire;

    /**
     * 用户注册：校验联系方式与密码，BCrypt 加密后持久化。
     *
     * @param req 注册请求（手机号/邮箱、密码、确认密码、昵称等）
     * @return 已保存的 {@link User} 实体
     * @throws IllegalArgumentException 手机号与邮箱均未填、两次密码不一致、手机号或邮箱已存在
     */
    @Override
    public User register(RegisterRequest req) {
        if ((req.getPhone() == null || req.getPhone().isBlank()) && (req.getEmail() == null || req.getEmail().isBlank())) {
            throw new IllegalArgumentException("手机号或邮箱至少填写一个");
        }
        if (!Objects.equals(req.getPassword(), req.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (req.getPhone() != null && userRepository.findByPhone(req.getPhone()).isPresent()) {
            throw new IllegalArgumentException("手机号已注册");
        }
        if (req.getEmail() != null && userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("邮箱已注册");
        }
        User user = new User();
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setNickname(req.getNickname());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        return userRepository.save(user);
    }

    /**
     * 用户密码登录：校验图形验证码后校验账号密码，签发 JWT 并写入 Redis。
     *
     * @param req 登录请求（account、password、captchaId、captchaCode）
     * @return 包含 token、userId、nickname、principalType、isSuperAdmin 等字段的 Map
     * @throws IllegalArgumentException 验证码错误/过期、用户不存在、密码错误
     */
    @Override
    public Map<String, Object> loginWithPassword(LoginRequest req) {
        if (!captchaService.verifyAndConsume(req.getCaptchaId(), req.getCaptchaCode())) {
            throw new IllegalArgumentException("图形验证码错误或已过期");
        }
        User user = findByAccount(req.getAccount());
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return buildUserLoginResult(user);
    }

    /**
     * 管理员账号密码登录：校验启用状态与明文密码（与业务库中存储一致），签发带超管标识的 JWT。
     *
     * @param req 管理员登录请求（username、password）
     * @return 包含 token、adminId、nickname、principalType、isSuperAdmin 的 Map
     * @throws IllegalArgumentException 账号不存在、已禁用、密码错误
     */
    @Override
    public Map<String, Object> loginAdmin(AdminLoginRequest req) {
        AdminAccount admin = adminAccountRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("管理员账号不存在"));
        if (!Boolean.TRUE.equals(admin.getEnabled())) {
            throw new IllegalArgumentException("管理员账号已禁用");
        }
        if (!Objects.equals(req.getPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        return buildAdminLoginResult(admin);
    }

    /**
     * 校验 JWT 是否与 Redis 中会话一致，可选滑动续期。
     *
     * @param token 请求携带的 Bearer token 原文
     * @return 解析成功且会话有效时返回 {@link AuthPrincipal}，否则返回 {@code null}
     */
    @Override
    public AuthPrincipal validateToken(String token) {
        AuthPrincipal principal;
        try {
            principal = jwtTokenProvider.parsePrincipal(token);
        } catch (Exception e) {
            // JWT 解析失败（过期、签名错误等）视为未登录
            return null;
        }
        String key = tokenPrefix + token;
        String redisSession = stringRedisTemplate.opsForValue().get(key);
        // Redis 中 value 格式：principalType:principalId，须与 JWT 内声明一致
        String expectedSession = principal.getPrincipalType() + ":" + principal.getPrincipalId();
        if (redisSession == null || !redisSession.equals(expectedSession)) {
            return null;
        }
        if (slidingExpire) {
            // 刷新 TTL 为配置的过期秒数，实现滑动过期
            stringRedisTemplate.expire(key, jwtTokenProvider.getExpireSeconds(), TimeUnit.SECONDS);
        }
        return principal;
    }

    /**
     * 登出：删除 Redis 中与该 token 绑定的会话。
     *
     * @param token 要失效的 token 原文
     */
    @Override
    public void logout(String token) {
        stringRedisTemplate.delete(tokenPrefix + token);
    }

    /**
     * 按手机号或邮箱解析用户；优先匹配手机号。
     *
     * @param account 登录账号（手机号或邮箱）
     * @return 匹配到的用户
     * @throws IllegalArgumentException 用户不存在
     */
    private User findByAccount(String account) {
        Optional<User> byPhone = userRepository.findByPhone(account);
        if (byPhone.isPresent()) {
            return byPhone.get();
        }
        return userRepository.findByEmail(account).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /**
     * 构建普通用户登录返回体并写入 Redis 会话。
     *
     * @param user 已认证用户
     * @return 前端所需字段 Map
     */
    private Map<String, Object> buildUserLoginResult(User user) {
        String token = jwtTokenProvider.createUserToken(user.getId());
        stringRedisTemplate.opsForValue().set(
                tokenPrefix + token,
                AuthPrincipal.TYPE_USER + ":" + user.getId(),
                jwtTokenProvider.getExpireSeconds(),
                TimeUnit.SECONDS
        );
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("principalType", AuthPrincipal.TYPE_USER);
        result.put("isSuperAdmin", false);
        return result;
    }

    /**
     * 构建管理员登录返回体并写入 Redis 会话。
     *
     * @param admin 已认证管理员
     * @return 前端所需字段 Map
     */
    private Map<String, Object> buildAdminLoginResult(AdminAccount admin) {
        boolean superAdmin = Boolean.TRUE.equals(admin.getSuperAdmin());
        String token = jwtTokenProvider.createAdminToken(admin.getId(), superAdmin);
        stringRedisTemplate.opsForValue().set(
                tokenPrefix + token,
                AuthPrincipal.TYPE_ADMIN + ":" + admin.getId(),
                jwtTokenProvider.getExpireSeconds(),
                TimeUnit.SECONDS
        );
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminId", admin.getId());
        result.put("nickname", admin.getNickname());
        result.put("principalType", AuthPrincipal.TYPE_ADMIN);
        result.put("isSuperAdmin", superAdmin);
        return result;
    }
}
