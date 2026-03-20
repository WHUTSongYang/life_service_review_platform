package com.lifereview.service;

/**
 * AI 帮写点评服务接口。
 * 根据店铺名称和类型生成点评文案，AI 不可用时使用本地兜底逻辑。
 */
public interface AiReviewService {

    // 生成店铺点评文案，长度约 100-180 字
    String generateReviewText(String shopName, String shopType);
}
