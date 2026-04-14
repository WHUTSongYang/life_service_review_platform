package com.lifereview.repository;

import com.lifereview.entity.ShopCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 店铺经营分类数据访问：对应表 {@code shop_categories}，维护前台展示用的启用分类与编码唯一性校验。
 */
@Mapper
public interface ShopCategoryRepository extends MybatisBaseRepository<ShopCategory> {
    /**
     * 查询所有已启用的分类，先按排序号升序、再按 id 升序。
     *
     * @return 分类列表
     */
    @Select("select * from shop_categories where enabled = true order by sort_no asc, id asc")
    List<ShopCategory> findByEnabledTrueOrderBySortNoAscIdAsc();

    /**
     * 统计指定编码的分类记录数，用于新增/编辑时判断编码是否重复。
     *
     * @param code 分类编码
     * @return 行数
     */
    @Select("select count(1) from shop_categories where code = #{code}")
    long countByCode(String code);

    /**
     * 判断分类编码是否已存在。
     *
     * @param code 分类编码
     * @return 已存在为 {@code true}
     */
    default boolean existsByCode(String code) {
        return countByCode(code) > 0;
    }
}
