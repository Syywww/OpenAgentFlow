package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent知识BINDING表。
 * <p>对应数据库表：agent_knowledge_binding。</p>
 */
@TableName("agent_knowledge_binding")
public class AgentKnowledgeBindingEntity {

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 知识库ID。 */
    @TableField("knowledge_base_id")
    private String knowledgeBaseId;

    /** 检索配置。 */
    @TableField("retrieval_config")
    private String retrievalConfig;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getRetrievalConfig() {
        return retrievalConfig;
    }

    public void setRetrievalConfig(String retrievalConfig) {
        this.retrievalConfig = retrievalConfig;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
