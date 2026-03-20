package com.lifereview.service;

import com.lifereview.entity.User;

import java.util.List;

/**
 * 用户关系服务接口。
 * 负责关注、取关及关注列表、粉丝列表查询。
 */
public interface UserRelationService {

    // 关注指定用户
    void follow(Long userId, Long followUserId);

    // 取消关注指定用户
    void unfollow(Long userId, Long followUserId);

    // 获取当前用户关注的用户列表
    List<User> listFollowing(Long userId);

    // 获取当前用户的粉丝列表
    List<User> listFollowers(Long userId);
}
