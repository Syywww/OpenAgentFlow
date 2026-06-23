package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * AgentMESSAGE表。
 * <p>对应数据库表：agent_message。</p>
 */
@TableName("agent_message")
public class AgentMessageEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 字段说明：SESSIONID。 */
    @TableField("session_id")
    private String sessionId;

    /** 父级MESSAGEID。 */
    @TableField("parent_message_id")
    private String parentMessageId;

    /** 角色。 */
    @TableField("role")
    private String role;

    /** 内容。 */
    @TableField("content")
    private String content;

    /** 内容类型。 */
    @TableField("content_type")
    private String contentType;

    /** 工具CALLID。 */
    @TableField("tool_call_id")
    private String toolCallId;

    /** Token数量。 */
    @TableField("token_count")
    private Integer tokenCount;

    /** 元数据JSON。 */
    @TableField("metadata")
    private String metadata;

    /** 创建时间。 */
    @TableField("created_at")
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

    public String getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(String parentMessageId) {
        this.parentMessageId = parentMessageId;
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
