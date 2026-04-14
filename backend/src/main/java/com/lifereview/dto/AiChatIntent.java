package com.lifereview.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * AI 客服意图路由结果，与路由模型返回的 JSON 字段一一对应。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatIntent {

    /** 主意图，如 off_topic、platform_knowledge、merchant_query、recommend 等 */
    private String intent;

    /** 子意图，如 search_shop、shop_detail、nearby、recommend_shops、recommend_products 等 */
    private String subIntent;

    /** 搜索或推荐相关的关键词 */
    private String keyword;

    /** 店铺或商品等业务类型筛选（与业务约定一致） */
    private String type;

    /** 当意图指向具体店铺时的店铺主键 ID */
    private Long shopId;
}
