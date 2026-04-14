package com.lifereview.controller;

import com.lifereview.common.ApiResponse;
import com.lifereview.dto.AiChatRequest;
import com.lifereview.dto.AiReviewRequest;
import com.lifereview.service.AiChatService;
import com.lifereview.service.AiReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * AI 功能统一入口控制器。
 * <p>URL 前缀：{@code /api/ai}。帮写点评与流式客服一般无需登录（以网关/安全配置为准）。</p>
 * <ul>
 *   <li>{@code /generate-review}：根据店铺名称与类型生成点评文案；模型不可用时可走本地模板。</li>
 *   <li>{@code /chat/stream}：智能客服 SSE，支持 RAG，按 chunk 推送文本。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    /** AI 帮写点评（文案生成）服务 */
    private final AiReviewService aiReviewService;

    /** AI 客服 SSE 流式对话服务 */
    private final AiChatService aiChatService;

    /**
     * AI 帮写点评：根据店铺名称、类型生成评价正文。
     *
     * @param req 包含 shopName、shopType 等字段的请求体
     * @return {@code content} 为生成文案的 Map 包装响应
     */
    @PostMapping("/generate-review")
    public ApiResponse<Map<String, String>> generateReview(@Valid @RequestBody AiReviewRequest req) {
        String text = aiReviewService.generateReviewText(req.getShopName(), req.getShopType());
        return ApiResponse.ok(Map.of("content", text));
    }

    /**
     * AI 智能客服：SSE 流式对话（{@code text/event-stream}）。
     *
     * @param req 会话消息、上下文等
     * @return 用于推送 token/chunk 的 {@link SseEmitter}
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest req) {
        return aiChatService.chatStream(req);
    }
}
