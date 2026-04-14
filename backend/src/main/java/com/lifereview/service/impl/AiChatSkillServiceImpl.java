package com.lifereview.service.impl;



import com.lifereview.dto.AiChatIntent;

import com.lifereview.dto.AiChatRequest;

import com.lifereview.dto.ShopProductItem;

import com.lifereview.entity.Shop;

import com.lifereview.service.ShopProductService;

import com.lifereview.service.ShopReviewService;

import com.lifereview.service.AiChatSkillService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;



import java.util.List;



/**

 * AI 客服技能数据服务实现。

 * <p>

 * 针对「商户查询」「推荐」等意图，仅调用现有 {@link ShopReviewService}、{@link ShopProductService}

 * 返回的真实数据，拼接为可注入大模型的「平台事实」纯文本，不捏造字段。

 */

@Service

@RequiredArgsConstructor

public class AiChatSkillServiceImpl implements AiChatSkillService {



    private final ShopReviewService shopReviewService;

    private final ShopProductService shopProductService;



    /** 推荐店铺列表最大条数，配置键 {@code app.ai.recommend-shop-limit} */

    @Value("${app.ai.recommend-shop-limit:8}")

    private int recommendShopLimit;



    /** 每个店铺在推荐商品意图下最多展示的商品数，配置键 {@code app.ai.recommend-product-per-shop} */

    @Value("${app.ai.recommend-product-per-shop:3}")

    private int recommendProductPerShop;



    /** 商户关键词搜索分页大小上限参考，配置键 {@code app.ai.merchant-search-page-size} */

    @Value("${app.ai.merchant-search-page-size:10}")

    private int merchantSearchPageSize;



    /** 附近店铺默认搜索半径（公里），配置键 {@code app.ai.nearby-default-radius-km} */

    @Value("${app.ai.nearby-default-radius-km:5}")

    private double nearbyDefaultRadiusKm;



    /** 附近店铺列表默认最大条数，配置键 {@code app.ai.nearby-default-limit} */

    @Value("${app.ai.nearby-default-limit:15}")

    private long nearbyDefaultLimit;



    /**

     * 按意图拼装注入大模型的平台事实文本；非技能意图返回空串。

     *

     * @param intent 路由结果（含 intent、subIntent、keyword、shopId 等）

     * @param req    当前对话请求（附近店等需要经纬度）

     * @return 供系统提示词使用的「平台数据」段落，或空字符串

     */

    @Override

    public String buildPlatformDataFacts(AiChatIntent intent, AiChatRequest req) {

        if (intent == null || intent.getIntent() == null) {

            return "";

        }

        return switch (intent.getIntent()) {

            case "merchant_query" -> merchantFacts(intent, req);

            case "recommend" -> recommendFacts(intent);

            default -> "";

        };

    }



    /**

     * 商户查询主分支：按子意图分发到搜索、详情或附近。

     *

     * @param intent 意图与子意图

     * @param req    请求体

     * @return 平台事实文本

     */

    private String merchantFacts(AiChatIntent intent, AiChatRequest req) {

        String sub = intent.getSubIntent() == null ? "" : intent.getSubIntent();

        // 未指定或搜索店铺：关键词/类型搜索

        if (sub.isEmpty() || "search_shop".equals(sub)) {

            return searchShopFacts(intent);

        }

        if ("shop_detail".equals(sub)) {

            return shopDetailFacts(intent);

        }

        if ("nearby".equals(sub)) {

            return nearbyFacts(req);

        }

        return searchShopFacts(intent);

    }



    /**

     * 根据关键词与类型搜索店铺并格式化结果列表。

     *

     * @param intent 含 keyword、type

     * @return 搜索结果描述或「未找到」说明

     */

