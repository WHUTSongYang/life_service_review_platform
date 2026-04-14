package com.lifereview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 用户发起 AI 客服对话的请求体：必带当前问题，可选历史与地理位置。
 */
@Data
public class AiChatRequest {

    /** 用户本轮输入的问题或指令文本，不能为空 */
    @NotBlank
    private String message;

    /** 此前多轮对话历史；为 null 或空列表表示首轮对话 */
    private List<AiChatMessage> history;

    /** 用户当前纬度（可选）；与经度同时提供时可支持「附近店铺」等意图 */
    private Double latitude;

    /** 用户当前经度（可选） */
    private Double longitude;
}
