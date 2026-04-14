package com.lifereview.repository;

import com.lifereview.entity.ShopProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 店铺商品数据访问：对应表 {@code shop_products}（及 XML），支持 C 端上架列表、管理端搜索与原子库存扣减/回加。
 */
@Mapper
public interface ShopProductRepository extends MybatisBaseRepository<ShopProduct> {
    /**
     * 查询指定店铺下已启用商品，按商品 id 从新到旧排序。
     *
     * @param shopId 店铺 id
     * @return 商品列表
     */
    List<ShopProduct> findByShopIdAndEnabledTrueOrderByIdDesc(Long shopId);

    /**
     * 管理端按店铺与关键词模糊搜索商品（具体字段由 Mapper XML 定义）。
     *
     * @param shopId  店铺 id
     * @param keyword 关键词，可为空表示不限
     * @return 匹配商品列表
     */
    List<ShopProduct> searchManageProducts(
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword
    );

    /**
     * 在库存充足时扣减一行商品库存，用于秒杀/下单等场景；返回受影响行数判断是否扣减成功。
     *
     * @param productId 商品 id
     * @return 更新行数，0 表示未扣减（库存不足或记录不存在）
     */
    int decreaseStockIfAvailable(@Param("productId") Long productId);

    /**
     * 按增量回加商品库存（如订单取消）。
     *
     * @param productId 商品 id
     * @param delta     增加的库存数量（正数）
     * @return 受影响行数
     */
    int increaseStock(@Param("productId") Long productId, @Param("delta") int delta);
}
