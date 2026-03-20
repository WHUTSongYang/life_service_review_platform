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
 * 提供博客的发布、修改、删除、列表查询及点赞接口，需登录。
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    // 创建博客动态，需登录
    @PostMapping
    public ApiResponse<Blog> create(@Valid @RequestBody BlogRequest req, HttpServletRequest request) {
        return ApiResponse.ok(blogService.create(currentUserId(request), req));
    }

    // 更新博客动态，仅作者可操作
    @PutMapping("/{blogId}")
    public ApiResponse<Blog> update(@PathVariable("blogId") Long blogId, @Valid @RequestBody BlogRequest req, HttpServletRequest request) {
        return ApiResponse.ok(blogService.update(currentUserId(request), blogId, req));
    }

    // 删除博客动态，仅作者可操作
    @DeleteMapping("/{blogId}")
    public ApiResponse<Void> delete(@PathVariable("blogId") Long blogId, HttpServletRequest request) {
        blogService.delete(currentUserId(request), blogId);
        return ApiResponse.ok(null);
    }

    // 获取所有博客列表，按时间倒序
    @GetMapping
    public ApiResponse<List<Blog>> list() {
        return ApiResponse.ok(blogService.listAll());
    }

    // 切换点赞状态，返回当前点赞数
    @PostMapping("/{blogId}/like")
    public ApiResponse<Map<String, Integer>> toggleLike(@PathVariable("blogId") Long blogId, HttpServletRequest request) {
        Integer likeCount = blogService.toggleLike(currentUserId(request), blogId);
        return ApiResponse.ok(Map.of("likeCount", likeCount));
    }

    // 从请求属性中获取当前登录用户 ID（由拦截器注入）
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
