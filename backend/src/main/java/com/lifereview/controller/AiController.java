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
 * AI 功能统一入口。
 * generate-review：根据店铺名称和类型生成点评文案，AI 不可用时走本地模板。
 * chat/stream：智能客服 SSE 流式对话，支持 RAG 知识库增强，逐 token 推送。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    // AI 帮写点评服务
    private final AiReviewService aiReviewService;

    // AI 客服流式对话服务
    private final AiChatService aiChatService;

    // AI 帮写点评，根据 shopName、shopType 生成评价文案
    @PostMapping("/generate-review")
    public ApiResponse<Map<String, String>> generateReview(@Valid @RequestBody AiReviewRequest req) {
        String text = aiReviewService.generateReviewText(req.getShopName(), req.getShopType());
        return ApiResponse.ok(Map.of("content", text));
    }

    // AI 智能客服 SSE 流式对话，event: chunk 推送文本，event: done 结束
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest req) {
        return aiChatService.chatStream(req);
    }
}
