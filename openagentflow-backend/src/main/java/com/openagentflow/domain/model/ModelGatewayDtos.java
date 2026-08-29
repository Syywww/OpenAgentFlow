package com.openagentflow.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型网关与模型治理 DTO 集合。
 */
public class ModelGatewayDtos {

    /**
     * 模型网关概览数据。
     */
    public static class Overview {
        /** 已启用的路由策略数量。 */
        private Long enabledPolicyCount;

        /** 已启用的模型数量。 */
        private Long enabledModelCount;

        /** 最近24小时模型调用次数。 */
        private Long callCount24h;

        /** 最近24小时失败调用次数。 */
        private Long failureCount24h;

        /** 最近24小时失败率。 */
        private BigDecimal failureRate24h;

        /** 最近24小时平均耗时毫秒。 */
        private BigDecimal avgLatencyMs24h;

        /** 最近24小时模型回退次数。 */
        private Long fallbackCount24h;

        public Long getEnabledPolicyCount() {
            return enabledPolicyCount;
        }

        public void setEnabledPolicyCount(Long enabledPolicyCount) {
            this.enabledPolicyCount = enabledPolicyCount;
        }

        public Long getEnabledModelCount() {
            return enabledModelCount;
        }

        public void setEnabledModelCount(Long enabledModelCount) {
            this.enabledModelCount = enabledModelCount;
        }

        public Long getCallCount24h() {
            return callCount24h;
        }

        public void setCallCount24h(Long callCount24h) {
            this.callCount24h = callCount24h;
        }

        public Long getFailureCount24h() {
            return failureCount24h;
        }

        public void setFailureCount24h(Long failureCount24h) {
            this.failureCount24h = failureCount24h;
        }

        public BigDecimal getFailureRate24h() {
            return failureRate24h;
        }

        public void setFailureRate24h(BigDecimal failureRate24h) {
            this.failureRate24h = failureRate24h;
        }

        public BigDecimal getAvgLatencyMs24h() {
            return avgLatencyMs24h;
        }

        public void setAvgLatencyMs24h(BigDecimal avgLatencyMs24h) {
            this.avgLatencyMs24h = avgLatencyMs24h;
        }

        public Long getFallbackCount24h() {
            return fallbackCount24h;
        }

        public void setFallbackCount24h(Long fallbackCount24h) {
            this.fallbackCount24h = fallbackCount24h;
        }
    }

    /**
     * 模型路由策略摘要。
     */
    public static class PolicySummary {
        /** 策略ID。 */
        private String id;

        /** 策略编码。 */
        private String policyCode;

        /** 策略名称。 */
        private String policyName;

        /** 适用场景类型。 */
        private String sceneType;

        /** 匹配规则JSON。 */
        private String matchRule;

        /** 匹配范围：GLOBAL / WORKSPACE。 */
        private String matchScope;

        /** WORKSPACE 范围命中的工作空间ID列表。 */
        private List<String> workspaceIds;

        /** 是否启用失败回退。 */
        private Boolean fallbackEnabled;

        /** 熔断连续失败次数阈值，空值用默认常量兜底。 */
        private Integer breakerFailureThreshold;

        /** 熔断持续时间（秒），空值用默认常量兜底。 */
        private Integer breakerTimeoutSeconds;

        /** 路由模式：weighted 按权重分发 / cost_first 按估算成本优选。 */
        private String routingMode;

        /** 策略状态。 */
        private String status;

        /** 候选模型列表。 */
        private List<CandidateSummary> candidates;

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

        public String getPolicyCode() {
            return policyCode;
        }

