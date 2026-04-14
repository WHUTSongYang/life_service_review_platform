package com.lifereview.service;
import com.lifereview.dto.AiChatIntent;
import com.lifereview.dto.AiChatRequest;

/**

 * 业务职责说明：按对话意图从业务库拉取可验证的事实数据，供 AI 客服 System Prompt 注入，避免编造店铺或商品信息。

 */

public interface AiChatSkillService {
    /**

     * 根据意图与当前请求上下文，从业务库组装可写入 System Prompt 的平台事实文本。

     *

     * @param intent 路由解析得到的对话意图

     * @param req    原始对话请求（含用户消息、位置等上下文）

     * @return 与当前意图相关的多行事实描述；无关或无可展示数据时返回空字符串

     */

    String buildPlatformDataFacts(AiChatIntent intent, AiChatRequest req);

}

