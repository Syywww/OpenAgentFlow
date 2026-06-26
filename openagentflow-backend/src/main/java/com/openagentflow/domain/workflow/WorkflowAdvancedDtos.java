package com.openagentflow.domain.workflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流生产级增强 DTO 集合。
 *
 * <p>该集合承载模板、API 发布、版本差异、人工确认、调试模式和治理能力的前后端交互对象。</p>
 */
public final class WorkflowAdvancedDtos {

    private WorkflowAdvancedDtos() {
    }

    /**
     * 工作流增强总览。
     */
    public static class Overview {
        /** 工作流总数。 */
        private Integer workflowCount;
        /** 已发布工作流数量。 */
        private Integer publishedCount;
        /** API 发布端点数量。 */
        private Integer apiEndpointCount;
        /** 待处理人工确认任务数量。 */
        private Integer pendingHumanTaskCount;
        /** 模板数量。 */
        private Integer templateCount;
        /** 近 24 小时运行数量。 */
        private Integer todayRunCount;
        /** 近 24 小时失败数量。 */
        private Integer todayFailedCount;
        /** 能力清单。 */
        private List<Capability> capabilities;

        public Integer getWorkflowCount() {
            return workflowCount;
        }

        public void setWorkflowCount(Integer workflowCount) {
            this.workflowCount = workflowCount;
        }

        public Integer getPublishedCount() {
            return publishedCount;
        }

        public void setPublishedCount(Integer publishedCount) {
            this.publishedCount = publishedCount;
        }

        public Integer getApiEndpointCount() {
            return apiEndpointCount;
        }

        public void setApiEndpointCount(Integer apiEndpointCount) {
            this.apiEndpointCount = apiEndpointCount;
        }

        public Integer getPendingHumanTaskCount() {
            return pendingHumanTaskCount;
        }

        public void setPendingHumanTaskCount(Integer pendingHumanTaskCount) {
            this.pendingHumanTaskCount = pendingHumanTaskCount;
        }

        public Integer getTemplateCount() {
            return templateCount;
        }

        public void setTemplateCount(Integer templateCount) {
            this.templateCount = templateCount;
        }

        public Integer getTodayRunCount() {
            return todayRunCount;
        }

        public void setTodayRunCount(Integer todayRunCount) {
            this.todayRunCount = todayRunCount;
        }

        public Integer getTodayFailedCount() {
            return todayFailedCount;
        }

        public void setTodayFailedCount(Integer todayFailedCount) {
            this.todayFailedCount = todayFailedCount;
        }

        public List<Capability> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(List<Capability> capabilities) {
            this.capabilities = capabilities;
        }
    }

    /**
     * 工作流增强能力项。
     */
    public static class Capability {
        /** 能力编码。 */
        private String code;
        /** 能力名称。 */
        private String name;
        /** 能力分类。 */
        private String category;
        /** 当前状态。 */
        private String status;
        /** 配置入口或关键字段。 */
        private String configKey;
        /** 能力说明。 */
        private String description;

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

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getConfigKey() {
            return configKey;
        }

