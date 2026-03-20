package com.lifereview.service;

import com.lifereview.dto.ReviewCommentCreateRequest;
import com.lifereview.dto.ReviewCommentItem;

import java.util.List;

/**
 * 点评评论服务接口。
 * 负责点评下的评论发表与列表查询。
 */
public interface ReviewCommentService {

    // 按点评 ID 查询评论列表
    List<ReviewCommentItem> listByReviewId(Long reviewId);

    // 在指定点评下发表评论，返回新建的评论项
    ReviewCommentItem create(Long currentUserId, Long reviewId, ReviewCommentCreateRequest req);
}
