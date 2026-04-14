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
 * 店铺与点评 REST 控制器。
 * <p>URL 根路径：{@code /api} 下多组资源。浏览类接口多可匿名；发点评、评论、点赞、改删点评需登录（由拦截器保证）。</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShopReviewController {

    /** 店铺与点评核心业务服务 */
    private final ShopReviewService shopReviewService;

    /** 点评下二级评论服务 */
    private final ReviewCommentService reviewCommentService;

    /**
     * 店铺列表：无搜索条件时返回全量并在响应头 {@code X-Total-Count} 中给出总数；有参数时走分页搜索。
     *
     * @param keyword 可选，名称等关键词
     * @param type    可选，店铺类型
     * @param page    可选，分页页码（与搜索联用）
     * @param size    可选，分页大小
     * @param response 用于写入 {@code X-Total-Count}
     * @return 店铺列表或搜索结果页数据
     */
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
        // 带筛选条件时走分页搜索，总数写入响应头便于前端分页
        ShopReviewService.ShopSearchResult result = shopReviewService.searchShops(safePage, safeSize, keyword, type);
        response.setHeader("X-Total-Count", String.valueOf(result.total()));
        return ApiResponse.ok(result.items());
    }

    /**
     * 创建店铺（具体权限以服务层为准）。
     *
     * @param shop 店铺实体字段
     * @return 创建后的店铺
     */
    @PostMapping("/shops")
    public ApiResponse<Shop> createShop(@RequestBody Shop shop) {
        return ApiResponse.ok(shopReviewService.createShop(shop));
    }

    /**
     * 更新店铺信息（通常为店主或管理员）。
     *
     * @param shopId 店铺主键
     * @param shop   待更新字段
     * @return 更新后的店铺
     */
    @PutMapping("/shops/{shopId}")
    public ApiResponse<Shop> updateShop(@PathVariable("shopId") Long shopId, @RequestBody Shop shop) {
        return ApiResponse.ok(shopReviewService.updateShop(shopId, shop));
    }

    /**
     * 查询某店铺下的点评列表。
     *
     * @param shopId 店铺主键
     * @return 点评实体列表
     */
    @GetMapping("/shops/{shopId}/reviews")
    public ApiResponse<List<Review>> listReviews(@PathVariable("shopId") Long shopId) {
        return ApiResponse.ok(shopReviewService.listReviews(shopId));
    }

    /**
     * 店铺详情（含缓存）。
     *
     * @param shopId 店铺主键
     * @return 店铺详情
     */
    @GetMapping("/shops/{shopId}")
    public ApiResponse<Shop> shopDetail(@PathVariable("shopId") Long shopId) {
        return ApiResponse.ok(shopReviewService.getShopDetail(shopId));
    }

    /**
     * 分页获取热门点评（按点赞等排序）。
     *
     * @param page     页码，默认 0
     * @param size     每页条数，默认 10
     * @param keyword  可选，内容关键词
     * @param shopType 可选，店铺类型过滤
     * @return 热门点评条目列表
     */
    @GetMapping("/reviews/hot")
    public ApiResponse<List<HotReviewItem>> listHotReviews(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "shopType", required = false) String shopType
    ) {
        return ApiResponse.ok(shopReviewService.listHotReviews(page, size, keyword, shopType));
    }

    /**
     * 分页获取最新点评（按时间排序）。
     *
     * @param page     页码，默认 0
     * @param size     每页条数，默认 10
     * @param keyword  可选，内容关键词
     * @param shopType 可选，店铺类型过滤
     * @return 最新点评条目列表
     */
    @GetMapping("/reviews/latest")
    public ApiResponse<List<HotReviewItem>> listLatestReviews(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "shopType", required = false) String shopType
    ) {
        return ApiResponse.ok(shopReviewService.listLatestReviews(page, size, keyword, shopType));
    }

    /**
     * 点评详情（聚合展示）。
     *
     * @param reviewId 点评主键
     * @return 详情 DTO
     */
    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewDetailItem> reviewDetail(@PathVariable("reviewId") Long reviewId) {
        return ApiResponse.ok(shopReviewService.getReviewDetail(reviewId));
    }

    /**
     * 某点评下的评论列表。
     *
     * @param reviewId 点评主键
     * @return 二级评论条目列表
     */
    @GetMapping("/reviews/{reviewId}/comments")
    public ApiResponse<List<ReviewCommentItem>> listComments(@PathVariable("reviewId") Long reviewId) {
        return ApiResponse.ok(reviewCommentService.listByReviewId(reviewId));
    }

    /**
     * 在指定点评下发表一条评论。
     *
     * @param reviewId 点评主键
     * @param req      评论内容
     * @param request  当前 HTTP 请求
     * @return 创建后的评论条目
     * @throws IllegalArgumentException 未登录
     */
    @PostMapping("/reviews/{reviewId}/comments")
    public ApiResponse<ReviewCommentItem> createComment(
            @PathVariable("reviewId") Long reviewId,
            @Valid @RequestBody ReviewCommentCreateRequest req,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(reviewCommentService.create(currentUserId(request), reviewId, req));
    }

    /**
     * 获取全部店铺类型/分类列表。
     *
     * @return 类型字符串列表
     */
    @GetMapping("/shops/types")
    public ApiResponse<List<String>> listShopTypes() {
        return ApiResponse.ok(shopReviewService.listShopTypes());
    }

    /**
     * 按经纬度与半径查询附近店铺。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param radiusKm  搜索半径（公里），默认 5
     * @param limit     返回条数上限，默认 20
     * @return 附近店铺摘要列表
     */
    @GetMapping("/shops/nearby")
    public ApiResponse<List<NearbyShopItem>> listNearbyShops(
            @RequestParam("longitude") Double longitude,
            @RequestParam("latitude") Double latitude,
            @RequestParam(name = "radiusKm", defaultValue = "5") Double radiusKm,
            @RequestParam(name = "limit", defaultValue = "20") Long limit
    ) {
        return ApiResponse.ok(shopReviewService.listNearbyShops(longitude, latitude, radiusKm, limit));
    }

    /**
     * 在指定店铺下发表点评。
     *
     * @param shopId  店铺主键
     * @param req     评分、正文等
     * @param request 当前 HTTP 请求
     * @return 创建后的点评实体
     * @throws IllegalArgumentException 未登录
     */
    @PostMapping("/shops/{shopId}/reviews")
    public ApiResponse<Review> createReview(@PathVariable("shopId") Long shopId, @Valid @RequestBody ReviewRequest req, HttpServletRequest request) {
        return ApiResponse.ok(shopReviewService.createReview(currentUserId(request), shopId, req));
    }

    /**
     * 更新点评（仅作者）。
     *
     * @param reviewId 点评主键
     * @param req      更新字段
     * @param request  当前 HTTP 请求
     * @return 更新后的点评实体
     * @throws IllegalArgumentException 未登录或无权限
     */
    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<Review> updateReview(@PathVariable("reviewId") Long reviewId, @Valid @RequestBody ReviewRequest req, HttpServletRequest request) {
        return ApiResponse.ok(shopReviewService.updateReview(currentUserId(request), reviewId, req));
    }

    /**
     * 删除点评（仅作者）。
     *
     * @param reviewId 点评主键
     * @param request  当前 HTTP 请求
     * @return 空数据成功响应
     * @throws IllegalArgumentException 未登录或无权限
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(@PathVariable("reviewId") Long reviewId, HttpServletRequest request) {
        shopReviewService.deleteReview(currentUserId(request), reviewId);
        return ApiResponse.ok(null);
    }

    /**
     * 切换当前用户对点评的点赞状态。
     *
     * @param reviewId 点评主键
     * @param request  当前 HTTP 请求
     * @return 包含最新 likeCount 的 Map
     * @throws IllegalArgumentException 未登录
     */
    @PostMapping("/reviews/{reviewId}/like")
    public ApiResponse<Map<String, Integer>> toggleReviewLike(@PathVariable("reviewId") Long reviewId, HttpServletRequest request) {
        Integer likeCount = shopReviewService.toggleReviewLike(currentUserId(request), reviewId);
        return ApiResponse.ok(Map.of("likeCount", likeCount));
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
