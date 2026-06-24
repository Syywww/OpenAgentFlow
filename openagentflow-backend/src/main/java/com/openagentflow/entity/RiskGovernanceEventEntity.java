package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 风险治理事件表。
 * <p>对应数据库表：risk_governance_event。</p>
 */
@TableName("risk_governance_event")
public class RiskGovernanceEventEntity {

    /** 风险治理事件主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 风险事件编码。 */
    @TableField("event_code")
    private String eventCode;

    /** 风险事件类型。 */
    @TableField("event_type")
    private String eventType;

    /** 来源类型。 */
    @TableField("source_type")
    private String sourceType;

    /** 来源记录ID。 */
    @TableField("source_id")
    private String sourceId;

    /** 风险级别。 */
    @TableField("risk_level")
    private String riskLevel;

    /** 处置状态。 */
    @TableField("status")
    private String status;

    /** 风险标题。 */
    @TableField("title")
    private String title;

    /** 风险描述。 */
    @TableField("description")
    private String description;

    /** 所属工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 关联智能体ID。 */
    @TableField("agent_id")
    private String agentId;

    /** 关联工具ID。 */
    @TableField("tool_id")
    private String toolId;

    /** 关联运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 关联规则编码。 */
    @TableField("rule_code")
    private String ruleCode;

    /** 风险证据JSON。 */
    @TableField("evidence_json")
    private String evidenceJson;

    /** 建议处置动作。 */
    @TableField("recommended_action")
    private String recommendedAction;

    /** 处置人ID。 */
    @TableField("handled_by")
    private String handledBy;

    /** 处置时间。 */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /** 处置备注。 */
    @TableField("handle_note")
    private String handleNote;

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

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
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

