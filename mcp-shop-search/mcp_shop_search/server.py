"""FastMCP stdio server: tools for merchant search via the platform REST API."""
# FastMCP 标准输入输出服务器：通过平台 REST API 提供商家搜索工具

# 导入 annotations 以支持 Python 3.9+ 的类型注解新特性
from __future__ import annotations

# 导入 traceback 模块用于格式化异常堆栈信息
import traceback
# 从 collections.abc 模块导入 Awaitable 和 Callable 类型注解
from collections.abc import Awaitable, Callable

# 导入 httpx 库用于发送 HTTP 请求
import httpx
# 从 mcp.server.fastmcp 导入 FastMCP 类，用于创建 MCP 服务器
from mcp.server.fastmcp import FastMCP

# 从 api_client 模块导入各个 API 客户端函数
from mcp_shop_search.api_client import (
    get_base_url,  # 获取 API 基础 URL
    get_shop_detail,  # 获取商家详情
    list_shop_types,  # 获取商家类型列表
    search_nearby_shops,  # 搜索附近商家
    search_shops,  # 搜索商家
)

# 创建 FastMCP 服务器实例
mcp = FastMCP(
    "life-review-shop-search",  # 服务器名称
    instructions=(
        "Tools to query merchant (shop) data from the Life Review Platform backend. "
        "Set LIFE_REVIEW_API_BASE (default http://localhost:8080) and ensure the Spring Boot "
        "server is running. All tools use public GET endpoints; no JWT required."
        # 工具说明：从生活服务平台后端查询商家数据的工具
        # 需要设置环境变量 LIFE_REVIEW_API_BASE（默认 http://localhost:8080）
        # 确保 Spring Boot 服务器正在运行
        # 所有工具都使用公共 GET 端点，不需要 JWT 认证
    ),
)

async def _with_client(coro: Callable[[httpx.AsyncClient, str], Awaitable[str]]) -> str:
    # 通用的异步客户端管理函数，用于处理 HTTP 请求的生命周期和错误
    # coro: 一个可等待的函数，接收 httpx.AsyncClient 和 str 参数，返回 str
    
    # 获取 API 基础 URL
    base = get_base_url()
    try:
        # 创建异步 HTTP 客户端，设置超时时间为 30 秒
        async with httpx.AsyncClient(timeout=httpx.Timeout(30.0)) as client:
            # 调用传入的协程函数，传入客户端和基础 URL
            return await coro(client, base)
    except httpx.HTTPStatusError as e:
        # 捕获 HTTP 状态码错误（如 4xx, 5xx）
        # 提取响应文本或错误消息
        msg = e.response.text or str(e)
        # 返回格式化的错误信息，包含状态码、请求 URL 和错误消息
        return f"HTTP {e.response.status_code} from {e.request.url!r}: {msg}"
    except httpx.RequestError as e:
        # 捕获网络请求错误（如连接失败、超时等）
        # 返回格式化的错误信息，包含错误描述和后端服务地址
        return f"Request failed: {e!s}\n(Is the backend up at {base!r}?)"
    except Exception:
        # 捕获其他所有未预期的异常
        # 返回完整的堆栈跟踪信息以便调试
        return traceback.format_exc()


# 定义搜索商家工具，通过@mcp.tool 装饰器注册到 MCP 服务器
@mcp.tool(name="search_shops")
async def search_shops_tool(
    keyword: str | None = None,  # 可选的搜索关键词
    shop_type: str | None = None,  # 可选的商家类型（中文）
    page: int = 0,  # 页码，从 0 开始
    size: int = 20,  # 每页大小，默认 20 条
) -> str:
    """Search shops by optional keyword and/or Chinese shop type, with pagination.

    Always uses paging (default page=0, size=20) so the backend never returns an unbounded full list.
    Maps to GET /api/shops. Response includes ApiResponse JSON and x_total_count when present.
    """
    # 按关键词和/或中文商家类型搜索商家，支持分页
    # 始终使用分页（默认 page=0, size=20），后端不会返回无边界的全量列表
    # 映射到 GET /api/shops 端点，响应包含 ApiResponse JSON 和 x_total_count（如果存在）

    # 定义内部异步函数用于执行实际的搜索操作
    async def run(client: httpx.AsyncClient, base: str) -> str:
        # 调用 api_client 中的 search_shops 函数
        return await search_shops(
            client,
            base,
            keyword=keyword,
            shop_type=shop_type,
            page=page,
            size=size,
        )

    # 使用通用客户端管理函数执行请求并返回结果
    return await _with_client(run)


# 定义获取商家详情工具，通过@mcp.tool 装饰器注册到 MCP 服务器
@mcp.tool(name="get_shop_detail")
async def get_shop_detail_tool(shop_id: int) -> str:
    """Get a single shop by numeric id. Maps to GET /api/shops/{shopId}."""
    # 根据数字 ID 获取单个商家详情，映射到 GET /api/shops/{shopId} 端点

    # 定义内部异步函数用于执行实际的获取操作
    async def run(client: httpx.AsyncClient, base: str) -> str:
        # 调用 api_client 中的 get_shop_detail 函数
        return await get_shop_detail(client, base, shop_id)

    # 使用通用客户端管理函数执行请求并返回结果
    return await _with_client(run)


# 定义搜索附近商家工具，通过@mcp.tool 装饰器注册到 MCP 服务器
@mcp.tool(name="search_nearby_shops")
async def search_nearby_shops_tool(
    longitude: float,  # 经度坐标（WGS84）
    latitude: float,  # 纬度坐标（WGS84）
    radius_km: float = 5.0,  # 搜索半径（公里），默认 5 公里
    limit: int = 20,  # 返回结果限制，默认 20 条
) -> str:
    """List shops near a WGS84 coordinate within radiusKm (default 5), sorted by distance.

    Maps to GET /api/shops/nearby.
    """
    # 列出 WGS84 坐标附近 radiusKm 范围内的商家（默认 5 公里），按距离排序
    # 映射到 GET /api/shops/nearby 端点

    # 定义内部异步函数用于执行实际的搜索操作
    async def run(client: httpx.AsyncClient, base: str) -> str:
        # 调用 api_client 中的 search_nearby_shops 函数
        return await search_nearby_shops(
            client,
            base,
            longitude=longitude,
            latitude=latitude,
            radius_km=radius_km,
            limit=limit,
        )

    # 使用通用客户端管理函数执行请求并返回结果
    return await _with_client(run)


# 定义列出商家类型工具，通过@mcp.tool 装饰器注册到 MCP 服务器
@mcp.tool(name="list_shop_types")
async def list_shop_types_tool() -> str:
    """List allowed shop category display names (Chinese). Maps to GET /api/shops/types."""
    # 列出允许的商家类别显示名称（中文），映射到 GET /api/shops/types 端点

    # 定义内部异步函数用于执行实际的获取操作
    async def run(client: httpx.AsyncClient, base: str) -> str:
        # 调用 api_client 中的 list_shop_types 函数
        return await list_shop_types(client, base)

    # 使用通用客户端管理函数执行请求并返回结果
    return await _with_client(run)


def main() -> None:
    # 主函数：启动 MCP 服务器
    # 使用 stdio（标准输入输出）作为传输方式
    mcp.run(transport="stdio")


# 判断是否为直接运行的主程序入口
if __name__ == "__main__":
    # 调用 main 函数启动服务器
    main()
