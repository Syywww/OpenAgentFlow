package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.agent.AgentDetail;
import com.openagentflow.domain.agent.AgentPublishRequest;
import com.openagentflow.domain.agent.AgentRequest;
import com.openagentflow.domain.agent.AgentSummary;
import com.openagentflow.domain.chat.ChatCompletionRequest;
import com.openagentflow.domain.chat.ChatCompletionResponse;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.IamUserEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.WorkflowDefinitionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.IamUserMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 应用服务。
 */
@Service
public class AgentService {

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** 用户 Mapper，用于补充所有者展示名称。 */
    private final IamUserMapper iamUserMapper;

    /** 资源级权限服务。 */
    private final AgentAccessService agentAccessService;

    /** 聊天调试服务。 */
    private final ChatService chatService;

    /** 工作流定义服务，用于判断 Agent 是否绑定工作流。 */
    private final WorkflowService workflowService;

    /** 工作流执行服务，用于把 Agent 运行切换到编排链路。 */
    private final WorkflowExecutionService workflowExecutionService;

    /** Agent 历史会话服务。 */
    private final AgentSessionService agentSessionService;

    /** 工作空间治理服务，用于资源归属和空间名称展示。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    /** JDBC 工具，用于统计绑定数量和写入版本快照。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** Agent生产发布质量门禁。 */
    private final ReleaseGateService releaseGateService;

    public AgentService(AgentMapper agentMapper,
                        ModelConfigMapper modelConfigMapper,
                        IamUserMapper iamUserMapper,
                        AgentAccessService agentAccessService,
                        ChatService chatService,
                        WorkflowService workflowService,
                        WorkflowExecutionService workflowExecutionService,
                        AgentSessionService agentSessionService,
                        WorkspaceGovernanceService workspaceGovernanceService,
                        JdbcTemplate jdbcTemplate,
                        ObjectMapper objectMapper,
                        ReleaseGateService releaseGateService) {
        this.agentMapper = agentMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.iamUserMapper = iamUserMapper;
        this.agentAccessService = agentAccessService;
        this.chatService = chatService;
        this.workflowService = workflowService;
        this.workflowExecutionService = workflowExecutionService;
        this.agentSessionService = agentSessionService;
        this.workspaceGovernanceService = workspaceGovernanceService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.releaseGateService = releaseGateService;
    }

    /**
     * 查询当前用户可见的 Agent 摘要列表。
     *
     * @return Agent 摘要列表
     */
    public List<AgentSummary> listAgents() {
        // 先按更新时间读取最近记录，再在内存中做资源级过滤；后续可替换为带 ACL 的分页 SQL。
        return agentMapper.selectList(new LambdaQueryWrapper<AgentEntity>()
                        .isNull(AgentEntity::getDeletedAt)
                        .orderByDesc(AgentEntity::getUpdatedAt)
                        .last("limit 100"))
                .stream()
                .filter(agentAccessService::canView)
                .map(this::toSummary)
                .toList();
    }

    /**
     * 查询 Agent 详情。
     *
     * @param id Agent ID
     * @return Agent 详情
     */
    public AgentDetail getAgent(String id) {
        AgentEntity entity = requireAgent(id);
        agentAccessService.assertCanView(entity);
        return toDetail(entity);
    }