        public void setPolicyCode(String policyCode) {
            this.policyCode = policyCode;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public String getSceneType() {
            return sceneType;
        }

        public void setSceneType(String sceneType) {
            this.sceneType = sceneType;
        }

        public String getMatchRule() {
            return matchRule;
        }

        public void setMatchRule(String matchRule) {
            this.matchRule = matchRule;
        }

        public String getMatchScope() {
            return matchScope;
        }

        public void setMatchScope(String matchScope) {
            this.matchScope = matchScope;
        }

        public List<String> getWorkspaceIds() {
            return workspaceIds;
        }

        public void setWorkspaceIds(List<String> workspaceIds) {
            this.workspaceIds = workspaceIds;
        }

        public Boolean getFallbackEnabled() {
            return fallbackEnabled;
        }

        public void setFallbackEnabled(Boolean fallbackEnabled) {
            this.fallbackEnabled = fallbackEnabled;
        }

        public Integer getBreakerFailureThreshold() {
            return breakerFailureThreshold;
        }

        public void setBreakerFailureThreshold(Integer breakerFailureThreshold) {
            this.breakerFailureThreshold = breakerFailureThreshold;
        }

        public Integer getBreakerTimeoutSeconds() {
            return breakerTimeoutSeconds;
        }

        public void setBreakerTimeoutSeconds(Integer breakerTimeoutSeconds) {
            this.breakerTimeoutSeconds = breakerTimeoutSeconds;
        }

        public String getRoutingMode() {
            return routingMode;
        }

        public void setRoutingMode(String routingMode) {
            this.routingMode = routingMode;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<CandidateSummary> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<CandidateSummary> candidates) {
            this.candidates = candidates;
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
     * 模型路由候选摘要。
     */
    public static class CandidateSummary {
        /** 候选ID。 */
        private String id;

        /** 策略ID。 */
        private String policyId;

        /** 模型ID。 */
        private String modelId;

        /** 模型名称。 */
        private String modelName;

        /** 模型编码。 */
        private String modelCode;

        /** 服务商名称。 */
        private String providerName;

        /** 候选优先级。 */
        private Integer priority;

        /** 候选权重。 */
        private BigDecimal weight;

        /** 最大允许平均耗时毫秒。 */
        private Integer maxLatencyMs;

        /** 最大允许每千Token成本。 */
        private BigDecimal maxCostPer1k;

        /** 是否启用。 */
        private Boolean enabled;

        /** 最近失败率。 */
        private BigDecimal recentFailureRate;

        /** 最近平均耗时毫秒。 */
        private BigDecimal recentAvgLatencyMs;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

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

        public String getModelCode() {
            return modelCode;
        }

        public void setModelCode(String modelCode) {
            this.modelCode = modelCode;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public Integer getMaxLatencyMs() {
            return maxLatencyMs;
        }

        public void setMaxLatencyMs(Integer maxLatencyMs) {
            this.maxLatencyMs = maxLatencyMs;
        }

        public BigDecimal getMaxCostPer1k() {
            return maxCostPer1k;
        }

        public void setMaxCostPer1k(BigDecimal maxCostPer1k) {
            this.maxCostPer1k = maxCostPer1k;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public BigDecimal getRecentFailureRate() {
            return recentFailureRate;
        }

        public void setRecentFailureRate(BigDecimal recentFailureRate) {
            this.recentFailureRate = recentFailureRate;
        }

        public BigDecimal getRecentAvgLatencyMs() {
            return recentAvgLatencyMs;
        }

        public void setRecentAvgLatencyMs(BigDecimal recentAvgLatencyMs) {
            this.recentAvgLatencyMs = recentAvgLatencyMs;
        }
    }

    /**
     * 保存模型路由策略请求。
     */
    public static class PolicyRequest {
        /** 策略编码。 */
        private String policyCode;

        /** 策略名称。 */
        private String policyName;

        /** 适用场景类型。 */
        private String sceneType;

        /** 匹配规则JSON。 */
        private String matchRule;

        /** 匹配范围：GLOBAL / WORKSPACE。 */
        private String matchScope;

        /** WORKSPACE 范围命中的工作空间ID列表。 */
        private List<String> workspaceIds;

        /** 是否启用失败回退。 */
        private Boolean fallbackEnabled;

        /** 熔断连续失败次数阈值，空值用默认常量兜底。 */
        private Integer breakerFailureThreshold;

        /** 熔断持续时间（秒），空值用默认常量兜底。 */
        private Integer breakerTimeoutSeconds;

        /** 路由模式：weighted 按权重分发 / cost_first 按估算成本优选。 */
        private String routingMode;

        /** 策略状态。 */
        private String status;

        /** 候选模型列表。 */
        private List<CandidateRequest> candidates;

        public String getPolicyCode() {
            return policyCode;
        }

        public void setPolicyCode(String policyCode) {
            this.policyCode = policyCode;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public String getSceneType() {
            return sceneType;
        }

        public void setSceneType(String sceneType) {
            this.sceneType = sceneType;
        }

        public String getMatchRule() {
            return matchRule;
        }

        public void setMatchRule(String matchRule) {
            this.matchRule = matchRule;
        }

        public String getMatchScope() {
            return matchScope;
        }

        public void setMatchScope(String matchScope) {
            this.matchScope = matchScope;
        }

        public List<String> getWorkspaceIds() {
            return workspaceIds;
        }

        public void setWorkspaceIds(List<String> workspaceIds) {
            this.workspaceIds = workspaceIds;
        }

        public Boolean getFallbackEnabled() {
            return fallbackEnabled;
        }

        public void setFallbackEnabled(Boolean fallbackEnabled) {
            this.fallbackEnabled = fallbackEnabled;
        }

        public Integer getBreakerFailureThreshold() {
            return breakerFailureThreshold;
        }

        public void setBreakerFailureThreshold(Integer breakerFailureThreshold) {
            this.breakerFailureThreshold = breakerFailureThreshold;
        }

        public Integer getBreakerTimeoutSeconds() {
            return breakerTimeoutSeconds;
        }

        public void setBreakerTimeoutSeconds(Integer breakerTimeoutSeconds) {
            this.breakerTimeoutSeconds = breakerTimeoutSeconds;
        }

        public String getRoutingMode() {
            return routingMode;
        }

        public void setRoutingMode(String routingMode) {
            this.routingMode = routingMode;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<CandidateRequest> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<CandidateRequest> candidates) {
            this.candidates = candidates;
        }
    }

    /**
     * 保存模型路由候选请求。
     */
    public static class CandidateRequest {
        /** 候选ID，更新时传入。 */
        private String id;

        /** 模型ID。 */
        private String modelId;

        /** 候选优先级。 */
        private Integer priority;

        /** 候选权重。 */
        private BigDecimal weight;

        /** 最大允许平均耗时毫秒。 */
        private Integer maxLatencyMs;

        /** 最大允许每千Token成本。 */
        private BigDecimal maxCostPer1k;

        /** 是否启用。 */
        private Boolean enabled;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public Integer getMaxLatencyMs() {
            return maxLatencyMs;
        }

        public void setMaxLatencyMs(Integer maxLatencyMs) {
            this.maxLatencyMs = maxLatencyMs;
        }

        public BigDecimal getMaxCostPer1k() {
            return maxCostPer1k;
        }

        public void setMaxCostPer1k(BigDecimal maxCostPer1k) {
            this.maxCostPer1k = maxCostPer1k;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 模型健康摘要。
     */
    public static class ModelHealthSummary {
        /** 模型ID。 */
        private String modelId;

        /** 模型名称。 */
        private String modelName;

        /** 模型编码。 */
        private String modelCode;

        /** 服务商名称。 */
        private String providerName;

        /** 模型状态。 */
        private String status;

        /** 健康状态。 */
        private String healthStatus;

        /** 最近调用次数。 */
        private Long recentCallCount;

        /** 最近失败次数。 */
        private Long recentFailureCount;

        /** 最近失败率。 */
        private BigDecimal recentFailureRate;

        /** 最近平均耗时毫秒。 */
        private BigDecimal recentAvgLatencyMs;

        /** 最近成本金额。 */
        private BigDecimal recentCost;

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

        public String getModelCode() {
            return modelCode;
        }

        public void setModelCode(String modelCode) {
            this.modelCode = modelCode;
        }

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public void setHealthStatus(String healthStatus) {
            this.healthStatus = healthStatus;
        }

        public Long getRecentCallCount() {
            return recentCallCount;
        }

        public void setRecentCallCount(Long recentCallCount) {
            this.recentCallCount = recentCallCount;
        }

        public Long getRecentFailureCount() {
            return recentFailureCount;
        }

        public void setRecentFailureCount(Long recentFailureCount) {
            this.recentFailureCount = recentFailureCount;
        }

        public BigDecimal getRecentFailureRate() {
            return recentFailureRate;
        }

        public void setRecentFailureRate(BigDecimal recentFailureRate) {
            this.recentFailureRate = recentFailureRate;
        }

        public BigDecimal getRecentAvgLatencyMs() {
            return recentAvgLatencyMs;
        }

        public void setRecentAvgLatencyMs(BigDecimal recentAvgLatencyMs) {
            this.recentAvgLatencyMs = recentAvgLatencyMs;
        }

        public BigDecimal getRecentCost() {
            return recentCost;
        }

        public void setRecentCost(BigDecimal recentCost) {
            this.recentCost = recentCost;
        }
    }

    /**
     * 最近模型网关调用摘要。
     */
    public static class GatewayCallSummary {
        /** 调用ID。 */
        private String id;

        /** 运行ID。 */
        private String runId;

        /** 场景类型。 */
        private String gatewaySceneType;

        /** 策略ID。 */
        private String routePolicyId;

        /** 策略名称。 */
        private String policyName;

        /** 服务商名称。 */
        private String providerName;

        /** 模型名称。 */
        private String modelName;

        /** 是否回退。 */
        private Boolean fallbackUsed;

        /** 是否成功。 */
        private Boolean success;

        /** 总Token数。 */
        private Integer totalTokens;

        /** 调用成本。 */
        private BigDecimal costAmount;

        /** 耗时毫秒。 */
        private Integer latencyMs;

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

        public String getGatewaySceneType() {
            return gatewaySceneType;
        }

        public void setGatewaySceneType(String gatewaySceneType) {
            this.gatewaySceneType = gatewaySceneType;
        }

        public String getRoutePolicyId() {
            return routePolicyId;
        }

        public void setRoutePolicyId(String routePolicyId) {
            this.routePolicyId = routePolicyId;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
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

        public Boolean getFallbackUsed() {
            return fallbackUsed;
        }

        public void setFallbackUsed(Boolean fallbackUsed) {
            this.fallbackUsed = fallbackUsed;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
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
}
