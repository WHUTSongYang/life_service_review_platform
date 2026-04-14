package com.lifereview.repository;

import com.lifereview.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 用户点评数据访问：对应表 {@code reviews}（及 XML 映射中的复杂查询），支持按店铺/用户列表、评分统计、全站热门与关键词搜索分页。
 */
@Mapper
public interface ReviewRepository extends MybatisBaseRepository<Review> {
    /**
     * 按店铺查询点评，按创建时间从新到旧排序。
     *
     * @param shopId 店铺 id
     * @return 点评列表
     */
    List<Review> findByShopIdOrderByCreatedAtDesc(Long shopId);

    /**
     * 按用户查询其发表的点评，按创建时间从新到旧排序。
     *
     * @param userId 用户 id
     * @return 点评列表
     */
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 计算指定店铺下全部点评的平均评分。
     *
     * @param shopId 店铺 id
     * @return 平均分，无数据时可能为 {@code null}（依 SQL 实现而定）
     */
    Double calcAvgScoreByShopId(@Param("shopId") Long shopId);

    /**
     * 统计指定店铺下的点评条数。
     *
     * @param shopId 店铺 id
     * @return 点评数量
     */
    long countByShopId(Long shopId);

    /**
     * 全站按点赞数降序、创建时间降序分页取原始记录（配合总数做 {@link Page}）。
     *
     * @param offset 偏移量
     * @param size   每页条数
     * @return 当前页记录列表
     */
    List<Review> findAllByOrderByLikeCountDescCreatedAtDescRecords(@Param("offset") long offset, @Param("size") int size);

    /**
     * 统计全站点评总条数。
     *
     * @return 总记录数
     */
    long countAllReviews();

    /**
     * 按关键词与店铺类型筛选后的「热门」点评分页原始记录。
     *
     * @param keyword  搜索关键词，可为空
     * @param shopType 店铺类型筛选，可为空
     * @param offset   偏移量
     * @param size     每页条数
     * @return 当前页记录
     */
    List<Review> searchHotRecords(@Param("keyword") String keyword,
                                  @Param("shopType") String shopType,
                                  @Param("offset") long offset,
                                  @Param("size") int size);

    /**
     * 热门点评搜索条件下的总条数。
     *
     * @param keyword  搜索关键词
     * @param shopType 店铺类型
     * @return 总条数
     */
    long countHotRecords(@Param("keyword") String keyword, @Param("shopType") String shopType);

    /**
     * 按关键词与店铺类型筛选后的「最新」点评分页原始记录。
     *
     * @param keyword  搜索关键词
     * @param shopType 店铺类型
     * @param offset   偏移量
     * @param size     每页条数
     * @return 当前页记录
     */
    List<Review> searchLatestRecords(@Param("keyword") String keyword,
                                     @Param("shopType") String shopType,
                                     @Param("offset") long offset,
                                     @Param("size") int size);

    /**
     * 最新点评搜索条件下的总条数。
     *
     * @param keyword  搜索关键词
     * @param shopType 店铺类型
     * @return 总条数
     */
    long countLatestRecords(@Param("keyword") String keyword, @Param("shopType") String shopType);

    /**
     * 全站热门点评分页：按点赞与时间排序，总数为全站点评数。
     *
     * @param pageable 分页参数（页码、每页大小等）
     * @return Spring Data {@link Page} 封装结果
     */
    default Page<Review> findAllByOrderByLikeCountDescCreatedAtDesc(Pageable pageable) {
        List<Review> records = findAllByOrderByLikeCountDescCreatedAtDescRecords(pageable.getOffset(), pageable.getPageSize());
        return new PageImpl<>(records, pageable, countAllReviews());
    }

    /**
     * 关键词与店铺类型下的热门点评分页。
     *
     * @param keyword   搜索关键词
     * @param shopType  店铺类型
     * @param pageable  分页参数
     * @return 分页结果
     */
    default Page<Review> searchHotReviews(String keyword, String shopType, Pageable pageable) {
        List<Review> records = searchHotRecords(keyword, shopType, pageable.getOffset(), pageable.getPageSize());
        long total = countHotRecords(keyword, shopType);
        return new PageImpl<>(records, pageable, total);
    }

    /**
     * 关键词与店铺类型下的最新点评分页。
     *
     * @param keyword   搜索关键词
     * @param shopType  店铺类型
     * @param pageable  分页参数
     * @return 分页结果
     */
    default Page<Review> searchLatestReviews(String keyword, String shopType, Pageable pageable) {
        List<Review> records = searchLatestRecords(keyword, shopType, pageable.getOffset(), pageable.getPageSize());
        long total = countLatestRecords(keyword, shopType);
        return new PageImpl<>(records, pageable, total);
    }
}
