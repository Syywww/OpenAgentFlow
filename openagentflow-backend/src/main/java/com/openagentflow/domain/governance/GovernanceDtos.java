package com.openagentflow.domain.governance;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计与风险治理中心传输对象集合。
 */
public final class GovernanceDtos {

    private GovernanceDtos() {
    }

    /**
     * 治理中心概览指标。
     */
    public static class Overview {

        /** 审计日志总数。 */
        private Long auditCount;

        /** 失败操作数量。 */
        private Long failedOperationCount;

        /** 打开状态风险数量。 */
        private Long openRiskCount;

        /** 高风险事件数量。 */
        private Long highRiskCount;

        /** 待确认请求数量。 */
        private Long pendingConfirmationCount;

        /** 护栏事件数量。 */
        private Long guardrailEventCount;

        /** 高风险工具数量。 */
        private Long highRiskToolCount;

        public Long getAuditCount() {
            return auditCount;
        }

        public void setAuditCount(Long auditCount) {
            this.auditCount = auditCount;
        }

        public Long getFailedOperationCount() {
            return failedOperationCount;
        }

        public void setFailedOperationCount(Long failedOperationCount) {
            this.failedOperationCount = failedOperationCount;
        }

        public Long getOpenRiskCount() {
            return openRiskCount;
        }

        public void setOpenRiskCount(Long openRiskCount) {
            this.openRiskCount = openRiskCount;
        }

        public Long getHighRiskCount() {
            return highRiskCount;
        }

        public void setHighRiskCount(Long highRiskCount) {
            this.highRiskCount = highRiskCount;
        }

        public Long getPendingConfirmationCount() {
            return pendingConfirmationCount;
        }

        public void setPendingConfirmationCount(Long pendingConfirmationCount) {
            this.pendingConfirmationCount = pendingConfirmationCount;
        }

        public Long getGuardrailEventCount() {
            return guardrailEventCount;
        }

        public void setGuardrailEventCount(Long guardrailEventCount) {
            this.guardrailEventCount = guardrailEventCount;
        }

        public Long getHighRiskToolCount() {
            return highRiskToolCount;
        }

        public void setHighRiskToolCount(Long highRiskToolCount) {
            this.highRiskToolCount = highRiskToolCount;
        }
    }

    /**
     * 审计日志摘要。
     */
    public static class AuditItem {

        /** 审计日志ID。 */
        private String id;

        /** 用户ID。 */
        private String userId;

        /** 用户名。 */
        private String username;

        /** 操作类型。 */
        private String operationType;

        /** 资源类型。 */
        private String resourceType;

        /** 请求方法。 */
        private String requestMethod;

        /** 请求路径。 */
        private String requestPath;

        /** 响应状态。 */
        private Integer responseStatus;

        /** 是否成功。 */
        private Boolean success;

        /** 失败原因。 */
        private String failureReason;

        /** 客户端IP。 */
        private String clientIp;

        /** 耗时毫秒。 */
        private Integer latencyMs;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getRequestMethod() {
            return requestMethod;
        }

        public void setRequestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
        }

        public String getRequestPath() {
            return requestPath;
        }

        public void setRequestPath(String requestPath) {
            this.requestPath = requestPath;
        }

        public Integer getResponseStatus() {
            return responseStatus;
        }

        public void setResponseStatus(Integer responseStatus) {
            this.responseStatus = responseStatus;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getClientIp() {
            return clientIp;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 风险治理事件摘要。
     */
    public static class RiskItem {

        /** 风险事件ID。 */
        private String id;

        /** 风险事件编码。 */
        private String eventCode;

        /** 风险事件类型。 */
        private String eventType;

        /** 来源类型。 */
        private String sourceType;

        /** 来源记录ID。 */
        private String sourceId;

        /** 风险级别。 */
        private String riskLevel;

        /** 风险级别展示名。 */
        private String riskLabel;

        /** 处置状态。 */
        private String status;

        /** 风险标题。 */
        private String title;

        /** 风险描述。 */
        private String description;

        /** 所属工作空间ID。 */
        private String workspaceId;

        /** 所属工作空间名称。 */
        private String workspaceName;

        /** 关联智能体ID。 */
        private String agentId;

        /** 关联工具ID。 */
        private String toolId;

        /** 关联运行ID。 */
        private String runId;

        /** 关联规则编码。 */
        private String ruleCode;

        /** 风险证据。 */
        private Map<String, Object> evidence;

        /** 建议处置动作。 */
        private String recommendedAction;

        /** 处置人ID。 */
        private String handledBy;

        /** 处置时间。 */
        private LocalDateTime handledAt;

        /** 处置备注。 */
        private String handleNote;

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

        public String getEventCode() {
            return eventCode;
        }

        public void setEventCode(String eventCode) {
            this.eventCode = eventCode;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getRiskLabel() {
            return riskLabel;
        }

        public void setRiskLabel(String riskLabel) {
            this.riskLabel = riskLabel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
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

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getToolId() {
            return toolId;
        }

        public void setToolId(String toolId) {
            this.toolId = toolId;
        }

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public Map<String, Object> getEvidence() {
            return evidence;
        }

        public void setEvidence(Map<String, Object> evidence) {
            this.evidence = evidence;
        }

        public String getRecommendedAction() {
            return recommendedAction;
        }

        public void setRecommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
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
     * 高风险确认请求摘要。
     */
    public static class ConfirmationItem {

        /** 确认请求ID。 */
        private String id;

        /** 工具ID。 */
        private String toolId;

        /** 工具名称。 */
        private String toolName;

        /** 请求用户ID。 */
        private String requesterUserId;

        /** 智能体ID。 */
        private String agentId;

        /** 运行ID。 */
        private String runId;

        /** 请求载荷。 */
        private Map<String, Object> requestPayload;

        /** 请求原因。 */
        private String reason;

        /** 确认状态。 */
        private String status;

        /** 确认人ID。 */
        private String confirmedBy;

        /** 确认时间。 */
        private LocalDateTime confirmedAt;

        /** 过期时间。 */
        private LocalDateTime expiredAt;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getToolId() {
            return toolId;
        }

        public void setToolId(String toolId) {
            this.toolId = toolId;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public String getRequesterUserId() {
            return requesterUserId;
        }

        public void setRequesterUserId(String requesterUserId) {
            this.requesterUserId = requesterUserId;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public Map<String, Object> getRequestPayload() {
            return requestPayload;
        }

        public void setRequestPayload(Map<String, Object> requestPayload) {
            this.requestPayload = requestPayload;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getConfirmedBy() {
            return confirmedBy;
        }

        public void setConfirmedBy(String confirmedBy) {
            this.confirmedBy = confirmedBy;
        }

        public LocalDateTime getConfirmedAt() {
            return confirmedAt;
        }

        public void setConfirmedAt(LocalDateTime confirmedAt) {
            this.confirmedAt = confirmedAt;
        }

        public LocalDateTime getExpiredAt() {
            return expiredAt;
        }

        public void setExpiredAt(LocalDateTime expiredAt) {
            this.expiredAt = expiredAt;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 风险处置请求。
     */
    public static class HandleRiskRequest {

        /** 处置状态。 */
        private String status;

        /** 处置备注。 */
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
}

