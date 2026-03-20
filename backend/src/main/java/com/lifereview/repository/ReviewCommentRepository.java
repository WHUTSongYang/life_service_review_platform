package com.lifereview.repository;

import com.lifereview.entity.ReviewComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 点评评论表 Mapper */
@Mapper
public interface ReviewCommentRepository extends MybatisBaseRepository<ReviewComment> {
    @Select("select * from review_comments where review_id = #{reviewId} order by created_at asc")
    List<ReviewComment> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

    @Delete("delete from review_comments where review_id = #{reviewId}")
    void deleteByReviewId(Long reviewId);
}
