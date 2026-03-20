package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.service.ShopSeedService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端种子数据控制器。
 * 提供示例店铺初始化接口，仅管理员可执行。
 */
@RestController
@RequestMapping("/api/admin/seed")
@RequiredArgsConstructor
public class AdminSeedController {

    private final ShopSeedService shopSeedService;

    // 管理员用户 ID 列表，逗号分隔
    @Value("${app.admin.user-ids:1}")
    private String adminUserIds;

    // 执行店铺种子数据初始化
    @PostMapping("/shops")
    public ApiResponse<Map<String, Object>> seedShops(HttpServletRequest request) {
        Long userId = currentUserId(request);
        Set<Long> admins = parseAdminIds();
        if (!admins.contains(userId)) {
            throw new IllegalArgumentException("仅管理员可执行初始化");
        }
        ShopSeedService.SeedResult result = shopSeedService.seedShops();
        return ApiResponse.ok(Map.of(
                "created", result.created(),
                "skipped", result.skipped(),
                "total", result.total()
        ));
    }

    private Set<Long> parseAdminIds() {
        return Arrays.stream(adminUserIds.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
