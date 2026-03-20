package com.lifereview.repository;

import com.lifereview.entity.ShopCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 店铺分类表 Mapper */
@Mapper
public interface ShopCategoryRepository extends MybatisBaseRepository<ShopCategory> {
    @Select("select * from shop_categories where enabled = true order by sort_no asc, id asc")
    List<ShopCategory> findByEnabledTrueOrderBySortNoAscIdAsc();

    @Select("select count(1) from shop_categories where code = #{code}")
    long countByCode(String code);

    default boolean existsByCode(String code) {
        return countByCode(code) > 0;
    }
}
