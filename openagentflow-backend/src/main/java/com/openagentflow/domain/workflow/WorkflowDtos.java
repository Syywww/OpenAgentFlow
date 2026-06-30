package com.openagentflow.domain.workflow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流模块 DTO 集合。
 *
 * <p>工作流前后端交互的数据结构集中放在这里，避免 P5 阶段新增大量小文件。</p>
 */
public final class WorkflowDtos {

    private WorkflowDtos() {
    }

    /**
     * 工作流列表摘要。
     */
    public static class Summary {
        /** 工作流主键 ID。 */
        private String id;
        /** 工作流编码，用于接口和低代码配置里的稳定引用。 */
        private String workflowCode;
        /** 工作流名称。 */
        private String workflowName;
        /** 工作流描述。 */
        private String description;
        /** 工作流类型，例如 agent_workflow。 */
        private String workflowType;
        /** 所属工作空间 ID。 */
        private String workspaceId;
        /** 所属工作空间名称。 */
        private String workspaceName;
        /** 工作流状态，draft/published/disabled/deleted。 */
        private String status;
        /** 工作流状态中文标签。 */
        private String statusLabel;
        /** 已发布版本号。 */
        private String publishedVersion;
        /** 可见性，private/public。 */
        private String visibility;
        /** 所有者用户 ID。 */
        private String ownerUserId;
        /** 节点数量。 */
        private Integer nodeCount;
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

        public String getWorkflowCode() {
            return workflowCode;
        }

