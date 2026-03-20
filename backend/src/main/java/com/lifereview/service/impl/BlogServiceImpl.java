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
 * 博客/动态服务实现类。
 * 负责博客的发布、修改、删除、列表查询及点赞。
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    // 博客数据访问
    private final BlogRepository blogRepository;
    // 点赞记录数据访问
    private final LikeRecordRepository likeRecordRepository;

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

    @Override
    public void delete(Long userId, Long blogId) {
        Blog blog = findById(blogId);
        assertOwner(blog.getUserId(), userId);
        blogRepository.delete(blog);
    }

    @Override
    public List<Blog> listAll() {
        return blogRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Integer toggleLike(Long userId, Long blogId) {
        Blog blog = findById(blogId);
        // 查询用户是否已点赞
        LikeRecord like = likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, LikeTargetType.BLOG, blogId).orElse(null);
        if (like == null) {
            // 未点赞则新增点赞记录并增加计数
            LikeRecord entity = new LikeRecord();
            entity.setUserId(userId);
            entity.setTargetType(LikeTargetType.BLOG);
            entity.setTargetId(blogId);
            likeRecordRepository.save(entity);
            blog.setLikeCount(blog.getLikeCount() + 1);
        } else {
            // 已点赞则删除记录并减少计数
            likeRecordRepository.delete(like);
            blog.setLikeCount(Math.max(0, blog.getLikeCount() - 1));
        }
        return blogRepository.save(blog).getLikeCount();
    }

    // 按 ID 查询博客，不存在则抛异常
    private Blog findById(Long blogId) {
        return blogRepository.findById(blogId).orElseThrow(() -> new IllegalArgumentException("博客不存在"));
    }

    // 校验操作者是否为内容所有者
    private void assertOwner(Long ownerId, Long userId) {
        if (!ownerId.equals(userId)) {
            throw new IllegalArgumentException("无权限操作他人内容");
        }
    }
}
