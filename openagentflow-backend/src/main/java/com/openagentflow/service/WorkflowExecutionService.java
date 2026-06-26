package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.chat.ToolCallRequest;
import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.domain.model.ModelRouteDecision;
import com.openagentflow.domain.tool.ToolExecutionResult;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.entity.RuntimeLlmCallEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.entity.RuntimeTraceStepEntity;
import com.openagentflow.entity.WorkflowDefinitionEntity;
import com.openagentflow.entity.WorkflowEdgeEntity;
import com.openagentflow.entity.WorkflowNodeEntity;
import com.openagentflow.entity.WorkflowRunEntity;
import com.openagentflow.entity.WorkflowStepRunEntity;
import com.openagentflow.entity.WorkflowVersionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.mapper.RuntimeLlmCallMapper;
import com.openagentflow.mapper.RuntimeRunMapper;
import com.openagentflow.mapper.RuntimeTraceStepMapper;
import com.openagentflow.mapper.WorkflowEdgeMapper;
import com.openagentflow.mapper.WorkflowNodeMapper;
import com.openagentflow.mapper.WorkflowRunMapper;
import com.openagentflow.mapper.WorkflowStepRunMapper;
import com.openagentflow.mapper.WorkflowVersionMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流执行引擎。
 *
 * <p>当前实现面向 MVP：按照连线从开始节点顺序推进，支持 START、LLM、RAG、TOOL、CONDITION、END。</p>
 */
@Service
public class WorkflowExecutionService {

    /** 模板变量占位符，例如 {{input}}。 */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    /** 工作流定义服务。 */
    private final WorkflowService workflowService;

    /** 工作流版本 Mapper。 */
    private final WorkflowVersionMapper workflowVersionMapper;

    /** 工作流节点 Mapper。 */
    private final WorkflowNodeMapper workflowNodeMapper;

    /** 工作流连线 Mapper。 */
    private final WorkflowEdgeMapper workflowEdgeMapper;

    /** 工作流运行 Mapper。 */
    private final WorkflowRunMapper workflowRunMapper;

    /** 工作流节点运行 Mapper。 */
    private final WorkflowStepRunMapper workflowStepRunMapper;

    /** Runtime 运行 Mapper。 */
    private final RuntimeRunMapper runtimeRunMapper;

    /** Runtime Trace 步骤 Mapper。 */
    private final RuntimeTraceStepMapper runtimeTraceStepMapper;

    /** Runtime LLM 调用 Mapper。 */
    private final RuntimeLlmCallMapper runtimeLlmCallMapper;

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** Agent 权限服务。 */
    private final AgentAccessService agentAccessService;

    /** 模型服务商服务。 */
    private final ModelProviderService modelProviderService;

    /** OpenAI-compatible 客户端。 */
    private final OpenAiCompatibleClient openAiCompatibleClient;

    /** RAG 知识库服务。 */
    private final KnowledgeBaseService knowledgeBaseService;

    /** 工具调用服务。 */
    private final ToolService toolService;

    /** 成本与用量服务。 */
    private final UsageCostService usageCostService;

    /** 模型网关服务。 */
    private final ModelGatewayService modelGatewayService;

    /** JDBC 工具，用于写入人工确认任务和策略命中日志。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    public WorkflowExecutionService(WorkflowService workflowService,
                                    WorkflowVersionMapper workflowVersionMapper,
                                    WorkflowNodeMapper workflowNodeMapper,
                                    WorkflowEdgeMapper workflowEdgeMapper,
                                    WorkflowRunMapper workflowRunMapper,
                                    WorkflowStepRunMapper workflowStepRunMapper,
                                    RuntimeRunMapper runtimeRunMapper,
                                    RuntimeTraceStepMapper runtimeTraceStepMapper,
                                    RuntimeLlmCallMapper runtimeLlmCallMapper,
                                    AgentMapper agentMapper,
                                    ModelConfigMapper modelConfigMapper,
                                    AgentAccessService agentAccessService,
                                    ModelProviderService modelProviderService,
                                    OpenAiCompatibleClient openAiCompatibleClient,
                                    KnowledgeBaseService knowledgeBaseService,
                                    ToolService toolService,
                                    UsageCostService usageCostService,
                                    ModelGatewayService modelGatewayService,
                                    JdbcTemplate jdbcTemplate,
                                    ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.workflowVersionMapper = workflowVersionMapper;
        this.workflowNodeMapper = workflowNodeMapper;
        this.workflowEdgeMapper = workflowEdgeMapper;
        this.workflowRunMapper = workflowRunMapper;
        this.workflowStepRunMapper = workflowStepRunMapper;
        this.runtimeRunMapper = runtimeRunMapper;
        this.runtimeTraceStepMapper = runtimeTraceStepMapper;
        this.runtimeLlmCallMapper = runtimeLlmCallMapper;
        this.agentMapper = agentMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.agentAccessService = agentAccessService;
        this.modelProviderService = modelProviderService;
        this.openAiCompatibleClient = openAiCompatibleClient;
        this.knowledgeBaseService = knowledgeBaseService;
        this.toolService = toolService;
        this.usageCostService = usageCostService;
        this.modelGatewayService = modelGatewayService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行指定工作流。
     *
     * @param workflowId 工作流 ID
     * @param request 运行请求
     * @param triggerType 触发类型
     * @return 运行结果
     */
    public WorkflowDtos.RunResult runWorkflow(String workflowId, WorkflowDtos.RunRequest request, String triggerType) {
        WorkflowDefinitionEntity workflow = workflowService.requireWorkflow(workflowId);
        if (!workflowService.canView(workflow)) {
            throw new BusinessException("WORKFLOW_FORBIDDEN", "没有运行该工作流的权限");
        }
        AgentEntity agent = resolveAgent(request == null ? null : request.getAgentId());
        WorkflowVersionEntity version = resolveVersion(workflow);
        List<WorkflowNodeEntity> nodes = listNodes(workflowId);
        List<WorkflowEdgeEntity> edges = listEdges(workflowId);
        if (nodes.isEmpty()) {
            throw new BusinessException("WORKFLOW_GRAPH_EMPTY", "工作流没有可执行节点");
        }

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("input", request == null ? "" : safeText(request.getInput()));
        context.put("lastOutput", request == null ? "" : safeText(request.getInput()));
        context.put("variables", request == null || request.getVariables() == null ? Map.of() : request.getVariables());
        context.put("debugMode", request != null && Boolean.TRUE.equals(request.getDebugMode()));
        context.put("dryRun", request != null && Boolean.TRUE.equals(request.getDryRun()));
        context.put("workflowDepth", request == null || request.getVariables() == null ? 0 : numberValue(request.getVariables().get("workflowDepth"), 0D).intValue());
        if (request != null && request.getVariables() != null) {
            // 把常用变量平铺一份，方便节点模板直接用 {{customerId}} 这类写法。
            context.putAll(request.getVariables());
        }

        WorkflowRunEntity workflowRun = createWorkflowRun(workflow, version, agent, request, context, triggerType);
        RuntimeRunEntity runtimeRun = createRuntimeRun(workflow, workflowRun, agent, request, version);
        List<WorkflowDtos.StepResult> stepResults = new ArrayList<>();
        LocalDateTime startedAt = LocalDateTime.now();
        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;
        int totalTokens = 0;
        String finalOutput = safeText(request == null ? "" : request.getInput());

        try {
            WorkflowNodeEntity current = findStartNode(nodes, request == null ? null : request.getStartNodeKey());
            int maxSteps = request == null || request.getMaxSteps() == null ? 100 : Math.max(1, Math.min(request.getMaxSteps(), 500));
            for (int guard = 0; current != null && guard < maxSteps; guard++) {
                NodeExecutionResult nodeResult = executeNode(current, agent, runtimeRun, context, request);
                stepResults.add(nodeResult.stepResult());
                totalPromptTokens += nodeResult.promptTokens();
                totalCompletionTokens += nodeResult.completionTokens();
                totalTokens += nodeResult.totalTokens();
                enforceBudget(workflow, runtimeRun, current, context, totalTokens);
                if (nodeResult.output() != null) {
                    context.put("lastOutput", nodeResult.output());
                    finalOutput = String.valueOf(nodeResult.output());
                }
                if ("WAITING".equalsIgnoreCase(nodeResult.status())) {
                    finishWaiting(workflowRun, runtimeRun, context, finalOutput, totalPromptTokens, totalCompletionTokens, totalTokens, startedAt);
                    return toRunResult(workflowRun, runtimeRun, context, finalOutput, stepResults, totalTokens, "等待人工确认");
                }
                if ("END".equalsIgnoreCase(current.getNodeType())) {
                    break;
                }
                current = nextNode(current, nodes, edges, context);
            }
            finishSuccess(workflowRun, runtimeRun, context, finalOutput, totalPromptTokens, totalCompletionTokens, totalTokens, startedAt);
            return toRunResult(workflowRun, runtimeRun, context, finalOutput, stepResults, totalTokens, null);
        } catch (Exception exception) {
            finishFailure(workflowRun, runtimeRun, context, exception, startedAt);
            return toRunResult(workflowRun, runtimeRun, context, finalOutput, stepResults, totalTokens, exception.getMessage());
        }
    }

