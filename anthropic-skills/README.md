# Anthropic Agent Skills（本地生活服务点评平台）

本目录按 [Agent Skills](https://agentskills.io/) / Claude Code 惯例，将 AI 客服的**路由意图与数据技能**写成独立 `SKILL.md`，供 Claude Code（`~/.claude/skills/` 或 `.claude/skills/`）或兼容客户端加载。

与后端实现对应关系：`AiChatIntentRouter` + `AiChatSkillServiceImpl` + `FaqRetrieverService` + `AiChatServiceImpl`。

| 目录 | intent | 说明 |
|------|--------|------|
| [off-topic](off-topic/SKILL.md) | `off_topic` | 与平台无关话题，固定拒答 |
| [platform-knowledge](platform-knowledge/SKILL.md) | `platform_knowledge` | FAQ / RAG 知识问答 |
| [merchant-query](merchant-query/SKILL.md) | `merchant_query` | 搜索店铺、详情、附近（MCP 对齐 `mcp-shop-search`） |
| [recommend](recommend/SKILL.md) | `recommend` | 推荐店铺或推荐商品 |

将所需子目录复制到 `~/.claude/skills/<name>/` 或项目 `.claude/skills/<name>/` 即可。
