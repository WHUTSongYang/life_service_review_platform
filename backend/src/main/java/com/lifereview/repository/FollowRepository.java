package com.lifereview.repository;

import com.lifereview.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 用户关注关系数据访问：对应表 {@code follows}，维护关注者与被关注者之间的关联。
 */
@Mapper
public interface FollowRepository extends MybatisBaseRepository<Follow> {
    /**
     * 查询指定用户是否已关注另一用户（单条关系）。
     *
     * @param userId       关注方用户 id
     * @param followUserId 被关注方用户 id
     * @return 存在关系时返回实体，否则 {@code null}
     */
    @Select("select * from follows where user_id = #{userId} and follow_user_id = #{followUserId} limit 1")
    Follow findOneByUserIdAndFollowUserId(Long userId, Long followUserId);

    /**
     * 查询某用户主动关注的全部关系（我关注的人）。
     *
     * @param userId 关注方用户 id
     * @return 关注关系列表
     */
    @Select("select * from follows where user_id = #{userId}")
    List<Follow> findByUserId(Long userId);

    /**
     * 查询关注指定用户的全部关系（粉丝列表）。
     *
     * @param followUserId 被关注方用户 id
     * @return 粉丝关系列表
     */
    @Select("select * from follows where follow_user_id = #{followUserId}")
    List<Follow> findByFollowUserId(Long followUserId);

    /**
     * 查询两人之间的关注关系是否存在，用 {@link Optional} 表示。
     *
     * @param userId       关注方用户 id
     * @param followUserId 被关注方用户 id
     * @return 存在则非空，否则为空
     */
    default Optional<Follow> findByUserIdAndFollowUserId(Long userId, Long followUserId) {
        return Optional.ofNullable(findOneByUserIdAndFollowUserId(userId, followUserId));
    }
}
