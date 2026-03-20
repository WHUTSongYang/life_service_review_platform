package com.lifereview.service;

import com.lifereview.dto.ReviewRequest;
import com.lifereview.dto.ShopManageUpdateRequest;
import com.lifereview.dto.HotReviewItem;
import com.lifereview.dto.NearbyShopItem;
import com.lifereview.dto.ReviewDetailItem;
import com.lifereview.entity.Review;
import com.lifereview.entity.Shop;
import java.util.List;

/**
 * 店铺与点评服务接口。
 * 负责店铺列表、详情、搜索、点评 CRUD、热门点评、附近店铺等。
 */
public interface ShopReviewService {

    // 获取所有店铺列表
    List<Shop> listShops();

    // 分页搜索店铺，支持关键词和分类筛选
    ShopSearchResult searchShops(int page, int size, String keyword, String type);

    // 获取店铺详情（含缓存）
    Shop getShopDetail(Long shopId);

    // 创建店铺
    Shop createShop(Shop shop);

    // 更新店铺信息
    Shop updateShop(Long shopId, Shop req);

    // 获取指定店铺的点评列表
    List<Review> listReviews(Long shopId);

    // 分页获取热门点评（按点赞数排序）
    List<HotReviewItem> listHotReviews(int page, int size, String keyword, String shopType);

    // 分页获取最新点评（按时间排序）
    List<HotReviewItem> listLatestReviews(int page, int size, String keyword, String shopType);

    // 获取所有店铺分类列表
    List<String> listShopTypes();

    // 获取当前用户可管理的店铺列表（超级管理员可见全部）
    List<Shop> listManageShops(Long userId, boolean superAdmin);

    // 更新管理端店铺信息
    Shop updateManageShop(Long userId, boolean superAdmin, Long shopId, ShopManageUpdateRequest req);

    // 获取点评详情（含评论、点赞等）
    ReviewDetailItem getReviewDetail(Long reviewId);

    // 按经纬度搜索附近店铺
    List<NearbyShopItem> listNearbyShops(double longitude, double latitude, double radiusKm, long limit);

    // 创建点评
    Review createReview(Long userId, Long shopId, ReviewRequest req);

    // 更新点评
    Review updateReview(Long userId, Long reviewId, ReviewRequest req);

    // 删除点评（仅作者可删）
    void deleteReview(Long userId, Long reviewId);

    // 切换点评点赞状态，返回当前点赞数
    Integer toggleReviewLike(Long userId, Long reviewId);

    // 店铺搜索结果：店铺列表与总数
    record ShopSearchResult(List<Shop> items, long total) {
    }
}
