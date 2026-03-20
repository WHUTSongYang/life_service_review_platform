// 包声明：DTO 所在包
package com.lifereview.dto;

import lombok.Data;

/** AI 客服对话历史中的单条消息：role 为 user 或 assistant，content 为文本 */
@Data
public class AiChatMessage {

    /** 消息角色：user 或 assistant */
    private String role;

    /** 消息文本内容 */
    private String content;
}
