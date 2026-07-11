package com.openagentflow.domain.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 异步任务中心前后端传输对象集合。
 */
public final class AsyncTaskDtos {

    private AsyncTaskDtos() {
    }

    /**
     * 异步任务列表查询条件。
     */
    public static class Query {

        /** 任务状态，支持 all 表示不过滤。 */
        private String status;

        /** 任务类型，支持 all 表示不过滤。 */
        private String taskType;

        /** 工作空间ID，支持 all 表示不过滤。 */
        private String workspaceId;

        /** 任务名称、编码、业务ID关键字。 */
        private String keyword;

        /** 当前页码，从 1 开始。 */
        private Integer pageNo;

        /** 每页大小。 */
        private Integer pageSize;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public String getWorkspaceId() {
            return workspaceId;
        }

        public void setWorkspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public Integer getPageNo() {
            return pageNo;
        }

        public void setPageNo(Integer pageNo) {
            this.pageNo = pageNo;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }
    }

    /**
     * 异步任务摘要。
     */
    public static class Summary {

        /** 异步任务ID。 */
        private String id;

        /** 异步任务编码。 */
        private String taskCode;

        /** 异步任务名称。 */
        private String taskName;

        /** 异步任务类型。 */
        private String taskType;

        /** 异步任务类型展示名。 */
        private String taskTypeLabel;

        /** 业务类型。 */
        private String bizType;

        /** 业务对象ID。 */
        private String bizId;

        /** 所属工作空间ID。 */
        private String workspaceId;

        /** 所属工作空间名称。 */
        private String workspaceName;

        /** 任务状态。 */
        private String status;

        /** 任务进度百分比。 */
        private Integer progressPercent;

        /** 当前阶段编码。 */
        private String currentStage;

        /** 当前阶段消息。 */
        private String currentMessage;

        /** 总步骤数。 */
        private Integer totalSteps;

        /** 已完成步骤数。 */
        private Integer finishedSteps;

        /** 已重试次数。 */
        private Integer retryCount;

        /** 最大重试次数。 */
        private Integer maxRetries;

        /** 是否请求取消。 */
        private Boolean cancelRequested;

        /** 错误消息。 */
        private String errorMessage;

        /** 当前 Kafka Topic。 */
        private String queueTopic;

        /** 当前 Worker ID。 */
        private String lockedBy;

        /** Worker 最近心跳时间。 */
        private LocalDateTime heartbeatAt;

        /** 下次重试时间。 */
        private LocalDateTime nextRetryAt;

        /** 死信入队时间。 */
        private LocalDateTime deadLetterAt;

        /** 开始时间。 */
        private LocalDateTime startedAt;

        /** 完成时间。 */
        private LocalDateTime finishedAt;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        /** 更新时间。 */
        private LocalDateTime updatedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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

        public String getTaskTypeLabel() {
            return taskTypeLabel;
        }

        public void setTaskTypeLabel(String taskTypeLabel) {
            this.taskTypeLabel = taskTypeLabel;
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

        public String getWorkspaceId() {
            return workspaceId;
        }

        public void setWorkspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
        }

        public String getWorkspaceName() {
            return workspaceName;
        }

        public void setWorkspaceName(String workspaceName) {
            this.workspaceName = workspaceName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(Integer progressPercent) {
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

        public String getLockedBy() {
            return lockedBy;
        }

        public void setLockedBy(String lockedBy) {
            this.lockedBy = lockedBy;
        }

        public LocalDateTime getHeartbeatAt() {
            return heartbeatAt;
        }

        public void setHeartbeatAt(LocalDateTime heartbeatAt) {
            this.heartbeatAt = heartbeatAt;
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

    /**
     * 异步任务详情。
     */
    public static class Detail extends Summary {

        /** 任务请求参数。 */
        private Map<String, Object> requestPayload;

        /** 任务结果数据。 */
        private Map<String, Object> resultPayload;

        /** 错误编码。 */
        private String errorCode;

        /** 任务日志列表。 */
        private List<LogItem> logs;

        public Map<String, Object> getRequestPayload() {
            return requestPayload;
        }

        public void setRequestPayload(Map<String, Object> requestPayload) {
            this.requestPayload = requestPayload;
        }

        public Map<String, Object> getResultPayload() {
            return resultPayload;
        }

        public void setResultPayload(Map<String, Object> resultPayload) {
            this.resultPayload = resultPayload;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public List<LogItem> getLogs() {
            return logs;
        }

        public void setLogs(List<LogItem> logs) {
            this.logs = logs;
        }
    }

    /**
     * 异步任务日志项。
     */
    public static class LogItem {

        /** 日志ID。 */
        private String id;

        /** 日志级别。 */
        private String logLevel;

        /** 阶段编码。 */
        private String stage;

        /** 日志消息。 */
        private String message;

        /** 日志详情。 */
        private Map<String, Object> detail;

        /** 日志对应进度百分比。 */
        private Integer progressPercent;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getDetail() {
            return detail;
        }

        public void setDetail(Map<String, Object> detail) {
            this.detail = detail;
        }

        public Integer getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(Integer progressPercent) {
            this.progressPercent = progressPercent;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 异步任务统计摘要。
     */
    public static class Overview {

        /** 全部任务数。 */
        private Long totalCount;

        /** 排队任务数。 */
        private Long pendingCount;

        /** 运行中任务数。 */
        private Long runningCount;

        /** 成功任务数。 */
        private Long successCount;

        /** 失败任务数。 */
        private Long failedCount;

        /** 取消任务数。 */
        private Long canceledCount;

        /** 进入死信队列的任务数。 */
        private Long deadLetterCount;

        public Long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Long totalCount) {
            this.totalCount = totalCount;
        }

        public Long getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(Long pendingCount) {
            this.pendingCount = pendingCount;
        }

        public Long getRunningCount() {
            return runningCount;
        }

        public void setRunningCount(Long runningCount) {
            this.runningCount = runningCount;
        }

        public Long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(Long successCount) {
            this.successCount = successCount;
        }

        public Long getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Long failedCount) {
            this.failedCount = failedCount;
        }

        public Long getCanceledCount() {
            return canceledCount;
        }

        public void setCanceledCount(Long canceledCount) {
            this.canceledCount = canceledCount;
        }

        public Long getDeadLetterCount() {
            return deadLetterCount;
        }

        public void setDeadLetterCount(Long deadLetterCount) {
            this.deadLetterCount = deadLetterCount;
        }
    }
}
