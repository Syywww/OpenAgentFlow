package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.chat.ToolCallRequest;
import com.openagentflow.domain.knowledge.KnowledgeSource;
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
            WorkflowNodeEntity current = findStartNode(nodes);
            for (int guard = 0; current != null && guard < 100; guard++) {
                NodeExecutionResult nodeResult = executeNode(current, agent, runtimeRun, context);
                stepResults.add(nodeResult.stepResult());
                totalPromptTokens += nodeResult.promptTokens();
                totalCompletionTokens += nodeResult.completionTokens();
                totalTokens += nodeResult.totalTokens();
                if (nodeResult.output() != null) {
                    context.put("lastOutput", nodeResult.output());
                    finalOutput = String.valueOf(nodeResult.output());
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
                                            Map<String, Object> context) {
        WorkflowStepRunEntity stepRun = createWorkflowStepRun(runtimeRun.getWorkflowRunId(), runtimeRun.getWorkflowId(), node, context);
        RuntimeTraceStepEntity traceStep = createTraceStep(runtimeRun, node, context);
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            Map<String, Object> config = parseMap(node.getConfigJson());
            NodeExecutionResult result = switch (safeText(node.getNodeType()).toUpperCase(Locale.ROOT)) {
                case "START" -> executeStart(node, context);
                case "RAG" -> executeRag(node, agent, runtimeRun, context, config);
                case "TOOL" -> executeTool(node, agent, runtimeRun, traceStep, context, config);
                case "CONDITION" -> executeCondition(node, context, config);
                case "END" -> executeEnd(node, context);
                default -> executeLlm(node, agent, runtimeRun, traceStep, context, config);
            };
            finishStepSuccess(stepRun, traceStep, result, startedAt);
            return result;
        } catch (Exception exception) {
            finishStepFailure(stepRun, traceStep, exception, startedAt);
            throw exception;
        }
    }

    /**
     * 执行开始节点。
     */
    private NodeExecutionResult executeStart(WorkflowNodeEntity node, Map<String, Object> context) {
        return NodeExecutionResult.success(node, Map.of("input", context.get("input")), 0, 0, 0);
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
        ModelConfigEntity model = resolveModel(config, agent);
        ModelProviderEntity provider = modelProviderService.requireProviderByModel(model);
        ChatRunContext chatContext = new ChatRunContext();
        chatContext.setAgent(agent);
        chatContext.setModel(model);
        chatContext.setProvider(provider);
        chatContext.setApiKey(modelProviderService.findApiKeyValue(provider.getId()));
        chatContext.setSources(sourcesFromContext(context));
        chatContext.setMessages(buildMessages(agent, config, context));
        traceStep.setModelId(model.getId());
        traceStep.setPromptText(toJson(chatContext.getMessages()));

        Double temperature = numberValue(config.get("temperature"), 0.3D);
        Integer maxTokens = numberValue(config.get("maxTokens"), model.getMaxOutputTokens() == null ? 2048D : model.getMaxOutputTokens().doubleValue()).intValue();
        usageCostService.assertWithinQuota(runtimeRun.getUserId(), runtimeRun.getAgentId(), provider, model, chatContext.getMessages(), maxTokens);
        LlmCallResult result = openAiCompatibleClient.complete(chatContext, temperature, maxTokens);
        BigDecimal cost = usageCostService.calculateCost(model, nullToZero(result.getPromptTokens()), nullToZero(result.getCompletionTokens()));
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
     * 根据连线选择下一个节点。
     */
    private WorkflowNodeEntity nextNode(WorkflowNodeEntity current,
                                        List<WorkflowNodeEntity> nodes,
                                        List<WorkflowEdgeEntity> edges,
                                        Map<String, Object> context) {
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
                    .orElse(outgoing.get(0));
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
        stepRun.setStatus("SUCCESS");
        stepRun.setOutputPayload(toJson(result.output()));
        stepRun.setTokenCount(result.totalTokens());
        stepRun.setCostAmount(result.costAmount());
        stepRun.setLatencyMs(latencyMs);
        stepRun.setFinishedAt(LocalDateTime.now());
        workflowStepRunMapper.updateById(stepRun);

        traceStep.setStatus("SUCCESS");
        traceStep.setOutputPayload(toJson(result.output()));
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
        if (!StringUtils.hasText(expr) || "always".equalsIgnoreCase(expr)) {
            return true;
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
            Object value = context.getOrDefault(matcher.group(1), "");
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
        result.setStatus(errorMessage == null ? "SUCCESS" : "FAILED");
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
            return new NodeExecutionResult(output, step, promptTokens, completionTokens, totalTokens, costAmount == null ? BigDecimal.ZERO : costAmount);
        }
    }
}
