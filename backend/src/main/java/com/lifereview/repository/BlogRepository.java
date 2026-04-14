package com.lifereview.repository;

import com.lifereview.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 博客/动态数据访问：对应表 {@code blogs}，提供按时间排序的列表查询。
 */
@Mapper
public interface BlogRepository extends MybatisBaseRepository<Blog> {
    /**
     * 查询全部博客记录，按创建时间从新到旧排序。
     *
     * @return 博客列表
     */
    @Select("select * from blogs order by created_at desc")
    List<Blog> findAllByOrderByCreatedAtDesc();
}
