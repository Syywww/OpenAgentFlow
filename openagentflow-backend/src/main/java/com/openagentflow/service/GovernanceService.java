package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.governance.GovernanceDtos;
import com.openagentflow.entity.AuditOperationLogEntity;
import com.openagentflow.entity.McpCapabilityEntity;
import com.openagentflow.entity.RiskGovernanceEventEntity;
import com.openagentflow.entity.RuntimeGuardrailEventEntity;
import com.openagentflow.entity.ToolConfirmRequestEntity;
import com.openagentflow.entity.ToolDefinitionEntity;
import com.openagentflow.entity.ToolInvocationLogEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AuditOperationLogMapper;
import com.openagentflow.mapper.McpCapabilityMapper;
import com.openagentflow.mapper.RiskGovernanceEventMapper;
import com.openagentflow.mapper.RuntimeGuardrailEventMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审计与风险治理中心服务。
 */
@Service
public class GovernanceService {

    /** 审计操作日志 Mapper。 */
    private final AuditOperationLogMapper auditOperationLogMapper;

    /** 风险治理事件 Mapper。 */
    private final RiskGovernanceEventMapper riskGovernanceEventMapper;

    /** 工具定义 Mapper。 */
    private final ToolDefinitionMapper toolDefinitionMapper;

    /** MCP 能力 Mapper。 */
    private final McpCapabilityMapper mcpCapabilityMapper;

    /** 工具确认请求 Mapper。 */
    private final ToolConfirmRequestMapper toolConfirmRequestMapper;

    /** 工具调用日志 Mapper。 */
    private final ToolInvocationLogMapper toolInvocationLogMapper;

    /** 护栏事件 Mapper。 */
    private final RuntimeGuardrailEventMapper runtimeGuardrailEventMapper;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** JDBC 工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** 高风险工具一次性令牌服务。 */
    private final ToolApprovalTokenService toolApprovalTokenService;

    public GovernanceService(AuditOperationLogMapper auditOperationLogMapper,
                             RiskGovernanceEventMapper riskGovernanceEventMapper,
                             ToolDefinitionMapper toolDefinitionMapper,
                             McpCapabilityMapper mcpCapabilityMapper,
                             ToolConfirmRequestMapper toolConfirmRequestMapper,
                             ToolInvocationLogMapper toolInvocationLogMapper,
                             RuntimeGuardrailEventMapper runtimeGuardrailEventMapper,
                             ObjectMapper objectMapper,
                             JdbcTemplate jdbcTemplate,
                             ToolApprovalTokenService toolApprovalTokenService) {
        this.auditOperationLogMapper = auditOperationLogMapper;
        this.riskGovernanceEventMapper = riskGovernanceEventMapper;
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.mcpCapabilityMapper = mcpCapabilityMapper;
        this.toolConfirmRequestMapper = toolConfirmRequestMapper;
        this.toolInvocationLogMapper = toolInvocationLogMapper;
        this.runtimeGuardrailEventMapper = runtimeGuardrailEventMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.toolApprovalTokenService = toolApprovalTokenService;
    }

    /**
     * 查询治理中心概览。
     *
     * @return 概览指标
     */
    public GovernanceDtos.Overview overview() {
        assertCanView();
        syncRiskEvents();
        GovernanceDtos.Overview overview = new GovernanceDtos.Overview();
        overview.setAuditCount(auditOperationLogMapper.selectCount(new LambdaQueryWrapper<>()));
        overview.setFailedOperationCount(auditOperationLogMapper.selectCount(new LambdaQueryWrapper<AuditOperationLogEntity>()
                .eq(AuditOperationLogEntity::getSuccess, false)));
        overview.setOpenRiskCount(riskGovernanceEventMapper.selectCount(new LambdaQueryWrapper<RiskGovernanceEventEntity>()
                .in(RiskGovernanceEventEntity::getStatus, List.of("open", "reviewing"))));
        overview.setHighRiskCount(riskGovernanceEventMapper.selectCount(new LambdaQueryWrapper<RiskGovernanceEventEntity>()
                .eq(RiskGovernanceEventEntity::getRiskLevel, "high")));
        overview.setPendingConfirmationCount(toolConfirmRequestMapper.selectCount(new LambdaQueryWrapper<ToolConfirmRequestEntity>()
                .eq(ToolConfirmRequestEntity::getStatus, "pending")));
        overview.setGuardrailEventCount(runtimeGuardrailEventMapper.selectCount(new LambdaQueryWrapper<>()));
        overview.setHighRiskToolCount(toolDefinitionMapper.selectCount(new LambdaQueryWrapper<ToolDefinitionEntity>()
                .eq(ToolDefinitionEntity::getRiskLevel, "high")
                .isNull(ToolDefinitionEntity::getDeletedAt)));
        return overview;
    }

