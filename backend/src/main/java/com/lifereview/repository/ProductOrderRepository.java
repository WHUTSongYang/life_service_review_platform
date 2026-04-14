package com.lifereview.repository;

import com.lifereview.entity.ProductOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 商品订单数据访问：对应表 {@code product_orders}，支持按用户/商品统计与订单列表查询。
 */
@Mapper
public interface ProductOrderRepository extends MybatisBaseRepository<ProductOrder> {
    /**
     * 统计指定用户对某商品的订单条数。
     *
     * @param userId    用户 id
     * @param productId 商品 id
     * @return 订单数量
     */
    @Select("select count(1) from product_orders where user_id = #{userId} and product_id = #{productId}")
    long countByUserIdAndProductId(Long userId, Long productId);

    /**
     * 按用户查询订单列表，按创建时间从新到旧排序。
     *
     * @param userId 用户 id
     * @return 订单列表
     */
    @Select("select * from product_orders where user_id = #{userId} order by created_at desc")
    List<ProductOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 按订单主键与用户 id 联合查询单条订单（校验归属）。
     *
     * @param id     订单 id
     * @param userId 用户 id
     * @return 匹配订单，不存在为 {@code null}
     */
    @Select("select * from product_orders where id = #{id} and user_id = #{userId} limit 1")
    ProductOrder findOneByIdAndUserId(Long id, Long userId);

    /**
     * 按用户与商品查询一条订单（若存在多条，由 SQL limit 1 决定其一）。
     *
     * @param userId    用户 id
     * @param productId 商品 id
     * @return 订单实体或 {@code null}
     */
    @Select("select * from product_orders where user_id = #{userId} and product_id = #{productId} limit 1")
    ProductOrder findOneByUserIdAndProductId(Long userId, Long productId);

    /**
     * 查询指定截止时间前创建的待支付订单。
     *
     * @param deadline 截止时间（含）
     * @return 超时待支付订单列表
     */
    @Select("select * from product_orders where status = 'PENDING' and created_at <= #{deadline}")
    List<ProductOrder> findPendingCreatedBefore(@Param("deadline") LocalDateTime deadline);

    /**
     * 仅当订单当前为待支付时，将其更新为已取消。
     *
     * @param orderId 订单 id
     * @return 受影响行数，1 表示成功取消
     */
    @Update("update product_orders set status = 'CANCELLED', updated_at = now() where id = #{orderId} and status = 'PENDING'")
    int cancelIfPending(@Param("orderId") Long orderId);

    /**
     * 判断用户是否已对某商品产生过订单（条数大于 0）。
     *
     * @param userId    用户 id
     * @param productId 商品 id
     * @return 已存在订单为 {@code true}
     */
    default boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return countByUserIdAndProductId(userId, productId) > 0;
    }

    /**
     * 按用户与商品查询订单，封装为 {@link Optional}。
     *
     * @param userId    用户 id
     * @param productId 商品 id
     * @return 存在则非空，否则为空
     */
    default java.util.Optional<ProductOrder> findByUserIdAndProductId(Long userId, Long productId) {
        return java.util.Optional.ofNullable(findOneByUserIdAndProductId(userId, productId));
    }

    /**
     * 按订单 id 与用户 id 查询订单，封装为 {@link Optional}。
     *
     * @param id     订单 id
     * @param userId 用户 id
     * @return 存在则非空，否则为空
     */
    default Optional<ProductOrder> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(findOneByIdAndUserId(id, userId));
    }
}
