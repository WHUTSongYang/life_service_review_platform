package com.lifereview.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 与 OpenAI 兼容 API 的模型 Bean 配置。
 * <p>
 * 集中创建：非流式对话模型（点评生成、意图路由 JSON）、流式对话模型（客服 SSE）、
 * 向量嵌入模型（RAG FAQ）。配置项来自 {@code app.ai.*} 与 {@code app.rag.*}，
 * 并按 {@code app.ai.enabled}、{@code app.rag.enabled} 条件装配。
 * </p>
 */
@Configuration
public class AiLangChainConfig {

    /** 大模型 HTTP API 基地址（兼容 OpenAI 协议的网关或直连） */
    @Value("${app.ai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    /** 调用大模型所需的 API Key */
    @Value("${app.ai.api-key:}")
    private String apiKey;

    /** 对话/路由使用的模型名称，如 deepseek-chat、qwen-plus 等 */
    @Value("${app.ai.model:deepseek-chat}")
    private String modelName;

    /** 单次 HTTP 调用超时时间（秒） */
    @Value("${app.ai.timeout-seconds:20}")
    private int timeoutSeconds;

    /**
     * 非流式聊天模型：中等温度与较短输出，用于 AI 帮写点评等场景。
     * <p>仅当 {@code app.ai.enabled=true} 时注册。</p>
     *
     * @return 配置完成的 {@link OpenAiChatModel}
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(normalizeUrl(baseUrl))
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.7)
                .maxTokens(300)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 非流式、低温度模型：用于 AI 客服意图路由，期望稳定 JSON 输出。
     * <p>Bean 名为 {@code openAiRouterModel}，供 {@link com.lifereview.service.AiChatIntentRouter} 注入。</p>
     *
     * @param routerMaxTokens 路由调用允许的最大生成 token 数
     * @return 专用于意图分类/路由的 {@link OpenAiChatModel}
     */
    @Bean(name = "openAiRouterModel")
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
    public OpenAiChatModel openAiRouterModel(
            @Value("${app.ai.router-max-tokens:256}") int routerMaxTokens) {
        return OpenAiChatModel.builder()
                .baseUrl(normalizeUrl(baseUrl))
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .maxTokens(routerMaxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 流式聊天模型：用于 AI 客服 SSE 等边生成边输出场景。
     * <p>仅当 {@code app.ai.enabled=true} 时注册。</p>
     *
     * @return 配置完成的 {@link OpenAiStreamingChatModel}
     */
    @Bean
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(normalizeUrl(baseUrl))
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.7)
                .maxTokens(800)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 文本向量嵌入模型：用于 RAG 知识库检索前的查询与文档向量化。
     * <p>仅当 {@code app.rag.enabled=true} 时注册。</p>
     *
     * @param embeddingModelName 嵌入模型名称，默认可与向量服务约定一致
     * @return 配置完成的 {@link OpenAiEmbeddingModel}
     */
    @Bean
    @ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Value("${app.rag.embedding-model:text-embedding-v3}") String embeddingModelName) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(normalizeUrl(baseUrl))
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 规范化 API 基地址：去空白、去尾部斜杠，避免与路径拼接出现双斜杠。
     *
     * @param url 配置中的原始 base-url，可为空
     * @return 可用于 LangChain4j 客户端的规范化 URL
     */
    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return "https://api.deepseek.com"; // 缺省回退
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
