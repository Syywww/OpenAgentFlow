package com.openagentflow.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作台全量概览对象。
 */
public class DashboardOverview {

    /** Agent 总数。 */
    private Long agentCount;

    /** 已发布 Agent 数量。 */
    private Long publishedAgentCount;

    /** 知识库总数。 */
    private Long knowledgeBaseCount;

    /** 工具总数。 */
    private Long toolCount;

    /** 启用工具数量。 */
    private Long enabledToolCount;

    /** MCP 服务总数。 */
    private Long mcpServerCount;

    /** 工作流总数。 */
    private Long workflowCount;

    /** 今日运行次数。 */
    private Long todayRunCount;

    /** 今日成功运行次数。 */
    private Long todaySuccessCount;

    /** 今日失败运行次数。 */
    private Long todayFailureCount;

    /** 今日运行成功率。 */
    private BigDecimal todaySuccessRate = BigDecimal.ZERO;

    /** 今日调用成本。 */
    private BigDecimal todayCost = BigDecimal.ZERO;

    /** 今日 Token 消耗。 */
    private Long todayTokenCount;

    /** 今日平均耗时毫秒。 */
    private BigDecimal todayAvgLatencyMs = BigDecimal.ZERO;

    /** 排队和运行中的任务数量。 */
    private Long taskBacklogCount;

    /** 打开的告警数量。 */
    private Long openAlertCount;

    /** 异常健康检查项数量。 */
    private Long unhealthyComponentCount;

    /** 知识库健康概览。 */
    private KnowledgeHealth knowledgeHealth = new KnowledgeHealth();

    /** 最近 7 天运行趋势。 */
    private List<RunTrendItem> runTrend = new ArrayList<>();

    /** 最近运行记录。 */
    private List<RecentRunItem> recentRuns = new ArrayList<>();

    /** 模型使用排行。 */
    private List<ModelUsageItem> modelUsage = new ArrayList<>();

    /** 运行中的任务列表。 */
    private List<TaskQueueItem> taskQueue = new ArrayList<>();

    /** 打开的告警事件列表。 */
    private List<AlertEventItem> openAlerts = new ArrayList<>();

    /** 平台健康检查列表。 */
    private List<HealthCheckItem> healthChecks = new ArrayList<>();

    /** 工作台运营洞察。 */
    private List<InsightItem> insights = new ArrayList<>();

    public Long getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(Long agentCount) {
        this.agentCount = agentCount;
    }

    public Long getPublishedAgentCount() {
        return publishedAgentCount;
    }

    public void setPublishedAgentCount(Long publishedAgentCount) {
        this.publishedAgentCount = publishedAgentCount;
    }

    public Long getKnowledgeBaseCount() {
        return knowledgeBaseCount;
    }

    public void setKnowledgeBaseCount(Long knowledgeBaseCount) {
        this.knowledgeBaseCount = knowledgeBaseCount;
    }

    public Long getToolCount() {
        return toolCount;
    }

    public void setToolCount(Long toolCount) {
        this.toolCount = toolCount;
    }

    public Long getEnabledToolCount() {
        return enabledToolCount;
    }

    public void setEnabledToolCount(Long enabledToolCount) {
        this.enabledToolCount = enabledToolCount;
    }

    public Long getMcpServerCount() {
        return mcpServerCount;
    }

    public void setMcpServerCount(Long mcpServerCount) {
        this.mcpServerCount = mcpServerCount;
    }

    public Long getWorkflowCount() {
        return workflowCount;
    }

    public void setWorkflowCount(Long workflowCount) {
        this.workflowCount = workflowCount;
    }

    public Long getTodayRunCount() {
        return todayRunCount;
    }

    public void setTodayRunCount(Long todayRunCount) {
        this.todayRunCount = todayRunCount;
    }

    public Long getTodaySuccessCount() {
        return todaySuccessCount;
    }

    public void setTodaySuccessCount(Long todaySuccessCount) {
        this.todaySuccessCount = todaySuccessCount;
    }

