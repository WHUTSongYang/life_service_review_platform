package com.lifereview.service;

import com.lifereview.dto.ProductOrderItem;
import com.lifereview.dto.ProductManageCreateRequest;
import com.lifereview.dto.ProductEditRequest;
import com.lifereview.dto.ProductStatusUpdateRequest;
import com.lifereview.dto.ProductStockUpdateRequest;
import com.lifereview.dto.ManageShopItem;
import com.lifereview.dto.ShopProductItem;
import java.util.List;

/**
 * 店铺商品服务接口。
 * 负责商品列表、秒杀、下单、订单管理及商户商品管理。
 */
public interface ShopProductService {

    // 按店铺 ID 查询商品列表（前台展示）
    List<ShopProductItem> listProductsByShop(Long shopId);

    // 查询当前用户可管理的店铺列表
    List<ManageShopItem> listManageShops(Long currentUserId);

    // 查询指定店铺下的管理商品列表，支持关键词搜索
    List<ShopProductItem> listManageProducts(Long currentUserId, Long shopId, String keyword);

    // 创建商品，返回新建的商品项
    ShopProductItem createProduct(Long currentUserId, ProductManageCreateRequest req);

    // 更新商品库存
    ShopProductItem updateStock(Long currentUserId, Long productId, ProductStockUpdateRequest req);

    // 更新商品上下架状态
    ShopProductItem updateStatus(Long currentUserId, Long productId, ProductStatusUpdateRequest req);

    // 编辑商品信息
    ShopProductItem editProduct(Long currentUserId, Long productId, ProductEditRequest req);

    // 秒杀下单，扣减库存并创建订单
    ProductOrderItem purchase(Long currentUserId, Long productId);

    // 查询当前用户的订单列表
    List<ProductOrderItem> listMyOrders(Long currentUserId);

    // 支付指定订单
    ProductOrderItem payOrder(Long currentUserId, Long orderId);

    // 取消指定订单
    ProductOrderItem cancelOrder(Long currentUserId, Long orderId);
}
