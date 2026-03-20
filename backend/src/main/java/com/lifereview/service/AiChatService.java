package com.lifereview.service;

import com.lifereview.dto.AiChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 客服流式对话服务接口。
 * 通过 SSE 推送 chunk 文本片段、error 错误、done 结束事件。
 */
public interface AiChatService {

    // 发起流式 AI 客服对话，返回 SseEmitter 用于推送响应
    SseEmitter chatStream(AiChatRequest req);
}
