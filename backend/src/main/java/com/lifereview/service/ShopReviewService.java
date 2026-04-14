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
 * 业务职责说明：店铺浏览与搜索、店铺信息管理、点评全生命周期及热门/附近/推荐等相关查询。
 */
public interface ShopReviewService {

    /**
     * 获取全部店铺列表。
     *
     * @return 店铺实体列表
     */
    List<Shop> listShops();

    /**
     * 分页搜索店铺，支持关键词与分类筛选。
     *
     * @param page    页码（从 0 或 1 起算以实现类为准）
     * @param size    每页条数
     * @param keyword 搜索关键词，可为空表示不按名称搜索
     * @param type    店铺分类，可为空
     * @return 当前页店铺列表与符合条件的总条数
     */
    ShopSearchResult searchShops(int page, int size, String keyword, String type);

    /**
     * 根据 ID 获取店铺详情（实现可配合缓存）。
     *
     * @param shopId 店铺 ID
     * @return 店铺实体
     */
    Shop getShopDetail(Long shopId);

    /**
     * 创建新店铺。
     *
     * @param shop 店铺实体（名称、地址等字段由调用方填充）
     * @return 持久化后的店铺实体
     */
    Shop createShop(Shop shop);

    /**
     * 更新店铺信息。
     *
     * @param shopId 店铺 ID
     * @param req    待合并的店铺字段
     * @return 更新后的店铺实体
     */
    Shop updateShop(Long shopId, Shop req);

    /**
     * 获取指定店铺下的点评列表。
     *
     * @param shopId 店铺 ID
     * @return 点评实体列表
     */
    List<Review> listReviews(Long shopId);

    /**
     * 分页获取热门点评（通常按点赞数等指标排序）。
     *
     * @param page    页码
     * @param size    每页条数
     * @param keyword 筛选关键词，可为空
     * @param shopType 店铺类型筛选，可为空
     * @return 热门点评项列表
     */
    List<HotReviewItem> listHotReviews(int page, int size, String keyword, String shopType);

    /**
     * 分页获取最新点评（按发布时间排序）。
     *
     * @param page    页码
     * @param size    每页条数
     * @param keyword 筛选关键词，可为空
     * @param shopType 店铺类型筛选，可为空
     * @return 最新点评项列表
     */
    List<HotReviewItem> listLatestReviews(int page, int size, String keyword, String shopType);

    /**
     * 获取系统中全部店铺分类名称。
     *
     * @return 分类名称列表
     */
    List<String> listShopTypes();

    /**
     * 获取当前用户可管理的店铺列表；超级管理员通常可查看全部。
     *
     * @param userId      当前用户 ID
     * @param superAdmin  是否为超级管理员
     * @return 可管理店铺实体列表
     */
    List<Shop> listManageShops(Long userId, boolean superAdmin);

    /**
     * 在管理端更新指定店铺信息。
     *
     * @param userId      当前操作者用户 ID
     * @param superAdmin  是否为超级管理员
     * @param shopId      店铺 ID
     * @param req         管理端店铺更新请求体
     * @return 更新后的店铺实体
     */
    Shop updateManageShop(Long userId, boolean superAdmin, Long shopId, ShopManageUpdateRequest req);

    /**
     * 获取点评详情（含评论、点赞状态等聚合信息）。
     *
     * @param reviewId 点评 ID
     * @return 点评详情 DTO
     */
    ReviewDetailItem getReviewDetail(Long reviewId);

    /**
     * 按经纬度与半径查询附近店铺。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param radiusKm  搜索半径（公里）
     * @param limit     返回条数上限
     * @return 附近店铺简要信息列表
     */
    List<NearbyShopItem> listNearbyShops(double longitude, double latitude, double radiusKm, long limit);

    /**
     * 按评分与点评数等综合规则取 Top 店铺（用于推荐等场景）。
     *
     * @param limit 返回条数上限
     * @return 店铺实体列表
     */
    List<Shop> listTopShopsByAvgScore(int limit);

    /**
     * 创建点评。
     *
     * @param userId 当前用户 ID
     * @param shopId 店铺 ID
     * @param req    点评内容请求体
     * @return 新建的点评实体
     */
    Review createReview(Long userId, Long shopId, ReviewRequest req);

    /**
     * 更新点评（通常为作者本人）。
     *
     * @param userId   当前用户 ID
     * @param reviewId 点评 ID
     * @param req      点评更新请求体
     * @return 更新后的点评实体
     */
    Review updateReview(Long userId, Long reviewId, ReviewRequest req);

    /**
     * 删除点评（通常为作者本人）。
     *
     * @param userId   当前用户 ID
     * @param reviewId 点评 ID
     */
    void deleteReview(Long userId, Long reviewId);

    /**
     * 切换当前用户对指定点评的点赞状态。
     *
     * @param userId   当前用户 ID
     * @param reviewId 点评 ID
     * @return 切换后的点赞总数
     */
    Integer toggleReviewLike(Long userId, Long reviewId);

    /**
     * 店铺分页搜索结果封装。
     *
     * @param items 当前页店铺列表
     * @param total 满足条件的总记录数
     */
    record ShopSearchResult(List<Shop> items, long total) {
    }
}
