package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异步任务中心主表。
 * <p>对应数据库表：async_task。</p>
 */
@TableName("async_task")
public class AsyncTaskEntity {

    /** 异步任务主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 父任务ID，用于构建任务DAG。 */
    @TableField("parent_task_id")
    private String parentTaskId;

    /** 根任务ID，用于聚合整条任务DAG。 */
    @TableField("root_task_id")
    private String rootTaskId;

    /** 当前任务分片序号。 */
    @TableField("shard_no")
    private Integer shardNo;

    /** 当前阶段分片总数。 */
    @TableField("shard_total")
    private Integer shardTotal;

    /** 任务幂等键。 */
    @TableField("idempotency_key")
    private String idempotencyKey;

    /** 异步任务编码。 */
    @TableField("task_code")
    private String taskCode;

    /** 异步任务名称。 */
    @TableField("task_name")
    private String taskName;

    /** 异步任务类型。 */
    @TableField("task_type")
    private String taskType;

    /** 业务类型。 */
    @TableField("biz_type")
    private String bizType;

    /** 业务对象ID。 */
    @TableField("biz_id")
    private String bizId;

    /** 来源业务表名。 */
    @TableField("source_table")
    private String sourceTable;

    /** 来源业务记录ID。 */
    @TableField("source_id")
    private String sourceId;

    /** 所属工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 任务所属用户ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 任务状态。 */
    @TableField("status")
    private String status;

    /** 任务优先级。 */
    @TableField("priority")
    private Integer priority;

    /** 任务进度百分比。 */
    @TableField("progress_percent")
    private BigDecimal progressPercent;

    /** 当前阶段编码。 */
    @TableField("current_stage")
    private String currentStage;

    /** 当前阶段消息。 */
    @TableField("current_message")
    private String currentMessage;

    /** 总步骤数。 */
    @TableField("total_steps")
    private Integer totalSteps;

    /** 已完成步骤数。 */
    @TableField("finished_steps")
    private Integer finishedSteps;

    /** 已重试次数。 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 最大重试次数。 */
    @TableField("max_retries")
    private Integer maxRetries;

    /** 是否请求取消。 */
    @TableField("cancel_requested")
    private Boolean cancelRequested;

    /** 任务请求参数JSON。 */
    @TableField("request_payload")
    private String requestPayload;

    /** 任务结果JSON。 */
    @TableField("result_payload")
    private String resultPayload;

    /** 错误编码。 */
    @TableField("error_code")
    private String errorCode;

    /** 错误消息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 当前 Kafka Topic。 */
    @TableField("queue_topic")
    private String queueTopic;

    /** 最近一次 Kafka 消息ID。 */
    @TableField("kafka_message_id")
    private String kafkaMessageId;

    /** 当前领取任务的 Worker ID。 */
    @TableField("locked_by")
    private String lockedBy;

    /** Worker 领取时间。 */
    @TableField("locked_at")
    private LocalDateTime lockedAt;

    /** Worker 最近心跳时间。 */
    @TableField("heartbeat_at")
    private LocalDateTime heartbeatAt;

    /** Worker 执行代次，用于阻止失效 Worker 提交旧结果。 */
    @TableField("lock_version")
    private Long lockVersion;

    /** 可恢复任务检查点JSON。 */
    @TableField("checkpoint_json")
    private String checkpointJson;

    /** 跨API、Outbox、Kafka和Worker的Trace ID。 */
    @TableField("trace_id")
    private String traceId;

    /** 最近一次成功投递 Kafka 的时间。 */
    @TableField("last_enqueued_at")
    private LocalDateTime lastEnqueuedAt;

    /** 下次允许执行时间。 */
    @TableField("next_retry_at")
    private LocalDateTime nextRetryAt;

    /** 进入死信队列的时间。 */
    @TableField("dead_letter_at")
    private LocalDateTime deadLetterAt;

    /** 开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 完成时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /** 创建人ID。 */
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

    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }
    public String getRootTaskId() { return rootTaskId; }
    public void setRootTaskId(String rootTaskId) { this.rootTaskId = rootTaskId; }
    public Integer getShardNo() { return shardNo; }
    public void setShardNo(Integer shardNo) { this.shardNo = shardNo; }
    public Integer getShardTotal() { return shardTotal; }
    public void setShardTotal(Integer shardTotal) { this.shardTotal = shardTotal; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(String currentMessage) {
        this.currentMessage = currentMessage;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getFinishedSteps() {
        return finishedSteps;
    }

    public void setFinishedSteps(Integer finishedSteps) {
        this.finishedSteps = finishedSteps;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Boolean getCancelRequested() {
        return cancelRequested;
    }

    public void setCancelRequested(Boolean cancelRequested) {
        this.cancelRequested = cancelRequested;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResultPayload() {
        return resultPayload;
    }

    public void setResultPayload(String resultPayload) {
        this.resultPayload = resultPayload;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getQueueTopic() {
        return queueTopic;
    }

    public void setQueueTopic(String queueTopic) {
        this.queueTopic = queueTopic;
    }

    public String getKafkaMessageId() {
        return kafkaMessageId;
    }

    public void setKafkaMessageId(String kafkaMessageId) {
        this.kafkaMessageId = kafkaMessageId;
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

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long lockVersion) {
        this.lockVersion = lockVersion;
    }

    public String getCheckpointJson() {
        return checkpointJson;
    }

    public void setCheckpointJson(String checkpointJson) {
        this.checkpointJson = checkpointJson;
    }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public LocalDateTime getLastEnqueuedAt() {
        return lastEnqueuedAt;
    }

    public void setLastEnqueuedAt(LocalDateTime lastEnqueuedAt) {
        this.lastEnqueuedAt = lastEnqueuedAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public LocalDateTime getDeadLetterAt() {
        return deadLetterAt;
    }

    public void setDeadLetterAt(LocalDateTime deadLetterAt) {
        this.deadLetterAt = deadLetterAt;
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
