package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.ProductOrderItem;
import com.lifereview.dto.ShopProductItem;
import com.lifereview.service.ShopProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品与订单控制器。
 * 提供商品列表、秒杀下单、订单列表、支付、取消接口，需登录。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShopProductController {

    private final ShopProductService shopProductService;

    // 获取指定店铺的商品列表（前台展示）
    @GetMapping("/shops/{shopId}/products")
    public ApiResponse<List<ShopProductItem>> listShopProducts(@PathVariable("shopId") Long shopId) {
        return ApiResponse.ok(shopProductService.listProductsByShop(shopId));
    }

    // 秒杀购买商品
    @PostMapping("/products/{productId}/purchase")
    public ApiResponse<ProductOrderItem> purchase(@PathVariable("productId") Long productId, HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.purchase(currentUserId(request), productId));
    }

    // 获取当前用户的订单列表
    @GetMapping("/orders/mine")
    public ApiResponse<List<ProductOrderItem>> listMineOrders(HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.listMyOrders(currentUserId(request)));
    }

    // 支付订单
    @PostMapping("/orders/{orderId}/pay")
    public ApiResponse<ProductOrderItem> payOrder(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.payOrder(currentUserId(request), orderId));
    }

    // 取消订单
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<ProductOrderItem> cancelOrder(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.cancelOrder(currentUserId(request), orderId));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
