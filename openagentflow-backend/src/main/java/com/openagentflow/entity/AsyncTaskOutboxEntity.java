package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 异步任务 Outbox 消息实体。
 * <p>对应数据库表：async_task_outbox，用于保证 MySQL 任务与 Kafka 待发送消息原子提交。</p>
 */
@TableName("async_task_outbox")
public class AsyncTaskOutboxEntity {

    /** Outbox 主键ID。 */
    @TableId("id")
    private String id;

    /** 关联异步任务ID。 */
    @TableField("task_id")
    private String taskId;

    /** Kafka 消息唯一ID。 */
    @TableField("message_id")
    private String messageId;

    /** Kafka Topic 名称。 */
    @TableField("topic_name")
    private String topicName;

    /** Kafka 分区键。 */
    @TableField("message_key")
    private String messageKey;

    /** 消息 Schema 版本。 */
    @TableField("schema_version")
    private Integer schemaVersion;

    /** 消息链路Trace ID。 */
    @TableField("trace_id")
    private String traceId;

    /** Kafka 消息JSON。 */
    @TableField("payload_json")
    private String payloadJson;

    /** Outbox 状态：pending、sending、sent、failed、dead。 */
    @TableField("status")
    private String status;

    /** Kafka 发送尝试次数。 */
    @TableField("attempt_count")
    private Integer attemptCount;

    /** Kafka 最大发送次数。 */
    @TableField("max_attempts")
    private Integer maxAttempts;

    /** 最早允许发送时间。 */
    @TableField("available_at")
    private LocalDateTime availableAt;

    /** 当前领取 Outbox 的发布器ID。 */
    @TableField("locked_by")
    private String lockedBy;

    /** 发布器领取时间。 */
    @TableField("locked_at")
    private LocalDateTime lockedAt;

    /** Kafka Broker 确认时间。 */
    @TableField("sent_at")
    private LocalDateTime sentAt;

    /** 最近一次发送错误。 */
    @TableField("last_error")
    private String lastError;

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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(LocalDateTime availableAt) {
        this.availableAt = availableAt;
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

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
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
