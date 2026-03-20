package com.lifereview.service;

import com.lifereview.dto.BlogRequest;
import com.lifereview.entity.Blog;

import java.util.List;

/**
 * 博客/动态服务接口。
 * 负责用户动态的发布、修改、删除、列表查询及点赞。
 */
public interface BlogService {

    // 创建博客动态，返回新建的 Blog 实体
    Blog create(Long userId, BlogRequest req);

    // 更新指定博客动态，返回更新后的 Blog
    Blog update(Long userId, Long blogId, BlogRequest req);

    // 删除指定博客动态（仅作者可删）
    void delete(Long userId, Long blogId);

    // 获取所有博客动态列表（按时间倒序）
    List<Blog> listAll();

    // 切换点赞状态，返回当前点赞数
    Integer toggleLike(Long userId, Long blogId);
}
