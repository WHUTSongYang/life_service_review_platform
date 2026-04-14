package com.lifereview.service;

import com.lifereview.dto.ReviewCommentCreateRequest;
import com.lifereview.dto.ReviewCommentItem;

import java.util.List;

/**
 * 业务职责说明：点评下的用户评论展示与发表。
 */
public interface ReviewCommentService {

    /**
     * 按点评 ID 查询其下全部评论。
     *
     * @param reviewId 点评 ID
     * @return 评论项列表
     */
    List<ReviewCommentItem> listByReviewId(Long reviewId);

    /**
     * 在指定点评下发表评论。
     *
     * @param currentUserId 当前登录用户 ID
     * @param reviewId      点评 ID
     * @param req           评论内容请求体
     * @return 新建的评论项
     */
    ReviewCommentItem create(Long currentUserId, Long reviewId, ReviewCommentCreateRequest req);
}