    public Long getTodayFailureCount() {
        return todayFailureCount;
    }

    public void setTodayFailureCount(Long todayFailureCount) {
        this.todayFailureCount = todayFailureCount;
    }

    public BigDecimal getTodaySuccessRate() {
        return todaySuccessRate;
    }

    public void setTodaySuccessRate(BigDecimal todaySuccessRate) {
        this.todaySuccessRate = todaySuccessRate;
    }

    public BigDecimal getTodayCost() {
        return todayCost;
    }

    public void setTodayCost(BigDecimal todayCost) {
        this.todayCost = todayCost;
    }

    public Long getTodayTokenCount() {
        return todayTokenCount;
    }

    public void setTodayTokenCount(Long todayTokenCount) {
        this.todayTokenCount = todayTokenCount;
    }

    public BigDecimal getTodayAvgLatencyMs() {
        return todayAvgLatencyMs;
    }

    public void setTodayAvgLatencyMs(BigDecimal todayAvgLatencyMs) {
        this.todayAvgLatencyMs = todayAvgLatencyMs;
    }

    public Long getTaskBacklogCount() {
        return taskBacklogCount;
    }

    public void setTaskBacklogCount(Long taskBacklogCount) {
        this.taskBacklogCount = taskBacklogCount;
    }

    public Long getOpenAlertCount() {
        return openAlertCount;
    }

    public void setOpenAlertCount(Long openAlertCount) {
        this.openAlertCount = openAlertCount;
    }

    public Long getUnhealthyComponentCount() {
        return unhealthyComponentCount;
    }

    public void setUnhealthyComponentCount(Long unhealthyComponentCount) {
        this.unhealthyComponentCount = unhealthyComponentCount;
    }

    public KnowledgeHealth getKnowledgeHealth() {
        return knowledgeHealth;
    }

    public void setKnowledgeHealth(KnowledgeHealth knowledgeHealth) {
        this.knowledgeHealth = knowledgeHealth;
    }

    public List<RunTrendItem> getRunTrend() {
        return runTrend;
    }

    public void setRunTrend(List<RunTrendItem> runTrend) {
        this.runTrend = runTrend;
    }

    public List<RecentRunItem> getRecentRuns() {
        return recentRuns;
    }

    public void setRecentRuns(List<RecentRunItem> recentRuns) {
        this.recentRuns = recentRuns;
    }

    public List<ModelUsageItem> getModelUsage() {
        return modelUsage;
    }

    public void setModelUsage(List<ModelUsageItem> modelUsage) {
        this.modelUsage = modelUsage;
    }

    public List<TaskQueueItem> getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(List<TaskQueueItem> taskQueue) {
        this.taskQueue = taskQueue;
    }

    public List<AlertEventItem> getOpenAlerts() {
        return openAlerts;
    }

    public void setOpenAlerts(List<AlertEventItem> openAlerts) {
        this.openAlerts = openAlerts;
    }

    public List<HealthCheckItem> getHealthChecks() {
        return healthChecks;
    }

    public void setHealthChecks(List<HealthCheckItem> healthChecks) {
        this.healthChecks = healthChecks;
    }

    public List<InsightItem> getInsights() {
        return insights;
    }

    public void setInsights(List<InsightItem> insights) {
        this.insights = insights;
    }

    /**
     * 知识库健康概览。
     */
    public static class KnowledgeHealth {

        /** 文档总数。 */
        private Long documentCount;

        /** 已解析文档数量。 */
        private Long parsedDocumentCount;

        /** 解析失败文档数量。 */
        private Long failedDocumentCount;

        /** 处理中或排队中文档数量。 */
        private Long processingDocumentCount;

        /** 分片总数。 */
        private Long chunkCount;

        /** 向量总数。 */
        private Long embeddingCount;

        /** 打开的知识治理问题数量。 */
        private Long openIssueCount;

        /** 高风险知识治理问题数量。 */
        private Long highRiskIssueCount;

        /** 未同步或同步失败的向量数量。 */
        private Long unsyncedEmbeddingCount;

