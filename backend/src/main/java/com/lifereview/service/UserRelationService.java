package com.lifereview.service;

import com.lifereview.entity.User;

import java.util.List;

/**
 * 业务职责说明：用户之间的关注关系维护及关注列表、粉丝列表查询。
 */
public interface UserRelationService {

    /**
     * 关注指定用户。
     *
     * @param userId        当前用户 ID
     * @param followUserId  被关注用户 ID
     */
    void follow(Long userId, Long followUserId);

    /**
     * 取消关注指定用户。
     *
     * @param userId        当前用户 ID
     * @param followUserId  被取消关注的用户 ID
     */
    void unfollow(Long userId, Long followUserId);

    /**
     * 查询当前用户关注的用户列表。
     *
     * @param userId 当前用户 ID
     * @return 被关注用户实体列表
     */
    List<User> listFollowing(Long userId);

    /**
     * 查询关注当前用户的粉丝列表。
     *
     * @param userId 当前用户 ID
     * @return 粉丝用户实体列表
     */
    List<User> listFollowers(Long userId);
}
