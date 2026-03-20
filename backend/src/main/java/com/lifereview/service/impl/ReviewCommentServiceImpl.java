package com.lifereview.service.impl;

import com.lifereview.dto.ReviewCommentCreateRequest;
import com.lifereview.dto.ReviewCommentItem;
import com.lifereview.entity.Review;
import com.lifereview.entity.ReviewComment;
import com.lifereview.entity.User;
import com.lifereview.repository.ReviewCommentRepository;
import com.lifereview.repository.ReviewRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.ReviewCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点评评论服务实现类。
 * 负责点评下的评论发表与列表查询。
 */
@Service
@RequiredArgsConstructor
public class ReviewCommentServiceImpl implements ReviewCommentService {

    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    public List<ReviewCommentItem> listByReviewId(Long reviewId) {
        ensureReviewExists(reviewId);
        List<ReviewComment> comments = reviewCommentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
        if (comments.isEmpty()) {
            return List.of();
        }
        // 批量查询评论用户昵称
        Set<Long> userIds = comments.stream().map(ReviewComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        return comments.stream().map(item -> ReviewCommentItem.builder()
                .id(item.getId())
                .reviewId(item.getReviewId())
                .userId(item.getUserId())
                .userNickname(users.containsKey(item.getUserId()) ? users.get(item.getUserId()).getNickname() : "匿名用户")
                .content(item.getContent())
                .createdAt(item.getCreatedAt())
                .build()).toList();
    }

    @Override
    @Transactional
    public ReviewCommentItem create(Long currentUserId, Long reviewId, ReviewCommentCreateRequest req) {
        ensureUserExists(currentUserId);
        ensureReviewExists(reviewId);
        ReviewComment entity = new ReviewComment();
        entity.setReviewId(reviewId);
        entity.setUserId(currentUserId);
        entity.setContent(req.getContent().trim());
        ReviewComment saved = reviewCommentRepository.save(entity);
        User user = userRepository.findById(currentUserId).orElse(null);
        return ReviewCommentItem.builder()
                .id(saved.getId())
                .reviewId(saved.getReviewId())
                .userId(saved.getUserId())
                .userNickname(user == null ? "匿名用户" : user.getNickname())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // 校验点评是否存在
    private void ensureReviewExists(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            throw new IllegalArgumentException("点评不存在");
        }
    }

    // 校验用户是否存在
    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }
}
