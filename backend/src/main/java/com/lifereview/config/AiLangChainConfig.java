// 包声明：配置类所在包
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
 * LangChain4j 模型统一配置类。
 * 集中创建三类模型 Bean：
 * - OpenAiChatModel：非流式，供 AI 帮写点评使用。
 * - OpenAiStreamingChatModel：流式，供 AI 客服 SSE 流式输出使用。
 * - OpenAiEmbeddingModel：向量嵌入，供 RAG FAQ 知识库检索使用。
 * 均读取 app.ai.* 配置（base-url、api-key、model 等），按 app.ai.enabled 或 app.rag.enabled 条件创建。
 */
@Configuration
public class AiLangChainConfig {

    /** 大模型 API 基地址，兼容 OpenAI 协议的网关（如 DashScope、DeepSeek） */
    @Value("${app.ai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    /** 大模型 API 密钥 */
    @Value("${app.ai.api-key:}")
    private String apiKey;

    /** 使用的模型名称，如 qwen-plus、deepseek-chat、gpt-4o-mini 等 */
    @Value("${app.ai.model:deepseek-chat}")
    private String modelName;

    /** 单次请求超时时间（秒） */
    @Value("${app.ai.timeout-seconds:20}")
    private int timeoutSeconds;

    /** 非流式对话模型，供 AI 帮写点评使用。仅当 app.ai.enabled=true 时创建 */
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

    /** 流式对话模型，供 AI 客服 SSE 流式输出。仅当 app.ai.enabled=true 时创建 */
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

    /** 向量嵌入模型，供 RAG 知识库使用。仅当 app.rag.enabled=true 时创建 */
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

    /** 规范化 URL：去除末尾斜杠，避免拼接路径时出现双斜杠 */
    private String normalizeUrl(String url) {
        // 空或空白时返回默认地址
        if (url == null || url.isBlank()) return "https://api.deepseek.com";
        // 去除首尾空白
        String trimmed = url.trim();
        // 若以 / 结尾则去掉
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