    /**
     * 创建 Agent。
     *
     * @param request 保存请求
     * @return 创建后的 Agent 详情
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentDetail createAgent(AgentRequest request) {
        String userId = currentUserIdOrThrow();
        AgentEntity entity = new AgentEntity();
        entity.setId(newId());
        entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "agent", entity.getId(), userId));
        fillEntity(entity, request, true);
        entity.setOwnerUserId(userId);
        entity.setCreatedBy(userId);
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "draft");
        entity.setPublishedVersion(null);
        entity.setVersion(0L);
        agentMapper.insert(entity);
        syncAgentPromptBinding(entity);
        agentAccessService.grantOwner(entity.getId(), userId);
        return toDetail(entity);
    }

    /**
     * 更新 Agent。
     *
     * @param id Agent ID
     * @param request 保存请求
     * @return 更新后的 Agent 详情
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentDetail updateAgent(String id, AgentRequest request) {
        AgentEntity entity = requireAgent(id);
        agentAccessService.assertCanManage(entity);
        fillEntity(entity, request, false);
        agentMapper.updateById(entity);
        syncAgentPromptBinding(entity);
        return getAgent(entity.getId());
    }

    /**
     * 发布 Agent 并生成版本快照。
     *
     * @param id Agent ID
     * @param request 发布请求
     * @return 发布后的 Agent 详情
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentDetail publishAgent(String id, AgentPublishRequest request) {
        AgentEntity entity = requireAgent(id);
        agentAccessService.assertCanManage(entity);
        String versionNo = StringUtils.hasText(request.getVersionNo())
                ? request.getVersionNo()
                : "v" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        releaseGateService.assertCanRelease("agent", entity.getId(), entity.getWorkspaceId(), versionNo);
        entity.setStatus("published");
        entity.setPublishedVersion(versionNo);
        agentMapper.updateById(entity);

        // 发布时保存完整 Agent 快照，后续可用于回滚、审计和线上版本对比。
        jdbcTemplate.update(
                "INSERT INTO agent_version (id, agent_id, version_no, snapshot, publish_note, status, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, 'published', ?)",
                newId(),
                entity.getId(),
                versionNo,
                toJson(toSnapshot(entity)),
                request.getPublishNote(),
                agentAccessService.currentUserId());
        return getAgent(entity.getId());
    }

    /**
     * 复制 Agent。
     *
     * @param id 来源 Agent ID
     * @return 新 Agent 详情
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentDetail copyAgent(String id) {
        String userId = currentUserIdOrThrow();
        AgentEntity source = requireAgent(id);
        agentAccessService.assertCanView(source);

        AgentEntity copy = new AgentEntity();
        copy.setId(newId());
        copy.setAgentCode(uniqueAgentCode(source.getAgentCode() + "-copy"));
        copy.setAgentName(source.getAgentName() + " 副本");
        copy.setAvatarUrl(source.getAvatarUrl());
        copy.setCategory(source.getCategory());
        copy.setDescription(source.getDescription());
        copy.setAgentType(source.getAgentType());
        copy.setModelId(source.getModelId());
        copy.setWorkspaceId(workspaceGovernanceService.attachResource(source.getWorkspaceId(), "agent", copy.getId(), userId));
        copy.setSystemPromptTemplateId(source.getSystemPromptTemplateId());
        copy.setSystemPromptVersionId(source.getSystemPromptVersionId());
        copy.setPromptBindingMode(source.getPromptBindingMode());
        copy.setPromptVariables(source.getPromptVariables());
        copy.setSystemPrompt(source.getSystemPrompt());
        copy.setModelParams(source.getModelParams());
        copy.setMemoryStrategy(source.getMemoryStrategy());
        copy.setVisibility("private");
        copy.setStatus("draft");
        copy.setOwnerUserId(userId);
        copy.setCreatedBy(userId);
        copy.setVersion(0L);
        agentMapper.insert(copy);
        syncAgentPromptBinding(copy);
        agentAccessService.grantOwner(copy.getId(), userId);
        return toDetail(copy);
    }

    /**
     * 软删除 Agent。
     *
     * @param id Agent ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(String id) {
        AgentEntity entity = requireAgent(id);
        agentAccessService.assertCanManage(entity);
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        agentMapper.updateById(entity);
        jdbcTemplate.update("UPDATE prompt_binding SET enabled=0,updated_at=NOW(3) WHERE resource_type='agent' AND resource_id=?", entity.getId());
    }

    /**
     * 通过 Agent 运行一次非流式调试。
     *
     * @param id Agent ID
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatCompletionResponse runAgent(String id, ChatCompletionRequest request) {
        AgentEntity entity = requireAgent(id);
        agentAccessService.assertCanView(entity);
        request.setAgentId(entity.getId());
        WorkflowDefinitionEntity workflow = workflowService.findEnabledWorkflowForAgent(entity.getId());
        if (workflow != null && shouldRunBoundWorkflow(workflow, request.getInput())) {
            String sessionId = ensureWorkflowSession(entity, request);
            WorkflowDtos.RunResult result = workflowExecutionService.runWorkflow(workflow.getId(), toWorkflowRunRequest(entity, request), "agent_run");
            agentSessionService.appendAssistantMessage(sessionId, safeText(result.getOutputText()), result.getTotalTokens(), Map.of(
                    "runId", safeText(result.getRuntimeRunId()),
                    "workflowRunId", safeText(result.getWorkflowRunId()),
                    "status", safeText(result.getStatus())
            ));
            return toWorkflowChatResponse(result, sessionId);
        }
        return chatService.complete(request);
    }

    /**
     * 通过 Agent 运行一次 SSE 流式调试。
     *
     * @param id Agent ID
     * @param request 聊天请求
     * @return SSE 发射器
     */
    public SseEmitter runAgentStream(String id, ChatCompletionRequest request) {
        AgentEntity entity = requireAgent(id);
        agentAccessService.assertCanView(entity);
        request.setAgentId(entity.getId());
        WorkflowDefinitionEntity workflow = workflowService.findEnabledWorkflowForAgent(entity.getId());
        if (workflow != null && shouldRunBoundWorkflow(workflow, request.getInput())) {
            String sessionId = ensureWorkflowSession(entity, request);
            return runWorkflowStream(entity, workflow, request, sessionId);
        }
        return chatService.completeStream(request);
    }

