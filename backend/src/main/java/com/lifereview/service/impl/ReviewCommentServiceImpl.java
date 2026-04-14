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
 * <p>负责点评下的评论列表查询与发表评论，含用户昵称展示与存在性校验。</p>
 */
@Service
@RequiredArgsConstructor
public class ReviewCommentServiceImpl implements ReviewCommentService {

    /** 评论数据访问 */
    private final ReviewCommentRepository reviewCommentRepository;
    /** 点评数据访问 */
    private final ReviewRepository reviewRepository;
    /** 用户数据访问 */
    private final UserRepository userRepository;

    /**
     * 按点评 ID 查询评论列表（按创建时间升序，含用户昵称）。
     *
     * @param reviewId 点评主键
     * @return 评论展示项列表，无评论时为空列表
     * @throws IllegalArgumentException 点评不存在时抛出
     */
    @Override
    public List<ReviewCommentItem> listByReviewId(Long reviewId) {
        ensureReviewExists(reviewId);
        List<ReviewComment> comments = reviewCommentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
        if (comments.isEmpty()) {
            return List.of();
        }
        // 批量查询评论用户昵称，避免 N+1
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

    /**
     * 当前用户在某条点评下发表评论。
     *
     * @param currentUserId 当前登录用户 ID
     * @param reviewId      点评主键
     * @param req           评论内容与参数
     * @return 新建评论的展示项（含昵称）
     * @throws IllegalArgumentException 用户或点评不存在时抛出
     */
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

    /**
     * 校验点评存在。
     *
     * @param reviewId 点评主键
     * @throws IllegalArgumentException 点评不存在时抛出
     */
    private void ensureReviewExists(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            throw new IllegalArgumentException("点评不存在");
        }
    }

    /**
     * 校验用户存在。
     *
     * @param userId 用户主键
     * @throws IllegalArgumentException 用户不存在时抛出
     */
    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在");
        }
    }
}
