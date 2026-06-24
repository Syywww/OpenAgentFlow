package com.openagentflow.domain.agent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 多 Agent 协作团队接口数据对象集合。
 */
public class AgentTeamDtos {

    /**
     * 协作团队保存请求。
     */
    public static class TeamRequest {
        /** 团队编码，不传时后端根据名称自动生成。 */
        private String teamCode;
        /** 团队名称。 */
        private String teamName;
        /** 团队说明。 */
        private String description;
        /** 协作模式：sequential、parallel、router、supervisor、reviewer。 */
        private String collaborationMode;
        /** 主控 Agent ID，用于 supervisor/router 模式的规划和汇总。 */
        private String coordinatorAgentId;
        /** 团队状态：draft、published、disabled、deleted。 */
        private String status;
        /** 团队成员列表。 */
        private List<MemberRequest> members = new ArrayList<>();

        public String getTeamCode() {
            return teamCode;
        }

        public void setTeamCode(String teamCode) {
            this.teamCode = teamCode;
        }

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCollaborationMode() {
            return collaborationMode;
        }

        public void setCollaborationMode(String collaborationMode) {
            this.collaborationMode = collaborationMode;
        }

        public String getCoordinatorAgentId() {
            return coordinatorAgentId;
        }

        public void setCoordinatorAgentId(String coordinatorAgentId) {
            this.coordinatorAgentId = coordinatorAgentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<MemberRequest> getMembers() {
            return members;
        }

        public void setMembers(List<MemberRequest> members) {
            this.members = members;
        }
    }

    /**
     * 协作团队成员保存请求。
     */
    public static class MemberRequest {
        /** 成员 Agent ID。 */
        private String agentId;
        /** 成员职责，例如 researcher、writer、reviewer、coordinator。 */
        private String memberRole;
        /** 交接策略 JSON 字符串，用于描述输入输出约束。 */
        private String handoffPolicy;
        /** 成员执行顺序，数值越小越靠前。 */
        private Integer sortOrder;
        /** 是否启用该成员。 */
        private Boolean enabled;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getMemberRole() {
            return memberRole;
        }

        public void setMemberRole(String memberRole) {
            this.memberRole = memberRole;
        }

        public String getHandoffPolicy() {
            return handoffPolicy;
        }