    /**
     * 判断本轮 Agent 输入是否应该触发已绑定工作流。
     *
     * <p>客服演示工作流包含固定订单工具节点，如果所有输入都执行工作流，会导致问候、天气、产品咨询也先查订单。
     * 这里对订单演示类工作流增加路由门控：只有识别到真实订单号和订单实时查询意图时才进入工作流。</p>
     *
     * @param workflow 绑定工作流
     * @param input 用户输入
     * @return 是否运行绑定工作流
     */
    private boolean shouldRunBoundWorkflow(WorkflowDefinitionEntity workflow, String input) {
        if (!isOrderDemoWorkflow(workflow)) {
            return true;
        }
        return hasOrderRuntimeIntent(input);
    }

    /**
     * 判断工作流是否属于订单演示查询链路。
     *
     * @param workflow 工作流实体
     * @return 是否订单演示工作流
     */
    private boolean isOrderDemoWorkflow(WorkflowDefinitionEntity workflow) {
        if (workflow == null) {
            return false;
        }
        String text = normalizeText(safeText(workflow.getWorkflowCode()) + " "
                + safeText(workflow.getWorkflowName()) + " "
                + safeText(workflow.getDescription()) + " "
                + safeText(workflow.getGraphJson()));
        return text.contains("demo_order_status_rest")
                || text.contains("demo_readonly_order_sql")
                || text.contains("订单工具")
                || text.contains("订单状态查询");
    }

    /**
     * 判断用户输入是否具备订单实时查询意图。
     *
     * @param input 用户输入
     * @return 是否可以进入订单工作流
     */
    private boolean hasOrderRuntimeIntent(String input) {
        // Agent 路由与订单工具共用同一套意图策略，防止同一句话在不同阶段得到不同判断。
        return OrderQueryIntentPolicy.shouldRunOrderWorkflow(input);
    }

