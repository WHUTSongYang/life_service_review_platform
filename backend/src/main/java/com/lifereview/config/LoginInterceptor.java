package com.lifereview.config;

import com.lifereview.dto.AuthPrincipal;
import com.lifereview.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录态拦截器。
 * <p>
 * 对 {@code /api/**} 请求在到达 Controller 前进行 JWT 校验：白名单路径直接放行；
 * 其余请求须在 {@code Authorization} 头携带 {@code Bearer} Token。校验成功后将
 * {@code principalType}、{@code principalId}、{@code currentUserId} 或 {@code currentAdminId}、
 * {@code isSuperAdmin} 写入 request 属性，供后续控制器读取。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    /** 认证服务，用于解析与校验 JWT */
    private final AuthService authService;

    /**
     * 在 Controller 处理前执行：白名单放行或校验 Bearer Token 并注入身份信息。
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应（未登录或 Token 无效时写入 401）
     * @param handler  即将调用的处理器（本实现未使用）
     * @return {@code true} 表示继续处理链；{@code false} 表示已响应客户端并终止
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true; // CORS 预检直接放行
        }
        if (uri.startsWith("/api/auth")) {
            return true; // 注册、登录等认证接口放行
        }
        if (uri.startsWith("/api/ai")) {
            return true; // AI 相关接口放行
        }
        if ("GET".equalsIgnoreCase(method)) {
            if ("/api/shops".equals(uri)
                    || "/api/shops/types".equals(uri)
                    || "/api/reviews/hot".equals(uri)
                    || "/api/reviews/latest".equals(uri)
                    || "/api/shops/nearby".equals(uri)
                    || uri.matches("^/api/shops/\\d+$")
                    || uri.matches("^/api/shops/\\d+/reviews$")
                    || uri.matches("^/api/shops/\\d+/products$")
                    || uri.matches("^/api/reviews/\\d+$")
                    || uri.matches("^/api/reviews/\\d+/comments$")) {
                return true; // 公开只读资源无需登录
            }
        }
        if (uri.startsWith("/uploads/")) {
            return true; // 本地上传静态资源映射放行
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401); // 缺少或格式错误的凭证
            return false;
        }
        String token = authHeader.substring(7);
        AuthPrincipal principal = authService.validateToken(token);
        if (principal == null) {
            response.setStatus(401); // Token 无效或已过期
            return false;
        }
        request.setAttribute("principalType", principal.getPrincipalType());
        request.setAttribute("principalId", principal.getPrincipalId());
        request.setAttribute("isSuperAdmin", principal.isSuperAdmin());
        if (principal.isUser()) {
            request.setAttribute("currentUserId", principal.getUserId()); // 普通用户 ID
        }
        if (principal.isAdmin()) {
            request.setAttribute("currentAdminId", principal.getAdminId()); // 管理员 ID
        }
        return true;
    }
}
