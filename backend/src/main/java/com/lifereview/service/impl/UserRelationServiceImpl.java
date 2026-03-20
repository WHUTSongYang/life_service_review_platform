package com.lifereview.service.impl;

import com.lifereview.entity.Follow;
import com.lifereview.entity.User;
import com.lifereview.repository.FollowRepository;
import com.lifereview.repository.UserRepository;
import com.lifereview.service.UserRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户关系服务实现类。
 * 负责关注、取消关注、关注列表、粉丝列表。
 */
@Service
@RequiredArgsConstructor
public class UserRelationServiceImpl implements UserRelationService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Override
    public void follow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        // 已关注则抛异常
        followRepository.findByUserIdAndFollowUserId(userId, followUserId).ifPresent(v -> {
            throw new IllegalArgumentException("已关注");
        });
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        followRepository.save(follow);
    }

    @Override
    public void unfollow(Long userId, Long followUserId) {
        Follow follow = followRepository.findByUserIdAndFollowUserId(userId, followUserId)
                .orElseThrow(() -> new IllegalArgumentException("尚未关注"));
        followRepository.delete(follow);
    }

    @Override
    public List<User> listFollowing(Long userId) {
        return followRepository.findByUserId(userId).stream()
                .map(Follow::getFollowUserId)
                .map(this::findUser)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> listFollowers(Long userId) {
        return followRepository.findByFollowUserId(userId).stream()
                .map(Follow::getUserId)
                .map(this::findUser)
                .collect(Collectors.toList());
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
