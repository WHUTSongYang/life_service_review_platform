package com.lifereview.repository;

import com.lifereview.entity.LikeRecord;
import com.lifereview.enums.LikeTargetType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 点赞记录数据访问：对应表 {@code like_records}，按用户与点赞目标（类型+业务 id）维度查询。
 */
@Mapper
public interface LikeRecordRepository extends MybatisBaseRepository<LikeRecord> {
    /**
     * 按用户、点赞目标类型与目标业务 id 查询单条点赞记录。
     *
     * @param userId     用户 id
     * @param targetType 点赞目标类型枚举
     * @param targetId   目标业务主键
     * @return 匹配记录，不存在为 {@code null}
     */
    @Select("""
            select * from like_records
            where user_id = #{userId} and target_type = #{targetType} and target_id = #{targetId}
            limit 1
            """)
    LikeRecord findOneByUserIdAndTargetTypeAndTargetId(@Param("userId") Long userId,
                                                        @Param("targetType") LikeTargetType targetType,
                                                        @Param("targetId") Long targetId);

    /**
     * 同上查询条件，结果封装为 {@link Optional}。
     *
     * @param userId     用户 id
     * @param targetType 点赞目标类型
     * @param targetId   目标业务主键
     * @return 存在则非空，否则为空
     */
    default Optional<LikeRecord> findByUserIdAndTargetTypeAndTargetId(Long userId, LikeTargetType targetType, Long targetId) {
        return Optional.ofNullable(findOneByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId));
    }
}
