package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工作流步骤运行表。
 * <p>对应数据库表：workflow_step_run。</p>
 */
@TableName("workflow_step_run")
public class WorkflowStepRunEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作流运行ID。 */
    @TableField("workflow_run_id")
    private String workflowRunId;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 节点密钥。 */
    @TableField("node_key")
    private String nodeKey;

    /** 节点名称。 */
    @TableField("node_name")
    private String nodeName;

    /** 节点类型。 */
    @TableField("node_type")
    private String nodeType;

    /** 输入载荷。 */
    @TableField("input_payload")
    private String inputPayload;

    /** 输出载荷。 */
    @TableField("output_payload")
    private String outputPayload;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** ATTEMPT序号。 */
    @TableField("attempt_no")
    private Integer attemptNo;

    /** Token数量。 */
    @TableField("token_count")
    private Integer tokenCount;

    /** 成本AMOUNT。 */
    @TableField("cost_amount")
    private BigDecimal costAmount;

    /** 耗时毫秒。 */
    @TableField("latency_ms")
    private Integer latencyMs;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 完成时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowRunId() {
        return workflowRunId;
    }

    public void setWorkflowRunId(String workflowRunId) {
        this.workflowRunId = workflowRunId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(String inputPayload) {
        this.inputPayload = inputPayload;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(String outputPayload) {
        this.outputPayload = outputPayload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