        public void setConfigKey(String configKey) {
            this.configKey = configKey;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 工作流模板摘要。
     */
    public static class TemplateSummary {
        /** 模板 ID。 */
        private String id;
        /** 模板编码。 */
        private String templateCode;
        /** 模板名称。 */
        private String templateName;
        /** 模板分类。 */
        private String templateCategory;
        /** 模板描述。 */
        private String description;
        /** 模板画布 JSON。 */
        private Map<String, Object> graphJson;
        /** 变量 Schema。 */
        private Map<String, Object> variableSchema;
        /** 默认执行策略。 */
        private Map<String, Object> defaultPolicy;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        public String getTemplateCategory() {
            return templateCategory;
        }

        public void setTemplateCategory(String templateCategory) {
            this.templateCategory = templateCategory;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getGraphJson() {
            return graphJson;
        }

        public void setGraphJson(Map<String, Object> graphJson) {
            this.graphJson = graphJson;
        }

        public Map<String, Object> getVariableSchema() {
            return variableSchema;
        }

        public void setVariableSchema(Map<String, Object> variableSchema) {
            this.variableSchema = variableSchema;
        }

        public Map<String, Object> getDefaultPolicy() {
            return defaultPolicy;
        }

        public void setDefaultPolicy(Map<String, Object> defaultPolicy) {
            this.defaultPolicy = defaultPolicy;
        }
    }

    /**
     * 工作流 API 端点摘要。
     */
    public static class ApiEndpointSummary {
        /** 端点 ID。 */
        private String id;
        /** 工作流 ID。 */
        private String workflowId;
        /** 工作流名称。 */
        private String workflowName;
        /** 端点编码。 */
        private String endpointCode;
        /** 端点名称。 */
        private String endpointName;
        /** 认证方式。 */
        private String authType;
        /** 每分钟限流次数。 */
        private Integer rateLimitPerMinute;
        /** 是否启用。 */
        private Boolean enabled;
        /** 最近调用时间。 */
        private LocalDateTime lastInvokedAt;

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

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public String getEndpointCode() {
            return endpointCode;
        }

        public void setEndpointCode(String endpointCode) {
            this.endpointCode = endpointCode;
        }

        public String getEndpointName() {
            return endpointName;
        }

        public void setEndpointName(String endpointName) {
            this.endpointName = endpointName;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public Integer getRateLimitPerMinute() {
            return rateLimitPerMinute;
        }

        public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public LocalDateTime getLastInvokedAt() {
            return lastInvokedAt;
        }

        public void setLastInvokedAt(LocalDateTime lastInvokedAt) {
            this.lastInvokedAt = lastInvokedAt;
        }
    }

    /**
     * API 发布请求。
     */
    public static class ApiPublishRequest {
        /** 端点编码。 */
        private String endpointCode;
        /** 端点名称。 */
        private String endpointName;
        /** 认证方式。 */
        private String authType;
        /** 每分钟限流次数。 */
        private Integer rateLimitPerMinute;
        /** 是否启用。 */
        private Boolean enabled;

        public String getEndpointCode() {
            return endpointCode;
        }

        public void setEndpointCode(String endpointCode) {
            this.endpointCode = endpointCode;
        }

        public String getEndpointName() {
            return endpointName;
        }

        public void setEndpointName(String endpointName) {
            this.endpointName = endpointName;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public Integer getRateLimitPerMinute() {
            return rateLimitPerMinute;
        }

        public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 人工确认任务摘要。
     */
    public static class HumanTaskSummary {
        /** 任务 ID。 */
        private String id;
        /** 工作流运行 ID。 */
        private String workflowRunId;
        /** 任务名称。 */
        private String taskName;
        /** 状态。 */
        private String status;
        /** 决策。 */
        private String decision;
        /** 任务载荷。 */
        private Map<String, Object> payload;
        /** 创建时间。 */
        private LocalDateTime createdAt;
        /** 过期时间。 */
        private LocalDateTime expiredAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getWorkflowRunId() {
            return workflowRunId;
        }

        public void setWorkflowRunId(String workflowRunId) {
            this.workflowRunId = workflowRunId;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(LocalDateTime expiredAt) {
            this.expiredAt = expiredAt;
        }
    }

    /**
     * 人工确认决策请求。
     */
    public static class HumanTaskDecisionRequest {
        /** 决策：approved、rejected、changed。 */
        private String decision;
        /** 处理备注。 */
        private String comment;
        /** 修改后的参数。 */
        private Map<String, Object> changedPayload;

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public Map<String, Object> getChangedPayload() {
            return changedPayload;
        }

        public void setChangedPayload(Map<String, Object> changedPayload) {
            this.changedPayload = changedPayload;
        }
    }

    /**
     * 版本差异结果。
     */
    public static class VersionDiff {
        /** 左侧版本号。 */
        private String leftVersion;
        /** 右侧版本号。 */
        private String rightVersion;
        /** 新增节点数量。 */
        private Integer addedNodes;
        /** 删除节点数量。 */
        private Integer removedNodes;
        /** 连线变化数量。 */
        private Integer changedEdges;
        /** 变更摘要。 */
        private List<String> changes;

        public String getLeftVersion() {
            return leftVersion;
        }

        public void setLeftVersion(String leftVersion) {
            this.leftVersion = leftVersion;
        }

        public String getRightVersion() {
            return rightVersion;
        }

        public void setRightVersion(String rightVersion) {
            this.rightVersion = rightVersion;
        }

        public Integer getAddedNodes() {
            return addedNodes;
        }

        public void setAddedNodes(Integer addedNodes) {
            this.addedNodes = addedNodes;
        }

        public Integer getRemovedNodes() {
            return removedNodes;
        }

        public void setRemovedNodes(Integer removedNodes) {
            this.removedNodes = removedNodes;
        }

        public Integer getChangedEdges() {
            return changedEdges;
        }

        public void setChangedEdges(Integer changedEdges) {
            this.changedEdges = changedEdges;
        }

        public List<String> getChanges() {
            return changes;
        }

        public void setChanges(List<String> changes) {
            this.changes = changes;
        }
    }
}
