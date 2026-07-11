package com.openagentflow.domain.task;

import java.time.Instant;

/**
 * Kafka 异步任务消息。
 */
public class AsyncTaskMessage {

    /** 消息唯一ID，用于排查重复投递。 */
    private String messageId;

    /** MySQL 异步任务ID。 */
    private String taskId;

    /** 异步任务类型。 */
    private String taskType;

    /** 当前投递尝试次数，首次投递为0。 */
    private Integer attempt;

    /** 消息生成时间。 */
    private Instant createdAt;

    /** 最早允许消费时间，用于重试 Topic 延迟消费。 */
    private Instant notBeforeAt;

    /** 上一次执行错误摘要。 */
    private String lastError;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getNotBeforeAt() {
        return notBeforeAt;
    }

    public void setNotBeforeAt(Instant notBeforeAt) {
        this.notBeforeAt = notBeforeAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
