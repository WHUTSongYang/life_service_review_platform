package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.HotReviewItem;
import com.lifereview.dto.NearbyShopItem;
import com.lifereview.dto.ReviewCommentCreateRequest;
import com.lifereview.dto.ReviewCommentItem;
import com.lifereview.dto.ReviewDetailItem;
import com.lifereview.dto.ReviewRequest;
import com.lifereview.entity.Review;
import com.lifereview.entity.Shop;
import com.lifereview.service.ShopReviewService;
import com.lifereview.service.ReviewCommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 店铺与点评控制器。
 * 提供店铺列表、详情、搜索、创建、更新；点评列表、发表点评；热门点评；点评评论；附近店铺（需定位）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShopReviewController {
    private final ShopReviewService shopReviewService;
    private final ReviewCommentService reviewCommentService;

    /** 店铺列表。无 keyword/type/page/size 时返回全部并在 X-Total-Count 返回总数；有参数时分页搜索 */
    @GetMapping("/shops")
    public ApiResponse<List<Shop>> listShops(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            HttpServletResponse response
    ) {
        boolean hasSearchCondition = (keyword != null && !keyword.isBlank())
                || (type != null && !type.isBlank())
                || page != null
                || size != null;
        if (!hasSearchCondition) {
            List<Shop> all = shopReviewService.listShops();
            response.setHeader("X-Total-Count", String.valueOf(all.size()));
            return ApiResponse.ok(all);
        }
        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 10 : size;
        ShopReviewService.ShopSearchResult result = shopReviewService.searchShops(safePage, safeSize, keyword, type);
        response.setHeader("X-Total-Count", String.valueOf(result.total()));
        return ApiResponse.ok(result.items());
    }

    /** 创建店铺。需登录，传入店铺信息 */
    @PostMapping("/shops")
    public ApiResponse<Shop> createShop(@RequestBody Shop shop) {
        return ApiResponse.ok(shopReviewService.createShop(shop));
    }

    /** 更新店铺信息。需为店铺店主 */
    @PutMapping("/shops/{shopId}")
    public ApiResponse<Shop> updateShop(@PathVariable("shopId") Long shopId, @RequestBody Shop shop) {
        return ApiResponse.ok(shopReviewService.updateShop(shopId, shop));
    }

    /** 某店铺的点评列表 */
    @GetMapping("/shops/{shopId}/reviews")
    public ApiResponse<List<Review>> listReviews(@PathVariable("shopId") Long shopId) {
        return ApiResponse.ok(shopReviewService.listReviews(shopId));
    }

    /** 店铺详情。含缓存 */
    @GetMapping("/shops/{shopId}")
    public ApiResponse<Shop> shopDetail(@PathVariable("shopId") Long shopId) {
        return ApiResponse.ok(shopReviewService.getShopDetail(shopId));
    }

    // 分页获取热门点评（按点赞数排序）
    @GetMapping("/reviews/hot")
    public ApiResponse<List<HotReviewItem>> listHotReviews(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "shopType", required = false) String shopType
    ) {
        return ApiResponse.ok(shopReviewService.listHotReviews(page, size, keyword, shopType));
    }

    // 分页获取最新点评（按时间排序）
    @GetMapping("/reviews/latest")
    public ApiResponse<List<HotReviewItem>> listLatestReviews(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "shopType", required = false) String shopType
    ) {
        return ApiResponse.ok(shopReviewService.listLatestReviews(page, size, keyword, shopType));
    }

    // 获取点评详情
    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewDetailItem> reviewDetail(@PathVariable("reviewId") Long reviewId) {
        return ApiResponse.ok(shopReviewService.getReviewDetail(reviewId));
    }

    // 获取点评下的评论列表
    @GetMapping("/reviews/{reviewId}/comments")
    public ApiResponse<List<ReviewCommentItem>> listComments(@PathVariable("reviewId") Long reviewId) {
        return ApiResponse.ok(reviewCommentService.listByReviewId(reviewId));
    }

    // 在点评下发表评论
    @PostMapping("/reviews/{reviewId}/comments")
    public ApiResponse<ReviewCommentItem> createComment(
            @PathVariable("reviewId") Long reviewId,
            @Valid @RequestBody ReviewCommentCreateRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(reviewCommentService.create(currentUserId(request), reviewId, req));
    }

    // 获取店铺分类列表
    @GetMapping("/shops/types")
    public ApiResponse<List<String>> listShopTypes() {
        return ApiResponse.ok(shopReviewService.listShopTypes());
    }

    // 按经纬度搜索附近店铺
    @GetMapping("/shops/nearby")
    public ApiResponse<List<NearbyShopItem>> listNearbyShops(
            @RequestParam("longitude") Double longitude,
            @RequestParam("latitude") Double latitude,
            @RequestParam(name = "radiusKm", defaultValue = "5") Double radiusKm,
            @RequestParam(name = "limit", defaultValue = "20") Long limit
    ) {
        return ApiResponse.ok(shopReviewService.listNearbyShops(longitude, latitude, radiusKm, limit));
    }

    // 在指定店铺下发表点评
    @PostMapping("/shops/{shopId}/reviews")
    public ApiResponse<Review> createReview(@PathVariable("shopId") Long shopId, @Valid @RequestBody ReviewRequest req, HttpServletRequest request) {
        return ApiResponse.ok(shopReviewService.createReview(currentUserId(request), shopId, req));
    }

    // 更新点评，仅作者可操作
    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<Review> updateReview(@PathVariable("reviewId") Long reviewId, @Valid @RequestBody ReviewRequest req, HttpServletRequest request) {
        return ApiResponse.ok(shopReviewService.updateReview(currentUserId(request), reviewId, req));
    }

    // 删除点评，仅作者可操作
    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(@PathVariable("reviewId") Long reviewId, HttpServletRequest request) {
        shopReviewService.deleteReview(currentUserId(request), reviewId);
        return ApiResponse.ok(null);
    }

    // 切换点评点赞状态
    @PostMapping("/reviews/{reviewId}/like")
    public ApiResponse<Map<String, Integer>> toggleReviewLike(@PathVariable("reviewId") Long reviewId, HttpServletRequest request) {
        Integer likeCount = shopReviewService.toggleReviewLike(currentUserId(request), reviewId);
        return ApiResponse.ok(Map.of("likeCount", likeCount));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        return (Long) userId;
    }
}
