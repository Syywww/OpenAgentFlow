package com.openagentflow.domain.ops;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 运营监控与告警中心传输对象集合。
 */
public final class OpsMonitorDtos {

    private OpsMonitorDtos() {
    }

    /**
     * 运营监控总览指标。
     */
    public static class Overview {

        /** 当前打开中的告警数量。 */
        private Long openAlertCount;

        /** 严重告警数量。 */
        private Long criticalAlertCount;

        /** 健康组件数量。 */
        private Long healthyComponentCount;

        /** 异常组件数量。 */
        private Long unhealthyComponentCount;

        /** 近一小时接口失败率。 */
        private BigDecimal apiFailureRate;

        /** 近一小时模型失败率。 */
        private BigDecimal modelFailureRate;

        /** 当前任务积压数量。 */
        private Long taskBacklogCount;

        /** 今日模型成本。 */
        private BigDecimal todayCost;

        /** 今日调用次数。 */
        private Long todayRunCount;

        /** 最近一次巡检时间。 */
        private LocalDateTime lastInspectionAt;

        public Long getOpenAlertCount() {
            return openAlertCount;
        }

        public void setOpenAlertCount(Long openAlertCount) {
            this.openAlertCount = openAlertCount;
        }

        public Long getCriticalAlertCount() {
            return criticalAlertCount;
        }

        public void setCriticalAlertCount(Long criticalAlertCount) {
            this.criticalAlertCount = criticalAlertCount;
        }

        public Long getHealthyComponentCount() {
            return healthyComponentCount;
        }

        public void setHealthyComponentCount(Long healthyComponentCount) {
            this.healthyComponentCount = healthyComponentCount;
        }

        public Long getUnhealthyComponentCount() {
            return unhealthyComponentCount;
        }

        public void setUnhealthyComponentCount(Long unhealthyComponentCount) {
            this.unhealthyComponentCount = unhealthyComponentCount;
        }

        public BigDecimal getApiFailureRate() {
            return apiFailureRate;
        }

        public void setApiFailureRate(BigDecimal apiFailureRate) {
            this.apiFailureRate = apiFailureRate;
        }

        public BigDecimal getModelFailureRate() {
            return modelFailureRate;
        }

        public void setModelFailureRate(BigDecimal modelFailureRate) {
            this.modelFailureRate = modelFailureRate;
        }

        public Long getTaskBacklogCount() {
            return taskBacklogCount;
        }

        public void setTaskBacklogCount(Long taskBacklogCount) {
            this.taskBacklogCount = taskBacklogCount;
        }

        public BigDecimal getTodayCost() {
            return todayCost;
        }

        public void setTodayCost(BigDecimal todayCost) {
            this.todayCost = todayCost;
        }

        public Long getTodayRunCount() {
            return todayRunCount;
        }

        public void setTodayRunCount(Long todayRunCount) {
            this.todayRunCount = todayRunCount;
        }

        public LocalDateTime getLastInspectionAt() {
            return lastInspectionAt;
        }

        public void setLastInspectionAt(LocalDateTime lastInspectionAt) {
            this.lastInspectionAt = lastInspectionAt;
        }
    }

    /**
     * 健康组件摘要。
     */
    public static class HealthItem {

        /** 组件编码。 */
        private String code;

        /** 组件名称。 */
        private String name;

        /** 组件类型。 */
        private String type;

        /** 健康状态。 */
        private String status;

        /** 状态说明。 */
        private String message;

        /** 检测耗时毫秒。 */
        private Integer latencyMs;

        /** 最近检测时间。 */
        private LocalDateTime checkedAt;

        /** 组件元数据。 */
        private Map<String, Object> metadata;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

        public LocalDateTime getCheckedAt() {
            return checkedAt;
        }