        public void setWorkflowCode(String workflowCode) {
            this.workflowCode = workflowCode;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getWorkflowType() {
            return workflowType;
        }

        public void setWorkflowType(String workflowType) {
            this.workflowType = workflowType;
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

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public String getPublishedVersion() {
            return publishedVersion;
        }

        public void setPublishedVersion(String publishedVersion) {
            this.publishedVersion = publishedVersion;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public Integer getNodeCount() {
            return nodeCount;
        }

        public void setNodeCount(Integer nodeCount) {
            this.nodeCount = nodeCount;
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
     * 工作流详情。
     */
    public static class Detail extends Summary {
        /** Vue Flow 画布 JSON。 */
        private Object graphJson;
        /** 工作流变量 Schema。 */
        private Object variableSchema;
        /** 工作流节点列表。 */
        private List<NodeDto> nodes;
        /** 工作流连线列表。 */
        private List<EdgeDto> edges;
        /** 发布版本列表。 */
        private List<VersionSummary> versions;

        public Object getGraphJson() {
            return graphJson;
        }

        public void setGraphJson(Object graphJson) {
            this.graphJson = graphJson;
        }

        public Object getVariableSchema() {
            return variableSchema;
        }

        public void setVariableSchema(Object variableSchema) {
            this.variableSchema = variableSchema;
        }

        public List<NodeDto> getNodes() {
            return nodes;
        }

        public void setNodes(List<NodeDto> nodes) {
            this.nodes = nodes;
        }

        public List<EdgeDto> getEdges() {
            return edges;
        }

        public void setEdges(List<EdgeDto> edges) {
            this.edges = edges;
        }

        public List<VersionSummary> getVersions() {
            return versions;
        }

        public void setVersions(List<VersionSummary> versions) {
            this.versions = versions;
        }
    }

    /**
     * 工作流保存请求。
     */
    public static class Request {
        /** 工作流编码。 */
        private String workflowCode;
        /** 工作流名称。 */
        private String workflowName;
        /** 工作流描述。 */
        private String description;
        /** 工作流类型。 */
        private String workflowType;
        /** 所属工作空间 ID。 */
        private String workspaceId;
        /** 画布 JSON。 */
        private Object graphJson;
        /** 变量 Schema。 */
        private Object variableSchema;
        /** 工作流状态。 */
        private String status;
        /** 可见性。 */
        private String visibility;
        /** 节点列表。 */
        private List<NodeDto> nodes;
        /** 连线列表。 */
        private List<EdgeDto> edges;

        public String getWorkflowCode() {
            return workflowCode;
        }

        public void setWorkflowCode(String workflowCode) {
            this.workflowCode = workflowCode;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getWorkflowType() {
            return workflowType;
        }

        public void setWorkflowType(String workflowType) {
            this.workflowType = workflowType;
        }

        public String getWorkspaceId() {
            return workspaceId;
        }

        public void setWorkspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
        }

        public Object getGraphJson() {
            return graphJson;
        }

        public void setGraphJson(Object graphJson) {
            this.graphJson = graphJson;
        }

        public Object getVariableSchema() {
            return variableSchema;
        }

        public void setVariableSchema(Object variableSchema) {
            this.variableSchema = variableSchema;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getVisibility() {
            return visibility;
        }

        public void setVisibility(String visibility) {
            this.visibility = visibility;
        }

        public List<NodeDto> getNodes() {
            return nodes;
        }

        public void setNodes(List<NodeDto> nodes) {
            this.nodes = nodes;
        }

        public List<EdgeDto> getEdges() {
            return edges;
        }

        public void setEdges(List<EdgeDto> edges) {
            this.edges = edges;
        }
    }

    /**
     * 工作流节点 DTO。
     */
    public static class NodeDto {
        /** 节点记录 ID。 */
        private String id;
        /** 节点业务 Key，和 Vue Flow 节点 ID 保持一致。 */
        private String nodeKey;
        /** 节点名称。 */
        private String nodeName;
        /** 节点类型，START/LLM/RAG/TOOL/CONDITION/END。 */
        private String nodeType;
        /** 画布 X 坐标。 */
        private Double positionX;
        /** 画布 Y 坐标。 */
        private Double positionY;
        /** 节点配置 JSON。 */
        private Object configJson;
        /** 输入 Schema。 */
        private Object inputSchema;
        /** 输出 Schema。 */
        private Object outputSchema;
        /** 重试策略。 */
        private Object retryPolicy;
        /** 是否启用节点。 */
        private Boolean enabled;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNodeKey() {
            return nodeKey;
        }

        public void setNodeKey(String nodeKey) {
            this.nodeKey = nodeKey;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public Double getPositionX() {
            return positionX;
        }

        public void setPositionX(Double positionX) {
            this.positionX = positionX;
        }

        public Double getPositionY() {
            return positionY;
        }

        public void setPositionY(Double positionY) {
            this.positionY = positionY;
        }

        public Object getConfigJson() {
            return configJson;
        }

        public void setConfigJson(Object configJson) {
            this.configJson = configJson;
        }

        public Object getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(Object inputSchema) {
            this.inputSchema = inputSchema;
        }

        public Object getOutputSchema() {
            return outputSchema;
        }

        public void setOutputSchema(Object outputSchema) {
            this.outputSchema = outputSchema;
        }

        public Object getRetryPolicy() {
            return retryPolicy;
        }

        public void setRetryPolicy(Object retryPolicy) {
            this.retryPolicy = retryPolicy;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 工作流连线 DTO。
     */
    public static class EdgeDto {
        /** 连线记录 ID。 */
        private String id;
        /** 连线业务 Key。 */
        private String edgeKey;
        /** 来源节点 Key。 */
        private String sourceNodeKey;
        /** 目标节点 Key。 */
        private String targetNodeKey;
        /** 条件表达式，供 CONDITION 节点选择分支。 */
        private String conditionExpr;
        /** 连线显示标签。 */
        private String label;
        /** 连线元数据 JSON。 */
        private Object metadata;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getEdgeKey() {
            return edgeKey;
        }

        public void setEdgeKey(String edgeKey) {
            this.edgeKey = edgeKey;
        }

        public String getSourceNodeKey() {
            return sourceNodeKey;
        }

        public void setSourceNodeKey(String sourceNodeKey) {
            this.sourceNodeKey = sourceNodeKey;
        }

        public String getTargetNodeKey() {
            return targetNodeKey;
        }

        public void setTargetNodeKey(String targetNodeKey) {
            this.targetNodeKey = targetNodeKey;
        }

        public String getConditionExpr() {
            return conditionExpr;
        }

        public void setConditionExpr(String conditionExpr) {
            this.conditionExpr = conditionExpr;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 工作流发布请求。
     */
    public static class PublishRequest {
        /** 版本号，为空时后端自动生成。 */
        private String versionNo;
        /** 发布环境，例如 dev/prod。 */
        private String publishEnv;
        /** 发布说明。 */
        private String publishNote;

        public String getVersionNo() {
            return versionNo;
        }

        public void setVersionNo(String versionNo) {
            this.versionNo = versionNo;
        }

        public String getPublishEnv() {
            return publishEnv;
        }

        public void setPublishEnv(String publishEnv) {
            this.publishEnv = publishEnv;
        }

        public String getPublishNote() {
            return publishNote;
        }

        public void setPublishNote(String publishNote) {
            this.publishNote = publishNote;
        }
    }

    /**
     * 工作流版本摘要。
     */
    public static class VersionSummary {
        /** 版本记录 ID。 */
        private String id;
        /** 版本号。 */
        private String versionNo;
        /** 发布环境。 */
        private String publishEnv;
        /** 发布说明。 */
        private String publishNote;
        /** 版本状态。 */
        private String status;
        /** 创建时间。 */
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVersionNo() {
            return versionNo;
        }

        public void setVersionNo(String versionNo) {
            this.versionNo = versionNo;
        }

        public String getPublishEnv() {
            return publishEnv;
        }

        public void setPublishEnv(String publishEnv) {
            this.publishEnv = publishEnv;
        }

        public String getPublishNote() {
            return publishNote;
        }

        public void setPublishNote(String publishNote) {
            this.publishNote = publishNote;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 工作流运行请求。
     */
    public static class RunRequest {
        /** 绑定运行的 Agent ID。 */
        private String agentId;
        /** 用户输入文本。 */
        private String input;
        /** 额外上下文变量。 */
        private Map<String, Object> variables;
        /** 是否开启调试模式，开启后保留更多节点中间态。 */
        private Boolean debugMode;
        /** 指定起始节点 Key，用于单步调试或局部重跑。 */
        private String startNodeKey;
        /** 最大执行步数，避免复杂工作流误循环。 */
        private Integer maxSteps;
        /** 是否空跑，空跑时不会真实调用模型、知识库和工具。 */
        private Boolean dryRun;
        /** 幂等键，用于防止重复提交。 */
        private String idempotencyKey;
        /** 恢复或重跑来源运行ID。 */
        private String parentRunId;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public void setVariables(Map<String, Object> variables) {
            this.variables = variables;
        }

        public Boolean getDebugMode() {
            return debugMode;
        }

        public void setDebugMode(Boolean debugMode) {
            this.debugMode = debugMode;
        }

        public String getStartNodeKey() {
            return startNodeKey;
        }

        public void setStartNodeKey(String startNodeKey) {
            this.startNodeKey = startNodeKey;
        }

        public Integer getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(Integer maxSteps) {
            this.maxSteps = maxSteps;
        }

        public Boolean getDryRun() {
            return dryRun;
        }

        public void setDryRun(Boolean dryRun) {
            this.dryRun = dryRun;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public String getParentRunId() {
            return parentRunId;
        }

        public void setParentRunId(String parentRunId) {
            this.parentRunId = parentRunId;
        }
    }

    /**
     * 工作流运行结果。
     */
    public static class RunResult {
        /** 工作流运行记录 ID。 */
        private String workflowRunId;
        /** Runtime Trace 运行 ID。 */
        private String runtimeRunId;
        /** 运行状态。 */
        private String status;
        /** 最终输出文本。 */
        private String outputText;
        /** 运行上下文快照。 */
        private Map<String, Object> context;
        /** 节点执行结果。 */
        private List<StepResult> steps;
        /** 总 Token。 */
        private Integer totalTokens;
        /** 总耗时毫秒。 */
        private Integer latencyMs;
        /** 错误信息。 */
        private String errorMessage;
        /** 工作流ID。 */
        private String workflowId;
        /** 工作流版本ID。 */
        private String workflowVersionId;
        /** Agent ID。 */
        private String agentId;
        /** 触发类型。 */
        private String triggerType;
        /** 幂等键。 */
        private String idempotencyKey;
        /** 父运行ID。 */
        private String parentRunId;
        /** 恢复起始节点Key。 */
        private String resumeFromNodeKey;
        /** 最近节点Key。 */
        private String lastNodeKey;
        /** 下一节点Key。 */
        private String nextNodeKey;
        /** 是否可恢复。 */
        private Boolean recoverable;
        /** 重跑次数。 */
        private Integer retryCount;
        /** 开始时间。 */
        private LocalDateTime startedAt;
        /** 完成时间。 */
        private LocalDateTime finishedAt;

        public String getWorkflowRunId() {
            return workflowRunId;
        }

        public void setWorkflowRunId(String workflowRunId) {
            this.workflowRunId = workflowRunId;
        }

        public String getRuntimeRunId() {
            return runtimeRunId;
        }

        public void setRuntimeRunId(String runtimeRunId) {
            this.runtimeRunId = runtimeRunId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOutputText() {
            return outputText;
        }

        public void setOutputText(String outputText) {
            this.outputText = outputText;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }

        public List<StepResult> getSteps() {
            return steps;
        }

        public void setSteps(List<StepResult> steps) {
            this.steps = steps;
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

        public String getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(String workflowId) {
            this.workflowId = workflowId;
        }

        public String getWorkflowVersionId() {
            return workflowVersionId;
        }

        public void setWorkflowVersionId(String workflowVersionId) {
            this.workflowVersionId = workflowVersionId;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getTriggerType() {
            return triggerType;
        }

        public void setTriggerType(String triggerType) {
            this.triggerType = triggerType;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public String getParentRunId() {
            return parentRunId;
        }

        public void setParentRunId(String parentRunId) {
            this.parentRunId = parentRunId;
        }

        public String getResumeFromNodeKey() {
            return resumeFromNodeKey;
        }

        public void setResumeFromNodeKey(String resumeFromNodeKey) {
            this.resumeFromNodeKey = resumeFromNodeKey;
        }

        public String getLastNodeKey() {
            return lastNodeKey;
        }

        public void setLastNodeKey(String lastNodeKey) {
            this.lastNodeKey = lastNodeKey;
        }

        public String getNextNodeKey() {
            return nextNodeKey;
        }

        public void setNextNodeKey(String nextNodeKey) {
            this.nextNodeKey = nextNodeKey;
        }

        public Boolean getRecoverable() {
            return recoverable;
        }

        public void setRecoverable(Boolean recoverable) {
            this.recoverable = recoverable;
        }

        public Integer getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(Integer retryCount) {
            this.retryCount = retryCount;
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
     * 节点运行结果。
     */
    public static class StepResult {
        /** 节点 Key。 */
        private String nodeKey;
        /** 节点名称。 */
        private String nodeName;
        /** 节点类型。 */
        private String nodeType;
        /** 节点状态。 */
        private String status;
        /** 输出载荷。 */
        private Object output;
        /** 节点 Token 数。 */
        private Integer tokenCount;
        /** 节点耗时毫秒。 */
        private Integer latencyMs;
        /** 错误信息。 */
        private String errorMessage;
        /** 节点尝试次数。 */
        private Integer attemptNo;
        /** 下一节点Key。 */
        private String nextNodeKey;
        /** 是否可恢复。 */
        private Boolean recoverable;
        /** 开始时间。 */
        private LocalDateTime startedAt;
        /** 完成时间。 */
        private LocalDateTime finishedAt;

        public String getNodeKey() {
            return nodeKey;
        }

        public void setNodeKey(String nodeKey) {
            this.nodeKey = nodeKey;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Object getOutput() {
            return output;
        }

        public void setOutput(Object output) {
            this.output = output;
        }

        public Integer getTokenCount() {
            return tokenCount;
        }

        public void setTokenCount(Integer tokenCount) {
            this.tokenCount = tokenCount;
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

        public Integer getAttemptNo() {
            return attemptNo;
        }

        public void setAttemptNo(Integer attemptNo) {
            this.attemptNo = attemptNo;
        }

        public String getNextNodeKey() {
            return nextNodeKey;
        }

        public void setNextNodeKey(String nextNodeKey) {
            this.nextNodeKey = nextNodeKey;
        }

        public Boolean getRecoverable() {
            return recoverable;
        }

        public void setRecoverable(Boolean recoverable) {
            this.recoverable = recoverable;
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
     * Agent 绑定工作流请求。
     */
    public static class BindingRequest {
        /** 需要绑定的工作流 ID 列表。 */
        private List<String> workflowIds;
        /** 触发模式，默认 agent_run。 */
        private String triggerMode;

        public List<String> getWorkflowIds() {
            return workflowIds;
        }

        public void setWorkflowIds(List<String> workflowIds) {
            this.workflowIds = workflowIds;
        }

        public String getTriggerMode() {
            return triggerMode;
        }

        public void setTriggerMode(String triggerMode) {
            this.triggerMode = triggerMode;
        }
    }

    /**
     * Agent 绑定工作流摘要。
     */
    public static class BindingSummary {
        /** Agent ID。 */
        private String agentId;
        /** 工作流 ID。 */
        private String workflowId;
        /** 工作流名称。 */
        private String workflowName;
        /** 工作流编码。 */
        private String workflowCode;
        /** 触发模式。 */
        private String triggerMode;
        /** 是否启用。 */
        private Boolean enabled;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
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

        public String getWorkflowCode() {
            return workflowCode;
        }

        public void setWorkflowCode(String workflowCode) {
            this.workflowCode = workflowCode;
        }

        public String getTriggerMode() {
            return triggerMode;
        }

        public void setTriggerMode(String triggerMode) {
            this.triggerMode = triggerMode;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