    /**
     * 为工作流 Agent 运行绑定历史会话，并先保存用户消息。
     *
     * @param agent Agent 实体
     * @param request 聊天请求
     * @return 会话 ID
     */
    private String ensureWorkflowSession(AgentEntity agent, ChatCompletionRequest request) {
        var session = agentSessionService.ensureSession(agent, request.getSessionId(), request.getInput());
        if (session == null) {
            return "";
        }
        request.setSessionId(session.getId());
        agentSessionService.appendUserMessage(session.getId(), request.getInput(), "");
        return session.getId();
    }

    /**
     * 将 Agent 调试请求转换为工作流运行请求。
     *
     * @param agent Agent 实体
     * @param request 聊天请求
     * @return 工作流运行请求
     */
    private WorkflowDtos.RunRequest toWorkflowRunRequest(AgentEntity agent, ChatCompletionRequest request) {
        WorkflowDtos.RunRequest runRequest = new WorkflowDtos.RunRequest();
        runRequest.setAgentId(agent.getId());
        runRequest.setInput(request.getInput());
        runRequest.setVariables(Map.of(
                "agentId", agent.getId(),
                "agentName", safeText(agent.getAgentName()),
                "modelId", safeText(request.getModelId()),
                "sessionId", safeText(request.getSessionId())
        ));
        return runRequest;
    }

