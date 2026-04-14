package com.lifereview.service.impl;

import com.lifereview.dto.AiChatIntent;
import com.lifereview.dto.AiChatMessage;
import com.lifereview.dto.AiChatRequest;
import com.lifereview.service.AiChatIntentRouter;
import com.lifereview.service.AiChatService;
import com.lifereview.service.AiChatSkillService;
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

/**
 * AI 客服流式对话服务实现。
 * <p>
 * 职责：意图路由、离题拒答、FAQ（RAG）检索、商户查询/推荐等平台事实注入，并通过 SSE 流式输出。
 * 当 {@code app.ai.enabled=false} 或未注入流式模型时，使用本地固定文案分片兜底，不调用大模型。
 */
@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    /** 离题意图时返回给前端的固定拒答文案 */
    private static final String OFF_TOPIC_REPLY = "我无法回答。";

    /**
     * 基础系统提示词。
     * 在 FAQ 检索不可用、未命中上下文且非「商户查询/推荐」技能分支时使用。
     */
    private static final String BASE_SYSTEM_PROMPT = """
            你是「本地生活服务点评」平台的中文客服助手。仅回答与本平台相关的问题：店铺浏览与搜索、点评与评分、附近商户、店铺入驻与管理、秒杀与订单、退款与客服渠道等。
            回答简洁、友好、可执行；不得编造平台不存在的功能或未提供的商户数据。
            """;

    /**
     * RAG 增强系统提示词模板。
     * {@code %s} 替换为检索到的 FAQ 段落拼接结果。
     */
    private static final String RAG_SYSTEM_PROMPT_TEMPLATE = """
            你是「本地生活服务点评」平台的中文客服助手。请严格基于以下参考资料回答用户问题。
            若参考资料中没有相关信息，请诚实说明并建议联系人工客服（热线400-000-0000）。
            不得编造平台不存在的功能；不得捏造未在资料中出现的店铺或订单规则。
            
            === 参考资料 ===
            %s
            === 参考资料结束 ===
            """;

    /**
     * 技能分支系统提示词模板（平台数据 + 可选 FAQ）。
     * 第一个 {@code %s} 为平台事实文本；第二个 {@code %s} 为 FAQ 块（可为空）。
     */
    private static final String SKILL_AND_RAG_TEMPLATE = """
            你是「本地生活服务点评」平台的中文客服助手。请仅根据下方「平台数据」与「参考资料」回答用户，不要编造数据中未出现的店铺名、价格、库存、地址或距离。
            若平台数据已说明无法查询（如缺少定位），请按数据提示引导用户。若无合适数据，如实说明并建议用户更换关键词或前往首页浏览。
            回答简洁、友好、可执行。
            
            === 平台数据 ===
            %s
            === 平台数据结束 ===
            %s
            """;

    /** LangChain4j 流式对话模型；AI 关闭或未配置时为 null */
    @Autowired(required = false)
    private OpenAiStreamingChatModel streamingChatModel;

    /** 用户消息意图路由器；未注入时按「平台知识」兜底 */
    @Autowired(required = false)
    private AiChatIntentRouter intentRouter;

    /** FAQ 向量/关键词检索服务 */
    @Autowired
    private FaqRetrieverService faqRetrieverService;

    /** 按意图拼装商户/推荐等平台事实文本 */
    @Autowired
    private AiChatSkillService aiChatSkillService;

    /** 是否启用大模型对话，对应配置 {@code app.ai.enabled} */
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    /**
     * 流式 AI 客服：意图路由、离题拒答、RAG/技能提示组装、SSE 推送分片；AI 关闭时本地兜底。
     * <p>不向调用方抛出受检异常；流式过程中的错误通过 SSE {@code error} 事件或日志体现。
     *
     * @param req 当前用户消息、可选历史、经纬度等
     * @return 已创建且异步写入事件的 {@link SseEmitter}（事件名 chunk / done / error）
     */
    @Override
    public SseEmitter chatStream(AiChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);

        // AI 未启用或无流式模型：异步输出本地兜底文案
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

        try {
            AiChatIntent intent = routeIntent(req.getMessage());

            // 离题：不调用模型，直接推送固定拒答
            if ("off_topic".equals(intent.getIntent())) {
                CompletableFuture.runAsync(() -> {
                    try {
                        streamFixedReply(OFF_TOPIC_REPLY, emitter);
                    } catch (Exception e) {
                        sendError(emitter, e.getMessage());
                    } finally {
                        sendDone(emitter);
                    }
                });
                return emitter;
            }

            List<ChatMessage> messages = buildMessagesWithIntent(req, intent);
            streamingChatModel.chat(
                    ChatRequest.builder().messages(messages).build(),
                    new StreamingChatResponseHandler() {
                        /**
                         * 模型流式分片到达时推送到前端。
                         *
                         * @param token 当前文本分片
                         */
                        @Override
                        public void onPartialResponse(String token) {
                            try {
                                emitter.send(SseEmitter.event().name("chunk").data(token));
                            } catch (IOException e) {
                                // 客户端可能已断开，仅调试日志
                                log.debug("SSE send failed (client may have disconnected): {}", e.getMessage());
                            }
                        }

                        /**
                         * 流式响应完整结束。
                         *
                         * @param response LangChain4j 完整响应对象
                         */
                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            sendDone(emitter);
                        }

                        /**
                         * 模型或链路异常：推送 error 并结束 emitter。
                         *
                         * @param error 底层异常
                         */
                        @Override
                        public void onError(Throwable error) {
                            log.error("LangChain4j streaming error", error);
                            sendError(emitter, error.getMessage());
                            sendDone(emitter);
                        }
                    }
            );
        } catch (Exception e) {
            // 启动流式调用失败：异步通知前端并关闭
            log.error("Failed to start streaming chat", e);
            CompletableFuture.runAsync(() -> {
                sendError(emitter, e.getMessage());
                sendDone(emitter);
            });
        }

        return emitter;
    }

    /**
     * 解析用户消息的客服意图；无路由器时默认为平台知识类。
     *
     * @param message 用户当前输入文本
     * @return 意图对象（含 intent / subIntent 等）
     */
    private AiChatIntent routeIntent(String message) {
        if (intentRouter != null) {
            return intentRouter.route(message);
        }
        AiChatIntent fallback = new AiChatIntent();
        fallback.setIntent("platform_knowledge");
        return fallback;
    }

    /**
     * 根据意图组装系统提示、多轮历史与当前用户消息，供大模型消费。
     *
     * @param req    请求体（含 history、message）
     * @param intent 路由后的意图
     * @return LangChain4j {@link ChatMessage} 列表
     */
    private List<ChatMessage> buildMessagesWithIntent(AiChatRequest req, AiChatIntent intent) {
        String systemPrompt = buildSystemPromptForIntent(req, intent);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        if (req.getHistory() != null) {
            // 将前端历史轮次映射为 UserMessage / AiMessage
            for (AiChatMessage item : req.getHistory()) {
                if (item == null) {
                    continue;
                }
                String content = safeText(item.getContent());
                if (content.isEmpty()) {
                    continue;
                }
                if ("assistant".equalsIgnoreCase(item.getRole())) {
                    messages.add(AiMessage.from(content));
                } else {
                    messages.add(UserMessage.from(content));
                }
            }
        }

        messages.add(UserMessage.from(safeText(req.getMessage())));
        return messages;
    }

    /**
     * 按意图选择系统提示：商户查询/推荐走「平台数据 + FAQ」模板，其余走纯 RAG 或基础提示。
     *
     * @param req    请求（用于技能事实与 FAQ 查询）
     * @param intent 当前意图
     * @return 系统提示词字符串
     */
    private String buildSystemPromptForIntent(AiChatRequest req, AiChatIntent intent) {
        String in = intent.getIntent() == null ? "platform_knowledge" : intent.getIntent();
        // 商户查询或推荐：注入技能拼装的事实，并可选附加 FAQ
        if ("merchant_query".equals(in) || "recommend".equals(in)) {
            String facts = aiChatSkillService.buildPlatformDataFacts(intent, req);
            if (facts == null || facts.isBlank()) {
                facts = "（当前无额外平台数据，请根据用户问题做一般性引导。）";
            }
            String faqBlock = buildFaqReferenceBlock(req.getMessage());
            if (faqBlock.isEmpty()) {
                return SKILL_AND_RAG_TEMPLATE.formatted(facts, "");
            }
            return SKILL_AND_RAG_TEMPLATE.formatted(facts,
                    "\n=== 参考资料（FAQ） ===\n" + faqBlock + "\n=== 参考资料结束 ===\n");
        }
        return buildSystemPromptWithRag(req.getMessage());
    }

    /**
     * 根据用户问题检索 FAQ 并格式化为参考资料块；检索不可用时返回空串。
     *
     * @param userQuery 用户当前问题文本
     * @return 带编号的 FAQ 段落拼接，或空字符串
     */
    private String buildFaqReferenceBlock(String userQuery) {
        if (!faqRetrieverService.isAvailable()) {
            return "";
        }
        List<String> contexts = faqRetrieverService.retrieve(userQuery);
        if (contexts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(contexts.get(i)).append("\n\n");
        }
        return sb.toString().strip();
    }

    /**
     * 构造「平台知识 + RAG」系统提示：有 FAQ 命中则嵌入模板，否则使用基础提示。
     *
     * @param userQuery 用户问题（用于检索）
     * @return 系统提示词
     */
    private String buildSystemPromptWithRag(String userQuery) {
        if (!faqRetrieverService.isAvailable()) {
            return BASE_SYSTEM_PROMPT;
        }
        List<String> contexts = faqRetrieverService.retrieve(userQuery);
        if (contexts.isEmpty()) {
            return BASE_SYSTEM_PROMPT;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(contexts.get(i)).append("\n\n");
        }
        return RAG_SYSTEM_PROMPT_TEMPLATE.formatted(sb.toString().strip());
    }

    /**
     * 将固定全文按固定步长分片，模拟流式输出到 SSE。
     *
     * @param text    完整文案
     * @param emitter SSE 发射器
     * @throws IOException 发送事件失败时抛出
     */
    private void streamFixedReply(String text, SseEmitter emitter) throws IOException {
        int batch = 12;
        for (int i = 0; i < text.length(); i += batch) {
            int end = Math.min(text.length(), i + batch);
            emitter.send(SseEmitter.event().name("chunk").data(text.substring(i, end)));
        }
    }

    /**
     * AI 关闭时的本地兜底：提示用户可问范围并 echo 当前问题。
     *
     * @param message 用户原问题
     * @param emitter SSE 发射器
     * @throws IOException 发送事件失败时抛出
     */
    private void streamLocalFallback(String message, SseEmitter emitter) throws IOException {
        String fallback = "你好，我是生活服务点评平台 AI 客服。你可以问我：店铺怎么选、评分怎么看、下单流程、退款取消等问题。你刚刚的问题是：" + safeText(message);
        int batch = 12;
        for (int i = 0; i < fallback.length(); i += batch) {
            int end = Math.min(fallback.length(), i + batch);
            emitter.send(SseEmitter.event().name("chunk").data(fallback.substring(i, end)));
        }
    }

    /**
     * 发送流结束标记并完成 SSE。
     *
     * @param emitter SSE 发射器
     */
    private void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }

    /**
     * 向前端推送错误事件（不改变 complete 语义，常与 {@link #sendDone} 配合）。
     *
     * @param emitter SSE 发射器
     * @param msg     错误简述
     */
    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data("AI 对话失败：" + msg));
        } catch (IOException ignored) {
        }
    }

    /**
     * 文本安全处理：null 视为空串，否则 trim。
     *
     * @param value 原始字符串
     * @return 非 null 的修剪后文本
     */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
