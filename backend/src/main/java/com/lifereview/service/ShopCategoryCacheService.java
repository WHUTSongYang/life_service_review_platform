package com.lifereview.service;

import java.util.List;

/**
 * 业务职责说明：店铺分类列表的缓存读取、合法性校验、名称规范化及缓存刷新。
 */
public interface ShopCategoryCacheService {

    /**
     * 获取当前启用的全部店铺分类名称（优先读缓存）。
     *
     * @return 分类名称列表
     */
    List<String> getEnabledCategoryNames();

    /**
     * 校验用户输入的分类是否合法，并返回规范化后的分类名。
     *
     * @param rawType 用户或上游传入的原始分类字符串
     * @return 校验通过后的规范化分类名
     */
    String validateAndNormalizeCategory(String rawType);

    /**
     * 对分类字符串做格式规范化（不校验是否在启用列表内）。
     *
     * @param rawType 原始分类字符串
     * @return 规范化后的分类名
     */
    String normalizeCategoryName(String rawType);

    /**
     * 从数据库重新加载分类并刷新 Redis 等缓存。
     */
    void refreshCache();
}
