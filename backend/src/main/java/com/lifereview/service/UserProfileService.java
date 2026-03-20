package com.lifereview.service;

import com.lifereview.dto.HotReviewItem;
import com.lifereview.dto.UserProfileItem;
import com.lifereview.dto.UserProfileUpdateRequest;

import java.util.List;

/**
 * 用户资料服务接口。
 * 负责用户资料的查询、更新（昵称/手机/邮箱）及我的点评列表。
 */
public interface UserProfileService {

    // 获取用户资料
    UserProfileItem getProfile(Long userId);

    // 更新用户资料
    UserProfileItem updateProfile(Long userId, UserProfileUpdateRequest req);

    // 获取当前用户发表的点评列表
    List<HotReviewItem> listMyReviews(Long userId);
}
