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

/** RAG 知识库检索服务。启动时加载 FAQ 文档、按 ## 拆分、向量化、存入内存；查询时按用户问题向量检索 TopK 段落 */
@Service
@Slf4j
public class FaqRetrieverService {

    /** 按 Markdown 二级标题 ## 拆分文档的正则 */
    private static final Pattern SECTION_SPLIT = Pattern.compile("(?=^## )", Pattern.MULTILINE);

    /** RAG 功能总开关，对应 application.yml 中的 app.rag.enabled */
    @Value("${app.rag.enabled:false}")
    private boolean ragEnabled;

    /** 向量检索时返回的最大结果数，对应 app.rag.top-k */
    @Value("${app.rag.top-k:3}")
    private int defaultTopK;

    /** 向量检索时的最低相似度阈值（0~1），低于此分数的结果会被过滤，对应 app.rag.min-score */
    @Value("${app.rag.min-score:0.5}")
    private double defaultMinScore;

    /** FAQ 静态知识文档的 Spring Resource 路径，对应 app.rag.faq-resource */
    @Value("${app.rag.faq-resource:classpath:rag/faq.md}")
    private Resource faqResource;

    /** 向量嵌入模型，由 AiLangChainConfig 条件注入。app.rag.enabled=false 时 Bean 不存在，此处为 null */
    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    /** 内存向量库，存储 FAQ 各段落的向量与原文 */
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;

    /** 标记索引是否成功完成 */
    private boolean initialized = false;

    /** 启动时自动执行：加载 FAQ 文档、按 ## 拆段、向量化、存入内存向量库。RAG 未启用或 EmbeddingModel 不可用时跳过 */
    @PostConstruct
    void init() {
        if (!ragEnabled || embeddingModel == null) {
            log.info("RAG disabled or EmbeddingModel not available, skipping FAQ indexing");
            return;
        }
        try {
            // 1. 读取 FAQ Markdown 文件全文
            String faqText = faqResource.getContentAsString(StandardCharsets.UTF_8);

            // 2. 按 "## " 二级标题拆分为多个知识段落
            List<TextSegment> segments = splitFaqSections(faqText);
            if (segments.isEmpty()) {
                log.warn("No FAQ sections found in {}", faqResource);
                return;
            }

            // 3. 逐段调用 EmbeddingModel 生成向量
            embeddingStore = new InMemoryEmbeddingStore<>();
            List<Embedding> embeddings = new ArrayList<>();
            for (TextSegment seg : segments) {
                Response<Embedding> resp = embeddingModel.embed(seg);
                embeddings.add(resp.content());
            }

            // 4. 批量存入内存向量库（Embedding 与 TextSegment 一一对应）
            embeddingStore.addAll(embeddings, segments);
            initialized = true;
            log.info("FAQ RAG initialized: {} sections indexed", segments.size());
        } catch (IOException e) {
            log.error("Failed to load FAQ resource: {}", faqResource, e);
        }
    }

    /** 根据用户问题检索最相关的 FAQ 段落，使用默认 topK 和 minScore */
    public List<String> retrieve(String query) {
        return retrieve(query, defaultTopK, defaultMinScore);
    }

    /** 根据用户问题检索最相关的 FAQ 段落。query 向量化后做余弦相似度搜索，返回 topK 条，相似度不低于 minScore */
    public List<String> retrieve(String query, int topK, double minScore) {
        if (!initialized || embeddingStore == null || embeddingModel == null) {
            return Collections.emptyList();
        }
        try {
            // 将用户问题转为查询向量
            Response<Embedding> queryResp = embeddingModel.embed(query);

            // 构造搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryResp.content())  // 查询向量
                    .maxResults(topK)                      // 最多返回 topK 条
                    .minScore(minScore)                    // 最低相似度阈值
                    .build();

            // 在内存向量库中执行搜索
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

            // 提取匹配结果中的原始文本
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

    /** 检查 RAG 知识库是否已就绪 */
    public boolean isAvailable() {
        return initialized;
    }

    /** 将 FAQ Markdown 按 ## 二级标题拆分为知识段落，跳过空白和一级标题 */
    private List<TextSegment> splitFaqSections(String markdown) {
        String[] parts = SECTION_SPLIT.split(markdown);
        List<TextSegment> segments = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.strip();
            // 跳过空段 和 一级标题（文档大标题不做知识段）
            if (trimmed.isEmpty() || trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                continue;
            }
            segments.add(TextSegment.from(trimmed));
        }
        return segments;
    }
}
