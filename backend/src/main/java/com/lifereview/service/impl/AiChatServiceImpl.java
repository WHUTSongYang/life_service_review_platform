package com.lifereview.service.impl;

import com.lifereview.dto.AiChatMessage;
import com.lifereview.dto.AiChatRequest;
import com.lifereview.service.AiChatService;
import com.lifereview.service.FaqRetrieverService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** AI 客服流式对话服务。LangChain4j 流式模型 + RAG 增强。用户提问时检索 FAQ 注入 Prompt，逐 token 推送 SSE。ai.enabled=false 时本地兜底 */
@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    /** 基础系统提示词 —— RAG 未命中或不可用时使用 */
    private static final String BASE_SYSTEM_PROMPT =
            "你是生活服务点评平台的中文客服助手。回答简洁、友好、可执行，不编造平台不存在的功能。";

    /** RAG 增强系统提示词模板，%s 替换为检索到的 FAQ 段落 */
    private static final String RAG_SYSTEM_PROMPT_TEMPLATE = """
            你是生活服务点评平台的中文客服助手。请严格基于以下参考资料回答用户问题。
            如果参考资料中没有相关信息，请诚实告知用户你不确定，并建议联系人工客服（热线400-000-0000）。
            不要编造平台不存在的功能，回答应简洁、友好、可执行。
            
            === 参考资料 ===
            %s
            === 参考资料结束 ===
            """;

    /** LangChain4j 流式对话模型，app.ai.enabled=false 时为 null */
    @Autowired(required = false)
    private OpenAiStreamingChatModel streamingChatModel;

    /** FAQ 知识库检索服务，用于 RAG 增强 */
    @Autowired
    private FaqRetrieverService faqRetrieverService;

    /** AI 功能总开关，对应 application.yml 中的 app.ai.enabled */
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    /** 处理 AI 客服流式对话。返回 SseEmitter 保持长连接，逐 token 推送 chunk 事件，结束时推送 done。req 含 message 和 history */
    @Override
    public SseEmitter chatStream(AiChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);  // 0L = 无超时

        // ========== 降级分支：AI 未启用或模型不可用 → 本地兜底 ==========
        if (!aiEnabled || streamingChatModel == null) {
            CompletableFuture.runAsync(() -> {
                try {
                    streamLocalFallback(req.getMessage(), emitter);
                } catch (Exception e) {
                    sendError(emitter, e.getMessage());
                } finally {
                    sendDone(emitter);
                }
            });
            return emitter;
        }

        // ========== 正常分支：RAG 检索 + LangChain4j 流式调用 ==========
        try {
            // 1. 构造带 RAG 上下文的消息列表
            List<ChatMessage> messages = buildMessagesWithRag(req);

            // 2. 发起流式调用，注册回调处理器
            streamingChatModel.chat(
                    ChatRequest.builder().messages(messages).build(),
                    new StreamingChatResponseHandler() {
                        /** 每产生一个 token 时触发，推送 chunk 事件到前端。客户端已断开时 send 抛异常，静默忽略 */
                        @Override
                        public void onPartialResponse(String token) {
                            try {
                                emitter.send(SseEmitter.event().name("chunk").data(token));
                            } catch (IOException e) {
                                log.debug("SSE send failed (client may have disconnected): {}", e.getMessage());
                            }
                        }

                        /** 模型回答完毕——发送 done 事件并关闭 SSE 连接 */
                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            sendDone(emitter);
                        }

                        /** 模型调用出错——发送 error 事件后关闭 SSE 连接 */
                        @Override
                        public void onError(Throwable error) {
                            log.error("LangChain4j streaming error", error);
                            sendError(emitter, error.getMessage());
                            sendDone(emitter);
                        }
                    }
            );
        } catch (Exception e) {
            // 流式调用启动失败（如参数构建异常）
            log.error("Failed to start streaming chat", e);
            CompletableFuture.runAsync(() -> {
                sendError(emitter, e.getMessage());
                sendDone(emitter);
            });
        }

        return emitter;
    }

    /** 构造消息列表：SystemMessage（含 RAG 检索的 FAQ 或基础提示词）、历史对话、当前用户提问 */
    private List<ChatMessage> buildMessagesWithRag(AiChatRequest req) {
        // 根据用户当前问题做 RAG 检索，决定使用增强 Prompt 还是基础 Prompt
        String systemPrompt = buildSystemPromptWithRag(req.getMessage());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        // 追加历史对话（保持上下文连贯性）
        if (req.getHistory() != null) {
            for (AiChatMessage item : req.getHistory()) {
                if (item == null) continue;
                String content = safeText(item.getContent());
                if (content.isEmpty()) continue;
                // 根据角色创建对应的 LangChain4j 消息类型
                if ("assistant".equalsIgnoreCase(item.getRole())) {
                    messages.add(AiMessage.from(content));
                } else {
                    messages.add(UserMessage.from(content));
                }
            }
        }

        // 追加当前用户提问
        messages.add(UserMessage.from(safeText(req.getMessage())));
        return messages;
    }

    /** 根据用户问题构造系统提示词。RAG 可用且检索到 FAQ 时注入检索结果，否则使用基础提示词 */
    private String buildSystemPromptWithRag(String userQuery) {
        if (!faqRetrieverService.isAvailable()) {
            return BASE_SYSTEM_PROMPT;
        }
        // 调用 FAQ 知识库检索，获取与用户问题最相关的 TopK 段落
        List<String> contexts = faqRetrieverService.retrieve(userQuery);
        if (contexts.isEmpty()) {
            return BASE_SYSTEM_PROMPT;
        }
        // 将检索结果编号拼接，注入到 RAG 提示词模板中
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(contexts.get(i)).append("\n\n");
        }
        return RAG_SYSTEM_PROMPT_TEMPLATE.formatted(sb.toString().strip());
    }

    /** 本地兜底：AI 不可用时按每 12 字符一批发送 chunk，模拟流式打字效果 */
    private void streamLocalFallback(String message, SseEmitter emitter) throws IOException {
        String fallback = "你好，我是生活服务点评平台 AI 客服。你可以问我：店铺怎么选、评分怎么看、下单流程、退款取消等问题。你刚刚的问题是：" + safeText(message);
        int batch = 12;  // 每次发送的字符数，模拟逐字打出效果
        for (int i = 0; i < fallback.length(); i += batch) {
            int end = Math.min(fallback.length(), i + batch);
            emitter.send(SseEmitter.event().name("chunk").data(fallback.substring(i, end)));
        }
    }

    /** 发送 SSE done 事件并关闭连接，标志流结束 */
    private void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }

    /** 发送 SSE error 事件，通知前端出错信息 */
    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data("AI 对话失败：" + msg));
        } catch (IOException ignored) {
        }
    }

    /** 安全文本处理：null → 空字符串，非 null → 去首尾空白 */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
