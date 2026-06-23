package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ToolCallRequest;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import com.openagentflow.domain.tool.AgentToolBindingRequest;
import com.openagentflow.domain.tool.AgentToolBindingSummary;
import com.openagentflow.domain.tool.ToolDefinitionRequest;
import com.openagentflow.domain.tool.ToolDefinitionSummary;
import com.openagentflow.domain.tool.ToolExecutionResult;
import com.openagentflow.domain.tool.ToolTestRequest;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentToolBindingEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.entity.RuntimeTraceStepEntity;
import com.openagentflow.entity.ToolConfirmRequestEntity;
import com.openagentflow.entity.ToolDefinitionEntity;
import com.openagentflow.entity.ToolInvocationLogEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.AgentToolBindingMapper;
import com.openagentflow.mapper.RuntimeTraceStepMapper;
import com.openagentflow.mapper.ToolConfirmRequestMapper;
import com.openagentflow.mapper.ToolDefinitionMapper;
import com.openagentflow.mapper.ToolInvocationLogMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具中心应用服务。
 */
@Service
public class ToolService {

    /** 路径参数占位符正则，例如 {orderId}。 */
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_\\-]+)}");

    /** HTTP 客户端。 */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 工具定义 Mapper。 */
    private final ToolDefinitionMapper toolDefinitionMapper;

    /** Agent 工具绑定 Mapper。 */
    private final AgentToolBindingMapper agentToolBindingMapper;

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** 工具调用日志 Mapper。 */
    private final ToolInvocationLogMapper toolInvocationLogMapper;

    /** 高风险确认请求 Mapper。 */
    private final ToolConfirmRequestMapper toolConfirmRequestMapper;

    /** Trace 步骤 Mapper。 */
    private final RuntimeTraceStepMapper runtimeTraceStepMapper;

    /** JDBC 工具，用于统计和数据库查询工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** Agent 权限服务。 */
    private final AgentAccessService agentAccessService;

    /** MCP 客户端服务，用于执行同步到工具中心的 MCP 工具。 */
    private final McpClientService mcpClientService;

    /** 工作空间治理服务。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    public ToolService(ToolDefinitionMapper toolDefinitionMapper,
                       AgentToolBindingMapper agentToolBindingMapper,
                       AgentMapper agentMapper,
                       ToolInvocationLogMapper toolInvocationLogMapper,
                       ToolConfirmRequestMapper toolConfirmRequestMapper,
                       RuntimeTraceStepMapper runtimeTraceStepMapper,
                       JdbcTemplate jdbcTemplate,
                       ObjectMapper objectMapper,
                       AgentAccessService agentAccessService,
                       McpClientService mcpClientService,
                       WorkspaceGovernanceService workspaceGovernanceService) {
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.agentToolBindingMapper = agentToolBindingMapper;
        this.agentMapper = agentMapper;
        this.toolInvocationLogMapper = toolInvocationLogMapper;
        this.toolConfirmRequestMapper = toolConfirmRequestMapper;
        this.runtimeTraceStepMapper = runtimeTraceStepMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.agentAccessService = agentAccessService;
        this.mcpClientService = mcpClientService;
        this.workspaceGovernanceService = workspaceGovernanceService;
    }

    /**
     * 查询工具列表。
     *
     * @return 工具摘要列表
     */
    public List<ToolDefinitionSummary> listTools() {
        return toolDefinitionMapper.selectList(new LambdaQueryWrapper<ToolDefinitionEntity>()
                        .isNull(ToolDefinitionEntity::getDeletedAt)
                        .orderByDesc(ToolDefinitionEntity::getUpdatedAt)
                        .last("limit 200"))
                .stream()
                .filter(this::canView)
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询工具详情。
     *
     * @param id 工具 ID
     * @return 工具摘要
     */
    public ToolDefinitionSummary getTool(String id) {
        ToolDefinitionEntity entity = requireTool(id);
        if (!canView(entity)) {
            throw new BusinessException("TOOL_FORBIDDEN", "没有访问该工具的权限");
        }
        return toSummary(entity);
    }

    /**
     * 创建工具。
     *
     * @param request 保存请求
     * @return 创建后的工具摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public ToolDefinitionSummary createTool(ToolDefinitionRequest request) {
        String userId = currentUserIdOrThrow();
        ToolDefinitionEntity entity = new ToolDefinitionEntity();
        entity.setId(newId());
        fillTool(entity, request, true);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "tool", entity.getId(), userId));
        entity.setSourceType("manual");
        entity.setVersion(0L);
        toolDefinitionMapper.insert(entity);
        return toSummary(entity);
    }

    /**
     * 更新工具。
     *
     * @param id 工具 ID
     * @param request 保存请求
     * @return 更新后的工具摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public ToolDefinitionSummary updateTool(String id, ToolDefinitionRequest request) {
        ToolDefinitionEntity entity = requireTool(id);
        assertCanManage(entity);
        fillTool(entity, request, false);
        entity.setVersion(entity.getVersion() == null ? 1L : entity.getVersion() + 1);
        toolDefinitionMapper.updateById(entity);
        return getTool(id);
    }

    /**
     * 软删除工具。
     *
     * @param id 工具 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTool(String id) {
        ToolDefinitionEntity entity = requireTool(id);
        assertCanManage(entity);
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        toolDefinitionMapper.updateById(entity);
    }

    /**
     * 测试工具连通性和执行结果。
     *
     * @param id 工具 ID
     * @param request 测试请求
     * @return 执行结果
     */
    public ToolExecutionResult testTool(String id, ToolTestRequest request) {
        ToolDefinitionEntity entity = requireTool(id);
        assertCanManage(entity);
        Map<String, Object> inputParams = request == null || request.getInputParams() == null ? Map.of() : request.getInputParams();
        return executeAndLog(entity, inputParams, null, null, null, currentUserId(), true);
    }

    /**
     * 查询 Agent 已绑定工具。
     *
     * @param agentId Agent ID
     * @return 工具绑定列表
     */
    public List<AgentToolBindingSummary> listAgentToolBindings(String agentId) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanView(agent);
        return agentToolBindingMapper.selectList(new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getAgentId, agentId)
                        .eq(AgentToolBindingEntity::getEnabled, true))
                .stream()
                .map(this::toBindingSummary)
                .toList();
    }

    /**
     * 保存 Agent 工具绑定。
     *
     * @param agentId Agent ID
     * @param request 绑定请求
     * @return 保存后的绑定列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AgentToolBindingSummary> saveAgentToolBindings(String agentId, AgentToolBindingRequest request) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanManage(agent);
        agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBindingEntity>()
                .eq(AgentToolBindingEntity::getAgentId, agentId));
        Set<String> toolIds = new LinkedHashSet<>(request == null || request.getToolIds() == null ? List.of() : request.getToolIds());
        for (String toolId : toolIds) {
            ToolDefinitionEntity tool = requireTool(toolId);
            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setAgentId(agentId);
            binding.setToolId(toolId);
            binding.setToolConfig("{}");
            binding.setRequireConfirm(Boolean.TRUE.equals(tool.getRequireConfirm()));
            binding.setEnabled(true);
            agentToolBindingMapper.insert(binding);
        }
        return listAgentToolBindings(agentId);
    }

    /**
     * 查询 Agent 可提供给模型的工具定义。
     *
     * @param agent Agent 实体
     * @return 模型工具定义列表
     */
    public List<ToolDefinitionForModel> listModelToolsForAgent(AgentEntity agent) {
        if (agent == null) {
            return List.of();
        }
        return listEnabledToolsForAgent(agent.getId()).stream()
                .map(this::toModelTool)
                .toList();
    }

    /**
     * 执行模型请求的工具调用，并记录 Trace 与工具调用日志。
     *
     * @param agent 当前 Agent
     * @param run 运行记录
     * @param parentStepId 父步骤 ID
     * @param call 模型请求的工具调用
     * @return 工具执行结果
     */
    public ToolExecutionResult executeToolCallForAgent(AgentEntity agent,
                                                       RuntimeRunEntity run,
                                                       String parentStepId,
                                                       ToolCallRequest call) {
        ToolDefinitionEntity tool = findBoundToolByCode(agent.getId(), call.getName());
        Map<String, Object> inputParams = parseMap(call.getArgumentsJson());
        RuntimeTraceStepEntity step = createToolTraceStep(run, parentStepId, tool, call, inputParams);
        ToolExecutionResult result = executeAndLog(tool, inputParams, agent.getId(), run.getId(), step.getId(), run.getUserId(), false);
        finishToolTraceStep(step, result);
        return result;
    }

    /**
     * 执行工具并写入调用日志。
     *
     * @param tool 工具定义
     * @param inputParams 输入参数
     * @param agentId Agent ID，可为空
     * @param runId 运行 ID，可为空
     * @param stepId Trace 步骤 ID，可为空
     * @param callerUserId 调用用户 ID，可为空
     * @param testMode 是否为工具测试
     * @return 执行结果
     */
    private ToolExecutionResult executeAndLog(ToolDefinitionEntity tool,
                                              Map<String, Object> inputParams,
                                              String agentId,
                                              String runId,
                                              String stepId,
                                              String callerUserId,
                                              boolean testMode) {
        Instant startedAt = Instant.now();
        ToolExecutionResult result;
        if (requiresManualConfirmation(tool) && !testMode) {
            result = createPendingConfirmation(tool, inputParams, agentId, runId, callerUserId);
            result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        } else {
            result = executeToolInternal(tool, inputParams);
        }
        saveInvocationLog(tool, inputParams, result, agentId, runId, stepId, callerUserId);
        return result;
    }

    /**
     * 执行具体工具类型。
     *
     * @param tool 工具定义
     * @param inputParams 输入参数
     * @return 执行结果
     */
    private ToolExecutionResult executeToolInternal(ToolDefinitionEntity tool, Map<String, Object> inputParams) {
        String type = safeText(tool.getToolType()).toUpperCase(Locale.ROOT);
        if ("DB_QUERY".equals(type)) {
            return executeDbQuery(tool, inputParams);
        }
        if ("MCP".equals(type)) {
            return mcpClientService.callTool(tool, inputParams);
        }
        return executeHttpTool(tool, inputParams);
    }

    /**
     * 执行 REST API 或 Webhook 工具。
     *
     * @param tool 工具定义
     * @param inputParams 输入参数
     * @return 执行结果
     */
    private ToolExecutionResult executeHttpTool(ToolDefinitionEntity tool, Map<String, Object> inputParams) {
        Instant startedAt = Instant.now();
        ToolExecutionResult result = new ToolExecutionResult();
        try {
            String method = StringUtils.hasText(tool.getRequestMethod()) ? tool.getRequestMethod().toUpperCase(Locale.ROOT) : "POST";
            String url = buildUrl(tool.getEndpointUrl(), inputParams, "GET".equals(method));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(tool.getTimeoutMs() == null ? 30000 : tool.getTimeoutMs()));
            appendHeaders(builder, tool);
            if ("GET".equals(method) || "DELETE".equals(method)) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(toJson(inputParams), StandardCharsets.UTF_8));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            result.setStatusCode(response.statusCode());
            result.setResponseBody(response.body());
            result.setSuccess(response.statusCode() >= 200 && response.statusCode() < 300);
        } catch (Exception exception) {
            result.setSuccess(false);
            result.setStatusCode(0);
            result.setErrorMessage(exception.getMessage());
        }
        result.setConfirmationRequired(false);
        result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        return result;
    }

    /**
     * 执行数据库查询工具。
     *
     * @param tool 工具定义
     * @param inputParams 输入参数
     * @return 查询结果
     */
    private ToolExecutionResult executeDbQuery(ToolDefinitionEntity tool, Map<String, Object> inputParams) {
        Instant startedAt = Instant.now();
        ToolExecutionResult result = new ToolExecutionResult();
        try {
            String sql = renderSql(tool.getEndpointUrl(), inputParams);
            if (!sql.trim().toLowerCase(Locale.ROOT).startsWith("select") || sql.contains(";")) {
                throw new BusinessException("TOOL_SQL_FORBIDDEN", "数据库查询工具只允许单条 SELECT 语句");
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            result.setSuccess(true);
            result.setStatusCode(200);
            result.setResponseBody(toJson(Map.of("rows", rows, "rowCount", rows.size())));
        } catch (Exception exception) {
            result.setSuccess(false);
            result.setStatusCode(0);
            result.setErrorMessage(exception.getMessage());
        }
        result.setConfirmationRequired(false);
        result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        return result;
    }

    /**
     * 创建高风险工具待确认结果。
     *
     * @param tool 工具定义
     * @param inputParams 输入参数
     * @param agentId Agent ID
     * @param runId 运行 ID
     * @param callerUserId 调用用户 ID
     * @return 待确认结果
     */
    private ToolExecutionResult createPendingConfirmation(ToolDefinitionEntity tool,
                                                          Map<String, Object> inputParams,
                                                          String agentId,
                                                          String runId,
                                                          String callerUserId) {
        ToolConfirmRequestEntity confirm = new ToolConfirmRequestEntity();
        confirm.setId(newId());
        confirm.setToolId(tool.getId());
        confirm.setRequesterUserId(callerUserId);
        confirm.setAgentId(agentId);
        confirm.setRunId(runId);
        confirm.setRequestPayload(toJson(inputParams));
        confirm.setReason("高风险工具需要人工二次确认");
        confirm.setStatus("pending");
        confirm.setExpiredAt(LocalDateTime.now().plusMinutes(30));
        toolConfirmRequestMapper.insert(confirm);

        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(false);
        result.setStatusCode(202);
        result.setConfirmationRequired(true);
        result.setConfirmationId(confirm.getId());
        result.setResponseBody("高风险工具已生成确认请求，当前未执行。确认请求 ID：" + confirm.getId());
        result.setErrorMessage("高风险工具待人工确认");
        return result;
    }

    /**
     * 创建工具调用 Trace 步骤。
     *
     * @param run 运行记录
     * @param parentStepId 父步骤 ID
     * @param tool 工具定义
     * @param call 模型工具调用
     * @param inputParams 输入参数
     * @return Trace 步骤
     */
    private RuntimeTraceStepEntity createToolTraceStep(RuntimeRunEntity run,
                                                       String parentStepId,
                                                       ToolDefinitionEntity tool,
                                                       ToolCallRequest call,
                                                       Map<String, Object> inputParams) {
        RuntimeTraceStepEntity step = new RuntimeTraceStepEntity();
        step.setId(newId());
        step.setRunId(run.getId());
        step.setParentStepId(parentStepId);
        step.setStepKey("tool_" + tool.getToolCode());
        step.setStepName("工具调用：" + tool.getToolName());
        step.setStepType("TOOL");
        step.setStatus("RUNNING");
        step.setInputPayload(toJson(Map.of(
                "toolCode", tool.getToolCode(),
                "toolCallId", call.getId(),
                "arguments", inputParams
        )));
        step.setTokenUsage("{}");
        step.setCostAmount(BigDecimal.ZERO);
        step.setStartedAt(LocalDateTime.now());
        runtimeTraceStepMapper.insert(step);
        return step;
    }

    /**
     * 完成工具 Trace 步骤。
     *
     * @param step Trace 步骤
     * @param result 执行结果
     */
    private void finishToolTraceStep(RuntimeTraceStepEntity step, ToolExecutionResult result) {
        step.setStatus(Boolean.TRUE.equals(result.getSuccess()) ? "SUCCESS" : "FAILED");
        step.setOutputPayload(toJson(result));
        step.setLatencyMs(result.getLatencyMs());
        step.setErrorMessage(result.getErrorMessage());
        step.setFinishedAt(LocalDateTime.now());
        runtimeTraceStepMapper.updateById(step);
    }

    /**
     * 保存工具调用日志。
     *
     * @param tool 工具定义
     * @param inputParams 输入参数
     * @param result 执行结果
     * @param agentId Agent ID
     * @param runId 运行 ID
     * @param stepId Trace 步骤 ID
     * @param callerUserId 调用用户 ID
     */
    private void saveInvocationLog(ToolDefinitionEntity tool,
                                   Map<String, Object> inputParams,
                                   ToolExecutionResult result,
                                   String agentId,
                                   String runId,
                                   String stepId,
                                   String callerUserId) {
        ToolInvocationLogEntity log = new ToolInvocationLogEntity();
        log.setId(newId());
        log.setToolId(tool.getId());
        log.setAgentId(agentId);
        log.setRunId(runId);
        log.setStepId(stepId);
        log.setCallerUserId(callerUserId);
        log.setToolCode(tool.getToolCode());
        log.setInputParams(toJson(inputParams));
        log.setOutputResult(toJson(result));
        log.setSuccess(Boolean.TRUE.equals(result.getSuccess()));
        log.setRiskLevel(tool.getRiskLevel());
        log.setLatencyMs(result.getLatencyMs());
        log.setErrorMessage(result.getErrorMessage());
        toolInvocationLogMapper.insert(log);
    }

    /**
     * 将工具实体转换为模型可用工具定义。
     *
     * @param entity 工具实体
     * @return 模型工具定义
     */
    private ToolDefinitionForModel toModelTool(ToolDefinitionEntity entity) {
        ToolDefinitionForModel tool = new ToolDefinitionForModel();
        tool.setId(entity.getId());
        tool.setName(entity.getToolCode());
        tool.setDescription(entity.getDescription());
        tool.setParameters(parseSchema(entity.getRequestSchema()));
        return tool;
    }

    /**
     * 将工具实体转换为展示摘要。
     *
     * @param entity 工具实体
     * @return 工具摘要
     */
    private ToolDefinitionSummary toSummary(ToolDefinitionEntity entity) {
        ToolDefinitionSummary summary = new ToolDefinitionSummary();
        summary.setId(entity.getId());
        summary.setToolCode(entity.getToolCode());
        summary.setToolName(entity.getToolName());
        summary.setToolType(entity.getToolType());
        summary.setWorkspaceId(entity.getWorkspaceId());
        summary.setWorkspaceName(findWorkspaceName(entity.getWorkspaceId()));
        summary.setDescription(entity.getDescription());
        summary.setRequestMethod(entity.getRequestMethod());
        summary.setEndpointUrl(entity.getEndpointUrl());
        summary.setAuthType(entity.getAuthType());
        summary.setAuthConfig(entity.getAuthConfig());
        summary.setHeaders(entity.getHeaders());
        summary.setRequestSchema(entity.getRequestSchema());
        summary.setResponseSchema(entity.getResponseSchema());
        summary.setTimeoutMs(entity.getTimeoutMs());
        summary.setRetryCount(entity.getRetryCount());
        summary.setRiskLevel(entity.getRiskLevel());
        summary.setRiskLabel(riskLabel(entity.getRiskLevel()));
        summary.setRequireConfirm(entity.getRequireConfirm());
        summary.setEnabled(entity.getEnabled());
        summary.setSourceType(entity.getSourceType());
        summary.setMcpServerId(entity.getMcpServerId());
        summary.setMcpToolName(entity.getMcpToolName());
        summary.setStatus(entity.getStatus());
        summary.setInvocationCount(countInvocations(entity.getId()));
        summary.setSuccessRate(successRate(entity.getId()));
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    /**
     * 将绑定实体转换为展示摘要。
     *
     * @param binding 绑定实体
     * @return 绑定摘要
     */
    private AgentToolBindingSummary toBindingSummary(AgentToolBindingEntity binding) {
        ToolDefinitionEntity tool = toolDefinitionMapper.selectById(binding.getToolId());
        AgentToolBindingSummary summary = new AgentToolBindingSummary();
        summary.setAgentId(binding.getAgentId());
        summary.setToolId(binding.getToolId());
        summary.setToolCode(tool == null ? "" : tool.getToolCode());
        summary.setToolName(tool == null ? "" : tool.getToolName());
        summary.setToolType(tool == null ? "" : tool.getToolType());
        summary.setRiskLevel(tool == null ? "" : tool.getRiskLevel());
        summary.setRequireConfirm(binding.getRequireConfirm());
        summary.setEnabled(binding.getEnabled());
        return summary;
    }

    /**
     * 填充工具实体。
     *
     * @param entity 工具实体
     * @param request 保存请求
     * @param create 是否创建场景
     */
    private void fillTool(ToolDefinitionEntity entity, ToolDefinitionRequest request, boolean create) {
        String code = StringUtils.hasText(request.getToolCode()) ? request.getToolCode().trim() : slugify(request.getToolName());
        entity.setToolCode(create ? uniqueToolCode(code) : code);
        entity.setToolName(request.getToolName().trim());
        entity.setToolType(StringUtils.hasText(request.getToolType()) ? request.getToolType() : "REST_API");
        if (!create && StringUtils.hasText(request.getWorkspaceId())) {
            entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "tool", entity.getId(), entity.getOwnerUserId()));
        }
        entity.setDescription(request.getDescription());
        entity.setRequestMethod(StringUtils.hasText(request.getRequestMethod()) ? request.getRequestMethod() : "GET");
        entity.setEndpointUrl(request.getEndpointUrl());
        entity.setAuthType(StringUtils.hasText(request.getAuthType()) ? request.getAuthType() : "none");
        entity.setAuthConfig(validJsonOrDefault(request.getAuthConfig(), "{}"));
        entity.setHeaders(validJsonOrDefault(request.getHeaders(), "{}"));
        entity.setRequestSchema(validJsonOrDefault(request.getRequestSchema(), "{\"type\":\"object\",\"properties\":{}}"));
        entity.setResponseSchema(validJsonOrDefault(request.getResponseSchema(), "{\"type\":\"object\"}"));
        entity.setTimeoutMs(request.getTimeoutMs() == null ? 30000 : request.getTimeoutMs());
        entity.setRetryCount(request.getRetryCount() == null ? 0 : request.getRetryCount());
        entity.setRiskLevel(StringUtils.hasText(request.getRiskLevel()) ? request.getRiskLevel() : "low");
        entity.setRequireConfirm(Boolean.TRUE.equals(request.getRequireConfirm()) || "high".equalsIgnoreCase(entity.getRiskLevel()));
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
        entity.setMcpServerId(request.getMcpServerId());
        entity.setMcpToolName(request.getMcpToolName());
    }

    /**
     * 查询 Agent 绑定的启用工具。
     *
     * @param agentId Agent ID
     * @return 工具实体列表
     */
    private List<ToolDefinitionEntity> listEnabledToolsForAgent(String agentId) {
        List<AgentToolBindingEntity> bindings = agentToolBindingMapper.selectList(new LambdaQueryWrapper<AgentToolBindingEntity>()
                .eq(AgentToolBindingEntity::getAgentId, agentId)
                .eq(AgentToolBindingEntity::getEnabled, true));
        List<ToolDefinitionEntity> tools = new ArrayList<>();
        for (AgentToolBindingEntity binding : bindings) {
            ToolDefinitionEntity tool = toolDefinitionMapper.selectById(binding.getToolId());
            if (tool != null && tool.getDeletedAt() == null && Boolean.TRUE.equals(tool.getEnabled()) && "active".equalsIgnoreCase(tool.getStatus())) {
                tools.add(tool);
            }
        }
        return tools;
    }

    /**
     * 根据工具编码查找 Agent 已绑定工具。
     *
     * @param agentId Agent ID
     * @param toolCode 工具编码
     * @return 工具实体
     */
    private ToolDefinitionEntity findBoundToolByCode(String agentId, String toolCode) {
        return listEnabledToolsForAgent(agentId).stream()
                .filter(tool -> tool.getToolCode().equals(toolCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException("TOOL_NOT_BOUND", "Agent 未绑定工具：" + toolCode));
    }

    /**
     * 查询工具实体。
     *
     * @param id 工具 ID
     * @return 工具实体
     */
    private ToolDefinitionEntity requireTool(String id) {
        ToolDefinitionEntity entity = toolDefinitionMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("TOOL_NOT_FOUND", "工具不存在");
        }
        return entity;
    }

    /**
     * 查询 Agent 实体。
     *
     * @param id Agent ID
     * @return Agent 实体
     */
    private AgentEntity requireAgent(String id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
        }
        return entity;
    }

    /**
     * 拼接 HTTP 工具 URL。
     *
     * @param template URL 模板
     * @param inputParams 输入参数
     * @param appendQuery 是否追加 query 参数
     * @return URL
     */
    private String buildUrl(String template, Map<String, Object> inputParams, boolean appendQuery) {
        if (!StringUtils.hasText(template)) {
            throw new BusinessException("TOOL_ENDPOINT_EMPTY", "工具请求地址不能为空");
        }
        Matcher matcher = PATH_PARAM_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        Set<String> usedKeys = new LinkedHashSet<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            usedKeys.add(key);
            Object value = inputParams.get(key);
            matcher.appendReplacement(buffer, UriUtils.encodePathSegment(value == null ? "" : String.valueOf(value), StandardCharsets.UTF_8));
        }
        matcher.appendTail(buffer);
        String url = buffer.toString();
        if (!appendQuery) {
            return url;
        }
        List<String> queryParts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : inputParams.entrySet()) {
            if (!usedKeys.contains(entry.getKey())) {
                queryParts.add(UriUtils.encodeQueryParam(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + UriUtils.encodeQueryParam(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            }
        }
        if (queryParts.isEmpty()) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + String.join("&", queryParts);
    }

    /**
     * 渲染只读 SQL。
     *
     * @param template SQL 模板
     * @param inputParams 输入参数
     * @return 渲染后的 SQL
     */
    private String renderSql(String template, Map<String, Object> inputParams) {
        String sql = template == null ? "" : template;
        for (Map.Entry<String, Object> entry : inputParams.entrySet()) {
            sql = sql.replace("{" + entry.getKey() + "}", sqlLiteral(entry.getValue()));
        }
        return sql;
    }

    /**
     * 转换 SQL 字面量。
     *
     * @param value 原始值
     * @return SQL 字面量
     */
    private String sqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }

    /**
     * 写入 HTTP 请求头和认证信息。
     *
     * @param builder 请求构建器
     * @param tool 工具定义
     */
    private void appendHeaders(HttpRequest.Builder builder, ToolDefinitionEntity tool) {
        builder.header("Content-Type", "application/json");
        parseMap(tool.getHeaders()).forEach((key, value) -> builder.header(key, String.valueOf(value)));
        Map<String, Object> auth = parseMap(tool.getAuthConfig());
        String authType = safeText(tool.getAuthType()).toLowerCase(Locale.ROOT);
        if ("bearer".equals(authType)) {
            Object token = auth.getOrDefault("token", auth.get("bearerToken"));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
        } else if ("api_key".equals(authType)) {
            String headerName = String.valueOf(auth.getOrDefault("headerName", "X-API-Key"));
            Object value = auth.getOrDefault("apiKey", auth.get("apiKeyValue"));
            if (value != null) {
                builder.header(headerName, String.valueOf(value));
            }
        } else if ("basic".equals(authType)) {
            String user = String.valueOf(auth.getOrDefault("username", ""));
            String password = String.valueOf(auth.getOrDefault("password", ""));
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * 判断工具是否需要人工确认。
     *
     * @param tool 工具定义
     * @return 是否需要人工确认
     */
    private boolean requiresManualConfirmation(ToolDefinitionEntity tool) {
        return Boolean.TRUE.equals(tool.getRequireConfirm()) || "high".equalsIgnoreCase(tool.getRiskLevel());
    }

    /**
     * 校验当前用户可管理工具。
     *
     * @param entity 工具实体
     */
    private void assertCanManage(ToolDefinitionEntity entity) {
        if (!canManage(entity)) {
            throw new BusinessException("TOOL_FORBIDDEN", "没有管理该工具的权限");
        }
    }

    /**
     * 判断当前用户可查看工具。
     *
     * @param entity 工具实体
     * @return 是否可查看
     */
    private boolean canView(ToolDefinitionEntity entity) {
        return entity != null && workspaceGovernanceService.canViewResource(
                "tool",
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getOwnerUserId(),
                entity.getCreatedBy(),
                "private");
    }

    /**
     * 判断当前用户可管理工具。
     *
     * @param entity 工具实体
     * @return 是否可管理
     */
    private boolean canManage(ToolDefinitionEntity entity) {
        return entity != null && workspaceGovernanceService.canManageResource(entity.getWorkspaceId(), entity.getOwnerUserId(), entity.getCreatedBy());
    }

    /**
     * 判断是否系统管理员。
     *
     * @return 是否管理员
     */
    private boolean isSystemManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> List.of("ROLE_super_admin", "ROLE_admin", "tool:manage").contains(authority));
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 获取当前用户 ID，未登录时抛出异常。
     *
     * @return 当前用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 查询工具调用次数。
     *
     * @param toolId 工具 ID
     * @return 调用次数
     */
    private Integer countInvocations(String toolId) {
        Number count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tool_invocation_log WHERE tool_id = ?", Number.class, toolId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询工具调用成功率。
     *
     * @param toolId 工具 ID
     * @return 成功率百分比
     */
    private Integer successRate(String toolId) {
        Number total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tool_invocation_log WHERE tool_id = ?", Number.class, toolId);
        Number success = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tool_invocation_log WHERE tool_id = ? AND success = 1", Number.class, toolId);
        if (total == null || total.intValue() == 0) {
            return 100;
        }
        return (int) Math.round(success.intValue() * 100D / total.intValue());
    }

    /**
     * 解析 JSON Schema。
     *
     * @param json Schema JSON
     * @return Schema 节点
     */
    private JsonNode parseSchema(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return objectMapper.createObjectNode().put("type", "object");
            }
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode().put("type", "object");
        }
    }

    /**
     * 解析 JSON Map。
     *
     * @param json JSON 字符串
     * @return Map
     */
    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            }));
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 保证 JSON 字符串有效。
     *
     * @param json 原始 JSON
     * @param fallback 默认 JSON
     * @return 有效 JSON
     */
    private String validJsonOrDefault(String json, String fallback) {
        try {
            if (!StringUtils.hasText(json)) {
                return fallback;
            }
            objectMapper.readTree(json);
            return json;
        } catch (Exception exception) {
            return fallback;
        }
    }

    /**
     * 风险等级中文标签。
     *
     * @param riskLevel 风险等级
     * @return 中文标签
     */
    private String riskLabel(String riskLevel) {
        if ("high".equalsIgnoreCase(riskLevel)) {
            return "高风险";
        }
        if ("medium".equalsIgnoreCase(riskLevel)) {
            return "中风险";
        }
        return "低风险";
    }

    /**
     * 生成唯一工具编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueToolCode(String baseCode) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "tool";
        String candidate = normalized;
        int suffix = 1;
        while (toolDefinitionMapper.selectCount(new LambdaQueryWrapper<ToolDefinitionEntity>()
                .eq(ToolDefinitionEntity::getToolCode, candidate)) > 0) {
            candidate = normalized + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * 将名称转换为工具编码。
     *
     * @param text 名称文本
     * @return 工具编码
     */
    private String slugify(String text) {
        String cleaned = text == null ? "tool" : text.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\u4e00-\\u9fa5]+", "_")
                .replaceAll("^_|_$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "tool";
    }

    /**
     * 安全文本。
     *
     * @param text 原始文本
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 生成 UUID。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 转换 JSON。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