        public void setHandoffPolicy(String handoffPolicy) {
            this.handoffPolicy = handoffPolicy;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 协作团队摘要。
     */
    public static class TeamSummary {
        /** 团队 ID。 */
        private String id;
        /** 团队编码。 */
        private String teamCode;
        /** 团队名称。 */
        private String teamName;
        /** 团队说明。 */
        private String description;
        /** 协作模式。 */
        private String collaborationMode;
        /** 协作模式中文标签。 */
        private String collaborationModeLabel;
        /** 主控 Agent ID。 */
        private String coordinatorAgentId;
        /** 主控 Agent 名称。 */
        private String coordinatorAgentName;
        /** 团队状态。 */
        private String status;
        /** 团队状态中文标签。 */
        private String statusLabel;
        /** 成员数量。 */
        private Integer memberCount;
        /** 近 7 天运行次数。 */
        private Integer runs7d;
        /** 近 7 天成功次数。 */
        private Integer success7d;
        /** 团队所有者用户 ID。 */
        private String ownerUserId;
        /** 当前用户是否可管理。 */
        private Boolean canManage;
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

        public String getTeamCode() {
            return teamCode;
        }

        public void setTeamCode(String teamCode) {
            this.teamCode = teamCode;
        }

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCollaborationMode() {
            return collaborationMode;
        }

        public void setCollaborationMode(String collaborationMode) {
            this.collaborationMode = collaborationMode;
        }

        public String getCollaborationModeLabel() {
            return collaborationModeLabel;
        }

        public void setCollaborationModeLabel(String collaborationModeLabel) {
            this.collaborationModeLabel = collaborationModeLabel;
        }

        public String getCoordinatorAgentId() {
            return coordinatorAgentId;
        }

        public void setCoordinatorAgentId(String coordinatorAgentId) {
            this.coordinatorAgentId = coordinatorAgentId;
        }

        public String getCoordinatorAgentName() {
            return coordinatorAgentName;
        }

        public void setCoordinatorAgentName(String coordinatorAgentName) {
            this.coordinatorAgentName = coordinatorAgentName;
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

        public Integer getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(Integer memberCount) {
            this.memberCount = memberCount;
        }

        public Integer getRuns7d() {
            return runs7d;
        }

        public void setRuns7d(Integer runs7d) {
            this.runs7d = runs7d;
        }

        public Integer getSuccess7d() {
            return success7d;
        }

        public void setSuccess7d(Integer success7d) {
            this.success7d = success7d;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public Boolean getCanManage() {
            return canManage;
        }

        public void setCanManage(Boolean canManage) {
            this.canManage = canManage;
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
     * 协作团队详情。
     */
    public static class TeamDetail extends TeamSummary {
        /** 团队成员明细。 */
        private List<MemberSummary> members = new ArrayList<>();
        /** 最近协作运行记录。 */
        private List<RunHistoryItem> recentRuns = new ArrayList<>();

        public List<MemberSummary> getMembers() {
            return members;
        }

        public void setMembers(List<MemberSummary> members) {
            this.members = members;
        }

        public List<RunHistoryItem> getRecentRuns() {
            return recentRuns;
        }

        public void setRecentRuns(List<RunHistoryItem> recentRuns) {
            this.recentRuns = recentRuns;
        }
    }

    /**
     * 协作团队成员摘要。
     */
    public static class MemberSummary {
        /** 团队 ID。 */
        private String teamId;
        /** 成员 Agent ID。 */
        private String agentId;
        /** 成员 Agent 名称。 */
        private String agentName;
        /** 成员 Agent 类型。 */
        private String agentType;
        /** 成员绑定模型名称。 */
        private String modelName;
        /** 成员职责。 */
        private String memberRole;
        /** 交接策略 JSON 字符串。 */
        private String handoffPolicy;
        /** 执行顺序。 */
        private Integer sortOrder;
        /** 是否启用。 */
        private Boolean enabled;
        /** 当前用户是否可运行该成员 Agent。 */
        private Boolean canRun;

        public String getTeamId() {
            return teamId;
        }

        public void setTeamId(String teamId) {
            this.teamId = teamId;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getAgentType() {
            return agentType;
        }

        public void setAgentType(String agentType) {
            this.agentType = agentType;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getMemberRole() {
            return memberRole;
        }

        public void setMemberRole(String memberRole) {
            this.memberRole = memberRole;
        }

        public String getHandoffPolicy() {
            return handoffPolicy;
        }

        public void setHandoffPolicy(String handoffPolicy) {
            this.handoffPolicy = handoffPolicy;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getCanRun() {
            return canRun;
        }

        public void setCanRun(Boolean canRun) {
            this.canRun = canRun;
        }
    }

    /**
     * 协作运行请求。
     */
    public static class RunRequest {
        /** 本次协作目标。 */
        private String objective;
        /** 共享上下文变量。 */
        private Map<String, Object> sharedContext;
        /** 是否在成员失败后继续执行后续成员。 */
        private Boolean continueOnError;

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public Map<String, Object> getSharedContext() {
            return sharedContext;
        }

        public void setSharedContext(Map<String, Object> sharedContext) {
            this.sharedContext = sharedContext;
        }

        public Boolean getContinueOnError() {
            return continueOnError;
        }

        public void setContinueOnError(Boolean continueOnError) {
            this.continueOnError = continueOnError;
        }
    }

    /**
     * 协作运行历史摘要。
     */
    public static class RunHistoryItem {
        /** 协作运行记录 ID。 */
        private String collaborationRunId;
        /** 顶层 Trace 运行 ID。 */
        private String runtimeRunId;
        /** 协作目标。 */
        private String objective;
        /** 最终结果摘要。 */
        private String finalResult;
        /** 运行状态。 */
        private String status;
        /** Token 消耗。 */
        private Integer totalTokens;
        /** 耗时毫秒。 */
        private Integer latencyMs;
        /** 开始时间。 */
        private LocalDateTime startedAt;
        /** 完成时间。 */
        private LocalDateTime finishedAt;

        public String getCollaborationRunId() {
            return collaborationRunId;
        }

        public void setCollaborationRunId(String collaborationRunId) {
            this.collaborationRunId = collaborationRunId;
        }

        public String getRuntimeRunId() {
            return runtimeRunId;
        }

        public void setRuntimeRunId(String runtimeRunId) {
            this.runtimeRunId = runtimeRunId;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public String getFinalResult() {
            return finalResult;
        }

        public void setFinalResult(String finalResult) {
            this.finalResult = finalResult;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
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
     * 协作运行响应。
     */
    public static class RunResult {
        /** 协作运行记录 ID。 */
        private String collaborationRunId;
        /** 顶层 Trace 运行 ID。 */
        private String runtimeRunId;
        /** 团队 ID。 */
        private String teamId;
        /** 团队名称。 */
        private String teamName;
        /** 协作目标。 */
        private String objective;
        /** 最终结果。 */
        private String finalResult;
        /** 运行状态。 */
        private String status;
        /** 总 Token 消耗。 */
        private Integer totalTokens;
        /** 总耗时毫秒。 */
        private Integer latencyMs;
        /** 错误信息。 */
        private String errorMessage;
        /** 成员执行步骤。 */
        private List<StepResult> steps = new ArrayList<>();

        public String getCollaborationRunId() {
            return collaborationRunId;
        }

        public void setCollaborationRunId(String collaborationRunId) {
            this.collaborationRunId = collaborationRunId;
        }

        public String getRuntimeRunId() {
            return runtimeRunId;
        }

        public void setRuntimeRunId(String runtimeRunId) {
            this.runtimeRunId = runtimeRunId;
        }

        public String getTeamId() {
            return teamId;
        }

        public void setTeamId(String teamId) {
            this.teamId = teamId;
        }

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public String getFinalResult() {
            return finalResult;
        }

        public void setFinalResult(String finalResult) {
            this.finalResult = finalResult;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
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

        public List<StepResult> getSteps() {
            return steps;
        }

        public void setSteps(List<StepResult> steps) {
            this.steps = steps;
        }
    }

    /**
     * 协作运行步骤响应。
     */
    public static class StepResult {
        /** 顶层 Trace Step ID。 */
        private String traceStepId;
        /** 成员 Agent ID。 */
        private String agentId;
        /** 成员 Agent 名称。 */
        private String agentName;
        /** 成员职责。 */
        private String memberRole;
        /** 步骤名称。 */
        private String stepName;
        /** 步骤输入。 */
        private String input;
        /** 步骤输出。 */
        private String output;
        /** 成员 Agent 自身运行 ID。 */
        private String childRunId;
        /** 步骤状态。 */
        private String status;
        /** Token 消耗。 */
        private Integer totalTokens;
        /** 耗时毫秒。 */
        private Integer latencyMs;
        /** 错误信息。 */
        private String errorMessage;

        public String getTraceStepId() {
            return traceStepId;
        }

        public void setTraceStepId(String traceStepId) {
            this.traceStepId = traceStepId;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getMemberRole() {
            return memberRole;
        }

        public void setMemberRole(String memberRole) {
            this.memberRole = memberRole;
        }

        public String getStepName() {
            return stepName;
        }

        public void setStepName(String stepName) {
            this.stepName = stepName;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public String getChildRunId() {
            return childRunId;
        }

        public void setChildRunId(String childRunId) {
            this.childRunId = childRunId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
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
    }
}
