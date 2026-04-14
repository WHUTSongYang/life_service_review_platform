package com.lifereview.repository;

import com.lifereview.entity.ReviewComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 点评下评论数据访问：对应表 {@code review_comments}，按点评 id 查询与批量删除。
 */
@Mapper
public interface ReviewCommentRepository extends MybatisBaseRepository<ReviewComment> {
    /**
     * 按点评主键查询其下全部评论，按创建时间从早到晚排序。
     *
     * @param reviewId 点评 id
     * @return 评论列表
     */
    @Select("select * from review_comments where review_id = #{reviewId} order by created_at asc")
    List<ReviewComment> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

    /**
     * 删除指定点评下的全部评论记录。
     *
     * @param reviewId 点评 id
     */
    @Delete("delete from review_comments where review_id = #{reviewId}")
    void deleteByReviewId(Long reviewId);
}
