package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Prompt版本与实验运行指标实体。 */
@TableName("prompt_runtime_metric")
public class PromptRuntimeMetricEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 模板ID。 */ @TableField("template_id") private String templateId;
    /** 版本ID。 */ @TableField("version_id") private String versionId;
    /** 实验ID。 */ @TableField("experiment_id") private String experimentId;
    /** 变体ID。 */ @TableField("variant_id") private String variantId;
    /** 运行ID。 */ @TableField("run_id") private String runId;
    /** Agent ID。 */ @TableField("agent_id") private String agentId;
    /** 是否成功。 */ @TableField("success") private Boolean success;
    /** 质量得分。 */ @TableField("quality_score") private BigDecimal qualityScore;
    /** 耗时毫秒。 */ @TableField("latency_ms") private Integer latencyMs;
    /** Token数量。 */ @TableField("token_count") private Integer tokenCount;
    /** 成本金额。 */ @TableField("cost_amount") private BigDecimal costAmount;
    /** 可信回答是否通过。 */ @TableField("trusted_answer_passed") private Boolean trustedAnswerPassed;
    /** 工具调用是否成功。 */ @TableField("tool_success") private Boolean toolSuccess;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }
    public String getExperimentId() { return experimentId; }
    public void setExperimentId(String experimentId) { this.experimentId = experimentId; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }
    public Boolean getTrustedAnswerPassed() { return trustedAnswerPassed; }
    public void setTrustedAnswerPassed(Boolean trustedAnswerPassed) { this.trustedAnswerPassed = trustedAnswerPassed; }
    public Boolean getToolSuccess() { return toolSuccess; }
    public void setToolSuccess(Boolean toolSuccess) { this.toolSuccess = toolSuccess; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