    /**
     * 执行单个节点。
     *
     * @param node 当前节点
     * @param agent 绑定 Agent
     * @param runtimeRun Runtime 运行记录
     * @param context 工作流上下文
     * @return 节点结果
     */
    private NodeExecutionResult executeNode(WorkflowNodeEntity node,
                                            AgentEntity agent,
                                            RuntimeRunEntity runtimeRun,
                                            Map<String, Object> context,
                                            WorkflowDtos.RunRequest request) throws Exception {
        Map<String, Object> config = parseMap(node.getConfigJson());
        Map<String, Object> retryPolicy = parseMap(node.getRetryPolicy());
        int retryCount = numberValue(firstNonNull(config.get("retryCount"), retryPolicy.get("retryCount")), 0D).intValue();
        int retryIntervalMs = numberValue(firstNonNull(config.get("retryIntervalMs"), retryPolicy.get("retryIntervalMs")), 0D).intValue();
        int timeoutMs = numberValue(firstNonNull(config.get("timeoutMs"), retryPolicy.get("timeoutMs")), 0D).intValue();
        String failureStrategy = stringValue(firstNonNull(config.get("failureStrategy"), retryPolicy.get("failureStrategy")), "STOP").toUpperCase(Locale.ROOT);
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryCount + 1; attempt++) {
            WorkflowStepRunEntity stepRun = createWorkflowStepRun(runtimeRun.getWorkflowRunId(), runtimeRun.getWorkflowId(), node, context);
            RuntimeTraceStepEntity traceStep = createTraceStep(runtimeRun, node, context);
            stepRun.setAttemptNo(attempt);
            workflowStepRunMapper.updateById(stepRun);
            LocalDateTime startedAt = LocalDateTime.now();
            try {
                NodeExecutionResult result = executeNodeOnce(node, agent, runtimeRun, traceStep, context, config, request);
                int latencyMs = (int) Duration.between(startedAt, LocalDateTime.now()).toMillis();
                if (timeoutMs > 0 && latencyMs > timeoutMs) {
                    writePolicyHit(runtimeRun, node, "timeout", "block", Map.of("timeoutMs", timeoutMs, "latencyMs", latencyMs), "节点执行超过超时阈值");
                    throw new BusinessException("WORKFLOW_NODE_TIMEOUT", "节点执行超时：" + node.getNodeName());
                }
                result.stepResult().setAttemptNo(attempt);
                finishStepSuccess(stepRun, traceStep, result, startedAt);
                return result;
            } catch (Exception exception) {
                lastException = exception;
                finishStepFailure(stepRun, traceStep, exception, startedAt);
                if (attempt <= retryCount) {
                    writePolicyHit(runtimeRun, node, "retry", "warn", Map.of("attempt", attempt, "retryCount", retryCount), exception.getMessage());
                    sleepQuietly(retryIntervalMs);
                }
            }
        }

