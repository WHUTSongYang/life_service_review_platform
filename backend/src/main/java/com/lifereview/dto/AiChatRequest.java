// 包声明：DTO 所在包
package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** AI 客服对话请求：message 必填，history 为可选对话历史 */
@Data
public class AiChatRequest {

    /** 用户当前输入的问题文本，不能为空 */
    @NotBlank
    private String message;

    /** 对话历史列表，null 或空表示首轮对话 */
    private List<AiChatMessage> history;
}
