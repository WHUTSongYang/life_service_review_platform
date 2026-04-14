package com.lifereview.service;

import com.lifereview.dto.BlogRequest;
import com.lifereview.entity.Blog;

import java.util.List;

/**
 * 业务职责说明：用户动态（博客）的发布、维护、查询与点赞互动。
 */
public interface BlogService {

    /**
     * 发布一条动态。
     *
     * @param userId 当前用户 ID
     * @param req    动态内容请求体
     * @return 新建的博客实体
     */
    Blog create(Long userId, BlogRequest req);

    /**
     * 更新指定动态（通常为作者本人）。
     *
     * @param userId 当前用户 ID
     * @param blogId 动态 ID
     * @param req    更新内容请求体
     * @return 更新后的博客实体
     */
    Blog update(Long userId, Long blogId, BlogRequest req);

    /**
     * 删除指定动态（通常为作者本人）。
     *
     * @param userId 当前用户 ID
     * @param blogId 动态 ID
     */
    void delete(Long userId, Long blogId);

    /**
     * 获取全部动态列表（一般按发布时间倒序）。
     *
     * @return 动态实体列表
     */
    List<Blog> listAll();

    /**
     * 切换当前用户对指定动态的点赞状态。
     *
     * @param userId 当前用户 ID
     * @param blogId 动态 ID
     * @return 切换后的点赞总数
     */
    Integer toggleLike(Long userId, Long blogId);
}
