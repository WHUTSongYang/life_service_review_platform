package com.lifereview.service.impl;

import com.lifereview.dto.BlogRequest;
import com.lifereview.entity.Blog;
import com.lifereview.entity.LikeRecord;
import com.lifereview.enums.LikeTargetType;
import com.lifereview.repository.BlogRepository;
import com.lifereview.repository.LikeRecordRepository;
import com.lifereview.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 博客（动态）服务实现。
 * <p>
 * 提供发布、修改、删除、列表查询及点赞切换；修改与删除仅允许作者本人操作。
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    /** 博客持久化 */
    private final BlogRepository blogRepository;
    /** 点赞记录持久化 */
    private final LikeRecordRepository likeRecordRepository;

    /**
     * 创建博客。
     *
     * @param userId 当前登录用户 id（作者）
     * @param req    标题、正文、图片等
     * @return 保存后的 {@link Blog}
     */
    @Override
    public Blog create(Long userId, BlogRequest req) {
        // 构建新博客实体
        Blog blog = new Blog();
        blog.setUserId(userId);
        blog.setTitle(req.getTitle());
        blog.setContent(req.getContent());
        blog.setImages(req.getImages());
        return blogRepository.save(blog);
    }

    /**
     * 更新博客；仅作者可改。
     *
     * @param userId 当前用户 id
     * @param blogId 博客主键
     * @param req    新标题、正文、图片
     * @return 更新后的 {@link Blog}
     * @throws IllegalArgumentException 博客不存在或非作者操作
     */
    @Override
    public Blog update(Long userId, Long blogId, BlogRequest req) {
        Blog blog = findById(blogId);
        assertOwner(blog.getUserId(), userId);
        // 更新标题、内容、图片
        blog.setTitle(req.getTitle());
        blog.setContent(req.getContent());
        blog.setImages(req.getImages());
        return blogRepository.save(blog);
    }

    /**
     * 删除博客；仅作者可删。
     *
     * @param userId 当前用户 id
     * @param blogId 博客主键
     * @throws IllegalArgumentException 博客不存在或非作者操作
     */
    @Override
    public void delete(Long userId, Long blogId) {
        Blog blog = findById(blogId);
        assertOwner(blog.getUserId(), userId);
        blogRepository.delete(blog);
    }

    /**
     * 按创建时间倒序返回全部博客。
     *
     * @return 博客列表
     */
    @Override
    public List<Blog> listAll() {
        return blogRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 切换当前用户对指定博客的点赞状态。
     *
     * @param userId 当前用户 id
     * @param blogId 博客主键
     * @return 操作后的点赞总数
     * @throws IllegalArgumentException 博客不存在
     */
    @Override
    public Integer toggleLike(Long userId, Long blogId) {
        Blog blog = findById(blogId);
        // 查询用户是否已点赞
        LikeRecord like = likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, LikeTargetType.BLOG, blogId).orElse(null);
        if (like == null) {
            // 未点赞：新增记录并 +1
            LikeRecord entity = new LikeRecord();
            entity.setUserId(userId);
            entity.setTargetType(LikeTargetType.BLOG);
            entity.setTargetId(blogId);
            likeRecordRepository.save(entity);
            blog.setLikeCount(blog.getLikeCount() + 1);
        } else {
            // 已点赞：删除记录并 -1（不低于 0）
            likeRecordRepository.delete(like);
            blog.setLikeCount(Math.max(0, blog.getLikeCount() - 1));
        }
        return blogRepository.save(blog).getLikeCount();
    }

    /**
     * 按主键查询博客。
     *
     * @param blogId 博客 id
     * @return 实体
     * @throws IllegalArgumentException 不存在
     */
    private Blog findById(Long blogId) {
        return blogRepository.findById(blogId).orElseThrow(() -> new IllegalArgumentException("博客不存在"));
    }

    /**
     * 校验操作者是否为资源所有者。
     *
     * @param ownerId 资源所属用户 id
     * @param userId  当前操作者用户 id
     * @throws IllegalArgumentException 非本人
     */
    private void assertOwner(Long ownerId, Long userId) {
        if (!ownerId.equals(userId)) {
            throw new IllegalArgumentException("无权限操作他人内容");
        }
    }
}
