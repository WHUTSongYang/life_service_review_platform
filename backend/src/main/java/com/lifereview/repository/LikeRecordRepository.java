package com.lifereview.repository;

import com.lifereview.entity.LikeRecord;
import com.lifereview.enums.LikeTargetType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/** 点赞记录表 Mapper */
@Mapper
public interface LikeRecordRepository extends MybatisBaseRepository<LikeRecord> {
    @Select("""
            select * from like_records
            where user_id = #{userId} and target_type = #{targetType} and target_id = #{targetId}
            limit 1
            """)
    LikeRecord findOneByUserIdAndTargetTypeAndTargetId(@Param("userId") Long userId,
                                                        @Param("targetType") LikeTargetType targetType,
                                                        @Param("targetId") Long targetId);

    default Optional<LikeRecord> findByUserIdAndTargetTypeAndTargetId(Long userId, LikeTargetType targetType, Long targetId) {
        return Optional.ofNullable(findOneByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId));
    }
}