    /**
     * 分页查询审计日志。
     *
     * @param success 成功过滤
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 审计日志分页
     */
    public PageResult<GovernanceDtos.AuditItem> listAudits(Boolean success, String keyword, Integer pageNo, Integer pageSize) {
        assertCanView();
        int current = pageNo == null ? 1 : Math.max(1, pageNo);
        // 未指定每页大小时统一按产品规范返回 10 条，便于治理列表保持一致。
        int size = pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize));
        LambdaQueryWrapper<AuditOperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (success != null) {
            wrapper.eq(AuditOperationLogEntity::getSuccess, success);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(item -> item.like(AuditOperationLogEntity::getUsername, value)
                    .or()
                    .like(AuditOperationLogEntity::getRequestPath, value)
                    .or()
                    .like(AuditOperationLogEntity::getResourceType, value)
                    .or()
                    .like(AuditOperationLogEntity::getClientIp, value));
        }
        Long total = auditOperationLogMapper.selectCount(wrapper);
        wrapper.orderByDesc(AuditOperationLogEntity::getCreatedAt)
                .last("limit " + ((current - 1) * size) + "," + size);
        List<GovernanceDtos.AuditItem> records = auditOperationLogMapper.selectList(wrapper).stream().map(this::toAuditItem).toList();
        return new PageResult<>(records, total, current, size);
    }

    /**
     * 分页查询风险治理事件。
     *
     * @param status 处置状态
     * @param riskLevel 风险级别
     * @param eventType 事件类型
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 风险事件分页
     */
    public PageResult<GovernanceDtos.RiskItem> listRisks(String status,
                                                         String riskLevel,
                                                         String eventType,
                                                         String keyword,
                                                         Integer pageNo,
                                                         Integer pageSize) {
        assertCanView();
        syncRiskEvents();
        int current = pageNo == null ? 1 : Math.max(1, pageNo);
        // 未指定每页大小时统一按产品规范返回 10 条，便于治理列表保持一致。
        int size = pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize));
        LambdaQueryWrapper<RiskGovernanceEventEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(RiskGovernanceEventEntity::getStatus, status);
        }
        if (StringUtils.hasText(riskLevel) && !"all".equalsIgnoreCase(riskLevel)) {
            wrapper.eq(RiskGovernanceEventEntity::getRiskLevel, riskLevel);
        }
        if (StringUtils.hasText(eventType) && !"all".equalsIgnoreCase(eventType)) {
            wrapper.eq(RiskGovernanceEventEntity::getEventType, eventType);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(item -> item.like(RiskGovernanceEventEntity::getTitle, value)
                    .or()
                    .like(RiskGovernanceEventEntity::getDescription, value)
                    .or()
                    .like(RiskGovernanceEventEntity::getEventCode, value));
        }
        Long total = riskGovernanceEventMapper.selectCount(wrapper);
        wrapper.orderByDesc(RiskGovernanceEventEntity::getCreatedAt)
                .last("limit " + ((current - 1) * size) + "," + size);
        List<GovernanceDtos.RiskItem> records = riskGovernanceEventMapper.selectList(wrapper).stream().map(this::toRiskItem).toList();
        return new PageResult<>(records, total, current, size);
    }

    /**
     * 查询高风险确认请求。
     *
     * @param status 状态
     * @return 确认请求列表
     */
    public List<GovernanceDtos.ConfirmationItem> listConfirmations(String status) {
        assertCanView();
        LambdaQueryWrapper<ToolConfirmRequestEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(ToolConfirmRequestEntity::getStatus, status);
        }
        wrapper.orderByDesc(ToolConfirmRequestEntity::getCreatedAt).last("limit 100");
        return toolConfirmRequestMapper.selectList(wrapper).stream().map(this::toConfirmationItem).toList();
    }

    /**
     * 处置风险事件。
     *
     * @param id 风险事件ID
     * @param request 处置请求
     * @return 处置后的事件
     */
    @Transactional(rollbackFor = Exception.class)
    public GovernanceDtos.RiskItem handleRisk(String id, GovernanceDtos.HandleRiskRequest request) {
        assertCanManage();
        RiskGovernanceEventEntity entity = riskGovernanceEventMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("RISK_NOT_FOUND", "风险事件不存在");
        }
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "resolved");
        entity.setHandleNote(request.getHandleNote());
        entity.setHandledBy(currentUserId());
        entity.setHandledAt(LocalDateTime.now());
        riskGovernanceEventMapper.updateById(entity);
        return toRiskItem(entity);
    }

    /**
     * 审批高风险确认请求。
     *
     * @param id 确认请求ID
     * @param approved 是否通过
     * @param note 审批备注
     * @return 确认请求摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public GovernanceDtos.ConfirmationItem decideConfirmation(String id, boolean approved, String note) {
        assertCanManage();
        ToolConfirmRequestEntity confirm = toolConfirmRequestMapper.selectById(id);
        if (confirm == null) {
            throw new BusinessException("CONFIRMATION_NOT_FOUND", "确认请求不存在");
        }
        if (!"pending".equals(confirm.getStatus())) {
            throw new BusinessException("CONFIRMATION_ALREADY_DECIDED", "确认请求已处置，不能重复审批");
        }
        if (approved && currentUserId() != null && currentUserId().equals(confirm.getRequesterUserId())) {
            throw new BusinessException("DUAL_APPROVAL_REQUIRED", "高风险工具必须由申请人以外的管理员审批");
        }
        confirm.setStatus(approved ? "approved" : "rejected");
        confirm.setConfirmedBy(currentUserId());
        confirm.setConfirmedAt(LocalDateTime.now());
        toolConfirmRequestMapper.updateById(confirm);
        RiskGovernanceEventEntity event = findRisk("tool_confirm_request", id, "TOOL_CONFIRM_PENDING");
        if (event != null) {
            event.setStatus(approved ? "resolved" : "rejected");
            event.setHandledBy(currentUserId());
            event.setHandledAt(LocalDateTime.now());
            event.setHandleNote(note);
            riskGovernanceEventMapper.updateById(event);
        }
        GovernanceDtos.ConfirmationItem item = toConfirmationItem(confirm);
        if (approved) item.setExecutionToken(toolApprovalTokenService.issue(id));
        return item;
    }

    /**
     * 同步已有业务风险来源到统一风险治理事件表。
     */
    private void syncRiskEvents() {
        syncToolRisks();
        syncMcpRisks();
        syncConfirmRisks();
        syncToolInvocationRisks();
        syncGuardrailRisks();
    }

    /**
     * 同步高风险工具资产。
     */
    private void syncToolRisks() {
        List<ToolDefinitionEntity> tools = toolDefinitionMapper.selectList(new LambdaQueryWrapper<ToolDefinitionEntity>()
                .isNull(ToolDefinitionEntity::getDeletedAt)
                .and(item -> item.eq(ToolDefinitionEntity::getRiskLevel, "high")
                        .or()
                        .eq(ToolDefinitionEntity::getRequireConfirm, true))
                .last("limit 200"));
        for (ToolDefinitionEntity tool : tools) {
            upsertRisk("TOOL_ASSET", "tool_definition", tool.getId(), tool.getRiskLevel(),
                    "高风险工具：" + tool.getToolName(),
                    "工具被标记为高风险或需要二次确认，请检查请求地址、认证信息和可执行动作。",
                    tool.getWorkspaceId(), null, tool.getId(), null, null,
                    Map.of("toolCode", safe(tool.getToolCode()), "toolType", safe(tool.getToolType()), "enabled", Boolean.TRUE.equals(tool.getEnabled())),
                    "确认工具是否只读、是否需要人工确认、是否限制调用用户和 Agent 绑定范围。");
        }
    }

    /**
     * 同步高风险 MCP 能力。
     */
    private void syncMcpRisks() {
        List<McpCapabilityEntity> capabilities = mcpCapabilityMapper.selectList(new LambdaQueryWrapper<McpCapabilityEntity>()
                .eq(McpCapabilityEntity::getRiskLevel, "high")
                .last("limit 200"));
        for (McpCapabilityEntity capability : capabilities) {
            upsertRisk("MCP_CAPABILITY", "mcp_capability", capability.getId(), capability.getRiskLevel(),
                    "高风险 MCP 能力：" + capability.getCapabilityName(),
                    "MCP Server 暴露的能力被识别为高风险，可能涉及文件系统、命令执行或外部网络访问。",
                    null, null, null, null, null,
                    Map.of("serverId", safe(capability.getServerId()), "capabilityType", safe(capability.getCapabilityType()), "enabled", Boolean.TRUE.equals(capability.getEnabled())),
                    "默认停用高风险 MCP 能力，只对可信空间和可信 Agent 开放。");
        }
    }

    /**
     * 同步待确认高风险工具请求。
     */
    private void syncConfirmRisks() {
        List<ToolConfirmRequestEntity> confirms = toolConfirmRequestMapper.selectList(new LambdaQueryWrapper<ToolConfirmRequestEntity>()
                .eq(ToolConfirmRequestEntity::getStatus, "pending")
                .last("limit 200"));
        for (ToolConfirmRequestEntity confirm : confirms) {
            ToolDefinitionEntity tool = toolDefinitionMapper.selectById(confirm.getToolId());
            upsertRisk("TOOL_CONFIRM_PENDING", "tool_confirm_request", confirm.getId(), "high",
                    "待确认高风险工具：" + (tool == null ? confirm.getToolId() : tool.getToolName()),
                    confirm.getReason(),
                    tool == null ? null : tool.getWorkspaceId(), confirm.getAgentId(), confirm.getToolId(), confirm.getRunId(), null,
                    Map.of("requestPayload", parseMap(confirm.getRequestPayload()), "expiredAt", safe(confirm.getExpiredAt())),
                    "由管理员或资源负责人审批，通过前确认入参和业务影响。");
        }
    }

    /**
     * 同步失败或高风险工具调用。
     */
    private void syncToolInvocationRisks() {
        List<ToolInvocationLogEntity> logs = toolInvocationLogMapper.selectList(new LambdaQueryWrapper<ToolInvocationLogEntity>()
                .and(item -> item.eq(ToolInvocationLogEntity::getRiskLevel, "high")
                        .or()
                        .eq(ToolInvocationLogEntity::getSuccess, false))
                .orderByDesc(ToolInvocationLogEntity::getCreatedAt)
                .last("limit 200"));
        for (ToolInvocationLogEntity log : logs) {
            ToolDefinitionEntity tool = toolDefinitionMapper.selectById(log.getToolId());
            String level = "high".equalsIgnoreCase(log.getRiskLevel()) ? "high" : "medium";
            upsertRisk("TOOL_INVOCATION", "tool_invocation_log", log.getId(), level,
                    "工具调用风险：" + safe(log.getToolCode()),
                    Boolean.TRUE.equals(log.getSuccess()) ? "高风险工具已被调用。" : "工具调用失败：" + safe(log.getErrorMessage()),
                    tool == null ? null : tool.getWorkspaceId(), log.getAgentId(), log.getToolId(), log.getRunId(), null,
                    Map.of("success", Boolean.TRUE.equals(log.getSuccess()), "latencyMs", value(log.getLatencyMs()), "errorMessage", safe(log.getErrorMessage())),
                    "查看 Trace 和工具入参/出参，确认是否需要禁用工具或调整 Schema。");
        }
    }

    /**
     * 同步运行时护栏事件。
     */
    private void syncGuardrailRisks() {
        List<RuntimeGuardrailEventEntity> events = runtimeGuardrailEventMapper.selectList(new LambdaQueryWrapper<RuntimeGuardrailEventEntity>()
                .orderByDesc(RuntimeGuardrailEventEntity::getCreatedAt)
                .last("limit 200"));
        for (RuntimeGuardrailEventEntity event : events) {
            String level = riskLevelByScore(event.getRiskScore());
            upsertRisk("GUARDRAIL_EVENT", "runtime_guardrail_event", event.getId(), level,
                    "护栏事件：" + safe(event.getPolicyCode()),
                    "运行时护栏触发，动作：" + safe(event.getAction()),
                    null, null, null, event.getRunId(), event.getPolicyCode(),
                    Map.of("guardrailType", safe(event.getGuardrailType()), "riskScore", event.getRiskScore() == null ? 0 : event.getRiskScore(), "detail", parseMap(event.getDetail())),
                    "检查输入输出内容、护栏策略和误拦截情况，必要时调整规则。");
        }
    }

    /**
     * 写入或保持一个风险事件。
     */
    private void upsertRisk(String eventType,
                            String sourceType,
                            String sourceId,
                            String riskLevel,
                            String title,
                            String description,
                            String workspaceId,
                            String agentId,
                            String toolId,
                            String runId,
                            String ruleCode,
                            Map<String, Object> evidence,
                            String recommendedAction) {
        if (!StringUtils.hasText(sourceId) || findRisk(sourceType, sourceId, eventType) != null) {
            return;
        }
        RiskGovernanceEventEntity event = new RiskGovernanceEventEntity();
        event.setId(newId());
        event.setEventCode(eventType.toLowerCase() + "-" + System.currentTimeMillis() + "-" + sourceId.substring(0, Math.min(8, sourceId.length())));
        event.setEventType(eventType);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setRiskLevel(StringUtils.hasText(riskLevel) ? riskLevel : "medium");
        event.setStatus("open");
        event.setTitle(title);
        event.setDescription(description);
        event.setWorkspaceId(workspaceId);
        event.setAgentId(agentId);
        event.setToolId(toolId);
        event.setRunId(runId);
        event.setRuleCode(ruleCode);
        event.setEvidenceJson(toJson(evidence));
        event.setRecommendedAction(recommendedAction);
        riskGovernanceEventMapper.insert(event);
    }

    /**
     * 查询已存在的风险事件。
     */
    private RiskGovernanceEventEntity findRisk(String sourceType, String sourceId, String eventType) {
        return riskGovernanceEventMapper.selectOne(new LambdaQueryWrapper<RiskGovernanceEventEntity>()
                .eq(RiskGovernanceEventEntity::getSourceType, sourceType)
                .eq(RiskGovernanceEventEntity::getSourceId, sourceId)
                .eq(RiskGovernanceEventEntity::getEventType, eventType)
                .last("limit 1"));
    }

    /**
     * 转换审计日志摘要。
     */
    private GovernanceDtos.AuditItem toAuditItem(AuditOperationLogEntity entity) {
        GovernanceDtos.AuditItem item = new GovernanceDtos.AuditItem();
        item.setId(entity.getId());
        item.setUserId(entity.getUserId());
        item.setUsername(entity.getUsername());
        item.setOperationType(entity.getOperationType());
        item.setResourceType(entity.getResourceType());
        item.setRequestMethod(entity.getRequestMethod());
        item.setRequestPath(entity.getRequestPath());
        item.setResponseStatus(entity.getResponseStatus());
        item.setSuccess(entity.getSuccess());
        item.setFailureReason(entity.getFailureReason());
        item.setClientIp(entity.getClientIp());
        item.setLatencyMs(entity.getLatencyMs());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    /**
     * 转换风险事件摘要。
     */
    private GovernanceDtos.RiskItem toRiskItem(RiskGovernanceEventEntity entity) {
        GovernanceDtos.RiskItem item = new GovernanceDtos.RiskItem();
        item.setId(entity.getId());
        item.setEventCode(entity.getEventCode());
        item.setEventType(entity.getEventType());
        item.setSourceType(entity.getSourceType());
        item.setSourceId(entity.getSourceId());
        item.setRiskLevel(entity.getRiskLevel());
        item.setRiskLabel(riskLabel(entity.getRiskLevel()));
        item.setStatus(entity.getStatus());
        item.setTitle(entity.getTitle());
        item.setDescription(entity.getDescription());
        item.setWorkspaceId(entity.getWorkspaceId());
        item.setWorkspaceName(findWorkspaceName(entity.getWorkspaceId()));
        item.setAgentId(entity.getAgentId());
        item.setToolId(entity.getToolId());
        item.setRunId(entity.getRunId());
        item.setRuleCode(entity.getRuleCode());
        item.setEvidence(parseMap(entity.getEvidenceJson()));
        item.setRecommendedAction(entity.getRecommendedAction());
        item.setHandledBy(entity.getHandledBy());
        item.setHandledAt(entity.getHandledAt());
        item.setHandleNote(entity.getHandleNote());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    /**
     * 转换确认请求摘要。
     */
    private GovernanceDtos.ConfirmationItem toConfirmationItem(ToolConfirmRequestEntity entity) {
        ToolDefinitionEntity tool = toolDefinitionMapper.selectById(entity.getToolId());
        GovernanceDtos.ConfirmationItem item = new GovernanceDtos.ConfirmationItem();
        item.setId(entity.getId());
        item.setToolId(entity.getToolId());
        item.setToolName(tool == null ? "" : tool.getToolName());
        item.setRequesterUserId(entity.getRequesterUserId());
        item.setAgentId(entity.getAgentId());
        item.setRunId(entity.getRunId());
        item.setRequestPayload(parseMap(entity.getRequestPayload()));
        item.setReason(entity.getReason());
        item.setStatus(entity.getStatus());
        item.setConfirmedBy(entity.getConfirmedBy());
        item.setConfirmedAt(entity.getConfirmedAt());
        item.setExpiredAt(entity.getExpiredAt());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    /**
     * 校验查看权限。
     */
    private void assertCanView() {
        if (!isGovernanceUser()) {
            throw new BusinessException("GOVERNANCE_FORBIDDEN", "没有查看审计与风险治理中心的权限");
        }
    }

    /**
     * 校验管理权限。
     */
    private void assertCanManage() {
        if (!isGovernanceManager()) {
            throw new BusinessException("GOVERNANCE_FORBIDDEN", "没有处置风险事件的权限");
        }
    }

    /**
     * 判断当前用户是否治理中心可见用户。
     */
    private boolean isGovernanceUser() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("governance:view") || hasAuthority("governance:manage");
    }

    /**
     * 判断当前用户是否治理中心管理员。
     */
    private boolean isGovernanceManager() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("governance:manage");
    }

    /**
     * 判断是否拥有指定权限。
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    /**
     * 获取当前用户ID。
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 根据分数计算风险级别。
     */
    private String riskLevelByScore(BigDecimal score) {
        if (score == null) {
            return "medium";
        }
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "high";
        }
        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "medium";
        }
        return "low";
    }

    /**
     * 查询工作空间名称。
     */
    private String findWorkspaceName(String workspaceId) {
        if (!StringUtils.hasText(workspaceId)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList("SELECT workspace_name FROM oaf_workspace WHERE id = ? LIMIT 1", String.class, workspaceId);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 风险级别中文标签。
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
     * 解析 JSON Map。
     */
    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 转换 JSON。
     */
    private String toJson(Object value) {
        try {
            return value == null ? "{}" : objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 安全文本。
     */
    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 安全数值。
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 生成 UUID。
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }
}
