package com.openagentflow.domain.session;

import java.time.LocalDateTime;

/**
 * Agent 会话消息摘要。
 */
public class AgentMessageSummary {

    /** 消息 ID。 */
    private String id;

    /** 会话 ID。 */
    private String sessionId;

    /** 消息角色：user、assistant、tool。 */
    private String role;

    /** 消息内容。 */
    private String content;

    /** 内容类型。 */
    private String contentType;

    /** 工具调用 ID。 */
    private String toolCallId;

    /** Token 数量。 */
    private Integer tokenCount;

    /** 元数据 JSON。 */
    private String metadata;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
