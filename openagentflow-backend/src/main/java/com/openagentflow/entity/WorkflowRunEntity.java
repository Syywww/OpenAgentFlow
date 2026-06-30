package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 工作流运行表。
 * <p>对应数据库表：workflow_run。</p>
 */
@TableName("workflow_run")
public class WorkflowRunEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 工作流版本ID。 */
    @TableField("workflow_version_id")
    private String workflowVersionId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** TRIGGER类型。 */
    @TableField("trigger_type")
    private String triggerType;

    /** TRIGGER用户ID。 */
    @TableField("trigger_user_id")
    private String triggerUserId;

    /** 输入载荷。 */
    @TableField("input_payload")
    private String inputPayload;

    /** 上下文JSON。 */
    @TableField("context_json")
    private String contextJson;

    /** 输出载荷。 */
    @TableField("output_payload")
    private String outputPayload;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 幂等键，用于防止前端重复点击或外部接口重复投递。 */
    @TableField("idempotency_key")
    private String idempotencyKey;

    /** 父运行ID，用于重跑或恢复运行时追溯来源。 */
    @TableField("parent_run_id")
    private String parentRunId;

    /** 从哪个节点恢复执行。 */
    @TableField("resume_from_node_key")
    private String resumeFromNodeKey;

    /** 最近完成或失败的节点Key。 */
    @TableField("last_node_key")
    private String lastNodeKey;

    /** 下一步预计执行的节点Key。 */
    @TableField("next_node_key")
    private String nextNodeKey;

    /** 当前运行锁持有者，便于后续多实例执行时做抢占保护。 */
    @TableField("locked_by")
    private String lockedBy;

    /** 当前运行锁定时间。 */
    @TableField("locked_at")
    private LocalDateTime lockedAt;

    /** 最近心跳时间，用于识别卡住或失联的运行。 */
    @TableField("heartbeat_at")
    private LocalDateTime heartbeatAt;

    /** 已重跑次数。 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 是否可从失败节点恢复。 */
    @TableField("recoverable")
    private Boolean recoverable;

    /** 运行快照JSON，保存恢复所需的关键上下文。 */
    @TableField("snapshot_json")
    private String snapshotJson;

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

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowVersionId() {
        return workflowVersionId;
    }

    public void setWorkflowVersionId(String workflowVersionId) {
        this.workflowVersionId = workflowVersionId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerUserId() {
        return triggerUserId;
    }

    public void setTriggerUserId(String triggerUserId) {
        this.triggerUserId = triggerUserId;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(String inputPayload) {
        this.inputPayload = inputPayload;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getParentRunId() {
        return parentRunId;
    }

    public void setParentRunId(String parentRunId) {
        this.parentRunId = parentRunId;
    }

    public String getResumeFromNodeKey() {
        return resumeFromNodeKey;
    }

    public void setResumeFromNodeKey(String resumeFromNodeKey) {
        this.resumeFromNodeKey = resumeFromNodeKey;
    }

    public String getLastNodeKey() {
        return lastNodeKey;
    }

    public void setLastNodeKey(String lastNodeKey) {
        this.lastNodeKey = lastNodeKey;
    }

    public String getNextNodeKey() {
        return nextNodeKey;
    }

    public void setNextNodeKey(String nextNodeKey) {
        this.nextNodeKey = nextNodeKey;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getHeartbeatAt() {
        return heartbeatAt;
    }

    public void setHeartbeatAt(LocalDateTime heartbeatAt) {
        this.heartbeatAt = heartbeatAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Boolean getRecoverable() {
        return recoverable;
    }

    public void setRecoverable(Boolean recoverable) {
        this.recoverable = recoverable;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
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
