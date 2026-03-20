package com.lifereview.service.impl;

import com.lifereview.service.AiReviewService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 帮写点评服务实现。
 * 使用 LangChain4j OpenAiChatModel 非流式调用大模型，根据店铺名称和类型生成 100-180 字评价文案。
 * app.ai.enabled=false 或 chatModel 不可用时，根据店铺类型返回预设模板（餐饮/美发/酒店/通用）。
 */
@Service
@Slf4j
public class AiReviewServiceImpl implements AiReviewService {

    /** LangChain4j 非流式对话模型，app.ai.enabled=false 时为 null */
    @Autowired(required = false)
    private OpenAiChatModel chatModel;

    /** AI 功能总开关，对应 application.yml 中的 app.ai.enabled */
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    /** 生成店铺点评文案。shopName、shopType 用于构造提示词，返回 100-180 字可直接发布的文案 */
    @Override
    public String generateReviewText(String shopName, String shopType) {
        // AI 可用：调用大模型生成；否则：走本地模板兜底
        if (aiEnabled && chatModel != null) {
            return callLangChainModel(shopName, shopType);
        }
        return localFallback(shopName, shopType);
    }

    /** 调用 LangChain4j 大模型。构造 SystemMessage 和 UserMessage，发起非流式请求，提取返回文本 */
    private String callLangChainModel(String shopName, String shopType) {
        // 构造用户提示词：包含写作要求和店铺信息
        String userPrompt = """
                请帮我写一段中文店铺评价，要求：
                1) 语气真实自然，不夸张；
                2) 100-180字；
                3) 可直接发布，不要出现标题或分点；
                4) 不要编造具体价格、地址、电话等敏感信息。
                
                店铺名称：%s
                店铺类型：%s
                """.formatted(safeText(shopName), safeText(shopType));

        // 组装消息列表：系统角色设定 + 用户提问
        List<ChatMessage> messages = List.of(
                SystemMessage.from("你是中文生活服务评价写作助手，请输出可直接发布的评价文案。"),
                UserMessage.from(userPrompt)
        );

        try {
            // 发起非流式调用，等待完整回答
            ChatResponse response = chatModel.chat(
                    ChatRequest.builder().messages(messages).build()
            );
            // 从响应中提取 AI 回复的文本内容
            String content = response.aiMessage().text();
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("AI 服务返回为空，请稍后重试");
            }
            return content.trim();
        } catch (Exception e) {
            log.error("LangChain4j review generation failed", e);
            throw new IllegalArgumentException("AI 生成失败：" + e.getMessage());
        }
    }

    /** 本地兜底：AI 不可用时根据店铺类型返回预设模板，覆盖餐饮、美发、酒店，其余通用 */
    private String localFallback(String shopName, String shopType) {
        String type = shopType == null ? "" : shopType.trim();
        if (type.contains("餐") || type.contains("饮")) {
            return "这次在" + shopName + "用餐体验不错，口味层次丰富，出餐速度稳定。环境整洁舒适，服务人员响应及时，整体值得回访。";
        }
        if (type.contains("理发") || type.contains("美发")) {
            return "在" + shopName + "做了服务，沟通需求很顺畅，技师手法熟练，过程细致。门店卫生和服务态度都在线，体验感较好。";
        }
        if (type.contains("酒店") || type.contains("民宿")) {
            return shopName + "入住流程顺利，房间整洁度和安静度表现不错。服务响应及时，位置出行方便，性价比较高。";
        }
        return "这次体验" + shopName + "整体感受良好，服务流程清晰，人员态度友好，环境维护到位，综合来看值得推荐。";
    }

    /** 安全文本处理：null → 空字符串，非 null → 去首尾空白 */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
