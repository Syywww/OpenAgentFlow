package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 工具调用日志表。
 * <p>对应数据库表：tool_invocation_log。</p>
 */
@TableName("tool_invocation_log")
public class ToolInvocationLogEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工具ID。 */
    @TableField("tool_id")
    private String toolId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 步骤ID。 */
    @TableField("step_id")
    private String stepId;

    /** 字段说明：SESSIONID。 */
    @TableField("session_id")
    private String sessionId;

    /** CALLER用户ID。 */
    @TableField("caller_user_id")
    private String callerUserId;

    /** 工具编码。 */
    @TableField("tool_code")
    private String toolCode;

    /** 输入参数。 */
    @TableField("input_params")
    private String inputParams;

    /** 输出结果。 */
    @TableField("output_result")
    private String outputResult;

    /** 成功。 */
    @TableField("success")
    private Boolean success;

    /** 风险级别。 */
    @TableField("risk_level")
    private String riskLevel;

    /** CONFIRMED人。 */
    @TableField("confirmed_by")
    private String confirmedBy;

    /** CONFIRMED时间。 */
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    /** 耗时毫秒。 */
    @TableField("latency_ms")
    private Integer latencyMs;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCallerUserId() {
        return callerUserId;
    }

    public void setCallerUserId(String callerUserId) {
        this.callerUserId = callerUserId;
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public String getInputParams() {
        return inputParams;
    }

    public void setInputParams(String inputParams) {
        this.inputParams = inputParams;
    }

    public String getOutputResult() {
        return outputResult;
    }

    public void setOutputResult(String outputResult) {
        this.outputResult = outputResult;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