        public void setCheckedAt(LocalDateTime checkedAt) {
            this.checkedAt = checkedAt;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 告警规则摘要。
     */
    public static class AlertRuleSummary {

        /** 告警规则ID。 */
        private String id;

        /** 告警规则编码。 */
        private String ruleCode;

        /** 告警规则名称。 */
        private String ruleName;

        /** 监控指标编码。 */
        private String metricCode;

        /** 指标来源模块。 */
        private String metricSource;

        /** 比较操作符。 */
        private String operator;

        /** 阈值。 */
        private BigDecimal thresholdValue;

        /** 告警级别。 */
        private String severity;

        /** 统计窗口分钟数。 */
        private Integer windowMinutes;

        /** 冷却分钟数。 */
        private Integer cooldownMinutes;

        /** 是否启用。 */
        private Boolean enabled;

        /** 通知渠道。 */
        private String notifyChannels;

        /** 规则说明。 */
        private String description;

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

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getMetricCode() {
            return metricCode;
        }

        public void setMetricCode(String metricCode) {
            this.metricCode = metricCode;
        }

        public String getMetricSource() {
            return metricSource;
        }

        public void setMetricSource(String metricSource) {
            this.metricSource = metricSource;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public BigDecimal getThresholdValue() {
            return thresholdValue;
        }

        public void setThresholdValue(BigDecimal thresholdValue) {
            this.thresholdValue = thresholdValue;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public Integer getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(Integer windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public Integer getCooldownMinutes() {
            return cooldownMinutes;
        }

        public void setCooldownMinutes(Integer cooldownMinutes) {
            this.cooldownMinutes = cooldownMinutes;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getNotifyChannels() {
            return notifyChannels;
        }

        public void setNotifyChannels(String notifyChannels) {
            this.notifyChannels = notifyChannels;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
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
     * 告警规则保存请求。
     */
    public static class AlertRuleRequest {

        /** 告警规则编码。 */
        private String ruleCode;

        /** 告警规则名称。 */
        private String ruleName;

        /** 监控指标编码。 */
        private String metricCode;

        /** 指标来源模块。 */
        private String metricSource;

        /** 比较操作符。 */
        private String operator;

        /** 阈值。 */
        private BigDecimal thresholdValue;

        /** 告警级别。 */
        private String severity;

        /** 统计窗口分钟数。 */
        private Integer windowMinutes;

        /** 冷却分钟数。 */
        private Integer cooldownMinutes;

        /** 是否启用。 */
        private Boolean enabled;

        /** 通知渠道。 */
        private String notifyChannels;

        /** 规则说明。 */
        private String description;

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getMetricCode() {
            return metricCode;
        }

        public void setMetricCode(String metricCode) {
            this.metricCode = metricCode;
        }

        public String getMetricSource() {
            return metricSource;
        }

        public void setMetricSource(String metricSource) {
            this.metricSource = metricSource;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public BigDecimal getThresholdValue() {
            return thresholdValue;
        }

        public void setThresholdValue(BigDecimal thresholdValue) {
            this.thresholdValue = thresholdValue;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public Integer getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(Integer windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public Integer getCooldownMinutes() {
            return cooldownMinutes;
        }

        public void setCooldownMinutes(Integer cooldownMinutes) {
            this.cooldownMinutes = cooldownMinutes;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getNotifyChannels() {
            return notifyChannels;
        }

        public void setNotifyChannels(String notifyChannels) {
            this.notifyChannels = notifyChannels;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 告警事件摘要。
     */
    public static class AlertEventSummary {

        /** 告警事件ID。 */
        private String id;

        /** 告警事件编码。 */
        private String eventCode;

        /** 告警规则编码。 */
        private String ruleCode;

        /** 告警标题。 */
        private String alertTitle;

        /** 告警级别。 */
        private String severity;

        /** 监控指标编码。 */
        private String metricCode;

        /** 指标来源模块。 */
        private String metricSource;

        /** 当前指标值。 */
        private BigDecimal metricValue;

        /** 阈值。 */
        private BigDecimal thresholdValue;

        /** 告警详情。 */
        private String alertDetail;

        /** 证据。 */
        private Map<String, Object> evidence;

        /** 告警状态。 */
        private String status;

        /** 通知状态。 */
        private String notifyStatus;

        /** 处理人用户ID。 */
        private String handledBy;

        /** 处理时间。 */
        private LocalDateTime handledAt;

        /** 处理备注。 */
        private String handleNote;

        /** 最近触发时间。 */
        private LocalDateTime lastTriggeredAt;

        /** 触发次数。 */
        private Integer triggerCount;

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

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public String getAlertTitle() {
            return alertTitle;
        }

        public void setAlertTitle(String alertTitle) {
            this.alertTitle = alertTitle;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getMetricCode() {
            return metricCode;
        }

        public void setMetricCode(String metricCode) {
            this.metricCode = metricCode;
        }

        public String getMetricSource() {
            return metricSource;
        }

        public void setMetricSource(String metricSource) {
            this.metricSource = metricSource;
        }

        public BigDecimal getMetricValue() {
            return metricValue;
        }

        public void setMetricValue(BigDecimal metricValue) {
            this.metricValue = metricValue;
        }

        public BigDecimal getThresholdValue() {
            return thresholdValue;
        }

        public void setThresholdValue(BigDecimal thresholdValue) {
            this.thresholdValue = thresholdValue;
        }

        public String getAlertDetail() {
            return alertDetail;
        }

        public void setAlertDetail(String alertDetail) {
            this.alertDetail = alertDetail;
        }

        public Map<String, Object> getEvidence() {
            return evidence;
        }

        public void setEvidence(Map<String, Object> evidence) {
            this.evidence = evidence;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotifyStatus() {
            return notifyStatus;
        }

        public void setNotifyStatus(String notifyStatus) {
            this.notifyStatus = notifyStatus;
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

        public LocalDateTime getLastTriggeredAt() {
            return lastTriggeredAt;
        }

        public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
            this.lastTriggeredAt = lastTriggeredAt;
        }

        public Integer getTriggerCount() {
            return triggerCount;
        }

        public void setTriggerCount(Integer triggerCount) {
            this.triggerCount = triggerCount;
        }
    }

    /**
     * 告警处理请求。
     */
    public static class AlertHandleRequest {

        /** 目标状态。 */
        private String status;

        /** 处理备注。 */
        private String handleNote;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getHandleNote() {
            return handleNote;
        }

        public void setHandleNote(String handleNote) {
            this.handleNote = handleNote;
        }
    }

    /**
     * 巡检项摘要。
     */
    public static class HealthCheckSummary {

        /** 巡检项ID。 */
        private String id;

        /** 巡检项编码。 */
        private String checkCode;

        /** 巡检项名称。 */
        private String checkName;

        /** 目标类型。 */
        private String targetType;

        /** 目标编码。 */
        private String targetCode;

        /** 巡检状态。 */
        private String status;

        /** 巡检消息。 */
        private String message;

        /** 最近耗时毫秒。 */
        private Integer latencyMs;

        /** 最近巡检时间。 */
        private LocalDateTime lastCheckedAt;

        /** 下一次巡检时间。 */
        private LocalDateTime nextCheckAt;

        /** 是否启用。 */
        private Boolean enabled;

        /** 元数据。 */
        private Map<String, Object> metadata;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCheckCode() {
            return checkCode;
        }

        public void setCheckCode(String checkCode) {
            this.checkCode = checkCode;
        }

        public String getCheckName() {
            return checkName;
        }

        public void setCheckName(String checkName) {
            this.checkName = checkName;
        }

        public String getTargetType() {
            return targetType;
        }

        public void setTargetType(String targetType) {
            this.targetType = targetType;
        }

        public String getTargetCode() {
            return targetCode;
        }

        public void setTargetCode(String targetCode) {
            this.targetCode = targetCode;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

        public LocalDateTime getLastCheckedAt() {
            return lastCheckedAt;
        }

        public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
            this.lastCheckedAt = lastCheckedAt;
        }

        public LocalDateTime getNextCheckAt() {
            return nextCheckAt;
        }

        public void setNextCheckAt(LocalDateTime nextCheckAt) {
            this.nextCheckAt = nextCheckAt;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 通知渠道摘要。
     */
    public static class NotifyChannelSummary {

        /** 通知渠道ID。 */
        private String id;

        /** 通知渠道编码。 */
        private String channelCode;

        /** 通知渠道名称。 */
        private String channelName;

        /** 通知渠道类型。 */
        private String channelType;

        /** 是否启用。 */
        private Boolean enabled;

        /** 最近测试状态。 */
        private String lastTestStatus;

        /** 最近测试消息。 */
        private String lastTestMessage;

        /** 最近测试时间。 */
        private LocalDateTime lastTestAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getChannelCode() {
            return channelCode;
        }

        public void setChannelCode(String channelCode) {
            this.channelCode = channelCode;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public String getChannelType() {
            return channelType;
        }

        public void setChannelType(String channelType) {
            this.channelType = channelType;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getLastTestStatus() {
            return lastTestStatus;
        }

        public void setLastTestStatus(String lastTestStatus) {
            this.lastTestStatus = lastTestStatus;
        }

        public String getLastTestMessage() {
            return lastTestMessage;
        }

        public void setLastTestMessage(String lastTestMessage) {
            this.lastTestMessage = lastTestMessage;
        }

        public LocalDateTime getLastTestAt() {
            return lastTestAt;
        }

        public void setLastTestAt(LocalDateTime lastTestAt) {
            this.lastTestAt = lastTestAt;
        }
    }
}
