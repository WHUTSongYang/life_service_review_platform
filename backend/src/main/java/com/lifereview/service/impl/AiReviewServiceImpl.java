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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 帮写点评服务实现。
 * <p>
 * 使用 LangChain4j {@link OpenAiChatModel} 进行非流式调用，根据店铺名称与类型生成约 100～180 字的中文评价文案。
 * 当 {@code app.ai.enabled=false} 或未注入模型时，按店铺类型关键字匹配返回本地模板（餐饮/美发/酒店/通用）。
 */
@Service
@Slf4j
public class AiReviewServiceImpl implements AiReviewService {

    /** LangChain4j 非流式对话模型；AI 关闭或未配置时为 null */
    @Autowired(required = false)
    @Qualifier("openAiChatModel")
    private OpenAiChatModel chatModel;

    /** AI 功能总开关，对应配置 {@code app.ai.enabled} */
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    /**
     * 生成可发布的店铺点评文案。
     *
     * @param shopName 店铺名称，用于提示词与模板
     * @param shopType 店铺类型或类目描述，用于风格与模板分支
     * @return 约 100～180 字的评价正文（已 trim）
     * @throws IllegalArgumentException 大模型返回空或调用失败时包装为业务提示
     */
    @Override
    public String generateReviewText(String shopName, String shopType) {
        // AI 可用：走 LangChain4j；否则：本地模板兜底
        if (aiEnabled && chatModel != null) {
            return callLangChainModel(shopName, shopType);
        }
        return localFallback(shopName, shopType);
    }

    /**
     * 组装系统消息与用户提示，调用非流式模型并解析回复文本。
     *
     * @param shopName 店铺名称
     * @param shopType 店铺类型
     * @return 模型生成的评价正文
     * @throws IllegalArgumentException 返回为空或底层异常时抛出
     */
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

    /**
     * AI 不可用时的固定模板兜底，按类型关键字分支。
     *
     * @param shopName 店铺名称
     * @param shopType 类型字符串（含「餐」「饮」「理发」等则匹配对应模板）
     * @return 预设句式组成的评价文案
     */
    private String localFallback(String shopName, String shopType) {
        String type = shopType == null ? "" : shopType.trim();
        // 餐饮类
        if (type.contains("餐") || type.contains("饮")) {
            return "这次在" + shopName + "用餐体验不错，口味层次丰富，出餐速度稳定。环境整洁舒适，服务人员响应及时，整体值得回访。";
        }
        // 美发类
        if (type.contains("理发") || type.contains("美发")) {
            return "在" + shopName + "做了服务，沟通需求很顺畅，技师手法熟练，过程细致。门店卫生和服务态度都在线，体验感较好。";
        }
        // 住宿类
        if (type.contains("酒店") || type.contains("民宿")) {
            return shopName + "入住流程顺利，房间整洁度和安静度表现不错。服务响应及时，位置出行方便，性价比较高。";
        }
        return "这次体验" + shopName + "整体感受良好，服务流程清晰，人员态度友好，环境维护到位，综合来看值得推荐。";
    }

    /**
     * 将 null 转为空串，非 null 去首尾空白。
     *
     * @param value 原始文本
     * @return 安全用于拼接提示词的字符串
     */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
