package com.lifereview.repository;

import com.lifereview.entity.ShopProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 店铺商品表 Mapper */
@Mapper
public interface ShopProductRepository extends MybatisBaseRepository<ShopProduct> {
    List<ShopProduct> findByShopIdAndEnabledTrueOrderByIdDesc(Long shopId);

    List<ShopProduct> searchManageProducts(
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword
    );

    int decreaseStockIfAvailable(@Param("productId") Long productId);

    int increaseStock(@Param("productId") Long productId, @Param("delta") int delta);
}