    private String searchShopFacts(AiChatIntent intent) {

        String kw = blankToNull(intent.getKeyword());

        String type = blankToNull(intent.getType());

        var result = shopReviewService.searchShops(0, Math.min(merchantSearchPageSize, 20), kw, type);

        if (result.items().isEmpty()) {

            return "【平台数据】未找到符合条件的店铺（关键词=" + (kw == null ? "无" : kw)

                    + "，类型=" + (type == null ? "不限" : type) + "）。";

        }

        StringBuilder sb = new StringBuilder();

        sb.append("【平台数据】店铺搜索结果（共 ").append(result.total()).append(" 条，当前展示前 ")

                .append(result.items().size()).append(" 条）：\n");

        for (Shop s : result.items()) {

            appendShopLine(sb, s);

        }

        return sb.toString();

    }



    /**

     * 店铺详情：优先按 shopId；否则按名称搜索取第一条再查详情。

     *

     * @param intent 含 shopId 或 keyword

     * @return 详情摘要或错误说明

     */

    private String shopDetailFacts(AiChatIntent intent) {

        if (intent.getShopId() != null) {

            try {

                Shop s = shopReviewService.getShopDetail(intent.getShopId());

                StringBuilder sb = new StringBuilder();

                sb.append("【平台数据】店铺详情：\n");

                appendShopLine(sb, s);

                return sb.toString();

            } catch (Exception ex) {

                return "【平台数据】未找到 id=" + intent.getShopId() + " 的店铺。";

            }

        }

        String kw = blankToNull(intent.getKeyword());

        if (kw != null) {

            var result = shopReviewService.searchShops(0, 1, kw, null);

            if (!result.items().isEmpty()) {

                Shop s = shopReviewService.getShopDetail(result.items().get(0).getId());

                StringBuilder sb = new StringBuilder();

                sb.append("【平台数据】根据名称匹配到店铺详情：\n");

                appendShopLine(sb, s);

                return sb.toString();

            }

        }

        return "【平台数据】未能确定要查询的店铺，请提供店铺名称或店铺 ID。";

    }



    /**

     * 附近店铺：依赖请求中的经纬度；缺失时返回引导文案。

     *

     * @param req 含 latitude、longitude

     * @return 附近列表描述或无法查询说明

     */

    private String nearbyFacts(AiChatRequest req) {

        Double lat = req.getLatitude();

        Double lng = req.getLongitude();

        if (lat == null || lng == null) {

            return "【平台数据】未收到用户经纬度，无法查询附近店铺。请在本页允许浏览器定位后重试，或改用「搜索店铺」并说明地点或店名。";

        }

        var list = shopReviewService.listNearbyShops(lng, lat, nearbyDefaultRadiusKm, nearbyDefaultLimit);

        if (list.isEmpty()) {

            return "【平台数据】该位置附近暂无可展示店铺（半径约 " + nearbyDefaultRadiusKm + " km）。";

        }

        StringBuilder sb = new StringBuilder();

        sb.append("【平台数据】附近店铺（按距离升序，最多 ").append(list.size()).append(" 条）：\n");

        for (var n : list) {

            sb.append("- id=").append(n.getId())

                    .append("，").append(n.getName())

                    .append("，类型=").append(nullToEmpty(n.getType()))

                    .append("，地址=").append(nullToEmpty(n.getAddress()))

                    .append("，评分=").append(n.getAvgScore() == null ? "-" : n.getAvgScore())

                    .append("，约 ").append(n.getDistanceKm()).append(" km\n");

        }

        return sb.toString();

    }



    /**

     * 推荐意图：子意图为商品推荐时走商品事实，否则走店铺推荐。

     *

     * @param intent 含 subIntent

     * @return 推荐类平台事实文本

     */

    private String recommendFacts(AiChatIntent intent) {

        String sub = intent.getSubIntent() == null ? "" : intent.getSubIntent();

        if ("recommend_products".equals(sub)) {

            return recommendProductsFacts();

        }

        return recommendShopsFacts();

    }



    /**

     * 按评分与点评数等规则取高分店铺列表作为推荐。

     *

     * @return 推荐店铺段落；无数据时返回说明文案

     */

