package com.lifereview.service;

import java.util.List;

/**
 * 店铺分类缓存服务接口。
 * 使用 Redis 缓存分类列表，提供分类校验与规范化。
 */
public interface ShopCategoryCacheService {

    // 获取所有启用的分类名称列表（从缓存读取）
    List<String> getEnabledCategoryNames();

    // 校验并规范化分类，非法则抛异常
    String validateAndNormalizeCategory(String rawType);

    // 规范化分类名称（不抛异常，仅做格式转换）
    String normalizeCategoryName(String rawType);

    // 刷新分类缓存，从数据库重新加载
    void refreshCache();
}