    /**
     * 将工作流运行结果转换为调试台兼容的聊天响应。
     *
     * @param result 工作流运行结果
     * @return 聊天响应
     */
    private ChatCompletionResponse toWorkflowChatResponse(WorkflowDtos.RunResult result, String sessionId) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setRunId(result.getRuntimeRunId());
        response.setSessionId(sessionId);
        response.setContent(result.getOutputText());
        response.setStatus(result.getStatus());
        response.setPromptTokens(0);
        response.setCompletionTokens(result.getTotalTokens());
        response.setTotalTokens(result.getTotalTokens());
        response.setLatencyMs(result.getLatencyMs());
        response.setErrorMessage(result.getErrorMessage());
        response.setSources(List.of());
        response.setToolResults(List.of(Map.of(
                "workflowRunId", safeText(result.getWorkflowRunId()),
                "status", safeText(result.getStatus())
        )));
        return response;
    }

    /**
     * 通过 SSE 返回工作流运行结果。
     *
     * @param agent Agent 实体
     * @param workflow 工作流实体
     * @param request 聊天请求
     * @return SSE 发射器
     */
    private SseEmitter runWorkflowStream(AgentEntity agent,
                                         WorkflowDefinitionEntity workflow,
                                         ChatCompletionRequest request,
                                         String sessionId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> {
            try {
                // 工作流流式运行会切换到异步线程，手动恢复登录上下文，避免运行引擎读取当前用户时误判未登录。
                SecurityContextHolder.getContext().setAuthentication(authentication);
                WorkflowDtos.RunResult result = workflowExecutionService.runWorkflow(workflow.getId(), toWorkflowRunRequest(agent, request), "agent_run_stream");
                agentSessionService.appendAssistantMessage(sessionId, safeText(result.getOutputText()), result.getTotalTokens(), Map.of(
                        "runId", safeText(result.getRuntimeRunId()),
                        "workflowRunId", safeText(result.getWorkflowRunId()),
                        "status", safeText(result.getStatus())
                ));
                sendSse(emitter, "meta", Map.of(
                        "runId", safeText(result.getRuntimeRunId()),
                        "sessionId", safeText(sessionId),
                        "workflowRunId", safeText(result.getWorkflowRunId()),
                        "workflowName", safeText(workflow.getWorkflowName())
                ));
                if (StringUtils.hasText(result.getOutputText())) {
                    sendSse(emitter, "delta", Map.of("content", safeText(result.getOutputText())));
                }
                if ("FAILED".equalsIgnoreCase(result.getStatus())) {
                    sendSse(emitter, "error", Map.of("message", safeText(result.getErrorMessage())));
                }
                sendSse(emitter, "done", Map.of(
                        "runId", safeText(result.getRuntimeRunId()),
                        "sessionId", safeText(sessionId),
                        "status", safeText(result.getStatus()),
                        "latencyMs", result.getLatencyMs() == null ? 0 : result.getLatencyMs(),
                        "promptTokens", 0,
                        "completionTokens", result.getTotalTokens() == null ? 0 : result.getTotalTokens(),
                        "totalTokens", result.getTotalTokens() == null ? 0 : result.getTotalTokens(),
                        "sources", List.of(),
                        "toolResults", List.of(Map.of("workflowRunId", safeText(result.getWorkflowRunId())))
                ));
                emitter.complete();
            } catch (Exception exception) {
                agentSessionService.appendAssistantMessage(sessionId, "工作流运行失败：" + safeText(exception.getMessage()), 0, Map.of(
                        "status", "FAILED",
                        "workflowId", safeText(workflow.getId())
                ));
                sendSse(emitter, "error", Map.of("message", safeText(exception.getMessage())));
                emitter.complete();
            } finally {
                // 清理异步线程上下文，避免线程复用时串到其他用户。
                SecurityContextHolder.clearContext();
            }
        });
        return emitter;
    }

    /**
     * 发送 SSE 事件。
     *
     * @param emitter SSE 发射器
     * @param name 事件名称
     * @param data 事件数据
     */
    private void sendSse(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(name)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            // 前端断开连接时不再继续抛出异常，避免后台异步线程产生无效错误。
        }
    }

    /**
     * 查询 Agent 实体，不存在或已删除时抛出业务异常。
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
     * 填充 Agent 实体。
     *
     * @param entity Agent 实体
     * @param request 保存请求
     * @param create 是否为创建场景
     */
    private void fillEntity(AgentEntity entity, AgentRequest request, boolean create) {
        String code = StringUtils.hasText(request.getAgentCode()) ? request.getAgentCode().trim() : slugify(request.getAgentName());
        entity.setAgentCode(create ? uniqueAgentCode(code) : code);
        entity.setAgentName(request.getAgentName().trim());
        entity.setAvatarUrl(request.getAvatarUrl());
        entity.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "通用");
        entity.setDescription(request.getDescription());
        entity.setAgentType(StringUtils.hasText(request.getAgentType()) ? request.getAgentType() : "chat_agent");
        entity.setModelId(request.getModelId());
        if (!create && StringUtils.hasText(request.getWorkspaceId())) {
            entity.setWorkspaceId(workspaceGovernanceService.attachResource(request.getWorkspaceId(), "agent", entity.getId(), entity.getOwnerUserId()));
        }
        entity.setSystemPromptTemplateId(request.getSystemPromptTemplateId());
        entity.setSystemPromptVersionId(request.getSystemPromptVersionId());
        String bindingMode = StringUtils.hasText(request.getPromptBindingMode())
                ? request.getPromptBindingMode().trim().toUpperCase(Locale.ROOT)
                : (StringUtils.hasText(request.getSystemPromptTemplateId()) ? "LOCKED" : "MANUAL");
        if (!List.of("MANUAL", "LOCKED", "FOLLOW_STABLE").contains(bindingMode)) {
            throw new BusinessException("PROMPT_BINDING_MODE_INVALID", "Prompt绑定模式仅支持MANUAL、LOCKED、FOLLOW_STABLE");
        }
        if ("FOLLOW_STABLE".equals(bindingMode)) {
            entity.setSystemPromptVersionId(null);
        }
        entity.setPromptBindingMode(bindingMode);
        entity.setPromptVariables(StringUtils.hasText(request.getPromptVariables()) ? request.getPromptVariables() : "{}");
        validatePromptBinding(entity);
        entity.setSystemPrompt(StringUtils.hasText(request.getSystemPrompt())
                ? request.getSystemPrompt()
                : "你是 OpenAgentFlow-Java 的智能体，请使用清晰、准确的中文回答用户。");
        entity.setModelParams(StringUtils.hasText(request.getModelParams()) ? request.getModelParams() : "{}");
        entity.setMemoryStrategy(StringUtils.hasText(request.getMemoryStrategy()) ? request.getMemoryStrategy() : "none");
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "private");
        if (!create && StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
    }

    /**
     * 转换为 Agent 摘要。
     *
     * @param entity Agent 实体
     * @return Agent 摘要
     */
    private AgentSummary toSummary(AgentEntity entity) {
        AgentSummary item = new AgentSummary();
        item.setId(entity.getId());
        item.setAgentCode(entity.getAgentCode());
        item.setAgentName(entity.getAgentName());
        item.setCategory(entity.getCategory());
        item.setDescription(entity.getDescription());
        item.setAgentType(entity.getAgentType());
        item.setModelId(entity.getModelId());
        item.setModelName(findModelName(entity.getModelId()));
        item.setWorkspaceId(entity.getWorkspaceId());
        item.setWorkspaceName(findWorkspaceName(entity.getWorkspaceId()));
        item.setKnowledgeCount(countBinding("agent_knowledge_binding", entity.getId()));
        item.setToolCount(countBinding("agent_tool_binding", entity.getId()));
        item.setStatus(entity.getStatus());
        item.setStatusLabel(statusLabel(entity.getStatus()));
        item.setVisibility(entity.getVisibility());
        item.setOwnerUserId(entity.getOwnerUserId());
        item.setOwnerName(findOwnerName(entity.getOwnerUserId()));
        item.setCanManage(agentAccessService.canManage(entity));
        item.setPublishedVersion(entity.getPublishedVersion());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    /**
     * 转换为 Agent 详情。
     *
     * @param entity Agent 实体
     * @return Agent 详情
     */
    private AgentDetail toDetail(AgentEntity entity) {
        AgentSummary summary = toSummary(entity);
        AgentDetail detail = new AgentDetail();
        detail.setId(summary.getId());
        detail.setAgentCode(summary.getAgentCode());
        detail.setAgentName(summary.getAgentName());
        detail.setCategory(summary.getCategory());
        detail.setDescription(summary.getDescription());
        detail.setAgentType(summary.getAgentType());
        detail.setModelId(summary.getModelId());
        detail.setModelName(summary.getModelName());
        detail.setWorkspaceId(summary.getWorkspaceId());
        detail.setWorkspaceName(summary.getWorkspaceName());
        detail.setKnowledgeCount(summary.getKnowledgeCount());
        detail.setToolCount(summary.getToolCount());
        detail.setStatus(summary.getStatus());
        detail.setStatusLabel(summary.getStatusLabel());
        detail.setVisibility(summary.getVisibility());
        detail.setOwnerUserId(summary.getOwnerUserId());
        detail.setOwnerName(summary.getOwnerName());
        detail.setCanManage(summary.getCanManage());
        detail.setPublishedVersion(summary.getPublishedVersion());
        detail.setCreatedAt(summary.getCreatedAt());
        detail.setUpdatedAt(summary.getUpdatedAt());
        detail.setAvatarUrl(entity.getAvatarUrl());
        detail.setSystemPromptTemplateId(entity.getSystemPromptTemplateId());
        detail.setSystemPromptVersionId(entity.getSystemPromptVersionId());
        detail.setPromptBindingMode(entity.getPromptBindingMode());
        detail.setPromptVariables(entity.getPromptVariables());
        detail.setSystemPrompt(entity.getSystemPrompt());
        detail.setModelParams(entity.getModelParams());
        detail.setMemoryStrategy(entity.getMemoryStrategy());
        detail.setCreatedBy(entity.getCreatedBy());
        detail.setDeletedAt(entity.getDeletedAt());
        detail.setVersion(entity.getVersion());
        return detail;
    }

    /**
     * 构建发布版本快照。
     *
     * @param entity Agent 实体
     * @return 快照对象
     */
    private Map<String, Object> toSnapshot(AgentEntity entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("agentCode", entity.getAgentCode());
        snapshot.put("agentName", entity.getAgentName());
        snapshot.put("category", entity.getCategory());
        snapshot.put("description", entity.getDescription());
        snapshot.put("agentType", entity.getAgentType());
        snapshot.put("modelId", entity.getModelId());
        snapshot.put("workspaceId", entity.getWorkspaceId());
        snapshot.put("systemPrompt", entity.getSystemPrompt());
        snapshot.put("systemPromptTemplateId", entity.getSystemPromptTemplateId());
        snapshot.put("systemPromptVersionId", entity.getSystemPromptVersionId());
        snapshot.put("promptBindingMode", entity.getPromptBindingMode());
        snapshot.put("promptVariables", entity.getPromptVariables());
        snapshot.put("modelParams", entity.getModelParams());
        snapshot.put("memoryStrategy", entity.getMemoryStrategy());
        snapshot.put("visibility", entity.getVisibility());
        return snapshot;
    }

    /**
     * 同步Agent的通用Prompt绑定关系。
     *
     * @param entity Agent实体
     */
    private void syncAgentPromptBinding(AgentEntity entity) {
        if (!StringUtils.hasText(entity.getSystemPromptTemplateId())) {
            jdbcTemplate.update("UPDATE prompt_binding SET enabled=0,updated_at=NOW(3) WHERE resource_type='agent' AND resource_id=? AND prompt_role='system'", entity.getId());
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO prompt_binding
                  (id,workspace_id,resource_type,resource_id,prompt_role,template_id,version_id,binding_mode,variable_values,enabled,created_by)
                VALUES (UUID(),?,'agent',?,'system',?,?,?,?,1,?)
                ON DUPLICATE KEY UPDATE template_id=VALUES(template_id),version_id=VALUES(version_id),
                  binding_mode=VALUES(binding_mode),variable_values=VALUES(variable_values),enabled=1,updated_at=NOW(3)
                """,
                entity.getWorkspaceId(), entity.getId(), entity.getSystemPromptTemplateId(), entity.getSystemPromptVersionId(),
                entity.getPromptBindingMode(), StringUtils.hasText(entity.getPromptVariables()) ? entity.getPromptVariables() : "{}",
                entity.getCreatedBy());
    }

    /**
     * 校验 Agent Prompt 模板、版本归属和变量 JSON，避免把不可运行配置保存到数据库。
     *
     * @param entity Agent实体
     */
    private void validatePromptBinding(AgentEntity entity) {
        try {
            if (!objectMapper.readTree(entity.getPromptVariables()).isObject()) {
                throw new BusinessException("PROMPT_VARIABLES_INVALID", "Agent Prompt变量必须是JSON对象");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("PROMPT_VARIABLES_INVALID", "Agent Prompt变量不是合法JSON");
        }
        if (!StringUtils.hasText(entity.getSystemPromptTemplateId())) {
            entity.setPromptBindingMode("MANUAL");
            entity.setSystemPromptVersionId(null);
            return;
        }
        Integer templateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM prompt_template WHERE id=? AND (workspace_id=? OR workspace_id IS NULL)",
                Integer.class, entity.getSystemPromptTemplateId(), entity.getWorkspaceId());
        if (templateCount == null || templateCount == 0) {
            throw new BusinessException("PROMPT_TEMPLATE_NOT_FOUND", "Agent绑定的Prompt模板不存在或不属于当前工作空间");
        }
        if ("LOCKED".equals(entity.getPromptBindingMode())) {
            if (!StringUtils.hasText(entity.getSystemPromptVersionId())) {
                throw new BusinessException("PROMPT_VERSION_REQUIRED", "锁定版本模式必须选择Prompt版本");
            }
            Integer versionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM prompt_template_version WHERE id=? AND template_id=?",
                    Integer.class, entity.getSystemPromptVersionId(), entity.getSystemPromptTemplateId());
            if (versionCount == null || versionCount == 0) {
                throw new BusinessException("PROMPT_VERSION_NOT_FOUND", "Agent锁定的Prompt版本不属于所选模板");
            }
        }
        if ("FOLLOW_STABLE".equals(entity.getPromptBindingMode())) {
            Integer stableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM prompt_template WHERE id=? AND stable_version_id IS NOT NULL",
                    Integer.class, entity.getSystemPromptTemplateId());
            if (stableCount == null || stableCount == 0) {
                throw new BusinessException("PROMPT_STABLE_VERSION_REQUIRED", "所选Prompt模板尚未建立稳定版本");
            }
        }
    }

    /**
     * 查询绑定数量。
     *
     * @param tableName 绑定表名
     * @param agentId Agent ID
     * @return 绑定数量
     */
    private Integer countBinding(String tableName, String agentId) {
        Number count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + tableName + " WHERE agent_id = ? AND enabled = 1",
                Number.class,
                agentId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询模型展示名称。
     *
     * @param modelId 模型 ID
     * @return 模型名称
     */
    private String findModelName(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return "";
        }
        ModelConfigEntity model = modelConfigMapper.selectById(modelId);
        return model == null ? "" : model.getModelName();
    }

    /**
     * 查询所有者展示名称。
     *
     * @param userId 用户 ID
     * @return 展示名称
     */
    private String findOwnerName(String userId) {
        if (!StringUtils.hasText(userId)) {
            return "";
        }
        IamUserEntity user = iamUserMapper.selectById(userId);
        return user == null ? "" : user.getDisplayName();
    }

    /**
     * 查询工作空间展示名称。
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间名称
     */
    private String findWorkspaceName(String workspaceId) {
        if (!StringUtils.hasText(workspaceId)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT workspace_name FROM oaf_workspace WHERE id = ? LIMIT 1",
                String.class,
                workspaceId);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 将状态转换为中文标签。
     *
     * @param status 状态编码
     * @return 中文标签
     */
    private String statusLabel(String status) {
        if ("published".equalsIgnoreCase(status)) {
            return "运行中";
        }
        if ("draft".equalsIgnoreCase(status)) {
            return "开发中";
        }
        if ("disabled".equalsIgnoreCase(status)) {
            return "已暂停";
        }
        if ("deleted".equalsIgnoreCase(status)) {
            return "已删除";
        }
        return StringUtils.hasText(status) ? status : "未知";
    }

    /**
     * 生成唯一 Agent 编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueAgentCode(String baseCode) {
        String normalized = StringUtils.hasText(baseCode) ? baseCode : "agent";
        String candidate = normalized;
        int suffix = 1;
        while (agentMapper.selectCount(new LambdaQueryWrapper<AgentEntity>()
                .eq(AgentEntity::getAgentCode, candidate)) > 0) {
            candidate = normalized + "-" + suffix++;
        }
        return candidate;
    }

    /**
     * 将名称转换为保守的编码。
     *
     * @param text 名称文本
     * @return 编码文本
     */
    private String slugify(String text) {
        String cleaned = text == null ? "agent" : text.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "agent";
    }

    /**
     * 获取当前用户 ID，未登录时抛出异常。
     *
     * @return 当前用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = agentAccessService.currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 安全文本兜底。
     *
     * @param text 原始文本
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 文本归一化，便于运行入口做轻量路由判断。
     *
     * @param text 原始文本
     * @return 小写文本
     */
    private String normalizeText(String text) {
        return safeText(text).trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 判断文本是否命中任一关键词。
     *
     * @param text 待检查文本
     * @param keywords 关键词列表
     * @return 是否命中
     */
    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 转换 JSON 字符串。
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
