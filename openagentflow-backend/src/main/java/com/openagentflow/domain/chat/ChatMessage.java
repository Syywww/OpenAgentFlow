package com.openagentflow.domain.chat;

import java.util.List;

/**
 * 聊天消息对象。
 */
public class ChatMessage {

    /** 消息角色，system、user、assistant、tool。 */
    private String role;

    /** 消息内容。 */
    private String content;

    /** 工具调用 ID，用于 role=tool 的消息。 */
    private String toolCallId;

    /** 工具名称，用于 role=tool 的消息。 */
    private String name;

    /** assistant 消息中携带的工具调用列表。 */
    private List<ToolCallRequest> toolCalls;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ToolCallRequest> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallRequest> toolCalls) {
        this.toolCalls = toolCalls;
    }
}
