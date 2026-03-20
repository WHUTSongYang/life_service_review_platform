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
 * 商品管理控制器。
 * 提供可管理店铺列表、商品列表、新增、编辑、上下架、库存更新接口，需登录。
 */
@RestController
@RequestMapping("/api/products/manage")
@RequiredArgsConstructor
public class ProductManageController {

    private final ShopProductService shopProductService;

    // 获取当前用户可管理的店铺列表
    @GetMapping("/shops")
    public ApiResponse<List<ManageShopItem>> listManageShops(HttpServletRequest request) {
        return ApiResponse.ok(shopProductService.listManageShops(currentUserId(request)));
    }

    // 获取管理商品列表，支持按店铺、关键词筛选
    @GetMapping
    public ApiResponse<List<ShopProductItem>> listProducts(
            @RequestParam(name = "shopId", required = false) Long shopId,
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(shopProductService.listManageProducts(currentUserId(request), shopId, keyword));
    }

    // 创建商品
    @PostMapping
    public ApiResponse<ShopProductItem> createProduct(
            @Valid @RequestBody ProductManageCreateRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(shopProductService.createProduct(currentUserId(request), req));
    }

    // 编辑商品信息
    @PutMapping("/{productId}")
    public ApiResponse<ShopProductItem> editProduct(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductEditRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(shopProductService.editProduct(currentUserId(request), productId, req));
    }

    // 更新商品库存
    @PutMapping("/{productId}/stock")
    public ApiResponse<ShopProductItem> updateStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductStockUpdateRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(shopProductService.updateStock(currentUserId(request), productId, req));
    }

    // 更新商品上下架状态
    @PutMapping("/{productId}/status")
    public ApiResponse<ShopProductItem> updateStatus(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(shopProductService.updateStatus(currentUserId(request), productId, req));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
