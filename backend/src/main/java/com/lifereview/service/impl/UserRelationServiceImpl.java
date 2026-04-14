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
 * <p>负责关注、取消关注、关注列表与粉丝列表查询。</p>
 */
@Service
@RequiredArgsConstructor
public class UserRelationServiceImpl implements UserRelationService {

    /** 关注关系仓储 */
    private final FollowRepository followRepository;
    /** 用户仓储 */
    private final UserRepository userRepository;

    /**
     * 当前用户关注另一用户（不可关注自己、不可重复关注）。
     *
     * @param userId       当前用户 ID
     * @param followUserId 被关注用户 ID
     * @throws IllegalArgumentException 关注自己、已关注或数据异常时抛出
     */
    @Override
    public void follow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        // 已存在关注记录则拒绝重复关注
        followRepository.findByUserIdAndFollowUserId(userId, followUserId).ifPresent(v -> {
            throw new IllegalArgumentException("已关注");
        });
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        followRepository.save(follow);
    }

    /**
     * 取消关注。
     *
     * @param userId       当前用户 ID
     * @param followUserId 被取消关注的用户 ID
     * @throws IllegalArgumentException 尚未关注时抛出
     */
    @Override
    public void unfollow(Long userId, Long followUserId) {
        Follow follow = followRepository.findByUserIdAndFollowUserId(userId, followUserId)
                .orElseThrow(() -> new IllegalArgumentException("尚未关注"));
        followRepository.delete(follow);
    }

    /**
     * 查询某用户关注的人列表。
     *
     * @param userId 用户 ID
     * @return 被关注用户实体列表（顺序与关注记录一致）
     * @throws IllegalArgumentException 关联用户不存在时抛出
     */
    @Override
    public List<User> listFollowing(Long userId) {
        return followRepository.findByUserId(userId).stream()
                .map(Follow::getFollowUserId)
                .map(this::findUser)
                .collect(Collectors.toList());
    }

    /**
     * 查询某用户的粉丝列表。
     *
     * @param userId 用户 ID
     * @return 粉丝用户实体列表
     * @throws IllegalArgumentException 关联用户不存在时抛出
     */
    @Override
    public List<User> listFollowers(Long userId) {
        return followRepository.findByFollowUserId(userId).stream()
                .map(Follow::getUserId)
                .map(this::findUser)
                .collect(Collectors.toList());
    }

    /**
     * 按主键查询用户，不存在抛异常。
     *
     * @param id 用户 ID
     * @return 用户实体
     * @throws IllegalArgumentException 不存在时抛出
     */
    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
