---
name: merchant-query
description: >-
  在 intent 为 merchant_query 时拉取实时店铺数据，支持搜索、详情、附近。
  当用户按关键词或类型搜店、按 id 或店名看详情、或问附近有什么店时使用。
  对应 MCP 工具 search_shops、get_shop_detail、search_nearby_shops（见 mcp-shop-search），与 Java AiChatSkillServiceImpl.merchantFacts 一致。
---

# 商户查询

## 说明

本技能实现 **merchant_query**，按 **sub_intent** 分支：

- **search_shop**：关键词 / 类型搜索（分页列表）。
- **shop_detail**：按 `shop_id` 查一家；若无 id 则按 `keyword` 先搜再取详情。
- **nearby**：半径附近检索；与 HTTP 对齐时需在对话请求体带 **纬度、经度**。

智能体通过 **MCP**（或与 MCP 相同的 HTTP 接口）获取**权威**店铺数据再摘要。流程：**意图 + 参数 → MCP 工具 → 有据回答**。

## 使用方式

- **何时使用**：`"intent": "merchant_query"`。
- **前置条件**：已配置 MCP 服务 `life-review-shops`，或可调后端 `GET /api/shops`、`/api/shops/{id}`、`/api/shops/nearby`，见 [mcp-shop-search/README.md](../../mcp-shop-search/README.md)。
- **调用时机**：拿到路由 JSON 后，将 `sub_intent` 映射到下方工具。

## 步骤

1. **确认意图**  
   `intent === "merchant_query"`。

2. **读取 sub_intent**  
   `search_shop` | `shop_detail` | `nearby`（为空时默认按搜索处理）。

3. **调用 MCP 工具**

   - **search_shop**  
     - 工具名：`search_shops`  
     - 参数：`keyword` 取自路由 `keyword`，`shop_type` 取自路由 `type`，按需传 `page` / `size`。

   - **shop_detail**  
     - 若有 `shop_id`：调用 `get_shop_detail`，传入 `shop_id`。  
     - 否则若有 `keyword`：先用 `search_shops` 取 size=1，再对第一条 id 调 `get_shop_detail`。

   - **nearby**  
     - 工具名：`search_nearby_shops`  
     - 参数：`longitude`、`latitude` 来自客户端上下文（与后端 `AiChatRequest` 一致）；`radius_km` / `limit` 按接口默认。  
     - 若无坐标，说明需要定位授权，**不要编造坐标**。

4. **校验返回**  
   仅使用接口返回的 JSON；空结果或错误时简短说明。

5. **组织回复**  
   列出 **id、名称、类型、地址、平均分、点评数**（附近结果再加 **distanceKm**）。若 API 未返回 `price`、`businessHours`，不要编造。

6. **可选**  
   用户只要分类名时可调 `list_shop_types`。

## 输入约定

路由 JSON（字段名与 `AiChatIntent` 一致）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| intent | string | 是 | `merchant_query`。 |
| sub_intent | string | 否 | `search_shop`、`shop_detail`、`nearby`。 |
| keyword | string | 否 | 搜索词或详情兜底用的店名。 |
| type | string | 否 | 店铺分类 / 搜索用 `shop_type`。 |
| shop_id | number | 否 | 详情用数字店铺 id。 |

附近场景下客户端还可能传（HTTP / 对话体，不一定在路由 JSON 里）：

| 字段 | 类型 | 说明 |
|------|------|------|
| latitude | number | 用户纬度。 |
| longitude | number | 用户经度。 |

示例：

```json
{
  "intent": "merchant_query",
  "sub_intent": "search_shop",
  "keyword": "火锅",
  "type": "餐饮",
  "shop_id": null
}
```

## 输出约定

每条店铺（以接口实际字段为准）：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | number | 店铺 id。 |
| name | string | 展示名称。 |
| type | string | 分类。 |
| address | string | 地址。 |
| avgScore | number | 平均评分。 |
| reviewCount | number | 点评数。 |
| distanceKm | number | 仅附近结果有。 |

## 示例

**用户：**「附近有什么火锅店？」（已共享定位）

**路由：**

```json
{
  "intent": "merchant_query",
  "sub_intent": "nearby",
  "keyword": "火锅",
  "type": null,
  "shop_id": null
}
```

**MCP：** 使用用户 `longitude`、`latitude` 调用 `search_nearby_shops`；若返回含全品类，可在客户端侧再按关键词过滤。

**回复：** 汇总附近结果：店名、距离、评分等。

## 仓库对应

- Java：`ShopReviewService.searchShops`、`getShopDetail`、`listNearbyShops`。  
- MCP 工具名：`search_shops`、`get_shop_detail`、`search_nearby_shops`、`list_shop_types`。
