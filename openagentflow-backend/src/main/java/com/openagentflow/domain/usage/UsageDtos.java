package com.openagentflow.domain.usage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成本与用量中心 DTO 集合。
 */
public final class UsageDtos {

    private UsageDtos() {
    }

    /**
     * 用量总览数据。
     */
    public static class Overview {
        /** 总调用次数。 */
        private Long callCount;
        /** 成功调用次数。 */
        private Long successCount;
        /** 失败调用次数。 */
        private Long failureCount;
        /** 总 Token 数。 */
        private Long totalTokens;
        /** 输入 Token 数。 */
        private Long promptTokens;
        /** 输出 Token 数。 */
        private Long completionTokens;
        /** 总成本。 */
        private BigDecimal totalCost;
        /** 平均耗时毫秒。 */
        private BigDecimal avgLatencyMs;
        /** 当前命中的配额规则数量。 */
        private Integer quotaRuleCount;
        /** 已超过或即将超过的配额规则数量。 */
        private Integer quotaRiskCount;

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

        public Long getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Long promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Long getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Long completionTokens) {
            this.completionTokens = completionTokens;
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

        public Integer getQuotaRuleCount() {
            return quotaRuleCount;
        }

        public void setQuotaRuleCount(Integer quotaRuleCount) {
            this.quotaRuleCount = quotaRuleCount;
        }

        public Integer getQuotaRiskCount() {
            return quotaRiskCount;
        }

        public void setQuotaRiskCount(Integer quotaRiskCount) {
            this.quotaRiskCount = quotaRiskCount;
        }
    }

    /**
     * 每日成本趋势。
     */
    public static class DailyUsage {
        /** 统计日期。 */
        private LocalDate statDate;
        /** 调用次数。 */
        private Long callCount;
        /** 成功次数。 */
        private Long successCount;
        /** 失败次数。 */
        private Long failureCount;
        /** 总 Token。 */
        private Long totalTokens;
        /** 总成本。 */
        private BigDecimal totalCost;

        public LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(LocalDate statDate) {
            this.statDate = statDate;
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
    }

    /**
     * 维度拆分数据。
     */
    public static class BreakdownItem {
        /** 维度 ID。 */
        private String id;
        /** 维度名称。 */
        private String name;
        /** 调用次数。 */
        private Long callCount;
        /** 总 Token。 */
        private Long totalTokens;
        /** 总成本。 */
        private BigDecimal totalCost;
        /** 平均耗时。 */
        private BigDecimal avgLatencyMs;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getCallCount() {
            return callCount;
        }

        public void setCallCount(Long callCount) {
            this.callCount = callCount;
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
    }

    /**
     * LLM 调用成本明细。
     */
    public static class CallDetail {
        /** 调用日志 ID。 */
        private String id;
        /** 运行 ID。 */
        private String runId;
        /** Trace 步骤 ID。 */
        private String stepId;
        /** 运行编号。 */
        private String runNo;
        /** 运行类型。 */
        private String runType;
        /** 服务商名称。 */
        private String providerName;
        /** 模型名称。 */
        private String modelName;
        /** Agent 名称。 */
        private String agentName;
        /** 工作流名称。 */
        private String workflowName;
        /** 用户名。 */
        private String userName;
        /** 输入 Token。 */
        private Integer promptTokens;
        /** 输出 Token。 */
        private Integer completionTokens;
        /** 总 Token。 */
        private Integer totalTokens;
        /** 调用成本。 */
        private BigDecimal costAmount;
        /** 耗时毫秒。 */
        private Integer latencyMs;
        /** 是否成功。 */
        private Boolean success;
        /** 错误信息。 */
        private String errorMessage;
        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
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

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
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

    /**
     * 配额规则保存请求。
     */
    public static class QuotaRequest {
        /** 配额主体类型。 */
        private String subjectType;
        /** 配额主体 ID。 */
        private String subjectId;
        /** 服务商 ID。 */
        private String providerId;
        /** 模型 ID。 */
        private String modelId;
        /** 配额周期：daily/monthly。 */
        private String quotaPeriod;
        /** Token 上限。 */
        private Long tokenLimit;
        /** 成本上限。 */
        private BigDecimal costLimit;

        public String getSubjectType() {
            return subjectType;
        }

        public void setSubjectType(String subjectType) {
            this.subjectType = subjectType;
        }

        public String getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(String subjectId) {
            this.subjectId = subjectId;
        }

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public String getQuotaPeriod() {
            return quotaPeriod;
        }

        public void setQuotaPeriod(String quotaPeriod) {
            this.quotaPeriod = quotaPeriod;
        }

        public Long getTokenLimit() {
            return tokenLimit;
        }

        public void setTokenLimit(Long tokenLimit) {
            this.tokenLimit = tokenLimit;
        }

        public BigDecimal getCostLimit() {
            return costLimit;
        }

        public void setCostLimit(BigDecimal costLimit) {
            this.costLimit = costLimit;
        }
    }

    /**
     * 配额规则展示对象。
     */
    public static class QuotaSummary extends QuotaRequest {
        /** 配额 ID。 */
        private String id;
        /** Token 已用。 */
        private Long tokenUsed;
        /** 成本已用。 */
        private BigDecimal costUsed;
        /** Token 使用率。 */
        private BigDecimal tokenUsageRate;
        /** 成本使用率。 */
        private BigDecimal costUsageRate;
        /** 下次重置时间。 */
        private LocalDateTime resetAt;
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

        public Long getTokenUsed() {
            return tokenUsed;
        }

        public void setTokenUsed(Long tokenUsed) {
            this.tokenUsed = tokenUsed;
        }

        public BigDecimal getCostUsed() {
            return costUsed;
        }

        public void setCostUsed(BigDecimal costUsed) {
            this.costUsed = costUsed;
        }

        public BigDecimal getTokenUsageRate() {
            return tokenUsageRate;
        }

        public void setTokenUsageRate(BigDecimal tokenUsageRate) {
            this.tokenUsageRate = tokenUsageRate;
        }

        public BigDecimal getCostUsageRate() {
            return costUsageRate;
        }

        public void setCostUsageRate(BigDecimal costUsageRate) {
            this.costUsageRate = costUsageRate;
        }

        public LocalDateTime getResetAt() {
            return resetAt;
        }

        public void setResetAt(LocalDateTime resetAt) {
            this.resetAt = resetAt;
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
     * 成本中心页面聚合数据。
     */
    public static class ConsoleData {
        /** 总览。 */
        private Overview overview;
        /** 趋势。 */
        private List<DailyUsage> daily;
        /** 模型拆分。 */
        private List<BreakdownItem> modelBreakdown;
        /** Agent 拆分。 */
        private List<BreakdownItem> agentBreakdown;

        public Overview getOverview() {
            return overview;
        }

        public void setOverview(Overview overview) {
            this.overview = overview;
        }

        public List<DailyUsage> getDaily() {
            return daily;
        }

        public void setDaily(List<DailyUsage> daily) {
            this.daily = daily;
        }

        public List<BreakdownItem> getModelBreakdown() {
            return modelBreakdown;
        }

        public void setModelBreakdown(List<BreakdownItem> modelBreakdown) {
            this.modelBreakdown = modelBreakdown;
        }

        public List<BreakdownItem> getAgentBreakdown() {
            return agentBreakdown;
        }

        public void setAgentBreakdown(List<BreakdownItem> agentBreakdown) {
            this.agentBreakdown = agentBreakdown;
        }
    }
}
