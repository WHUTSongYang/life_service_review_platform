package com.lifereview.repository;

import com.lifereview.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 博客/动态表 Mapper */
@Mapper
public interface BlogRepository extends MybatisBaseRepository<Blog> {
    @Select("select * from blogs order by created_at desc")
    List<Blog> findAllByOrderByCreatedAtDesc();
}
