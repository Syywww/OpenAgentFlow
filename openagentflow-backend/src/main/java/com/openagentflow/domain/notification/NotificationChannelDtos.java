package com.openagentflow.domain.notification;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 外部通知渠道请求与响应对象集合。
 */
public final class NotificationChannelDtos {

    private NotificationChannelDtos() {
    }

    /** 通知渠道保存请求。 */
    public static class ChannelRequest {
        /** 渠道编码。 */
        private String channelCode;
        /** 渠道名称。 */
        private String channelName;
        /** 渠道类型。 */
        private String channelType;
        /** 渠道配置。 */
        private Map<String, Object> config;
        /** 是否启用。 */
        private Boolean enabled;

        public String getChannelCode() { return channelCode; }
        public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
        public String getChannelName() { return channelName; }
        public void setChannelName(String channelName) { this.channelName = channelName; }
        public String getChannelType() { return channelType; }
        public void setChannelType(String channelType) { this.channelType = channelType; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    /** 通知渠道摘要。 */
    public static class ChannelSummary {
        /** 渠道ID。 */
        private String id;
        /** 渠道编码。 */
        private String channelCode;
        /** 渠道名称。 */
        private String channelName;
        /** 渠道类型。 */
        private String channelType;
        /** 已脱敏渠道配置。 */
        private Map<String, Object> config;
        /** 是否启用。 */
        private Boolean enabled;
        /** 最近测试状态。 */
        private String lastTestStatus;
        /** 最近测试消息。 */
        private String lastTestMessage;
        /** 最近测试时间。 */
        private LocalDateTime lastTestAt;
        /** 最近成功投递时间。 */
        private LocalDateTime lastSuccessAt;
        /** 连续失败次数。 */
        private Integer failureCount;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getChannelCode() { return channelCode; }
        public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
        public String getChannelName() { return channelName; }
        public void setChannelName(String channelName) { this.channelName = channelName; }
        public String getChannelType() { return channelType; }
        public void setChannelType(String channelType) { this.channelType = channelType; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getLastTestStatus() { return lastTestStatus; }
        public void setLastTestStatus(String lastTestStatus) { this.lastTestStatus = lastTestStatus; }
        public String getLastTestMessage() { return lastTestMessage; }
        public void setLastTestMessage(String lastTestMessage) { this.lastTestMessage = lastTestMessage; }
        public LocalDateTime getLastTestAt() { return lastTestAt; }
        public void setLastTestAt(LocalDateTime lastTestAt) { this.lastTestAt = lastTestAt; }
        public LocalDateTime getLastSuccessAt() { return lastSuccessAt; }
        public void setLastSuccessAt(LocalDateTime lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
    }

    /** 渠道连通性测试结果。 */
    public static class TestResult {
        /** 是否测试成功。 */
        private Boolean success;
        /** HTTP状态码，站内渠道为空。 */
        private Integer statusCode;
        /** 请求耗时毫秒。 */
        private Long latencyMs;
        /** 测试结果说明。 */
        private String message;

        public TestResult(Boolean success, Integer statusCode, Long latencyMs, String message) {
            this.success = success;
            this.statusCode = statusCode;
            this.latencyMs = latencyMs;
            this.message = message;
        }

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public Integer getStatusCode() { return statusCode; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
        public Long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /** 外部通知投递明细。 */
    public static class DeliveryItem {
        /** 投递ID。 */
        private String id;
        /** 告警事件ID。 */
        private String alertEventId;
        /** 告警标题。 */
        private String alertTitle;
        /** 渠道名称。 */
        private String channelName;
        /** 渠道类型。 */
        private String channelType;
        /** 投递状态。 */
        private String status;
        /** 已尝试次数。 */
        private Integer attemptCount;
        /** 下次重试时间。 */
        private LocalDateTime nextRetryAt;
        /** 响应摘要。 */
        private String responseSummary;
        /** 错误说明。 */
        private String errorMessage;
        /** 成功发送时间。 */
        private LocalDateTime sentAt;
        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAlertEventId() { return alertEventId; }
        public void setAlertEventId(String alertEventId) { this.alertEventId = alertEventId; }
        public String getAlertTitle() { return alertTitle; }
        public void setAlertTitle(String alertTitle) { this.alertTitle = alertTitle; }
        public String getChannelName() { return channelName; }
        public void setChannelName(String channelName) { this.channelName = channelName; }
        public String getChannelType() { return channelType; }
        public void setChannelType(String channelType) { this.channelType = channelType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getAttemptCount() { return attemptCount; }
        public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
        public LocalDateTime getNextRetryAt() { return nextRetryAt; }
        public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
        public String getResponseSummary() { return responseSummary; }
        public void setResponseSummary(String responseSummary) { this.responseSummary = responseSummary; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
