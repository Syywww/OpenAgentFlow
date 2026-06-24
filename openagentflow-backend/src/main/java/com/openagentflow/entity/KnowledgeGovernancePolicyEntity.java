package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识库治理策略实体。
 * <p>对应数据库表：knowledge_governance_policy。</p>
 */
@TableName("knowledge_governance_policy")
public class KnowledgeGovernancePolicyEntity {

    /** 主键ID。 */
    @TableId("id")
    private String id;

    /** 策略编码。 */
    @TableField("policy_code")
    private String policyCode;

    /** 策略名称。 */
    @TableField("policy_name")
    private String policyName;

    /** 限定知识库ID，为空表示全局策略。 */
    @TableField("kb_id")
    private String kbId;

    /** 文档超过多少天未更新视为陈旧。 */
    @TableField("stale_days")
    private Integer staleDays;

    /** 分片最小Token数量。 */
    @TableField("min_chunk_tokens")
    private Integer minChunkTokens;

    /** 分片最大Token数量。 */
    @TableField("max_chunk_tokens")
    private Integer maxChunkTokens;

    /** 允许的最大失败文档数。 */
    @TableField("max_failed_documents")
    private Integer maxFailedDocuments;

    /** 是否要求知识库绑定至少一个智能体。 */
    @TableField("require_agent_binding")
    private Boolean requireAgentBinding;

    /** 是否要求向量同步到Milvus。 */
    @TableField("require_milvus_sync")
    private Boolean requireMilvusSync;

    /** 是否启用自动生成治理问题。 */
    @TableField("auto_issue_enabled")
    private Boolean autoIssueEnabled;

    /** 策略状态。 */
    @TableField("status")
    private String status;

    /** 创建人用户ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public Integer getStaleDays() {
        return staleDays;
    }

    public void setStaleDays(Integer staleDays) {
        this.staleDays = staleDays;
    }

    public Integer getMinChunkTokens() {
        return minChunkTokens;
    }

    public void setMinChunkTokens(Integer minChunkTokens) {
        this.minChunkTokens = minChunkTokens;
    }

    public Integer getMaxChunkTokens() {
        return maxChunkTokens;
    }

    public void setMaxChunkTokens(Integer maxChunkTokens) {
        this.maxChunkTokens = maxChunkTokens;
    }

    public Integer getMaxFailedDocuments() {
        return maxFailedDocuments;
    }

    public void setMaxFailedDocuments(Integer maxFailedDocuments) {
        this.maxFailedDocuments = maxFailedDocuments;
    }

    public Boolean getRequireAgentBinding() {
        return requireAgentBinding;
    }

    public void setRequireAgentBinding(Boolean requireAgentBinding) {
        this.requireAgentBinding = requireAgentBinding;
    }

    public Boolean getRequireMilvusSync() {
        return requireMilvusSync;
    }

    public void setRequireMilvusSync(Boolean requireMilvusSync) {
        this.requireMilvusSync = requireMilvusSync;
    }

    public Boolean getAutoIssueEnabled() {
        return autoIssueEnabled;
    }

    public void setAutoIssueEnabled(Boolean autoIssueEnabled) {
        this.autoIssueEnabled = autoIssueEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