    private String recommendShopsFacts() {

        List<Shop> shops = shopReviewService.listTopShopsByAvgScore(recommendShopLimit);

        if (shops.isEmpty()) {

            var fallback = shopReviewService.searchShops(0, Math.min(recommendShopLimit, 10), null, null);

            shops = fallback.items();

        }

        if (shops.isEmpty()) {

            return "【平台数据】当前没有可推荐的店铺数据。";

        }

        StringBuilder sb = new StringBuilder();

        sb.append("【平台数据】推荐店铺（按评分与点评数排序，最多 ").append(shops.size()).append(" 条）：\n");

        for (Shop s : shops) {

            appendShopLine(sb, s);

        }

        return sb.toString();

    }



    /**

     * 在高分店铺下抽取上架且库存有效的商品，每店条数与总条数受配置限制。

     *

     * @return 推荐商品段落；无可售商品时返回说明

     */

    private String recommendProductsFacts() {

        List<Shop> shops = shopReviewService.listTopShopsByAvgScore(Math.min(recommendShopLimit, 10));

        if (shops.isEmpty()) {

            var fallback = shopReviewService.searchShops(0, 5, null, null);

            shops = fallback.items();

        }

        if (shops.isEmpty()) {

            return "【平台数据】当前没有可推荐的商品（无店铺数据）。";

        }

        StringBuilder sb = new StringBuilder();

        sb.append("【平台数据】推荐商品（取自评分较高的店铺的部分上架商品，每店最多 ")

                .append(recommendProductPerShop).append(" 件）：\n");

        int total = 0;

        int maxTotal = recommendShopLimit * recommendProductPerShop;

        for (Shop shop : shops) {

            List<ShopProductItem> products = shopProductService.listProductsByShop(shop.getId());

            int n = 0;

            for (ShopProductItem p : products) {

                if (n >= recommendProductPerShop) {

                    break;

                }

                // 跳过下架或明确无库存商品

                if (Boolean.FALSE.equals(p.getEnabled()) || (p.getStock() != null && p.getStock() <= 0)) {

                    continue;

                }

                sb.append("- 店铺「").append(shop.getName()).append("」(id=").append(shop.getId()).append(") 商品：")

                        .append(p.getName()).append("，价格=").append(p.getPrice())

                        .append("，库存=").append(p.getStock() == null ? "-" : p.getStock()).append("\n");

                n++;

                total++;

                if (total >= maxTotal) {

                    return sb.toString();

                }

            }

        }

        if (total == 0) {

            return "【平台数据】当前评分较高的店铺中暂无在售商品，请稍后再试或浏览店铺列表。";

        }

        return sb.toString();

    }



    /**

     * 向 StringBuilder 追加一行店铺摘要（id、名称、类型、地址、评分、点评数）。

     *

     * @param sb 目标缓冲区

     * @param s  店铺实体

     */

    private static void appendShopLine(StringBuilder sb, Shop s) {

        sb.append("- id=").append(s.getId())

                .append("，").append(s.getName())

                .append("，类型=").append(nullToEmpty(s.getType()))

                .append("，地址=").append(nullToEmpty(s.getAddress()))

                .append("，评分=").append(s.getAvgScore() == null ? "-" : s.getAvgScore())

                .append("，点评数=").append(s.getReviewCount() == null ? 0 : s.getReviewCount())

                .append("\n");

    }



    /**

     * 空白或 null 字符串统一为 null，便于「未填」判断。

     *

     * @param v 原始串

     * @return trim 后非空串，否则 null

     */

    private static String blankToNull(String v) {

        if (v == null || v.isBlank()) {

            return null;

        }

        return v.trim();

    }



    /**

     * null 安全转为展示用空串。

     *

     * @param v 原始串

     * @return 非 null 字符串

     */

    private static String nullToEmpty(String v) {

        return v == null ? "" : v;

    }

}

