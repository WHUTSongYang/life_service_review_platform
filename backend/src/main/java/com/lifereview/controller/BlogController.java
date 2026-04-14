package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.BlogRequest;
import com.lifereview.entity.Blog;
import com.lifereview.service.BlogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 博客/动态控制器。
 * <p>URL 前缀：{@code /api/blogs}。写操作与点赞需登录；列表接口是否匿名以拦截器配置为准。</p>
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    /** 博客 CRUD 与点赞业务服务 */
    private final BlogService blogService;

    /**
     * 创建博客动态。
     *
     * @param req     标题、正文等
     * @param request 当前 HTTP 请求
     * @return 创建后的博客实体
     * @throws IllegalArgumentException 未登录
     */
    @PostMapping
    public ApiResponse<Blog> create(@Valid @RequestBody BlogRequest req, HttpServletRequest request) {
        return ApiResponse.ok(blogService.create(currentUserId(request), req));
    }

    /**
     * 更新博客动态（仅作者）。
     *
     * @param blogId  博客主键
     * @param req     更新字段
     * @param request 当前 HTTP 请求
     * @return 更新后的博客实体
     * @throws IllegalArgumentException 未登录或无权限
     */
    @PutMapping("/{blogId}")
    public ApiResponse<Blog> update(@PathVariable("blogId") Long blogId, @Valid @RequestBody BlogRequest req, HttpServletRequest request) {
        return ApiResponse.ok(blogService.update(currentUserId(request), blogId, req));
    }

    /**
     * 删除博客动态（仅作者）。
     *
     * @param blogId  博客主键
     * @param request 当前 HTTP 请求
     * @return 空数据成功响应
     * @throws IllegalArgumentException 未登录或无权限
     */
    @DeleteMapping("/{blogId}")
    public ApiResponse<Void> delete(@PathVariable("blogId") Long blogId, HttpServletRequest request) {
        blogService.delete(currentUserId(request), blogId);
        return ApiResponse.ok(null);
    }

    /**
     * 获取全部博客列表（按时间倒序）。
     *
     * @return 博客列表
     */
    @GetMapping
    public ApiResponse<List<Blog>> list() {
        return ApiResponse.ok(blogService.listAll());
    }

    /**
     * 切换当前用户对博客的点赞状态。
     *
     * @param blogId  博客主键
     * @param request 当前 HTTP 请求
     * @return 包含最新 likeCount 的 Map
     * @throws IllegalArgumentException 未登录
     */
    @PostMapping("/{blogId}/like")
    public ApiResponse<Map<String, Integer>> toggleLike(@PathVariable("blogId") Long blogId, HttpServletRequest request) {
        Integer likeCount = blogService.toggleLike(currentUserId(request), blogId);
        return ApiResponse.ok(Map.of("likeCount", likeCount));
    }

    /** 从请求属性解析当前登录用户 ID（由拦截器注入），未登录则抛出异常。 */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
