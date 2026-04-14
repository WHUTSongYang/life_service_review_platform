package com.lifereview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifereview.dto.AiChatIntent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.List;
/**
 * AI 客服意图路由服务。
 *
 * <p>
 * <p>
 * 通过专用路由模型仅输出 JSON，将用户消息分类为：拒答（无关话题）、平台知识、商户查询、推荐等，
 * <p>
 * 供后续技能分支与 RAG/商户检索使用。
 *
 * </p>
 *
 * @see AiChatIntent
 */

@Service
@Slf4j
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiChatIntentRouter {
    /**
     * 解析失败、空消息或非法 intent 时回退使用的默认意图（平台知识）。
     */
    private static final AiChatIntent FALLBACK = intentOf("platform_knowledge", null, null, null, null);

    /**
     * 路由模型的系统提示词：约束只输出 JSON，并说明各 intent/sub_intent 与字段含义。
     */
    private static final String ROUTER_SYSTEM = """
            
            你是意图分类器，只输出一个 JSON 对象，不要 Markdown、不要解释。
            
            平台是「本地生活服务点评」：店铺浏览、点评、附近商户、秒杀商品与订单等。
            
            intent 取值：
            
            - off_topic：与该平台完全无关（如纯数学、编程题、天气、其他公司产品、闲聊与该平台业务无关等）
            
            - platform_knowledge：平台规则、使用说明、FAQ、点评与订单一般问题等，且不需要查实时店铺列表/详情/附近/推荐
            
            - merchant_query：用户想搜索店铺、看某店详情、或查附近店铺
            
            - recommend：用户明确要求推荐店铺或推荐商品/秒杀商品
            
            sub_intent（在 merchant_query / recommend 时尽量填写）：
            
            - search_shop：按名称或类型搜索店铺
            
            - shop_detail：指定某家店详情（尽量填 shop_id；若用户只说店名则 keyword 填店名）
            
            - nearby：附近有什么店（需要用户位置，无位置时仍标 nearby）
            
            - recommend_shops：推荐好店/高分店等
            
            - recommend_products：推荐商品、秒杀、买什么
            
            其他字段：keyword（搜索词）、type（店铺分类）、shop_id（数字 ID，若能从用户话中确定）。
            
            输出示例：{"intent":"merchant_query","sub_intent":"search_shop","keyword":"火锅","type":"","shop_id":null}
            
            """;


    /**
     * 将用户最新一条消息填入路由用户提示的模板（占位符为 {@code %s}）。
     */

    private static final String ROUTER_USER_TEMPLATE = """
            
            用户最新一条消息：
            
            \"%s\"
            
            """;


    /**
     * 用于将模型返回的 JSON 反序列化为 {@link AiChatIntent}。
     */

    private final ObjectMapper objectMapper;

    /**
     * 低温度、专用于意图分类的 OpenAI 聊天模型（Bean 名 {@code openAiRouterModel}）。
     */

    private final OpenAiChatModel routerModel;


    /**
     * 构造意图路由器。
     *
     * @param objectMapper JSON 解析器
     * @param routerModel  路由用 OpenAI 聊天模型
     */

    public AiChatIntentRouter(ObjectMapper objectMapper, @Qualifier("openAiRouterModel") OpenAiChatModel routerModel) {

        this.objectMapper = objectMapper;

        this.routerModel = routerModel;

    }


    /**
     * 根据用户最新一条消息调用路由模型，解析 JSON 为意图对象。
     *
     * <p>
     * <p>
     * 若消息为空或仅空白、无法提取 JSON、解析异常或 intent 不在白名单内，则返回 {@link #FALLBACK}（platform_knowledge）。
     *
     * </p>
     *
     * @param userMessage 用户最新一条自然语言消息，可为 null
     * @return 归一化后的 {@link AiChatIntent}，失败时为默认平台知识意图
     */

    public AiChatIntent route(String userMessage) {

        if (userMessage == null || userMessage.isBlank()) {

            return FALLBACK;

        }

        try {
            // 组装系统消息与用户消息，调用路由模型得到原始文本
            String raw = routerModel.chat(ChatRequest.builder()
                    .messages(List.<ChatMessage>of(
                            SystemMessage.from(ROUTER_SYSTEM),
                            UserMessage.from(ROUTER_USER_TEMPLATE.formatted(userMessage.trim().replace("\"", "'")))))
                    .build()).aiMessage().text();
            // 从原文中提取首个成对花括号 JSON 子串
            String json = extractJsonBlock(raw);
            if (json == null) {
                log.warn("Intent router JSON not found, raw={}", truncate(raw));
                return FALLBACK;
            }
            AiChatIntent parsed = objectMapper.readValue(json, AiChatIntent.class);
            return normalize(parsed);
        } catch (Exception e) {
            log.warn("Intent router failed: {}", e.getMessage());
            return FALLBACK;
        }
    }


    /**
     * 构造指定字段的 {@link AiChatIntent} 实例（用于默认值与内部构造）。
     *
     * @param intent 主意图字符串
     * @param sub    子意图，可为 null
     * @param kw     关键词，可为 null
     * @param type   店铺分类等类型，可为 null
     * @param shopId 店铺 ID，可为 null
     * @return 已设置上述字段的新意图对象
     */

    private static AiChatIntent intentOf(String intent, String sub, String kw, String type, Long shopId) {
        AiChatIntent i = new AiChatIntent();
        i.setIntent(intent);
        i.setSubIntent(sub);
        i.setKeyword(kw);
        i.setType(type);
        i.setShopId(shopId);
        return i;
    }


    /**
     * 校验 {@code intent} 取值是否在允许集合内，并将 {@code sub_intent} 规范为小写。
     *
     * @param p 模型解析得到的意图，可为 null
     * @return 合法则返回同一对象（已就地规范化）；不合法或 intent 为空则返回 {@link #FALLBACK}
     */

    private static AiChatIntent normalize(AiChatIntent p) {
        if (p == null || p.getIntent() == null || p.getIntent().isBlank()) {
            return FALLBACK;
        }
        String intent = p.getIntent().trim().toLowerCase();
        if (!List.of("off_topic", "platform_knowledge", "merchant_query", "recommend").contains(intent)) {
            return FALLBACK;
        }
        p.setIntent(intent);
        if (p.getSubIntent() != null) {
            p.setSubIntent(p.getSubIntent().trim().toLowerCase());
        }
        return p;
    }


    /**
     * 从模型原文中提取首个与花括号深度配对的 JSON 子串（从第一个 {@code '{'} 开始）。
     *
     * @param s 模型输出的原始字符串，可为 null
     * @return 提取到的 JSON 文本；无法配对或未找到 {@code '{'} 时返回 null
     */

    private static String extractJsonBlock(String s) {
        if (s == null) {
            return null;
        }
        int start = s.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }


    /**
     * 将过长字符串截断用于日志输出，避免刷屏。
     *
     * @param s 原始字符串，可为 null
     * @return 长度不超过 200 的字符串；超长时追加省略号；null 时返回空串
     */

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

}


