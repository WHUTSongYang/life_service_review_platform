package com.lifereview.repository;

import com.lifereview.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/** 关注关系表 Mapper */
@Mapper
public interface FollowRepository extends MybatisBaseRepository<Follow> {
    @Select("select * from follows where user_id = #{userId} and follow_user_id = #{followUserId} limit 1")
    Follow findOneByUserIdAndFollowUserId(Long userId, Long followUserId);

    @Select("select * from follows where user_id = #{userId}")
    List<Follow> findByUserId(Long userId);

    @Select("select * from follows where follow_user_id = #{followUserId}")
    List<Follow> findByFollowUserId(Long followUserId);

    default Optional<Follow> findByUserIdAndFollowUserId(Long userId, Long followUserId) {
        return Optional.ofNullable(findOneByUserIdAndFollowUserId(userId, followUserId));
    }
}