        public Long getDocumentCount() {
            return documentCount;
        }

        public void setDocumentCount(Long documentCount) {
            this.documentCount = documentCount;
        }

        public Long getParsedDocumentCount() {
            return parsedDocumentCount;
        }

        public void setParsedDocumentCount(Long parsedDocumentCount) {
            this.parsedDocumentCount = parsedDocumentCount;
        }

        public Long getFailedDocumentCount() {
            return failedDocumentCount;
        }

        public void setFailedDocumentCount(Long failedDocumentCount) {
            this.failedDocumentCount = failedDocumentCount;
        }

        public Long getProcessingDocumentCount() {
            return processingDocumentCount;
        }

        public void setProcessingDocumentCount(Long processingDocumentCount) {
            this.processingDocumentCount = processingDocumentCount;
        }

        public Long getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(Long chunkCount) {
            this.chunkCount = chunkCount;
        }

        public Long getEmbeddingCount() {
            return embeddingCount;
        }

        public void setEmbeddingCount(Long embeddingCount) {
            this.embeddingCount = embeddingCount;
        }

        public Long getOpenIssueCount() {
            return openIssueCount;
        }

        public void setOpenIssueCount(Long openIssueCount) {
            this.openIssueCount = openIssueCount;
        }

        public Long getHighRiskIssueCount() {
            return highRiskIssueCount;
        }

        public void setHighRiskIssueCount(Long highRiskIssueCount) {
            this.highRiskIssueCount = highRiskIssueCount;
        }

        public Long getUnsyncedEmbeddingCount() {
            return unsyncedEmbeddingCount;
        }

        public void setUnsyncedEmbeddingCount(Long unsyncedEmbeddingCount) {
            this.unsyncedEmbeddingCount = unsyncedEmbeddingCount;
        }
    }

    /**
     * 运行趋势条目。
     */
    public static class RunTrendItem {

        /** 统计日期。 */
        private LocalDate statDate;

        /** 运行次数。 */
        private Long runCount;

        /** 成功次数。 */
        private Long successCount;

        /** 失败次数。 */
        private Long failureCount;

        /** Token 总数。 */
        private Long tokenCount;

        /** 成本金额。 */
        private BigDecimal costAmount = BigDecimal.ZERO;

        public LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(LocalDate statDate) {
            this.statDate = statDate;
        }

        public Long getRunCount() {
            return runCount;
        }

        public void setRunCount(Long runCount) {
            this.runCount = runCount;
        }

        public Long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(Long successCount) {
            this.successCount = successCount;
        }

        public Long getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(Long failureCount) {
            this.failureCount = failureCount;
        }

        public Long getTokenCount() {
            return tokenCount;
        }

        public void setTokenCount(Long tokenCount) {
            this.tokenCount = tokenCount;
        }

        public BigDecimal getCostAmount() {
            return costAmount;
        }

        public void setCostAmount(BigDecimal costAmount) {
            this.costAmount = costAmount;
        }
    }

    /**
     * 最近运行条目。
     */
    public static class RecentRunItem {

        /** 运行主键 ID。 */
        private String id;

        /** 运行编号。 */
        private String runNo;

        /** 运行类型。 */
        private String runType;

        /** 运行对象名称。 */
        private String targetName;

        /** 发起用户名称。 */
        private String userName;

        /** 运行状态。 */
        private String status;

        /** 运行状态中文标签。 */
        private String statusLabel;

        /** 总 Token 数。 */
        private Long totalTokens;

        /** 总成本。 */
        private BigDecimal totalCost = BigDecimal.ZERO;

        /** 耗时毫秒。 */
        private Long latencyMs;

        /** 错误消息。 */
        private String errorMessage;

        /** 开始时间。 */
        private LocalDateTime startedAt;

        /** 结束时间。 */
        private LocalDateTime finishedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRunNo() {
            return runNo;
        }

        public void setRunNo(String runNo) {
            this.runNo = runNo;
        }

        public String getRunType() {
            return runType;
        }

