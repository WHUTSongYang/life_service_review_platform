package com.lifereview.service;

import com.lifereview.dto.ProductOrderItem;
import com.lifereview.dto.ProductManageCreateRequest;
import com.lifereview.dto.SeckillPurchaseMessage;
import com.lifereview.dto.SeckillPurchaseQueryResult;
import com.lifereview.dto.SeckillPurchaseSubmitResult;
import com.lifereview.dto.ProductEditRequest;
import com.lifereview.dto.ProductStatusUpdateRequest;
import com.lifereview.dto.ProductStockUpdateRequest;
import com.lifereview.dto.ManageShopItem;
import com.lifereview.dto.ShopProductItem;
import java.util.List;

/**
 * 业务职责说明：店铺商品的展示、商户端商品维护、秒杀下单与订单支付/取消等交易流程。
 */
public interface ShopProductService {

    /**
     * 按店铺 ID 查询前台可展示的商品列表。
     *
     * @param shopId 店铺 ID
     * @return 商品项列表
     */
    List<ShopProductItem> listProductsByShop(Long shopId);

    /**
     * 查询当前用户具备管理权限的店铺列表。
     *
     * @param currentUserId 当前登录用户 ID
     * @return 可管理店铺项列表
     */
    List<ManageShopItem> listManageShops(Long currentUserId, boolean superAdmin);

    /**
     * 管理端查询指定店铺下的商品，支持关键词过滤。
     *
     * @param currentUserId 当前登录用户 ID
     * @param shopId        店铺 ID
     * @param keyword       关键词，可为空表示不过滤
     * @return 商品项列表
     */
    List<ShopProductItem> listManageProducts(Long currentUserId, boolean superAdmin, Long shopId, String keyword);

    /**
     * 创建新商品。
     *
     * @param currentUserId 当前登录用户 ID
     * @param req           商品创建请求体
     * @return 新建商品项 DTO
     */
    ShopProductItem createProduct(Long currentUserId, boolean superAdmin, ProductManageCreateRequest req);

    /**
     * 更新商品库存。
     *
     * @param currentUserId 当前登录用户 ID
     * @param productId     商品 ID
     * @param req           库存更新请求体
     * @return 更新后的商品项 DTO
     */
    ShopProductItem updateStock(Long currentUserId, boolean superAdmin, Long productId, ProductStockUpdateRequest req);

    /**
     * 更新商品上下架等业务状态。
     *
     * @param currentUserId 当前登录用户 ID
     * @param productId     商品 ID
     * @param req           状态更新请求体
     * @return 更新后的商品项 DTO
     */
    ShopProductItem updateStatus(Long currentUserId, boolean superAdmin, Long productId, ProductStatusUpdateRequest req);

    /**
     * 编辑商品基本信息（名称、价格等）。
     *
     * @param currentUserId 当前登录用户 ID
     * @param productId     商品 ID
     * @param req           编辑请求体
     * @return 更新后的商品项 DTO
     */
    ShopProductItem editProduct(Long currentUserId, boolean superAdmin, Long productId, ProductEditRequest req);

    /**
     * 提交秒杀购买请求；若 Kafka 等异步通道开启则投递消息后异步建单，否则同步建单。
     *
     * @param currentUserId 当前登录用户 ID
     * @param productId     秒杀商品 ID
     * @return 受理结果（含 requestId 等供异步轮询使用）
     */
    SeckillPurchaseSubmitResult submitSeckillPurchase(Long currentUserId, Long productId);

    /**
     * 根据请求 ID 查询异步秒杀处理结果（供前端轮询）。
     *
     * @param currentUserId 当前登录用户 ID
     * @param requestId     提交秒杀时返回的请求标识
     * @return 查询结果（成功、处理中或失败原因等）
     */
    SeckillPurchaseQueryResult getSeckillPurchaseResult(Long currentUserId, String requestId);

    /**
     * 由消息消费者调用：根据队列消息执行秒杀建单并回写 Redis 等存储中的结果。
     *
     * @param message 秒杀购买消息体
     */
    void processSeckillPurchaseMessage(SeckillPurchaseMessage message);

    /**
     * 在单线程/事务边界内同步执行秒杀扣减与建单（分布式锁、Lua、数据库等由实现类编排）。
     *
     * @param userId    下单用户 ID
     * @param productId 商品 ID
     * @return 生成的订单项信息
     */
    ProductOrderItem executeSeckillPurchase(Long userId, Long productId);

    /**
     * 查询当前用户的商品订单列表。
     *
     * @param currentUserId 当前登录用户 ID
     * @return 订单项列表
     */
    List<ProductOrderItem> listMyOrders(Long currentUserId);

    /**
     * 将指定订单标记为已支付（或触发支付成功后的业务逻辑）。
     *
     * @param currentUserId 当前登录用户 ID
     * @param orderId       订单 ID
     * @return 更新后的订单项
     */
    ProductOrderItem payOrder(Long currentUserId, Long orderId);

    /**
     * 取消指定订单。
     *
     * @param currentUserId 当前登录用户 ID
     * @param orderId       订单 ID
     * @return 更新后的订单项
     */
    ProductOrderItem cancelOrder(Long currentUserId, Long orderId);
}
