package com.lifereview.service;

import com.lifereview.dto.HotReviewItem;
import com.lifereview.dto.UserProfileItem;
import com.lifereview.dto.UserProfileUpdateRequest;

import java.util.List;

/**
 * 业务职责说明：用户个人资料的查询与修改，以及「我的点评」列表查询。
 */
public interface UserProfileService {

    /**
     * 获取用户公开资料信息。
     *
     * @param userId 用户 ID
     * @return 用户资料 DTO
     */
    UserProfileItem getProfile(Long userId);

    /**
     * 更新用户资料（如昵称、手机、邮箱等）。
     *
     * @param userId 用户 ID
     * @param req    资料更新请求体
     * @return 更新后的用户资料 DTO
     */
    UserProfileItem updateProfile(Long userId, UserProfileUpdateRequest req);

    /**
     * 查询当前用户发表的点评列表。
     *
     * @param userId 用户 ID
     * @return 点评项列表（结构与热门点评等列表项一致）
     */
    List<HotReviewItem> listMyReviews(Long userId);
}