        public void setRunType(String runType) {
            this.runType = runType;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public Long getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Long totalTokens) {
            this.totalTokens = totalTokens;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }

        public Long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Long latencyMs) {
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
    }

    /**
     * 模型使用条目。
     */
    public static class ModelUsageItem {

        /** 模型 ID。 */
        private String modelId;

        /** 模型名称。 */
        private String modelName;

        /** 服务商名称。 */
        private String providerName;

        /** 调用次数。 */
        private Long callCount;

        /** 成功调用次数。 */
        private Long successCount;

        /** 失败调用次数。 */
        private Long failureCount;

        /** Token 总数。 */
        private Long totalTokens;

        /** 成本金额。 */
        private BigDecimal totalCost = BigDecimal.ZERO;

        /** 平均耗时毫秒。 */
        private BigDecimal avgLatencyMs = BigDecimal.ZERO;

        /** 调用占比，0 到 100。 */
        private BigDecimal usagePercent = BigDecimal.ZERO;

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public Long getCallCount() {
            return callCount;
        }

        public void setCallCount(Long callCount) {
            this.callCount = callCount;
        }

        public Long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(Long successCount) {
            this.successCount = successCount;
        }

        public Long getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(Long failureCount) {
            this.failureCount = failureCount;
        }

        public Long getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Long totalTokens) {
            this.totalTokens = totalTokens;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }

        public BigDecimal getAvgLatencyMs() {
            return avgLatencyMs;
        }

        public void setAvgLatencyMs(BigDecimal avgLatencyMs) {
            this.avgLatencyMs = avgLatencyMs;
        }

        public BigDecimal getUsagePercent() {
            return usagePercent;
        }

        public void setUsagePercent(BigDecimal usagePercent) {
            this.usagePercent = usagePercent;
        }
    }

    /**
     * 任务队列条目。
     */
    public static class TaskQueueItem {

        /** 任务 ID。 */
        private String id;

        /** 任务名称。 */
        private String taskName;

        /** 任务类型。 */
        private String taskType;

        /** 任务状态。 */
        private String status;

        /** 进度百分比。 */
        private BigDecimal progressPercent = BigDecimal.ZERO;

        /** 当前阶段消息。 */
        private String currentMessage;

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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(BigDecimal progressPercent) {
            this.progressPercent = progressPercent;
        }

        public String getCurrentMessage() {
            return currentMessage;
        }

        public void setCurrentMessage(String currentMessage) {
            this.currentMessage = currentMessage;
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
     * 告警事件条目。
     */
    public static class AlertEventItem {

        /** 告警事件 ID。 */
        private String id;

        /** 告警标题。 */
        private String alertTitle;

        /** 告警级别。 */
        private String severity;

        /** 告警状态。 */
        private String status;

        /** 指标来源模块。 */
        private String metricSource;

        /** 当前指标值。 */
        private BigDecimal metricValue = BigDecimal.ZERO;

        /** 阈值。 */
        private BigDecimal thresholdValue = BigDecimal.ZERO;

        /** 最近触发时间。 */
        private LocalDateTime lastTriggeredAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
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

        public LocalDateTime getLastTriggeredAt() {
            return lastTriggeredAt;
        }

        public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
            this.lastTriggeredAt = lastTriggeredAt;
        }
    }

    /**
     * 健康检查条目。
     */
    public static class HealthCheckItem {

        /** 巡检项 ID。 */
        private String id;

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
        private Long latencyMs;

        /** 最近巡检时间。 */
        private LocalDateTime lastCheckedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public Long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public LocalDateTime getLastCheckedAt() {
            return lastCheckedAt;
        }

        public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
            this.lastCheckedAt = lastCheckedAt;
        }
    }

    /**
     * 运营洞察条目。
     */
    public static class InsightItem {

        /** 洞察标题。 */
        private String title;

        /** 洞察内容。 */
        private String content;

        /** 洞察类型。 */
        private String tone;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTone() {
            return tone;
        }

        public void setTone(String tone) {
            this.tone = tone;
        }
    }
}
