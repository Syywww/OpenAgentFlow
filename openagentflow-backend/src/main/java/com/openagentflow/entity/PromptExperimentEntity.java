package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 提示词实验表。
 * <p>对应数据库表：prompt_experiment。</p>
 */
@TableName("prompt_experiment")
public class PromptExperimentEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 实验编码。 */
    @TableField("experiment_code")
    private String experimentCode;

    /** 实验名称。 */
    @TableField("experiment_name")
    private String experimentName;

    /** 提示词模板ID。 */
    @TableField("prompt_template_id")
    private String promptTemplateId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 数据集ID。 */
    @TableField("dataset_id")
    private String datasetId;

    /** TRAFFIC策略。 */
    @TableField("traffic_policy")
    private String trafficPolicy;

    /** 主要胜出指标。 */ @TableField("metric_key") private String metricKey;
    /** 自动选优最小样本数。 */ @TableField("min_sample_size") private Integer minSampleSize;
    /** 自动选优置信阈值。 */ @TableField("confidence_threshold") private java.math.BigDecimal confidenceThreshold;
    /** 是否启用自动选优。 */ @TableField("auto_winner_enabled") private Boolean autoWinnerEnabled;
    /** 胜出变体ID。 */ @TableField("winner_variant_id") private String winnerVariantId;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 所有者用户ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** ENDED时间。 */
    @TableField("ended_at")
    private LocalDateTime endedAt;

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

    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getExperimentCode() {
        return experimentCode;
    }

    public void setExperimentCode(String experimentCode) {
        this.experimentCode = experimentCode;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public void setPromptTemplateId(String promptTemplateId) {
        this.promptTemplateId = promptTemplateId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getTrafficPolicy() {
        return trafficPolicy;
    }

    public void setTrafficPolicy(String trafficPolicy) {
        this.trafficPolicy = trafficPolicy;
    }

    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public Integer getMinSampleSize() { return minSampleSize; }
    public void setMinSampleSize(Integer minSampleSize) { this.minSampleSize = minSampleSize; }
    public java.math.BigDecimal getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(java.math.BigDecimal confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    public Boolean getAutoWinnerEnabled() { return autoWinnerEnabled; }
    public void setAutoWinnerEnabled(Boolean autoWinnerEnabled) { this.autoWinnerEnabled = autoWinnerEnabled; }
    public String getWinnerVariantId() { return winnerVariantId; }
    public void setWinnerVariantId(String winnerVariantId) { this.winnerVariantId = winnerVariantId; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
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
