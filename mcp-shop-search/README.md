# 商户检索 MCP（Python）

通过 **stdio** 向 Cursor 等 MCP 客户端暴露本平台的商户只读查询能力，内部使用 HTTP 调用 Spring Boot 已公开的 `GET` 接口（无需 JWT）。

## 依赖

- Python 3.10+
- 后端已启动（默认 `http://localhost:8080`）

## 安装

在仓库本目录执行：

```bash
pip install -e .
```

或：

```bash
pip install -r requirements.txt
```

（若未用可编辑安装，需将 `mcp-shop-search` 所在目录加入 `PYTHONPATH`，或使用 `python -m mcp_shop_search`。）

## 环境变量

| 变量 | 说明 |
|------|------|
| `LIFE_REVIEW_API_BASE` | 后端根地址，默认 `http://localhost:8080` |

## 运行（stdio）

```bash
python -m mcp_shop_search
```

或：

```bash
mcp-shop-search
```

## Cursor 中注册 MCP

1. 先启动本项目的 **Spring Boot** 后端。
2. 打开 **Cursor → Settings → MCP**，添加服务器，类型选 **stdio**。
3. **Command** 示例（按本机 Python 路径调整）：

```json
{
  "mcpServers": {
    "life-review-shops": {
      "command": "python",
      "args": ["-m", "mcp_shop_search"],
      "cwd": "C:/path/to/life_service_review_platform/mcp-shop-search",
      "env": {
        "LIFE_REVIEW_API_BASE": "http://localhost:8080",
        "PYTHONPATH": "C:/path/to/life_service_review_platform/mcp-shop-search"
      }
    }
  }
}
```

若使用虚拟环境，将 `command` 改为该环境下的 `python` 可执行文件路径。

**若 Cursor 日志出现 `No module named mcp_shop_search`：**

1. 在 `mcp-shop-search` 目录下用 **与 `command` 相同的解释器** 执行一次：`pip install -e .`（推荐，安装后任意工作目录均可找到包）。
2. 或在 `env` 中增加 `PYTHONPATH`，值为本目录绝对路径（含 `mcp_shop_search` 包一层的父目录，见上例）。

## 暴露的工具

| 工具名 | 说明 |
|--------|------|
| `search_shops` | `keyword`、`shop_type` 可选；`page`、`size` 分页（默认 0/20），对应 `GET /api/shops` |
| `get_shop_detail` | `shop_id`，对应 `GET /api/shops/{id}` |
| `search_nearby_shops` | `longitude`、`latitude`；`radius_km`（默认 5）、`limit`（默认 20），对应 `GET /api/shops/nearby` |
| `list_shop_types` | 分类中文名列表，对应 `GET /api/shops/types` |

返回值为格式化后的 JSON 文本（含后端 `ApiResponse` 与列表场景的 `x_total_count` 头）。

## 安全说明

仅适用于**开发/联调**环境；不要对生产库暴露无鉴权 MCP。若未来需要带登录态接口，再增加可选 `Authorization` 环境变量扩展即可。
