package com.lifereview.repository;

import com.lifereview.entity.ProductOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/** 商品订单表 Mapper */
@Mapper
public interface ProductOrderRepository extends MybatisBaseRepository<ProductOrder> {
    @Select("select count(1) from product_orders where user_id = #{userId} and product_id = #{productId}")
    long countByUserIdAndProductId(Long userId, Long productId);

    @Select("select * from product_orders where user_id = #{userId} order by created_at desc")
    List<ProductOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("select * from product_orders where id = #{id} and user_id = #{userId} limit 1")
    ProductOrder findOneByIdAndUserId(Long id, Long userId);

    default boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return countByUserIdAndProductId(userId, productId) > 0;
    }

    default Optional<ProductOrder> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(findOneByIdAndUserId(id, userId));
    }
}
