package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.ProductOrderItem;
import com.lifereview.dto.SeckillPurchaseQueryResult;
import com.lifereview.dto.SeckillPurchaseSubmitResult;
import com.lifereview.dto.ShopProductItem;
import com.lifereview.service.ShopProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台商品与订单控制器。
 * <p>URL 根路径：{@code /api} 下多个子路径。商品列表可匿名；下单、订单、支付、取消需登录。</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShopProductController {

    /** 商品展示、秒杀下单与订单流转业务服务 */
    private final ShopProductService shopProductService;

    /**
     * 按店铺查询在售商品列表（前台展示）。
     *
     * @param shopId 店铺主键
     * @return 商品条目列表
     */
    @GetMapping("/shops/{shopId}/products")
    public ApiResponse<List<ShopProductItem>> listShopProducts(@PathVariable("shopId") Long shopId) {
        return ApiResponse.ok(shopProductService.listProductsByShop(shopId));
    }

    /**
     * 秒杀/抢购下单：异步（Kafka）时返回 202 与 requestId；同步时直接返回订单。
     *
     * @param productId 商品主键
     * @param request   当前 HTTP 请求
     * @return HTTP 200 且 body 为订单，或 HTTP 202 且 body 含异步 requestId
     * @throws IllegalArgumentException 未登录或业务失败
     */
    @PostMapping("/products/{productId}/purchase")
    public ResponseEntity<ApiResponse<?>> purchase(@PathVariable("productId") Long productId, HttpServletRequest request) {
        SeckillPurchaseSubmitResult result = shopProductService.submitSeckillPurchase(currentUserId(request), productId);
        if (result.isAsync()) {
            // 异步链路：仅返回 requestId，客户端轮询查询结果
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("requestId", result.getRequestId());
            data.put("status", "PENDING");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(data));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.getOrder()));
    }

    /**
     * 根据 requestId 查询异步秒杀下单结果。
     *
     * @param requestId 下单时返回的请求 ID
     * @param request     当前 HTTP 请求
     * @return 查询结果（成功/失败/进行中）
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/products/purchase-requests/{requestId}")
    public ApiResponse<SeckillPurchaseQueryResult> getSeckillPurchaseResult(
            @PathVariable("requestId") String requestId,
            HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.getSeckillPurchaseResult(currentUserId(request), requestId));
    }

    /**
     * 当前登录用户的订单列表。
     *
     * @param request 当前 HTTP 请求
     * @return 订单条目列表
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/orders/mine")
    public ApiResponse<List<ProductOrderItem>> listMineOrders(HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.listMyOrders(currentUserId(request)));
    }

    /**
     * 支付指定订单（业务模拟或对接支付后的状态更新）。
     *
     * @param orderId 订单主键
     * @param request 当前 HTTP 请求
     * @return 更新后的订单条目
     * @throws IllegalArgumentException 未登录或状态非法
     */
    @PostMapping("/orders/{orderId}/pay")
    public ApiResponse<ProductOrderItem> payOrder(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.payOrder(currentUserId(request), orderId));
    }

    /**
     * 取消订单。
     *
     * @param orderId 订单主键
     * @param request 当前 HTTP 请求
     * @return 更新后的订单条目
     * @throws IllegalArgumentException 未登录或状态不允许取消
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<ProductOrderItem> cancelOrder(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.cancelOrder(currentUserId(request), orderId));
    }

    /** 从请求属性解析当前登录用户 ID，未登录则抛出异常。 */
    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
