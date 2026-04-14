"""HTTP client for Life Review Platform shop APIs (read-only GET)."""
# 生活服务平台商家 API 的 HTTP 客户端（只读 GET 请求）

# 导入 annotations 以支持 Python 3.9+ 的类型注解新特性
from __future__ import annotations

# 导入 json 模块用于处理 JSON 数据
import json
# 导入 os 模块用于访问操作系统功能（如环境变量）
import os
# 从 typing 模块导入 Any 类型，用于表示任意类型
from typing import Any

# 导入 httpx 库用于发送 HTTP 请求
import httpx

# 定义默认的 API 基础 URL，指向本地 8080 端口
DEFAULT_BASE = "http://localhost:8080"


def get_base_url() -> str:
    # 从环境变量 LIFE_REVIEW_API_BASE 获取基础 URL，如果不存在则使用默认值
    # rstrip("/") 用于移除末尾的斜杠，避免后续拼接 URL 时出现双斜杠
    return os.environ.get("LIFE_REVIEW_API_BASE", DEFAULT_BASE).rstrip("/")


def _format_result(
    body: dict[str, Any],  # 要格式化的响应体字典
    extra: dict[str, Any] | None = None,  # 可选的额外信息字典，用于添加元数据
) -> str:
    # 创建响应体的副本，避免修改原始数据
    out: dict[str, Any] = dict(body)
    # 如果提供了额外信息，则将其更新到输出字典中
    if extra:
        out.update(extra)
    # 将字典转换为格式化的 JSON 字符串
    # ensure_ascii=False 确保正确显示中文等非 ASCII 字符
    # indent=2 使输出具有可读性的缩进格式
    return json.dumps(out, ensure_ascii=False, indent=2)


async def search_shops(
    client: httpx.AsyncClient,  # httpx 异步客户端实例，用于发送 HTTP 请求
    base: str,  # API 基础 URL
    *,  # 分隔符，表示后面的参数必须是关键字参数
    keyword: str | None = None,  # 可选的搜索关键词
    shop_type: str | None = None,  # 可选的商家类型筛选
    page: int = 0,  # 页码，从 0 开始
    size: int = 20,  # 每页显示的商家数量，默认 20 条
) -> str:
    """GET /api/shops with pagination (avoids unbounded full list)."""
    # GET 请求/api/shops，支持分页（避免返回无边界的全量列表）
    
    # 构建查询参数字典，包含分页信息
    params: dict[str, Any] = {"page": page, "size": size}
    # 如果提供了关键词且不为空字符串，则添加到参数中
    if keyword is not None and str(keyword).strip():
        params["keyword"] = keyword.strip()
    # 如果提供了商家类型且不为空字符串，则添加到参数中
    if shop_type is not None and str(shop_type).strip():
        params["type"] = shop_type.strip()
    # 发送 GET 请求到/api/shops 端点
    r = await client.get(f"{base}/api/shops", params=params)
    # 如果响应状态码表示错误，则抛出异常
    r.raise_for_status()
    # 解析 JSON 响应体
    body = r.json()
    # 从响应头获取总数量信息（如果存在）
    total = r.headers.get("X-Total-Count")
    # 创建额外信息字典
    extra: dict[str, Any] = {}
    # 如果存在总数量信息，则将其添加到额外信息中
    if total is not None:
        extra["x_total_count"] = total
    # 格式化并返回结果，包含响应体和可能的总数量信息
    return _format_result(body, extra if extra else None)


async def get_shop_detail(client: httpx.AsyncClient, base: str, shop_id: int) -> str:
    # 根据商家 ID 获取单个商家的详细信息
    # client: httpx 异步客户端实例
    # base: API 基础 URL
    # shop_id: 商家的唯一标识符（整数）
    
    # 发送 GET 请求到/api/shops/{shop_id}端点
    r = await client.get(f"{base}/api/shops/{shop_id}")
    # 如果响应状态码表示错误，则抛出异常
    r.raise_for_status()
    # 解析并格式化返回的 JSON 数据
    return _format_result(r.json())


async def search_nearby_shops(
    client: httpx.AsyncClient,  # httpx 异步客户端实例，用于发送 HTTP 请求
    base: str,  # API 基础 URL
    *,  # 分隔符，表示后面的参数必须是关键字参数
    longitude: float,  # 经度坐标（WGS84 坐标系）
    latitude: float,  # 纬度坐标（WGS84 坐标系）
    radius_km: float = 5.0,  # 搜索半径，单位为公里，默认 5 公里
    limit: int = 20,  # 返回结果的最大数量，默认 20 条
) -> str:
    """搜索指定坐标附近的商家，按距离排序"""
    
    # 构建查询参数字典，包含地理位置和搜索范围信息
    params = {
        "longitude": longitude,  # 经度
        "latitude": latitude,  # 纬度
        "radiusKm": radius_km,  # 搜索半径（公里）
        "limit": limit,  # 结果数量限制
    }
    # 发送 GET 请求到/api/shops/nearby 端点
    r = await client.get(f"{base}/api/shops/nearby", params=params)
    # 如果响应状态码表示错误，则抛出异常
    r.raise_for_status()
    # 解析并格式化返回的 JSON 数据
    return _format_result(r.json())


async def list_shop_types(client: httpx.AsyncClient, base: str) -> str:
    # 获取所有允许的商家类型列表
    # client: httpx 异步客户端实例
    # base: API 基础 URL
    
    # 发送 GET 请求到/api/shops/types 端点
    r = await client.get(f"{base}/api/shops/types")
    # 如果响应状态码表示错误，则抛出异常
    r.raise_for_status()
    # 解析并格式化返回的 JSON 数据
    return _format_result(r.json())
