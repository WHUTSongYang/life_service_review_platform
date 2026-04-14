package com.lifereview.repository;

import com.lifereview.entity.Shop;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 店铺主数据访问：对应表 {@code shops}（及 XML 映射），支持类型去重、店主维度查询、关键词搜索分页与推荐用高分店铺截取。
 */
@Mapper
public interface ShopRepository extends MybatisBaseRepository<Shop> {
    /**
     * 查询当前库中所有不重复的店铺类型/分类名称。
     *
     * @return 类型名称列表
     */
    List<String> findDistinctTypes();

    /**
     * 统计同名且同地址的店铺数量，用于入驻或编辑时唯一性校验。
     *
     * @param name    店铺名称
     * @param address 店铺地址
     * @return 匹配行数
     */
    long countByNameAndAddress(@Param("name") String name, @Param("address") String address);

    /**
     * 按店主用户 id 查询其名下店铺，按店铺 id 从新到旧排序。
     *
     * @param ownerUserId 店主用户 id
     * @return 店铺列表
     */
    List<Shop> findByOwnerUserIdOrderByIdDesc(Long ownerUserId);

    /**
     * 按关键词与类型筛选的店铺搜索分页原始记录。
     *
     * @param keyword 搜索关键词
     * @param type    店铺类型
     * @param offset  偏移量
     * @param size    每页条数
     * @return 当前页店铺列表
     */
    List<Shop> searchShopsRecords(@Param("keyword") String keyword,
                                  @Param("type") String type,
                                  @Param("offset") long offset,
                                  @Param("size") int size);

    /**
     * 店铺搜索条件下的总条数。
     *
     * @param keyword 搜索关键词
     * @param type    店铺类型
     * @return 总条数
     */
    long countSearchShops(@Param("keyword") String keyword, @Param("type") String type);

    /**
     * 按平均评分与点评数等规则排序，取前若干条店铺（如 AI 推荐、榜单）。
     *
     * @param limit 最大返回条数
     * @return 店铺列表
     */
    List<Shop> findTopByAvgScoreDesc(@Param("limit") int limit);

    /**
     * 判断是否存在同名同址店铺。
     *
     * @param name    店铺名称
     * @param address 店铺地址
     * @return 已存在为 {@code true}
     */
    default boolean existsByNameAndAddress(String name, String address) {
        return countByNameAndAddress(name, address) > 0;
    }

    /**
     * 按关键词与类型分页搜索店铺。
     *
     * @param keyword   搜索关键词
     * @param type      店铺类型
     * @param pageable  分页参数
     * @return 分页结果
     */
    default Page<Shop> searchShops(String keyword, String type, Pageable pageable) {
        List<Shop> records = searchShopsRecords(keyword, type, pageable.getOffset(), pageable.getPageSize());
        long total = countSearchShops(keyword, type);
        return new PageImpl<>(records, pageable, total);
    }
}
