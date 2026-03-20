package com.lifereview.repository;

import com.lifereview.entity.Shop;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** 店铺表 Mapper：分类、搜索、按店主查询 */
@Mapper
public interface ShopRepository extends MybatisBaseRepository<Shop> {
    List<String> findDistinctTypes();

    long countByNameAndAddress(@Param("name") String name, @Param("address") String address);

    List<Shop> findByOwnerUserIdOrderByIdDesc(Long ownerUserId);

    List<Shop> searchShopsRecords(@Param("keyword") String keyword,
                                  @Param("type") String type,
                                  @Param("offset") long offset,
                                  @Param("size") int size);

    long countSearchShops(@Param("keyword") String keyword, @Param("type") String type);

    default boolean existsByNameAndAddress(String name, String address) {
        return countByNameAndAddress(name, address) > 0;
    }

    default Page<Shop> searchShops(String keyword, String type, Pageable pageable) {
        List<Shop> records = searchShopsRecords(keyword, type, pageable.getOffset(), pageable.getPageSize());
        long total = countSearchShops(keyword, type);
        return new PageImpl<>(records, pageable, total);
    }
}
