package com.lifereview.repository;

import com.lifereview.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** 点评表 Mapper */
@Mapper
public interface ReviewRepository extends MybatisBaseRepository<Review> {
    List<Review> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    Double calcAvgScoreByShopId(@Param("shopId") Long shopId);

    long countByShopId(Long shopId);

    List<Review> findAllByOrderByLikeCountDescCreatedAtDescRecords(@Param("offset") long offset, @Param("size") int size);

    long countAllReviews();

    List<Review> searchHotRecords(@Param("keyword") String keyword,
                                  @Param("shopType") String shopType,
                                  @Param("offset") long offset,
                                  @Param("size") int size);

    long countHotRecords(@Param("keyword") String keyword, @Param("shopType") String shopType);

    List<Review> searchLatestRecords(@Param("keyword") String keyword,
                                     @Param("shopType") String shopType,
                                     @Param("offset") long offset,
                                     @Param("size") int size);

    long countLatestRecords(@Param("keyword") String keyword, @Param("shopType") String shopType);

    default Page<Review> findAllByOrderByLikeCountDescCreatedAtDesc(Pageable pageable) {
        List<Review> records = findAllByOrderByLikeCountDescCreatedAtDescRecords(pageable.getOffset(), pageable.getPageSize());
        return new PageImpl<>(records, pageable, countAllReviews());
    }

    default Page<Review> searchHotReviews(String keyword, String shopType, Pageable pageable) {
        List<Review> records = searchHotRecords(keyword, shopType, pageable.getOffset(), pageable.getPageSize());
        long total = countHotRecords(keyword, shopType);
        return new PageImpl<>(records, pageable, total);
    }

    default Page<Review> searchLatestReviews(String keyword, String shopType, Pageable pageable) {
        List<Review> records = searchLatestRecords(keyword, shopType, pageable.getOffset(), pageable.getPageSize());
        long total = countLatestRecords(keyword, shopType);
        return new PageImpl<>(records, pageable, total);
    }
}
