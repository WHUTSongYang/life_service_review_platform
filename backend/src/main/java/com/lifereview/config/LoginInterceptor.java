// 包声明：配置类所在包
package com.lifereview.config;

import com.lifereview.dto.AuthPrincipal;
import com.lifereview.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器。
 * 拦截 /api/** 路径，白名单内的请求直接放行，其余需在 Authorization 头携带 Bearer token。
 * 校验通过后将 principalType、principalId、currentUserId 或 currentAdminId、isSuperAdmin 写入 request 属性，供 Controller 使用。
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {
    // 认证服务，用于校验 JWT token
    private final AuthService authService;

    /**
     * 白名单放行：OPTIONS 预检、/api/auth 认证相关、/api/ai AI 接口、部分 GET 公开接口（店铺列表、详情、点评等）、/uploads/ 静态资源。
     * 其他请求需 Bearer token，校验失败返回 401。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取请求 URI 和 HTTP 方法
        String uri = request.getRequestURI();
        String method = request.getMethod();
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        // 认证相关接口（注册、登录等）放行
        if (uri.startsWith("/api/auth")) {
            return true;
        }
        // AI 接口放行
        if (uri.startsWith("/api/ai")) {
            return true;
        }
        // GET 公开接口：店铺列表、类型、热门点评、最新点评、附近店铺、店铺详情、店铺点评、店铺商品、点评详情、点评评论  不需要登录即可查看
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
                return true;
            }
        }
        // 静态资源路径放行
        if (uri.startsWith("/uploads/")) {
            return true;
        }

        // 获取 Authorization 头
        String authHeader = request.getHeader("Authorization");
        // 无 token 或格式错误（非 Bearer）返回 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        // 提取 Bearer 后的 token 字符串
        String token = authHeader.substring(7);
        // 校验 token，解析出用户或管理员身份
        AuthPrincipal principal = authService.validateToken(token);
        // 校验失败返回 401
        if (principal == null) {
            response.setStatus(401);
            return false;
        }
        // 将身份信息写入 request，供后续 Controller 使用
        request.setAttribute("principalType", principal.getPrincipalType());
        request.setAttribute("principalId", principal.getPrincipalId());
        request.setAttribute("isSuperAdmin", principal.isSuperAdmin());
        // 若为普通用户，写入 currentUserId
        if (principal.isUser()) {
            request.setAttribute("currentUserId", principal.getUserId());
        }
        // 若为管理员，写入 currentAdminId
        if (principal.isAdmin()) {
            request.setAttribute("currentAdminId", principal.getAdminId());
        }
        // 放行请求
        return true;
    }
}
