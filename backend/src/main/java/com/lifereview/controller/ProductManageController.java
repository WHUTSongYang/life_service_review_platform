package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.ManageShopItem;
import com.lifereview.dto.ProductEditRequest;
import com.lifereview.dto.ProductManageCreateRequest;
import com.lifereview.dto.ProductStatusUpdateRequest;
import com.lifereview.dto.ProductStockUpdateRequest;
import com.lifereview.dto.ShopProductItem;
import com.lifereview.service.ShopProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家商品管理控制器。
 * <p>URL 前缀：{@code /api/products/manage}。仅店铺可管理者可访问对应资源，需登录。</p>
 */
@RestController
@RequestMapping("/api/products/manage")
@RequiredArgsConstructor
public class ProductManageController {

    /** 商品与订单相关业务服务（管理端能力） */
    private final ShopProductService shopProductService;

    /**
     * 获取当前用户具备管理权限的店铺列表。
     *
     * @param request 当前 HTTP 请求
     * @return 可管理店铺摘要列表
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping("/shops")
    public ApiResponse<List<ManageShopItem>> listManageShops(HttpServletRequest request) {
        boolean superAdmin = isSuperAdmin(request);
        return ApiResponse.ok(shopProductService.listManageShops(currentUserId(request, superAdmin), superAdmin));
    }

    /**
     * 分页/筛选查询可管理商品列表。
     *
     * @param shopId  可选，限定店铺
     * @param keyword 可选，标题等关键词
     * @param request 当前 HTTP 请求
     * @return 商品条目列表
     * @throws IllegalArgumentException 未登录
     */
    @GetMapping
    public ApiResponse<List<ShopProductItem>> listProducts(
            @RequestParam(name = "shopId", required = false) Long shopId,
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpServletRequest request
    ) {
        boolean superAdmin = isSuperAdmin(request);
        return ApiResponse.ok(shopProductService.listManageProducts(currentUserId(request, superAdmin), superAdmin, shopId, keyword));
    }

    /**
     * 在可管理店铺下创建商品。
     *
     * @param req     商品字段
     * @param request 当前 HTTP 请求
     * @return 创建后的商品条目
     * @throws IllegalArgumentException 未登录或无权限
     */
    @PostMapping
    public ApiResponse<ShopProductItem> createProduct(
            @Valid @RequestBody ProductManageCreateRequest req,
            HttpServletRequest request
    ) {
        boolean superAdmin = isSuperAdmin(request);
        return ApiResponse.ok(shopProductService.createProduct(currentUserId(request, superAdmin), superAdmin, req));
    }

    /**
     * 编辑商品信息。
     *
     * @param productId 商品主键
     * @param req       可编辑字段
     * @param request   当前 HTTP 请求
     * @return 更新后的商品条目
     * @throws IllegalArgumentException 未登录或无权限
     */
    @PutMapping("/{productId}")
    public ApiResponse<ShopProductItem> editProduct(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductEditRequest req,
            HttpServletRequest request
    ) {
        boolean superAdmin = isSuperAdmin(request);
        return ApiResponse.ok(shopProductService.editProduct(currentUserId(request, superAdmin), superAdmin, productId, req));
    }

    /**
     * 更新商品库存。
     *
     * @param productId 商品主键
     * @param req       库存数量等
     * @param request   当前 HTTP 请求
     * @return 更新后的商品条目
     * @throws IllegalArgumentException 未登录或无权限
     */
    @PutMapping("/{productId}/stock")
    public ApiResponse<ShopProductItem> updateStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductStockUpdateRequest req,
            HttpServletRequest request
    ) {
        boolean superAdmin = isSuperAdmin(request);
        return ApiResponse.ok(shopProductService.updateStock(currentUserId(request, superAdmin), superAdmin, productId, req));
    }

    /**
     * 更新商品上下架状态。
     *
     * @param productId 商品主键
     * @param req       状态枚举等
     * @param request   当前 HTTP 请求
     * @return 更新后的商品条目
     * @throws IllegalArgumentException 未登录或无权限
     */
    @PutMapping("/{productId}/status")
    public ApiResponse<ShopProductItem> updateStatus(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest req,
            HttpServletRequest request
    ) {
        boolean superAdmin = isSuperAdmin(request);
        return ApiResponse.ok(shopProductService.updateStatus(currentUserId(request, superAdmin), superAdmin, productId, req));
    }

    /** 从请求属性解析当前登录用户 ID，未登录则抛出异常。 */
    private Long currentUserId(HttpServletRequest request, boolean superAdmin) {
        Object userId = request.getAttribute("currentUserId");
        if (userId != null) {
            return (Long) userId;
        }
        if (superAdmin) {
            return null;
        }
        throw new IllegalArgumentException("未登录");
    }

    /** 判断当前请求是否来自超级管理员。 */
    private boolean isSuperAdmin(HttpServletRequest request) {
        Object value = request.getAttribute("isSuperAdmin");
        return Boolean.TRUE.equals(value);
    }
}
