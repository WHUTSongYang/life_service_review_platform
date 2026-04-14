package com.lifereview.dto;

import lombok.Data;

/**
 * AI 客服对话历史中的单条消息，用于多轮上下文传递。
 */
@Data
public class AiChatMessage {

    /** 消息角色，取值为 user（用户）或 assistant（助手） */
    private String role;

    /** 该条消息的文本内容 */
    private String content;
}
