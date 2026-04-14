---
name: recommend
description: >-
  在 intent 为 recommend 时推荐高分店铺或高分店铺下的在售商品。
  当用户问「推荐什么店」「买什么秒杀/商品」时使用。对应 Java 中 listTopShopsByAvgScore、searchShops 兜底、listProductsByShop；
  MCP 无独立 recommend 工具时需组合 search_shops 或扩展 MCP。
---

# 推荐店铺与商品

## 说明

本技能实现 **recommend**，按 **sub_intent**：

- **recommend_shops**：按评分与点评数排序的店铺列表（无数据时退化为通用搜索）。
- **recommend_products**：在若干高分店铺下，每店最多取 N 个有货上架商品，直到总条数上限。

线上由 **Shop**、**ShopProduct** 聚合；MCP 可能不直接暴露「推荐」——需**组合**多次 `search_shops` 或新增后端接口。

## 使用方式

- **何时使用**：`"intent": "recommend"`。
- **前置条件**：与其他店铺 API 相同基地址；商品需按店铺拉列表（Java：`GET /api/shops/{shopId}/products`）。

## 步骤

1. **确认意图**  
   `intent === "recommend"`。

2. **读取 sub_intent**  
   `recommend_shops` 或 `recommend_products`（为空时默认推荐店铺）。

3. **MCP / HTTP 策略**

   - **recommend_shops**  
     - 理想情况：有直接返回高分店铺的接口。  
     - 否则：用较宽关键词 `search_shops`，若响应含 `avg_score` 可在客户端排序，或分页多取再取 Top。

   - **recommend_products**  
     - 先得到候选店铺 id（同店铺推荐步骤）。  
     - 对每个店铺 id 调商品列表接口（或 MCP 扩展）。  
     - 过滤上架且库存大于 0；按每店上限与总上限截断。

4. **校验**  
   不编造价格与库存；缺字段则省略。

5. **回复**  
   简短引导 + 列表：推荐店铺时列店铺行；推荐商品时带店名 + 商品行。

## 输入约定

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| intent | string | 是 | `recommend`。 |
| sub_intent | string | 否 | `recommend_shops` 或 `recommend_products`。 |
| keyword | string | 否 | 可选收窄条件（路由可为空）。 |
| type | string | 否 | 可选分类筛选。 |
| shop_id | number | 否 | 推荐场景一般为 null。 |

示例：

```json
{
  "intent": "recommend",
  "sub_intent": "recommend_products",
  "keyword": null,
  "type": null,
  "shop_id": null
}
```

## 输出约定

**店铺：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 店铺 id。 |
| name | string | 名称。 |
| type | string | 分类。 |
| address | string | 地址。 |
| avgScore | number | 评分。 |
| reviewCount | number | 点评数。 |

**商品（每行）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| shopId | number | 所属店铺。 |
| productId | number | 商品 id。 |
| name | string | 商品名。 |
| price | number | 价格。 |
| stock | number | 库存。 |

## 示例

**用户：**「给我推荐几个值得去的店」

**路由：**

```json
{
  "intent": "recommend",
  "sub_intent": "recommend_shops"
}
```

**智能体：** 按评分拉取高分店铺（服务或组合 MCP），输出名称、评分、地址等。

## 仓库对应

- Java：`AiChatSkillServiceImpl.recommendShopsFacts`、`recommendProductsFacts`；条数受 `app.ai.recommend-shop-limit`、`app.ai.recommend-product-per-shop` 约束。  
- 若要在 MCP 层一等公民支持「推荐」，可扩展 `mcp-shop-search`。
