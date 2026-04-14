---
name: platform-knowledge
description: >-
  基于平台 FAQ 与规则回答问题，不查询实时店铺库存。在 intent 为 platform_knowledge、或用户询问注册、点评、订单、秒杀规则、退款、AI 功能等与 faq.md 静态知识一致的问题时使用。
  可对 FAQ 文档做可选向量检索（RAG）。
---

# 平台知识（FAQ / RAG）

## 说明

本技能对应 **platform_knowledge**：说明产品如何运作（账号、点评、浏览店铺、订单、秒杀、退款、隐私、联系方式等），且**不需要**拉取当前店铺列表或推荐结果。线上由**检索器**加载 `faq.md`，按 `##` 切段、向量化，将 Top-K 片段注入系统提示词（**RAG**）。智能体**仅**依据给出的 FAQ 摘录与基础系统指令作答，不编造接口或商户行数据。

## 使用方式

- **何时使用**：`"intent": "platform_knowledge"`，或属于 FAQ 类问题且非 `merchant_query` / `recommend`。
- **前置条件**：可访问 FAQ 资源；若开启 RAG 则需嵌入模型 API。
- **调用时机**：路由之后；检索可与对话模型同进程执行（不必走 MCP）。

## 步骤

1. **确认意图**  
   确保 `intent === "platform_knowledge"`（或按产品规则在路由 JSON 解析失败时降级）。

2. **加载 FAQ 上下文（RAG）**  
   若使用向量检索：对用户问句做嵌入，从 `faq.md` 取 Top-K 段并设最低相似度阈值。若关闭 RAG，仅用短系统人设。

3. **不要为「纯规则问题」调商户 MCP**  
   除非用户明确要求实时数据，否则不要用 `search_shops` / `get_shop_detail` 回答纯政策问题。

4. **组织回复**  
   简洁作答，落在 FAQ 范围内。若摘录中无答案，如实说明并按 FAQ 建议引导人工客服。

5. **MCP**  
   本技能通常**不需要** MCP 工具。若部署了「读 FAQ 文件」类工具可选用；默认进程内 RAG 即可。

## 输入约定

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| intent | string | 是 | `platform_knowledge`。 |
| user_message | string | 是 | 当前用户文本，用作检索查询。 |

路由示例：

```json
{
  "intent": "platform_knowledge",
  "sub_intent": null,
  "keyword": null,
  "type": null,
  "shop_id": null
}
```

## 输出约定

| 字段 | 类型 | 说明 |
|------|------|------|
| reply | string | 基于 FAQ / 系统规则的自然语言回答。 |
| sources | string 数组 | 可选：若前端展示，可列出所用 FAQ 段落标题或摘要。 |

## 示例

**用户：**「秒杀订单超时没支付会怎样？」

**路由输出：**

```json
{ "intent": "platform_knowledge" }
```

**智能体动作：**

1. 检索秒杀/订单相关 FAQ（RAG）或按规则在内存中匹配。  
2. 据此作答；除非用户问「现在有什么秒杀」，否则不必查 `search_shops`。

## 仓库对应

- 后端：`FaqRetrieverService` + `AiChatServiceImpl` 组装带 FAQ 段的系统提示词。  
- 知识文件：`backend/src/main/resources/rag/faq.md`。
