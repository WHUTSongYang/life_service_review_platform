package com.lifereview.service;

import com.lifereview.dto.AiChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 业务职责说明：AI 客服流式对话；通过 SSE 向客户端推送文本片段、错误与结束事件。
 */
public interface AiChatService {

    /**
     * 发起流式 AI 客服对话。
     *
     * @param req 对话请求（消息历史、上下文等）
     * @return 用于向客户端推送 chunk、error、done 等事件的 SSE 发射器
     */
    SseEmitter chatStream(AiChatRequest req);
}
