package com.lifereview.service;

/**
 * 业务职责说明：根据店铺信息辅助生成点评文案；AI 不可用时由本地规则兜底。
 */
public interface AiReviewService {

    /**
     * 生成适合发布的店铺点评正文（约 100～180 字）。
     *
     * @param shopName 店铺名称
     * @param shopType 店铺类型或分类描述
     * @return 生成的点评文本；AI 不可用时返回兜底文案
     */
    String generateReviewText(String shopName, String shopType);
}
