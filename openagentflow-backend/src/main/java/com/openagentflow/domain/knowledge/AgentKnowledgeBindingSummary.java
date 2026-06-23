package com.openagentflow.domain.knowledge;

import java.time.LocalDateTime;

/**
 * Agent 知识库绑定摘要。
 */
public class AgentKnowledgeBindingSummary {

    /** Agent ID。 */
    private String agentId;

    /** 知识库 ID。 */
    private String knowledgeBaseId;

    /** 知识库名称。 */
    private String kbName;

    /** 检索配置 JSON。 */
    private String retrievalConfig;

    /** 是否启用。 */
    private Boolean enabled;

    /** 创建时间。 */
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

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
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
