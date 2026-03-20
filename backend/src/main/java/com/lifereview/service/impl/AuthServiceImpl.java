package com.lifereview.service.impl;

import com.lifereview.dto.AdminLoginRequest;
import com.lifereview.dto.AuthPrincipal;
import com.lifereview.dto.CodeLoginRequest;
import com.lifereview.dto.LoginRequest;
import com.lifereview.dto.RegisterRequest;
import com.lifereview.entity.AdminAccount;
import com.lifereview.entity.User;
import com.lifereview.repository.AdminAccountRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.AuthService;
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
 * 支持用户密码登录、验证码登录、管理员登录；用户注册；JWT 签发与校验；Redis 存储会话实现滑动过期。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 中 token 的 key 前缀，如 login:token: */
    @Value("${app.session.redis-prefix}")
    private String tokenPrefix;

    /** 是否启用滑动过期：每次校验 token 时顺延过期时间 */
    @Value("${app.session.sliding-expire:true}")
    private boolean slidingExpire;

    /** 用户注册。校验手机号/邮箱至少填一个、密码一致、手机号邮箱不重复，密码 BCrypt 加密后入库 */
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

    /** 密码登录。account 支持手机号或邮箱，校验密码后签发 JWT 并写入 Redis */
    @Override
    public Map<String, Object> loginWithPassword(LoginRequest req) {
        User user = findByAccount(req.getAccount());
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return buildUserLoginResult(user);
    }

    /** 验证码登录。当前验证码写死 123456，通过后签发 JWT 并写入 Redis */
    @Override
    public Map<String, Object> loginWithCode(CodeLoginRequest req) {
        if (!"123456".equals(req.getCode())) {
            throw new IllegalArgumentException("验证码错误");
        }
        User user = findByAccount(req.getAccount());
        return buildUserLoginResult(user);
    }

    /** 管理员登录。校验用户名、密码、enabled 状态，签发带 superAdmin 标识的 JWT */
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

    /** 校验 token。解析 JWT、校验 Redis 会话存在且一致，可选滑动续期，返回 AuthPrincipal 或 null */
    @Override
    public AuthPrincipal validateToken(String token) {
        AuthPrincipal principal;
        try {
            principal = jwtTokenProvider.parsePrincipal(token);
        } catch (Exception e) {
            return null;
        }
        String key = tokenPrefix + token;
        String redisSession = stringRedisTemplate.opsForValue().get(key);
        String expectedSession = principal.getPrincipalType() + ":" + principal.getPrincipalId();          //value  存放的是账户类型 和id
        if (redisSession == null || !redisSession.equals(expectedSession)) {
            return null;
        }
        if (slidingExpire) {   //如果开启了滑动过期         jwtTokenProvider.getExpireSeconds()返回配置的 token 过期秒数   重新设置 key 的过期时间为「当前时间 + 过期秒数」
            stringRedisTemplate.expire(key, jwtTokenProvider.getExpireSeconds(), TimeUnit.SECONDS);
        }
        return principal;
    }

    /** 登出。从 Redis 删除该 token 对应的会话 */
    @Override
    public void logout(String token) {
        stringRedisTemplate.delete(tokenPrefix + token);
    }

    /** 按手机号或邮箱查找用户，先查手机号再查邮箱 */
    private User findByAccount(String account) {
        Optional<User> byPhone = userRepository.findByPhone(account);
        if (byPhone.isPresent()) {
            return byPhone.get();
        }
        return userRepository.findByEmail(account).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /** 构建用户登录结果：签发 USER 类型 token，写入 Redis，返回 token、userId、nickname、principalType */
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

    /** 构建管理员登录结果：签发 ADMIN 类型 token 并写入 Redis，返回 token、adminId、nickname、principalType、isSuperAdmin */
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