        context.put("error", lastException == null ? "节点执行失败" : lastException.getMessage());
        context.put("lastFailedNode", node.getNodeKey());
        if ("CONTINUE".equals(failureStrategy)) {
            Object fallback = config.getOrDefault("fallbackOutput", "节点失败后按策略继续");
            writePolicyHit(runtimeRun, node, "failure", "fallback", config, "节点失败后继续执行");
            return NodeExecutionResult.failure(node, fallback, lastException == null ? "" : lastException.getMessage(), "FAILED_CONTINUED");
        }
        if ("GOTO".equals(failureStrategy)) {
            String target = stringValue(config.get("failureTargetNodeKey"), "");
            if (StringUtils.hasText(target)) {
                context.put("__nextNodeKey", target);
                writePolicyHit(runtimeRun, node, "failure", "fallback", config, "节点失败后跳转到：" + target);
                return NodeExecutionResult.failure(node, config.getOrDefault("fallbackOutput", ""), lastException == null ? "" : lastException.getMessage(), "FAILED_GOTO");
            }
        }
        throw lastException == null ? new BusinessException("WORKFLOW_NODE_FAILED", "节点执行失败") : lastException;
    }

    /**
     * 执行单次节点逻辑，外层负责重试、超时和失败策略。
     */
    private NodeExecutionResult executeNodeOnce(WorkflowNodeEntity node,
                                                AgentEntity agent,
                                                RuntimeRunEntity runtimeRun,
                                                RuntimeTraceStepEntity traceStep,
                                                Map<String, Object> context,
                                                Map<String, Object> config,
                                                WorkflowDtos.RunRequest request) {
        if (request != null && Boolean.TRUE.equals(request.getDryRun()) && isExternalNode(node.getNodeType())) {
            return dryRunResult(node, config);
        }
        return switch (safeText(node.getNodeType()).toUpperCase(Locale.ROOT)) {
            case "START" -> executeStart(node, context);
            case "RAG" -> executeRag(node, agent, runtimeRun, context, config);
            case "TOOL" -> executeTool(node, agent, runtimeRun, traceStep, context, config);
            case "CONDITION" -> executeCondition(node, context, config);
            case "HUMAN", "APPROVAL" -> executeHuman(node, runtimeRun, context, config);
            case "LOOP", "BATCH" -> executeLoop(node, context, config);
            case "SUBFLOW" -> executeSubflow(node, agent, context, config);
            case "PLUGIN" -> executePlugin(node, agent, runtimeRun, traceStep, context, config);
            case "PARALLEL" -> executeParallel(node, context, config);
            case "JOIN" -> executeJoin(node, context);
            case "API", "WEBHOOK", "NOTIFY" -> executePlugin(node, agent, runtimeRun, traceStep, context, config);
            case "END" -> executeEnd(node, context);
            default -> executeLlm(node, agent, runtimeRun, traceStep, context, config);
        };
    }

    /**
     * 执行开始节点。
     */
    private NodeExecutionResult executeStart(WorkflowNodeEntity node, Map<String, Object> context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("input", context.get("input"));
        return NodeExecutionResult.success(node, output, 0, 0, 0);
    }

    /**
     * 执行 RAG 节点。
     */
    private NodeExecutionResult executeRag(WorkflowNodeEntity node,
                                           AgentEntity agent,
                                           RuntimeRunEntity runtimeRun,
                                           Map<String, Object> context,
                                           Map<String, Object> config) {
        if (agent == null) {
            throw new BusinessException("WORKFLOW_AGENT_REQUIRED", "RAG 节点需要绑定 Agent 后运行");
        }
        String queryTemplate = stringValue(config.get("queryTemplate"), "{{input}}");
        String query = renderTemplate(queryTemplate, context);
        List<KnowledgeSource> sources = knowledgeBaseService.retrieveForAgent(agent, query, runtimeRun.getId());
        context.put("sources", sources);
        return NodeExecutionResult.success(node, sources, 0, 0, 0);
    }

    /**
     * 执行 LLM 节点。
     */
    private NodeExecutionResult executeLlm(WorkflowNodeEntity node,
                                           AgentEntity agent,
                                           RuntimeRunEntity runtimeRun,
                                           RuntimeTraceStepEntity traceStep,
                                           Map<String, Object> context,
                                           Map<String, Object> config) {
        ModelRouteDecision routeDecision = modelGatewayService.resolveAgentChatRoute(stringValue(config.get("modelId"), ""), agent);
        ModelConfigEntity model = routeDecision.getModel();
        ModelProviderEntity provider = routeDecision.getProvider();
        ChatRunContext chatContext = new ChatRunContext();
        chatContext.setAgent(agent);
        chatContext.setModel(model);
        chatContext.setProvider(provider);
        chatContext.setApiKey(routeDecision.getApiKey());
        chatContext.setRouteDecision(routeDecision);
        chatContext.setSources(sourcesFromContext(context));
        chatContext.setMessages(buildMessages(agent, config, context));
        traceStep.setModelId(model.getId());
        traceStep.setPromptText(toJson(chatContext.getMessages()));

        Double temperature = numberValue(config.get("temperature"), 0.3D);
        Integer maxTokens = numberValue(config.get("maxTokens"), model.getMaxOutputTokens() == null ? 2048D : model.getMaxOutputTokens().doubleValue()).intValue();
        LlmCallResult result = invokeWithGatewayFallback(chatContext,
                current -> openAiCompatibleClient.complete(current, temperature, maxTokens),
                current -> usageCostService.assertWithinQuota(runtimeRun.getUserId(), runtimeRun.getAgentId(), current.getProvider(), current.getModel(), current.getMessages(), maxTokens));
        traceStep.setModelId(chatContext.getModel().getId());
        BigDecimal cost = usageCostService.calculateCost(chatContext.getModel(), nullToZero(result.getPromptTokens()), nullToZero(result.getCompletionTokens()));
        traceStep.setCostAmount(cost);
        saveLlmCall(runtimeRun, traceStep, chatContext, result, true, null, cost);
        context.put("answer", safeText(result.getContent()));
        return NodeExecutionResult.success(
                node,
                safeText(result.getContent()),
                nullToZero(result.getPromptTokens()),
                nullToZero(result.getCompletionTokens()),
                nullToZero(result.getTotalTokens()),
                cost
        );
    }

    /**
     * 执行工具节点。
     */
    private NodeExecutionResult executeTool(WorkflowNodeEntity node,
                                            AgentEntity agent,
                                            RuntimeRunEntity runtimeRun,
                                            RuntimeTraceStepEntity traceStep,
                                            Map<String, Object> context,
                                            Map<String, Object> config) {
        if (agent == null) {
            throw new BusinessException("WORKFLOW_AGENT_REQUIRED", "工具节点需要绑定 Agent 后运行");
        }
        String toolName = stringValue(config.get("toolName"), stringValue(config.get("toolCode"), ""));
        if (!StringUtils.hasText(toolName)) {
            throw new BusinessException("WORKFLOW_TOOL_EMPTY", "工具节点未配置工具编码");
        }
        Map<String, Object> arguments = renderMap(parseMap(config.get("arguments")), context);
        ToolCallRequest call = new ToolCallRequest();
        call.setId("wf_tool_" + newId());
        call.setName(toolName);
        call.setArgumentsJson(toJson(arguments));
        ToolExecutionResult result = toolService.executeToolCallForAgent(agent, runtimeRun, traceStep.getId(), call);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("toolName", toolName);
        output.put("success", Boolean.TRUE.equals(result.getSuccess()));
        output.put("statusCode", result.getStatusCode());
        output.put("latencyMs", result.getLatencyMs());
        output.put("responseBody", safeText(result.getResponseBody()));
        output.put("errorMessage", safeText(result.getErrorMessage()));
        context.put("toolResult", output);
        return NodeExecutionResult.success(node, output, 0, 0, 0);
    }

    /**
     * 执行条件节点。
     */
    private NodeExecutionResult executeCondition(WorkflowNodeEntity node,
                                                 Map<String, Object> context,
                                                 Map<String, Object> config) {
        String expression = stringValue(config.get("conditionExpr"), "success");
        boolean matched = matches(expression, context);
        context.put("conditionMatched", matched);
        return NodeExecutionResult.success(node, Map.of("matched", matched, "expression", expression), 0, 0, 0);
    }

    /**
     * 执行结束节点。
     */
    private NodeExecutionResult executeEnd(WorkflowNodeEntity node, Map<String, Object> context) {
        Object output = context.getOrDefault("lastOutput", context.getOrDefault("answer", ""));
        return NodeExecutionResult.success(node, output, 0, 0, 0);
    }

    /**
     * 执行人工确认节点，节点会暂停工作流并写入人工任务表。
     */
    private NodeExecutionResult executeHuman(WorkflowNodeEntity node,
                                             RuntimeRunEntity runtimeRun,
                                             Map<String, Object> context,
                                             Map<String, Object> config) {
        String taskId = newId();
        int expireMinutes = numberValue(config.get("expireMinutes"), 60D).intValue();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nodeKey", node.getNodeKey());
        payload.put("nodeName", node.getNodeName());
        payload.put("context", context);
        payload.put("suggestion", renderTemplate(stringValue(config.get("suggestion"), "{{lastOutput}}"), context));
        jdbcTemplate.update("""
                INSERT INTO workflow_human_task
                  (id, workflow_run_id, step_run_id, task_name, assignee_user_id, payload, status, expired_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), 'pending', DATE_ADD(NOW(3), INTERVAL ? MINUTE))
                """,
                taskId,
                runtimeRun.getWorkflowRunId(),
                null,
                stringValue(config.get("taskName"), node.getNodeName()),
                stringValue(config.get("assigneeUserId"), runtimeRun.getUserId()),
                toJson(payload),
                expireMinutes);
        context.put("humanTaskId", taskId);
        return NodeExecutionResult.waiting(node, Map.of("taskId", taskId, "message", "等待人工确认"));
    }

    /**
     * 执行循环/批处理节点，将列表逐项渲染为结果数组。
     */
    private NodeExecutionResult executeLoop(WorkflowNodeEntity node,
                                            Map<String, Object> context,
                                            Map<String, Object> config) {
        Object source = StringUtils.hasText(stringValue(config.get("itemPath"), ""))
                ? pathValue(context, stringValue(config.get("itemPath"), ""))
                : config.get("items");
        Object fallbackSource = source == null ? context.get("lastOutput") : source;
        List<?> items = source instanceof List<?> list
                ? list
                : (fallbackSource == null ? List.of() : List.of(fallbackSource));
        int maxLoops = Math.min(numberValue(config.get("maxLoops"), 20D).intValue(), 200);
        List<Object> outputs = new ArrayList<>();
        String template = stringValue(config.get("itemTemplate"), "{{item}}");
        for (int index = 0; index < Math.min(items.size(), maxLoops); index++) {
            Map<String, Object> itemContext = new LinkedHashMap<>(context);
            itemContext.put("item", items.get(index));
            itemContext.put("index", index);
            outputs.add(renderTemplate(template, itemContext));
        }
        context.put(node.getNodeKey(), outputs);
        context.put("loopResults", outputs);
        return NodeExecutionResult.success(node, outputs, 0, 0, 0);
    }

    /**
     * 执行子工作流节点，可把通用流程封装为可复用节点。
     */
    private NodeExecutionResult executeSubflow(WorkflowNodeEntity node,
                                               AgentEntity agent,
                                               Map<String, Object> context,
                                               Map<String, Object> config) {
        String workflowId = stringValue(config.get("workflowId"), "");
        if (!StringUtils.hasText(workflowId)) {
            throw new BusinessException("WORKFLOW_SUBFLOW_EMPTY", "子工作流节点未配置 workflowId");
        }
        int depth = numberValue(context.get("workflowDepth"), 0D).intValue();
        if (depth >= 3) {
            throw new BusinessException("WORKFLOW_SUBFLOW_DEPTH", "子工作流嵌套层级超过限制");
        }
        WorkflowDtos.RunRequest subRequest = new WorkflowDtos.RunRequest();
        subRequest.setAgentId(agent == null ? null : agent.getId());
        subRequest.setInput(renderTemplate(stringValue(config.get("inputTemplate"), "{{lastOutput}}"), context));
        Map<String, Object> variables = new LinkedHashMap<>(context);
        variables.put("workflowDepth", depth + 1);
        subRequest.setVariables(variables);
        subRequest.setDryRun(Boolean.TRUE.equals(context.get("dryRun")));
        WorkflowDtos.RunResult result = runWorkflow(workflowId, subRequest, "subflow");
        context.put(node.getNodeKey(), result.getOutputText());
        context.put("subflowResult", result);
        return NodeExecutionResult.success(node, result.getOutputText(), 0, 0, result.getTotalTokens() == null ? 0 : result.getTotalTokens());
    }

    /**
     * 执行插件节点，当前优先复用工具执行器；未配置工具时返回插件占位输出。
     */
    private NodeExecutionResult executePlugin(WorkflowNodeEntity node,
                                              AgentEntity agent,
                                              RuntimeRunEntity runtimeRun,
                                              RuntimeTraceStepEntity traceStep,
                                              Map<String, Object> context,
                                              Map<String, Object> config) {
        if (StringUtils.hasText(stringValue(config.get("toolName"), "")) || StringUtils.hasText(stringValue(config.get("toolCode"), ""))) {
            return executeTool(node, agent, runtimeRun, traceStep, context, config);
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pluginCode", stringValue(config.get("pluginCode"), node.getNodeKey()));
        output.put("status", "SKIPPED");
        output.put("message", "插件节点尚未绑定执行器，已按安全策略跳过");
        context.put(node.getNodeKey(), output);
        return NodeExecutionResult.success(node, output, 0, 0, 0);
    }

    /**
     * 执行并行节点。当前实现以安全顺序收集分支目标，实际分支由后续连线推进。
     */
    private NodeExecutionResult executeParallel(WorkflowNodeEntity node,
                                                Map<String, Object> context,
                                                Map<String, Object> config) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("mode", "parallel");
        output.put("strategy", stringValue(config.get("joinStrategy"), "all"));
        output.put("message", "并行分支已进入可观测执行模式");
        context.put("parallelState", output);
        return NodeExecutionResult.success(node, output, 0, 0, 0);
    }

    /**
     * 执行汇聚节点，汇总上下文中的分支结果。
     */
    private NodeExecutionResult executeJoin(WorkflowNodeEntity node, Map<String, Object> context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("lastOutput", context.get("lastOutput"));
        output.put("toolResult", context.get("toolResult"));
        output.put("sources", context.get("sources"));
        output.put("loopResults", context.get("loopResults"));
        context.put("joinResult", output);
        return NodeExecutionResult.success(node, output, 0, 0, 0);
    }

    /**
     * 空跑外部节点，避免调试时真实消耗模型、向量库或外部工具。
     */
    private NodeExecutionResult dryRunResult(WorkflowNodeEntity node, Map<String, Object> config) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("dryRun", true);
        output.put("nodeType", node.getNodeType());
        output.put("config", config);
        output.put("message", "调试空跑模式未触发真实外部调用");
        return NodeExecutionResult.success(node, output, 0, 0, 0);
    }

    /**
     * 根据连线选择下一个节点。
     */
    private WorkflowNodeEntity nextNode(WorkflowNodeEntity current,
                                        List<WorkflowNodeEntity> nodes,
                                        List<WorkflowEdgeEntity> edges,
                                        Map<String, Object> context) {
        String forcedTarget = stringValue(context.remove("__nextNodeKey"), "");
        if (StringUtils.hasText(forcedTarget)) {
            return nodes.stream()
                    .filter(node -> forcedTarget.equals(node.getNodeKey()))
                    .findFirst()
                    .orElse(null);
        }
        List<WorkflowEdgeEntity> outgoing = edges.stream()
                .filter(edge -> current.getNodeKey().equals(edge.getSourceNodeKey()))
                .toList();
        if (outgoing.isEmpty()) {
            return null;
        }
        WorkflowEdgeEntity selected = outgoing.get(0);
        if ("CONDITION".equalsIgnoreCase(current.getNodeType())) {
            selected = outgoing.stream()
                    .filter(edge -> matches(edge.getConditionExpr(), context))
                    .findFirst()
                    .orElse(outgoing.stream()
                            .filter(edge -> "default".equalsIgnoreCase(safeText(edge.getConditionExpr()))
                                    || "else".equalsIgnoreCase(safeText(edge.getConditionExpr())))
                            .findFirst()
                            .orElse(outgoing.get(0)));
        }
        String targetKey = selected.getTargetNodeKey();
        return nodes.stream()
                .filter(node -> targetKey.equals(node.getNodeKey()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建工作流运行记录。
     */
    private WorkflowRunEntity createWorkflowRun(WorkflowDefinitionEntity workflow,
                                                WorkflowVersionEntity version,
                                                AgentEntity agent,
                                                WorkflowDtos.RunRequest request,
                                                Map<String, Object> context,
                                                String triggerType) {
        WorkflowRunEntity run = new WorkflowRunEntity();
        run.setId(newId());
        run.setWorkflowId(workflow.getId());
        run.setWorkflowVersionId(version == null ? null : version.getId());
        run.setAgentId(agent == null ? null : agent.getId());
        run.setTriggerType(StringUtils.hasText(triggerType) ? triggerType : "manual");
        run.setTriggerUserId(agentAccessService.currentUserId());
        run.setInputPayload(toJson(Map.of(
                "input", request == null ? "" : safeText(request.getInput()),
                "variables", request == null || request.getVariables() == null ? Map.of() : request.getVariables()
        )));
        run.setContextJson(toJson(context));
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        workflowRunMapper.insert(run);
        return run;
    }

    /**
     * 创建 Runtime 运行记录，用于 Trace 页面统一展示。
     */
    private RuntimeRunEntity createRuntimeRun(WorkflowDefinitionEntity workflow,
                                              WorkflowRunEntity workflowRun,
                                              AgentEntity agent,
                                              WorkflowDtos.RunRequest request,
                                              WorkflowVersionEntity version) {
        RuntimeRunEntity run = new RuntimeRunEntity();
        run.setId(newId());
        run.setRunNo("wf_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now()));
        run.setRunType("WORKFLOW");
        run.setAgentId(agent == null ? null : agent.getId());
        run.setWorkflowId(workflow.getId());
        run.setWorkflowRunId(workflowRun.getId());
        run.setUserId(agentAccessService.currentUserId());
        run.setInputText(request == null ? "" : safeText(request.getInput()));
        run.setInputPayload(workflowRun.getInputPayload());
        run.setStatus("RUNNING");
        run.setTotalTokens(0);
        run.setPromptTokens(0);
        run.setCompletionTokens(0);
        run.setTotalCost(BigDecimal.ZERO);
        run.setMetadata(toJson(Map.of(
                "workflowName", workflow.getWorkflowName(),
                "versionNo", version == null ? "" : version.getVersionNo()
        )));
        run.setStartedAt(LocalDateTime.now());
        runtimeRunMapper.insert(run);
        return run;
    }

    /**
     * 创建节点运行记录。
     */
    private WorkflowStepRunEntity createWorkflowStepRun(String workflowRunId,
                                                        String workflowId,
                                                        WorkflowNodeEntity node,
                                                        Map<String, Object> context) {
        WorkflowStepRunEntity step = new WorkflowStepRunEntity();
        step.setId(newId());
        step.setWorkflowRunId(workflowRunId);
        step.setWorkflowId(workflowId);
        step.setNodeKey(node.getNodeKey());
        step.setNodeName(node.getNodeName());
        step.setNodeType(node.getNodeType());
        step.setInputPayload(toJson(Map.of("context", context)));
        step.setStatus("RUNNING");
        step.setAttemptNo(1);
        step.setTokenCount(0);
        step.setCostAmount(BigDecimal.ZERO);
        step.setStartedAt(LocalDateTime.now());
        workflowStepRunMapper.insert(step);
        return step;
    }

    /**
     * 创建 Runtime Trace 步骤。
     */
    private RuntimeTraceStepEntity createTraceStep(RuntimeRunEntity run, WorkflowNodeEntity node, Map<String, Object> context) {
        RuntimeTraceStepEntity step = new RuntimeTraceStepEntity();
        step.setId(newId());
        step.setRunId(run.getId());
        step.setStepKey(node.getNodeKey());
        step.setStepName(node.getNodeName());
        step.setStepType(safeText(node.getNodeType()).toUpperCase(Locale.ROOT));
        step.setStatus("RUNNING");
        step.setInputPayload(toJson(Map.of("context", context, "nodeConfig", parseMap(node.getConfigJson()))));
        step.setTokenUsage("{}");
        step.setCostAmount(BigDecimal.ZERO);
        step.setStartedAt(LocalDateTime.now());
        runtimeTraceStepMapper.insert(step);
        return step;
    }

    /**
     * 完成节点成功状态。
     */
    private void finishStepSuccess(WorkflowStepRunEntity stepRun,
                                   RuntimeTraceStepEntity traceStep,
                                   NodeExecutionResult result,
                                   LocalDateTime startedAt) {
        int latencyMs = (int) Duration.between(startedAt, LocalDateTime.now()).toMillis();
        stepRun.setStatus(result.status());
        stepRun.setOutputPayload(toJson(result.output()));
        stepRun.setTokenCount(result.totalTokens());
        stepRun.setCostAmount(result.costAmount());
        stepRun.setErrorMessage(result.errorMessage());
        stepRun.setLatencyMs(latencyMs);
        stepRun.setFinishedAt(LocalDateTime.now());
        workflowStepRunMapper.updateById(stepRun);

        traceStep.setStatus(result.status());
        traceStep.setOutputPayload(toJson(result.output()));
        traceStep.setErrorMessage(result.errorMessage());
        traceStep.setTokenUsage(toJson(Map.of(
                "promptTokens", result.promptTokens(),
                "completionTokens", result.completionTokens(),
                "totalTokens", result.totalTokens()
        )));
        traceStep.setCostAmount(result.costAmount());
        traceStep.setLatencyMs(latencyMs);
        traceStep.setFinishedAt(LocalDateTime.now());
        runtimeTraceStepMapper.updateById(traceStep);
        result.stepResult().setLatencyMs(latencyMs);
    }

    /**
     * 完成节点失败状态。
     */
    private void finishStepFailure(WorkflowStepRunEntity stepRun,
                                   RuntimeTraceStepEntity traceStep,
                                   Exception exception,
                                   LocalDateTime startedAt) {
        int latencyMs = (int) Duration.between(startedAt, LocalDateTime.now()).toMillis();
        stepRun.setStatus("FAILED");
        stepRun.setErrorMessage(exception.getMessage());
        stepRun.setLatencyMs(latencyMs);
        stepRun.setFinishedAt(LocalDateTime.now());
        workflowStepRunMapper.updateById(stepRun);

        traceStep.setStatus("FAILED");
        traceStep.setErrorMessage(exception.getMessage());
        traceStep.setLatencyMs(latencyMs);
        traceStep.setFinishedAt(LocalDateTime.now());
        runtimeTraceStepMapper.updateById(traceStep);
    }

    /**
     * 完成工作流成功状态。
     */
    private void finishSuccess(WorkflowRunEntity workflowRun,
                               RuntimeRunEntity runtimeRun,
                               Map<String, Object> context,
                               String output,
                               int promptTokens,
                               int completionTokens,
                               int totalTokens,
                               LocalDateTime startedAt) {
        int latencyMs = (int) Duration.between(startedAt, LocalDateTime.now()).toMillis();
        workflowRun.setStatus("SUCCESS");
        workflowRun.setContextJson(toJson(context));
        workflowRun.setOutputPayload(toJson(Map.of("output", safeText(output))));
        workflowRun.setFinishedAt(LocalDateTime.now());
        workflowRunMapper.updateById(workflowRun);

        runtimeRun.setStatus("SUCCESS");
        runtimeRun.setOutputText(safeText(output));
        runtimeRun.setOutputPayload(toJson(Map.of("output", safeText(output), "context", context)));
        runtimeRun.setPromptTokens(promptTokens);
        runtimeRun.setCompletionTokens(completionTokens);
        runtimeRun.setTotalTokens(totalTokens);
        runtimeRun.setTotalCost(sumWorkflowCost(runtimeRun.getId()));
        runtimeRun.setLatencyMs(latencyMs);
        runtimeRun.setFinishedAt(LocalDateTime.now());
        runtimeRunMapper.updateById(runtimeRun);
    }

    /**
     * 完成工作流等待人工确认状态。
     */
    private void finishWaiting(WorkflowRunEntity workflowRun,
                               RuntimeRunEntity runtimeRun,
                               Map<String, Object> context,
                               String output,
                               int promptTokens,
                               int completionTokens,
                               int totalTokens,
                               LocalDateTime startedAt) {
        int latencyMs = (int) Duration.between(startedAt, LocalDateTime.now()).toMillis();
        workflowRun.setStatus("WAITING");
        workflowRun.setContextJson(toJson(context));
        workflowRun.setOutputPayload(toJson(Map.of("output", safeText(output), "waiting", true)));
        workflowRun.setFinishedAt(null);
        workflowRunMapper.updateById(workflowRun);

        runtimeRun.setStatus("WAITING");
        runtimeRun.setOutputText(safeText(output));
        runtimeRun.setOutputPayload(toJson(Map.of("output", safeText(output), "context", context, "waiting", true)));
        runtimeRun.setPromptTokens(promptTokens);
        runtimeRun.setCompletionTokens(completionTokens);
        runtimeRun.setTotalTokens(totalTokens);
        runtimeRun.setTotalCost(sumWorkflowCost(runtimeRun.getId()));
        runtimeRun.setLatencyMs(latencyMs);
        runtimeRunMapper.updateById(runtimeRun);
    }

    /**
     * 完成工作流失败状态。
     */
    private void finishFailure(WorkflowRunEntity workflowRun,
                               RuntimeRunEntity runtimeRun,
                               Map<String, Object> context,
                               Exception exception,
                               LocalDateTime startedAt) {
        int latencyMs = (int) Duration.between(startedAt, LocalDateTime.now()).toMillis();
        workflowRun.setStatus("FAILED");
        workflowRun.setContextJson(toJson(context));
        workflowRun.setErrorMessage(exception.getMessage());
        workflowRun.setFinishedAt(LocalDateTime.now());
        workflowRunMapper.updateById(workflowRun);

        runtimeRun.setStatus("FAILED");
        runtimeRun.setErrorMessage(exception.getMessage());
        runtimeRun.setLatencyMs(latencyMs);
        runtimeRun.setFinishedAt(LocalDateTime.now());
        runtimeRunMapper.updateById(runtimeRun);
    }

    /**
     * 保存 LLM 调用日志。
     */
    /**
     * 通过模型网关执行工作流 LLM 调用，并在允许时自动切换候选模型。
     *
     * @param context 聊天运行上下文
     * @param invoker 实际模型调用函数
     * @param precheck 调用前预检查
     * @return LLM 调用结果
     */
    private LlmCallResult invokeWithGatewayFallback(ChatRunContext context,
                                                    Function<ChatRunContext, LlmCallResult> invoker,
                                                    Consumer<ChatRunContext> precheck) {
        try {
            precheck.accept(context);
            return invoker.apply(context);
        } catch (Exception firstException) {
            ModelRouteDecision fallback = modelGatewayService.nextFallbackDecision(context.getRouteDecision(), firstException.getMessage());
            if (fallback == null) {
                throw firstException;
            }
            // 回退后用新的模型上下文继续执行，后续日志会记录实际模型。
            context.setRouteDecision(fallback);
            context.setModel(fallback.getModel());
            context.setProvider(fallback.getProvider());
            context.setApiKey(fallback.getApiKey());
            precheck.accept(context);
            return invoker.apply(context);
        }
    }

    private void saveLlmCall(RuntimeRunEntity run,
                             RuntimeTraceStepEntity step,
                             ChatRunContext context,
                             LlmCallResult result,
                             boolean success,
                             String errorMessage,
                             BigDecimal cost) {
        RuntimeLlmCallEntity call = new RuntimeLlmCallEntity();
        call.setId(newId());
        call.setRunId(run.getId());
        call.setStepId(step.getId());
        call.setProviderId(context.getProvider().getId());
        call.setModelId(context.getModel().getId());
        if (context.getRouteDecision() != null) {
            call.setRoutePolicyId(context.getRouteDecision().getRoutePolicyId());
            call.setGatewaySceneType(context.getRouteDecision().getSceneType());
            call.setRouteDecision(modelGatewayService.toDecisionJson(context.getRouteDecision()));
            call.setFallbackUsed(Boolean.TRUE.equals(context.getRouteDecision().getFallbackUsed()));
        }
        call.setRequestMessages(toJson(context.getMessages()));
        call.setResponseMessage(toJson(Map.of("content", safeText(result.getContent()))));
        call.setStream(false);
        call.setPromptTokens(nullToZero(result.getPromptTokens()));
        call.setCompletionTokens(nullToZero(result.getCompletionTokens()));
        call.setTotalTokens(nullToZero(result.getTotalTokens()));
        call.setCostAmount(cost == null ? BigDecimal.ZERO : cost);
        call.setLatencyMs(nullToZero(result.getLatencyMs()));
        call.setSuccess(success);
        call.setErrorMessage(errorMessage);
        runtimeLlmCallMapper.insert(call);
        usageCostService.recordActualUsage(run, context.getProvider(), context.getModel(), call.getTotalTokens(), call.getCostAmount(), success, call.getLatencyMs());
    }

    /**
     * 汇总工作流运行下所有 LLM 调用成本。
     *
     * @param runId Runtime 运行 ID
     * @return 成本总额
     */
    private BigDecimal sumWorkflowCost(String runId) {
        return runtimeLlmCallMapper.selectList(new LambdaQueryWrapper<RuntimeLlmCallEntity>()
                        .eq(RuntimeLlmCallEntity::getRunId, runId))
                .stream()
                .map(RuntimeLlmCallEntity::getCostAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建 LLM 消息列表。
     */
    private List<ChatMessage> buildMessages(AgentEntity agent, Map<String, Object> config, Map<String, Object> context) {
        List<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = stringValue(config.get("systemPrompt"),
                agent != null && StringUtils.hasText(agent.getSystemPrompt())
                        ? agent.getSystemPrompt()
                        : "你是 OpenAgentFlow-Java 工作流中的 LLM 节点，请根据上下文给出清晰、准确的中文回答。");
        messages.add(new ChatMessage("system", renderTemplate(systemPrompt, context)));
        List<KnowledgeSource> sources = sourcesFromContext(context);
        if (!sources.isEmpty()) {
            messages.add(new ChatMessage("system", buildRagPrompt(sources)));
        }
        String promptTemplate = stringValue(config.get("promptTemplate"), "{{input}}");
        messages.add(new ChatMessage("user", renderTemplate(promptTemplate, context)));
        return messages;
    }

    /**
     * 构建 RAG 引用提示词。
     */
    private String buildRagPrompt(List<KnowledgeSource> sources) {
        StringBuilder builder = new StringBuilder("以下是 RAG 节点检索到的参考来源，请优先依据来源回答，并在需要时说明依据。\n");
        for (int index = 0; index < sources.size(); index++) {
            KnowledgeSource source = sources.get(index);
            builder.append("\n[来源").append(index + 1).append("] ")
                    .append(source.getDocumentName()).append(" / 分片 ").append(source.getChunkNo())
                    .append("\n").append(source.getQuoteText());
        }
        return builder.toString();
    }

    /**
     * 解析模型。
     */
    private ModelConfigEntity resolveModel(Map<String, Object> config, AgentEntity agent) {
        String modelId = stringValue(config.get("modelId"), agent == null ? "" : agent.getModelId());
        if (StringUtils.hasText(modelId)) {
            return modelProviderService.requireModel(modelId);
        }
        return modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getModelType, "chat")
                        .eq(ModelConfigEntity::getStatus, "enabled")
                        .orderByDesc(ModelConfigEntity::getIsDefault)
                        .orderByDesc(ModelConfigEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "请先配置可用的 Chat 模型"));
    }

    /**
     * 解析 Agent。
     */
    private AgentEntity resolveAgent(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return null;
        }
        AgentEntity agent = agentMapper.selectById(agentId);
        if (agent == null || agent.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
        }
        agentAccessService.assertCanView(agent);
        return agent;
    }

    /**
     * 查找当前发布版本。
     */
    private WorkflowVersionEntity resolveVersion(WorkflowDefinitionEntity workflow) {
        if (StringUtils.hasText(workflow.getPublishedVersion())) {
            WorkflowVersionEntity version = workflowVersionMapper.selectOne(new LambdaQueryWrapper<WorkflowVersionEntity>()
                    .eq(WorkflowVersionEntity::getWorkflowId, workflow.getId())
                    .eq(WorkflowVersionEntity::getVersionNo, workflow.getPublishedVersion())
                    .last("limit 1"));
            if (version != null) {
                return version;
            }
        }
        return workflowVersionMapper.selectList(new LambdaQueryWrapper<WorkflowVersionEntity>()
                        .eq(WorkflowVersionEntity::getWorkflowId, workflow.getId())
                        .orderByDesc(WorkflowVersionEntity::getCreatedAt)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询启用节点。
     */
    private List<WorkflowNodeEntity> listNodes(String workflowId) {
        return workflowNodeMapper.selectList(new LambdaQueryWrapper<WorkflowNodeEntity>()
                        .eq(WorkflowNodeEntity::getWorkflowId, workflowId)
                        .eq(WorkflowNodeEntity::getEnabled, true)
                        .orderByAsc(WorkflowNodeEntity::getCreatedAt))
                .stream()
                .sorted(Comparator.comparing(item -> item.getCreatedAt() == null ? LocalDateTime.MIN : item.getCreatedAt()))
                .toList();
    }

    /**
     * 查询连线。
     */
    private List<WorkflowEdgeEntity> listEdges(String workflowId) {
        return workflowEdgeMapper.selectList(new LambdaQueryWrapper<WorkflowEdgeEntity>()
                        .eq(WorkflowEdgeEntity::getWorkflowId, workflowId)
                        .orderByAsc(WorkflowEdgeEntity::getCreatedAt))
                .stream()
                .sorted(Comparator.comparing(item -> item.getCreatedAt() == null ? LocalDateTime.MIN : item.getCreatedAt()))
                .toList();
    }

    /**
     * 查找开始节点。
     */
    private WorkflowNodeEntity findStartNode(List<WorkflowNodeEntity> nodes) {
        return findStartNode(nodes, null);
    }

    /**
     * 查找起始节点，调试模式允许从指定节点开始。
     */
    private WorkflowNodeEntity findStartNode(List<WorkflowNodeEntity> nodes, String startNodeKey) {
        if (StringUtils.hasText(startNodeKey)) {
            return nodes.stream()
                    .filter(node -> startNodeKey.equals(node.getNodeKey()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("WORKFLOW_START_NODE_NOT_FOUND", "调试起始节点不存在"));
        }
        return nodes.stream()
                .filter(node -> "START".equalsIgnoreCase(node.getNodeType()))
                .findFirst()
                .orElse(nodes.get(0));
    }

    /**
     * 判断条件表达式是否命中。
     */
    private boolean matches(String expression, Map<String, Object> context) {
        String expr = safeText(expression).trim();
        if (!StringUtils.hasText(expr) || "always".equalsIgnoreCase(expr) || "default".equalsIgnoreCase(expr)) {
            return true;
        }
        if (expr.contains("&&")) {
            for (String part : expr.split("&&")) {
                if (!matches(part.trim(), context)) {
                    return false;
                }
            }
            return true;
        }
        if (expr.contains("||")) {
            for (String part : expr.split("\\|\\|")) {
                if (matches(part.trim(), context)) {
                    return true;
                }
            }
            return false;
        }
        if ("success".equalsIgnoreCase(expr)) {
            return !context.containsKey("error");
        }
        String lastOutput = String.valueOf(context.getOrDefault("lastOutput", ""));
        if (expr.toLowerCase(Locale.ROOT).startsWith("contains:")) {
            return lastOutput.contains(expr.substring("contains:".length()));
        }
        if (expr.toLowerCase(Locale.ROOT).startsWith("equals:")) {
            return lastOutput.equals(expr.substring("equals:".length()));
        }
        if (expr.toLowerCase(Locale.ROOT).startsWith("json:")) {
            String path = expr.substring("json:".length());
            return pathValue(context, path) != null;
        }
        for (String operator : List.of(">=", "<=", "==", "!=", ">", "<")) {
            int index = expr.indexOf(operator);
            if (index > 0) {
                Object left = pathValue(context, expr.substring(0, index).trim());
                String right = expr.substring(index + operator.length()).trim().replace("\"", "");
                return compare(left, right, operator);
            }
        }
        if ("true".equalsIgnoreCase(expr) || "false".equalsIgnoreCase(expr)) {
            return Boolean.parseBoolean(expr);
        }
        return Boolean.TRUE.equals(context.get("conditionMatched"));
    }

    /**
     * 渲染字符串模板。
     */
    private String renderTemplate(String template, Map<String, Object> context) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template == null ? "" : template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = pathValue(context, matcher.group(1));
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value instanceof String ? (String) value : toJson(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * 渲染参数 Map。
     */
    private Map<String, Object> renderMap(Map<String, Object> source, Map<String, Object> context) {
        Map<String, Object> rendered = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value instanceof String text) {
                rendered.put(key, renderTemplate(text, context));
            } else if (value instanceof Map<?, ?> map) {
                rendered.put(key, renderMap(parseMap(map), context));
            } else if (value instanceof List<?> list) {
                rendered.put(key, list.stream()
                        .map(item -> item instanceof String text ? renderTemplate(text, context) : item)
                        .toList());
            } else {
                rendered.put(key, value);
            }
        });
        return rendered;
    }

    /**
     * 从上下文中读取 RAG 来源。
     */
    @SuppressWarnings("unchecked")
    private List<KnowledgeSource> sourcesFromContext(Map<String, Object> context) {
        Object sources = context.get("sources");
        if (sources instanceof List<?> list && (list.isEmpty() || list.get(0) instanceof KnowledgeSource)) {
            return (List<KnowledgeSource>) list;
        }
        return List.of();
    }

    /**
     * 转换运行结果。
     */
    private WorkflowDtos.RunResult toRunResult(WorkflowRunEntity workflowRun,
                                               RuntimeRunEntity runtimeRun,
                                               Map<String, Object> context,
                                               String output,
                                               List<WorkflowDtos.StepResult> steps,
                                               int totalTokens,
                                               String errorMessage) {
        WorkflowDtos.RunResult result = new WorkflowDtos.RunResult();
        result.setWorkflowRunId(workflowRun.getId());
        result.setRuntimeRunId(runtimeRun.getId());
        result.setStatus(StringUtils.hasText(workflowRun.getStatus()) ? workflowRun.getStatus() : (errorMessage == null ? "SUCCESS" : "FAILED"));
        result.setOutputText(safeText(output));
        result.setContext(context);
        result.setSteps(steps);
        result.setTotalTokens(totalTokens);
        result.setLatencyMs(runtimeRun.getLatencyMs());
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 解析 JSON Map。
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
     * 解析对象 Map。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        if (value instanceof String text) {
            return parseMap(text);
        }
        return new LinkedHashMap<>();
    }

    /**
     * 读取字符串配置。
     */
    /**
     * 根据点路径读取上下文值，支持 a.b[0] 这类表达式。
     */
    private Object pathValue(Map<String, Object> context, String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        if (context.containsKey(path)) {
            return context.get(path);
        }
        Object current = context;
        for (String rawPart : path.split("\\.")) {
            String part = rawPart.trim();
            if (!StringUtils.hasText(part)) {
                continue;
            }
            int arrayIndex = -1;
            if (part.contains("[") && part.endsWith("]")) {
                int start = part.indexOf('[');
                arrayIndex = numberValue(part.substring(start + 1, part.length() - 1), -1D).intValue();
                part = part.substring(0, start);
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
            if (arrayIndex >= 0) {
                if (current instanceof List<?> list && arrayIndex < list.size()) {
                    current = list.get(arrayIndex);
                } else {
                    return null;
                }
            }
        }
        return current;
    }

    /**
     * 比较条件表达式两侧的值。
     */
    private boolean compare(Object left, String right, String operator) {
        if (left == null) {
            return "!=".equals(operator);
        }
        try {
            double leftNumber = Double.parseDouble(String.valueOf(left));
            double rightNumber = Double.parseDouble(right);
            return switch (operator) {
                case ">" -> leftNumber > rightNumber;
                case ">=" -> leftNumber >= rightNumber;
                case "<" -> leftNumber < rightNumber;
                case "<=" -> leftNumber <= rightNumber;
                case "==" -> leftNumber == rightNumber;
                case "!=" -> leftNumber != rightNumber;
                default -> false;
            };
        } catch (Exception ignored) {
            return switch (operator) {
                case "==" -> String.valueOf(left).equals(right);
                case "!=" -> !String.valueOf(left).equals(right);
                default -> false;
            };
        }
    }

    /**
     * 检查工作流或节点预算，超过预算时阻断运行。
     */
    private void enforceBudget(WorkflowDefinitionEntity workflow,
                               RuntimeRunEntity runtimeRun,
                               WorkflowNodeEntity node,
                               Map<String, Object> context,
                               int totalTokens) {
        Map<String, Object> nodeConfig = parseMap(node.getConfigJson());
        Map<String, Object> graph = parseMap(workflow.getGraphJson());
        Map<String, Object> workflowPolicy = parseMap(graph.get("executionPolicy"));
        int budgetTokens = numberValue(firstNonNull(nodeConfig.get("budgetTokens"), workflowPolicy.get("budgetTokens")), 0D).intValue();
        if (budgetTokens > 0 && totalTokens > budgetTokens) {
            writePolicyHit(runtimeRun, node, "budget", "block", Map.of("budgetTokens", budgetTokens, "actualTokens", totalTokens), "工作流 Token 超过预算");
            throw new BusinessException("WORKFLOW_BUDGET_EXCEEDED", "工作流 Token 超过预算");
        }
        String sandboxLevel = stringValue(firstNonNull(nodeConfig.get("sandboxLevel"), workflowPolicy.get("sandboxLevel")), "low");
        if ("high".equalsIgnoreCase(sandboxLevel) && "TOOL".equalsIgnoreCase(node.getNodeType()) && !Boolean.TRUE.equals(nodeConfig.get("humanConfirmed"))) {
            writePolicyHit(runtimeRun, node, "sandbox", "warn", nodeConfig, "高风险工具节点建议增加人工确认");
            context.put("sandboxWarning", "高风险工具节点建议增加人工确认");
        }
    }

    /**
     * 写入工作流策略命中日志。
     */
    private void writePolicyHit(RuntimeRunEntity run,
                                WorkflowNodeEntity node,
                                String policyType,
                                String hitResult,
                                Object policySnapshot,
                                String message) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO workflow_policy_hit_log
                      (id, workflow_id, workflow_run_id, node_key, policy_type, hit_result, policy_snapshot, message)
                    VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                    """,
                    newId(),
                    run.getWorkflowId(),
                    run.getWorkflowRunId(),
                    node.getNodeKey(),
                    policyType,
                    hitResult,
                    toJson(policySnapshot),
                    message);
        } catch (Exception ignored) {
            // 策略日志不能影响主流程执行。
        }
    }

    /**
     * 返回第一个非空值。
     */
    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    /**
     * 安静等待重试间隔。
     */
    private void sleepQuietly(int millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(Math.min(millis, 5000));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 判断节点是否会触发真实外部调用。
     */
    private boolean isExternalNode(String nodeType) {
        return List.of("LLM", "RAG", "TOOL", "PLUGIN", "API", "WEBHOOK", "NOTIFY").contains(safeText(nodeType).toUpperCase(Locale.ROOT));
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    /**
     * 读取数字配置。
     */
    private Double numberValue(Object value, Double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (Exception exception) {
            return fallback;
        }
    }

    /**
     * 空字符串兜底。
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 空整数转 0。
     */
    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 转换 JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 生成 UUID。
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 节点执行的内部结果。
     */
    private record NodeExecutionResult(Object output,
                                       WorkflowDtos.StepResult stepResult,
                                       String status,
                                       String errorMessage,
                                       int promptTokens,
                                       int completionTokens,
                                       int totalTokens,
                                       BigDecimal costAmount) {
        /**
         * 构造成功结果。
         */
        private static NodeExecutionResult success(WorkflowNodeEntity node,
                                                   Object output,
                                                   int promptTokens,
                                                   int completionTokens,
                                                   int totalTokens) {
            return success(node, output, promptTokens, completionTokens, totalTokens, BigDecimal.ZERO);
        }

        /**
         * 构造带成本的成功结果。
         */
        private static NodeExecutionResult success(WorkflowNodeEntity node,
                                                   Object output,
                                                   int promptTokens,
                                                   int completionTokens,
                                                   int totalTokens,
                                                   BigDecimal costAmount) {
            WorkflowDtos.StepResult step = new WorkflowDtos.StepResult();
            step.setNodeKey(node.getNodeKey());
            step.setNodeName(node.getNodeName());
            step.setNodeType(node.getNodeType());
            step.setStatus("SUCCESS");
            step.setOutput(output);
            step.setTokenCount(totalTokens);
            return new NodeExecutionResult(output, step, "SUCCESS", null, promptTokens, completionTokens, totalTokens, costAmount == null ? BigDecimal.ZERO : costAmount);
        }

        /**
         * 构造等待人工确认的结果。
         */
        private static NodeExecutionResult waiting(WorkflowNodeEntity node, Object output) {
            WorkflowDtos.StepResult step = new WorkflowDtos.StepResult();
            step.setNodeKey(node.getNodeKey());
            step.setNodeName(node.getNodeName());
            step.setNodeType(node.getNodeType());
            step.setStatus("WAITING");
            step.setOutput(output);
            step.setTokenCount(0);
            return new NodeExecutionResult(output, step, "WAITING", null, 0, 0, 0, BigDecimal.ZERO);
        }

        /**
         * 构造失败但被策略接管的结果。
         */
        private static NodeExecutionResult failure(WorkflowNodeEntity node, Object output, String errorMessage, String status) {
            WorkflowDtos.StepResult step = new WorkflowDtos.StepResult();
            step.setNodeKey(node.getNodeKey());
            step.setNodeName(node.getNodeName());
            step.setNodeType(node.getNodeType());
            step.setStatus(status);
            step.setOutput(output);
            step.setTokenCount(0);
            step.setErrorMessage(errorMessage);
            return new NodeExecutionResult(output, step, status, errorMessage, 0, 0, 0, BigDecimal.ZERO);
        }
    }
}
