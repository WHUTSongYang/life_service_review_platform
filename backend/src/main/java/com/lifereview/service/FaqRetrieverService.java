package com.lifereview.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * RAG（检索增强生成）场景下的 FAQ 知识库检索服务。
 * <p>
 * 应用启动时从配置的 Markdown 资源加载 FAQ，按二级标题 {@code ##} 拆分为段落，
 * 逐段向量化并写入内存向量库；查询时根据用户问题向量检索最相关的若干段落文本。
 * </p>
 */
@Service
@Slf4j
public class FaqRetrieverService {

    /** 按 Markdown 二级标题行首 {@code ## } 进行前瞻拆分用的正则（多行模式）。 */
    private static final Pattern SECTION_SPLIT = Pattern.compile("(?=^## )", Pattern.MULTILINE);

    /** 是否启用 RAG FAQ 索引与检索，对应配置项 {@code app.rag.enabled}。 */
    @Value("${app.rag.enabled:false}")
    private boolean ragEnabled;

    /** 向量检索默认返回的最大条数，对应配置项 {@code app.rag.top-k}。 */
    @Value("${app.rag.top-k:3}")
    private int defaultTopK;

    /** 向量检索默认最低相似度阈值（0～1），低于该分数的匹配会被过滤，对应 {@code app.rag.min-score}。 */
    @Value("${app.rag.min-score:0.5}")
    private double defaultMinScore;

    /** FAQ Markdown 静态资源位置，对应 {@code app.rag.faq-resource}，默认 {@code classpath:rag/faq.md}。 */
    @Value("${app.rag.faq-resource:classpath:rag/faq.md}")
    private Resource faqResource;

    /** 文本向量嵌入模型；由 {@code AiLangChainConfig} 等条件注入。RAG 关闭时可能不存在，此时为 null。 */
    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    /** 内存向量存储，保存各 FAQ 段落的 {@link Embedding} 与 {@link TextSegment}。 */
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;

    /** 是否已成功完成 FAQ 索引（向量化并写入 {@link #embeddingStore}）。 */
    private boolean initialized = false;

    /**
     * 启动后初始化：读取 FAQ 文档、按 {@code ##} 拆段、向量化并批量写入内存向量库。
     * <p>
     * 若 RAG 未启用、无可用 {@link EmbeddingModel}，或文档无有效段落，则跳过或记录告警；
     * 读取资源失败时记录错误日志，不向外抛出。
     * </p>
     */
    @PostConstruct
    void init() {
        if (!ragEnabled || embeddingModel == null) {
            log.info("RAG disabled or EmbeddingModel not available, skipping FAQ indexing");
            return;
        }
        try {
            // 1. 读取 FAQ Markdown 全文
            String faqText = faqResource.getContentAsString(StandardCharsets.UTF_8);

            // 2. 按二级标题拆分为多个知识段落
            List<TextSegment> segments = splitFaqSections(faqText);
            if (segments.isEmpty()) {
                log.warn("No FAQ sections found in {}", faqResource);
                return;
            }

            // 3. 逐段生成嵌入向量
            embeddingStore = new InMemoryEmbeddingStore<>();
            List<Embedding> embeddings = new ArrayList<>();
            for (TextSegment seg : segments) {
                Response<Embedding> resp = embeddingModel.embed(seg);
                embeddings.add(resp.content());
            }

            // 4. 向量与段落一一对应写入内存库
            embeddingStore.addAll(embeddings, segments);
            initialized = true;
            log.info("FAQ RAG initialized: {} sections indexed", segments.size());
        } catch (IOException e) {
            log.error("Failed to load FAQ resource: {}", faqResource, e);
        }
    }

    /**
     * 按用户问题检索最相关的 FAQ 段落文本，使用默认 {@code topK} 与 {@code minScore}。
     *
     * @param query 用户自然语言问题，建议非 null；行为与三参数重载一致
     * @return 匹配到的段落文本列表；未初始化或检索失败时返回空列表
     */
    public List<String> retrieve(String query) {
        return retrieve(query, defaultTopK, defaultMinScore);
    }

    /**
     * 将用户问题编码为查询向量，在内存向量库中按相似度检索，返回分数不低于 {@code minScore} 的前若干段原文。
     *
     * @param query    用户自然语言问题
     * @param topK     最多返回的匹配条数
     * @param minScore 最低相似度阈值（0～1）
     * @return 按检索顺序排列的段落文本列表；索引未就绪或发生异常时返回空列表
     */
    public List<String> retrieve(String query, int topK, double minScore) {
        if (!initialized || embeddingStore == null || embeddingModel == null) {
            return Collections.emptyList();
        }
        try {
            // 问题向量化作为查询向量
            Response<Embedding> queryResp = embeddingModel.embed(query);

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryResp.content())
                    .maxResults(topK)
                    .minScore(minScore)
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

            List<String> texts = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : result.matches()) {
                if (match.embedded() != null) {
                    texts.add(match.embedded().text());
                }
            }
            return texts;
        } catch (Exception e) {
            log.warn("FAQ retrieval failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 判断 FAQ 向量索引是否已就绪，可供检索使用。
     *
     * @return 已成功完成初始化并向量入库时为 true，否则为 false
     */
    public boolean isAvailable() {
        return initialized;
    }

    /**
     * 将整篇 FAQ Markdown 按二级标题 {@code ##} 拆成多个 {@link TextSegment}，跳过空段与单独的一级标题段。
     *
     * @param markdown FAQ 全文
     * @return 非空段落列表；无有效段时可能为空列表
     */
    private List<TextSegment> splitFaqSections(String markdown) {
        String[] parts = SECTION_SPLIT.split(markdown);
        List<TextSegment> segments = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.strip();
            // 跳过空段以及单独的一级标题（文档总标题不作为知识段）
            if (trimmed.isEmpty() || trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                continue;
            }
            segments.add(TextSegment.from(trimmed));
        }
        return segments;
    }
}
