package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.trace.RunDetail;
import com.openagentflow.domain.trace.RunStats;
import com.openagentflow.domain.trace.RunSummary;
import com.openagentflow.domain.trace.TraceStepDetail;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.IamUserEntity;
import com.openagentflow.entity.KnowledgeRetrievalLogEntity;
import com.openagentflow.entity.RuntimeLlmCallEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.entity.RuntimeTraceStepEntity;
import com.openagentflow.entity.ToolInvocationLogEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.IamUserMapper;
import com.openagentflow.mapper.KnowledgeRetrievalLogMapper;
import com.openagentflow.mapper.RuntimeLlmCallMapper;
import com.openagentflow.mapper.RuntimeRunMapper;
import com.openagentflow.mapper.RuntimeTraceStepMapper;
import com.openagentflow.mapper.ToolInvocationLogMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行 Trace 查询服务。
 */
@Service
public class TraceService {

    /** 运行记录 Mapper。 */
    private final RuntimeRunMapper runtimeRunMapper;

    /** Trace 步骤 Mapper。 */
    private final RuntimeTraceStepMapper runtimeTraceStepMapper;

    /** LLM 调用 Mapper。 */
    private final RuntimeLlmCallMapper runtimeLlmCallMapper;

    /** 工具调用日志 Mapper。 */
    private final ToolInvocationLogMapper toolInvocationLogMapper;

    /** RAG 检索日志 Mapper。 */
    private final KnowledgeRetrievalLogMapper knowledgeRetrievalLogMapper;

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** 用户 Mapper。 */
    private final IamUserMapper iamUserMapper;

    /** JDBC 工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** Agent 权限服务。 */
    private final AgentAccessService agentAccessService;

    public TraceService(RuntimeRunMapper runtimeRunMapper,
                        RuntimeTraceStepMapper runtimeTraceStepMapper,
                        RuntimeLlmCallMapper runtimeLlmCallMapper,
                        ToolInvocationLogMapper toolInvocationLogMapper,
                        KnowledgeRetrievalLogMapper knowledgeRetrievalLogMapper,
                        AgentMapper agentMapper,
                        IamUserMapper iamUserMapper,
                        JdbcTemplate jdbcTemplate,
                        ObjectMapper objectMapper,
                        AgentAccessService agentAccessService) {
        this.runtimeRunMapper = runtimeRunMapper;
        this.runtimeTraceStepMapper = runtimeTraceStepMapper;
        this.runtimeLlmCallMapper = runtimeLlmCallMapper;
        this.toolInvocationLogMapper = toolInvocationLogMapper;
        this.knowledgeRetrievalLogMapper = knowledgeRetrievalLogMapper;
        this.agentMapper = agentMapper;
        this.iamUserMapper = iamUserMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.agentAccessService = agentAccessService;
    }

    /**
     * 分页查询运行记录。
     *
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @param agentId Agent 筛选
     * @param keyword 关键词
     * @return 分页运行记录
     */
    public PageResult<RunSummary> listRuns(Integer pageNo, Integer pageSize, String status, String agentId, String keyword) {
        int current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<RuntimeRunEntity> wrapper = new LambdaQueryWrapper<RuntimeRunEntity>()
                .orderByDesc(RuntimeRunEntity::getStartedAt);
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(RuntimeRunEntity::getStatus, status);
        }
        if (StringUtils.hasText(agentId) && !"all".equalsIgnoreCase(agentId)) {
            wrapper.eq(RuntimeRunEntity::getAgentId, agentId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(RuntimeRunEntity::getRunNo, keyword)
                    .or()
                    .like(RuntimeRunEntity::getInputText, keyword)
                    .or()
                    .like(RuntimeRunEntity::getOutputText, keyword));
        }
        // 先单独统计总数，保证前端分页条能拿到准确总量。
        Long total = runtimeRunMapper.selectCount(wrapper);
        // 当前基础框架未强制要求启用 MyBatis-Plus 分页拦截器，因此这里显式追加 LIMIT/OFFSET。
        int offset = (current - 1) * size;
        List<RunSummary> records = runtimeRunMapper.selectList(wrapper.last("LIMIT " + size + " OFFSET " + offset)).stream()
                .filter(this::canViewRun)
                .map(this::toSummary)
                .toList();
        return new PageResult<>(records, total, current, size);
    }

    /**
     * 查询运行详情。
     *
     * @param runId 运行 ID
     * @return 运行详情
     */
    public RunDetail getRunDetail(String runId) {
        RuntimeRunEntity run = requireRun(runId);
        assertCanViewRun(run);
        RunDetail detail = new RunDetail();
        copySummary(toSummary(run), detail);
        detail.setInputPayload(parseJson(run.getInputPayload()));
        detail.setOutputPayload(parseJson(run.getOutputPayload()));
        detail.setMetadata(parseJson(run.getMetadata()));
        detail.setSteps(listRunSteps(runId));
        detail.setRetrievalLogs(listRetrievalLogs(runId));
        detail.setToolInvocations(listToolInvocations(runId));
        detail.setLlmCalls(listLlmCalls(runId));
        return detail;
    }

    /**
     * 查询运行步骤详情列表。
     *
     * @param runId 运行 ID
     * @return 步骤详情列表
     */
    public List<TraceStepDetail> listRunSteps(String runId) {
        RuntimeRunEntity run = requireRun(runId);
        assertCanViewRun(run);
        return runtimeTraceStepMapper.selectList(new LambdaQueryWrapper<RuntimeTraceStepEntity>()
                        .eq(RuntimeTraceStepEntity::getRunId, runId)
                        .orderByAsc(RuntimeTraceStepEntity::getStartedAt))
                .stream()
                .map(this::toStepDetail)
                .toList();
    }

    /**
     * 查询运行日志基础统计。
     *
     * @return 基础统计
     */
    public RunStats getRunStats() {
        RunStats stats = new RunStats();
        stats.setTotalRuns(number("SELECT COUNT(1) FROM runtime_run"));
        stats.setSuccessRuns(number("SELECT COUNT(1) FROM runtime_run WHERE status = 'SUCCESS'"));
        stats.setFailedRuns(number("SELECT COUNT(1) FROM runtime_run WHERE status = 'FAILED'"));
        stats.setRunningRuns(number("SELECT COUNT(1) FROM runtime_run WHERE status = 'RUNNING'"));
        Number avg = jdbcTemplate.queryForObject("SELECT COALESCE(AVG(latency_ms), 0) FROM runtime_run WHERE latency_ms IS NOT NULL", Number.class);
        Number tokens = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(total_tokens), 0) FROM runtime_run", Number.class);
        stats.setAvgLatencyMs(avg == null ? 0 : avg.intValue());
        stats.setTotalTokens(tokens == null ? 0L : tokens.longValue());
        return stats;
    }

    /**
     * 转换运行摘要。
     *
     * @param entity 运行实体
     * @return 运行摘要
     */
    private RunSummary toSummary(RuntimeRunEntity entity) {
        RunSummary summary = new RunSummary();
        summary.setId(entity.getId());
        summary.setRunNo(entity.getRunNo());
        summary.setRunType(entity.getRunType());
        summary.setAgentId(entity.getAgentId());
        summary.setAgentName(findAgentName(entity.getAgentId()));
        summary.setUserId(entity.getUserId());
        summary.setUserName(findUserName(entity.getUserId()));
        summary.setInputText(entity.getInputText());
        summary.setOutputText(entity.getOutputText());
        summary.setStatus(entity.getStatus());
        summary.setStatusLabel(statusLabel(entity.getStatus()));
        summary.setTotalTokens(nullToZero(entity.getTotalTokens()));
        summary.setPromptTokens(nullToZero(entity.getPromptTokens()));
        summary.setCompletionTokens(nullToZero(entity.getCompletionTokens()));
        summary.setTotalCost(entity.getTotalCost() == null ? BigDecimal.ZERO : entity.getTotalCost());
        summary.setLatencyMs(nullToZero(entity.getLatencyMs()));
        summary.setErrorMessage(entity.getErrorMessage());
        summary.setStepCount(stepCount(entity.getId()));
        summary.setStartedAt(entity.getStartedAt());
        summary.setFinishedAt(entity.getFinishedAt());
        return summary;
    }

    /**
     * 转换 Trace 步骤详情。
     *
     * @param entity Trace 步骤实体
     * @return 步骤详情
     */
    private TraceStepDetail toStepDetail(RuntimeTraceStepEntity entity) {
        TraceStepDetail detail = new TraceStepDetail();
        detail.setId(entity.getId());
        detail.setRunId(entity.getRunId());
        detail.setParentStepId(entity.getParentStepId());
        detail.setStepKey(entity.getStepKey());
        detail.setStepName(entity.getStepName());
        detail.setStepType(entity.getStepType());
        detail.setStatus(entity.getStatus());
        detail.setInputPayload(parseJson(entity.getInputPayload()));
        detail.setOutputPayload(parseJson(entity.getOutputPayload()));
        detail.setPrompt(parseJson(entity.getPromptText()));
        detail.setTokenUsage(parseJson(entity.getTokenUsage()));
        detail.setCostAmount(entity.getCostAmount() == null ? BigDecimal.ZERO : entity.getCostAmount());
        detail.setLatencyMs(nullToZero(entity.getLatencyMs()));
        detail.setErrorMessage(entity.getErrorMessage());
        detail.setLlmCall(findLlmCall(entity.getRunId(), entity.getId()));
        detail.setToolInvocation(findToolInvocation(entity.getId()));
        detail.setRetrievalLogs("RAG".equalsIgnoreCase(entity.getStepType()) ? listRetrievalLogs(entity.getRunId()) : List.of());
        detail.setStartedAt(entity.getStartedAt());
        detail.setFinishedAt(entity.getFinishedAt());
        return detail;
    }

    /**
     * 查询 RAG 检索日志。
     *
     * @param runId 运行 ID
     * @return 日志列表
     */
    private List<Map<String, Object>> listRetrievalLogs(String runId) {
        return knowledgeRetrievalLogMapper.selectList(new LambdaQueryWrapper<KnowledgeRetrievalLogEntity>()
                        .eq(KnowledgeRetrievalLogEntity::getRunId, runId)
                        .orderByAsc(KnowledgeRetrievalLogEntity::getCreatedAt))
                .stream()
                .map(this::retrievalLogMap)
                .toList();
    }

    /**
     * 查询工具调用日志。
     *
     * @param runId 运行 ID
     * @return 日志列表
     */
    private List<Map<String, Object>> listToolInvocations(String runId) {
        return toolInvocationLogMapper.selectList(new LambdaQueryWrapper<ToolInvocationLogEntity>()
                        .eq(ToolInvocationLogEntity::getRunId, runId)
                        .orderByAsc(ToolInvocationLogEntity::getCreatedAt))
                .stream()
                .map(this::toolInvocationMap)
                .toList();
    }

    /**
     * 查询 LLM 调用日志。
     *
     * @param runId 运行 ID
     * @return 日志列表
     */
    private List<Map<String, Object>> listLlmCalls(String runId) {
        return runtimeLlmCallMapper.selectList(new LambdaQueryWrapper<RuntimeLlmCallEntity>()
                        .eq(RuntimeLlmCallEntity::getRunId, runId)
                        .orderByAsc(RuntimeLlmCallEntity::getCreatedAt))
                .stream()
                .map(this::llmCallMap)
                .toList();
    }

    /**
     * 查询步骤关联 LLM 调用。
     *
     * @param runId 运行 ID
     * @param stepId 步骤 ID
     * @return LLM 调用信息
     */
    private Map<String, Object> findLlmCall(String runId, String stepId) {
        RuntimeLlmCallEntity call = runtimeLlmCallMapper.selectOne(new LambdaQueryWrapper<RuntimeLlmCallEntity>()
                .eq(RuntimeLlmCallEntity::getRunId, runId)
                .eq(RuntimeLlmCallEntity::getStepId, stepId)
                .last("limit 1"));
        return call == null ? null : llmCallMap(call);
    }

    /**
     * 查询步骤关联工具调用。
     *
     * @param stepId 步骤 ID
     * @return 工具调用信息
     */
    private Map<String, Object> findToolInvocation(String stepId) {
        ToolInvocationLogEntity log = toolInvocationLogMapper.selectOne(new LambdaQueryWrapper<ToolInvocationLogEntity>()
                .eq(ToolInvocationLogEntity::getStepId, stepId)
                .last("limit 1"));
        return log == null ? null : toolInvocationMap(log);
    }

    /**
     * 转换 RAG 检索日志 Map。
     *
     * @param log 检索日志
     * @return Map
     */
    private Map<String, Object> retrievalLogMap(KnowledgeRetrievalLogEntity log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("kbId", log.getKbId());
        map.put("agentId", log.getAgentId());
        map.put("runId", log.getRunId());
        map.put("queryText", log.getQueryText());
        map.put("topK", log.getTopK());
        map.put("scoreThreshold", log.getScoreThreshold());
        map.put("resultCount", log.getResultCount());
        map.put("latencyMs", log.getLatencyMs());
        map.put("results", parseJson(log.getResults()));
        map.put("milvusResultIds", parseJson(log.getMilvusResultIds()));
        map.put("searchParams", parseJson(log.getMilvusSearchParams()));
        map.put("createdAt", log.getCreatedAt());
        return map;
    }

    /**
     * 转换工具调用日志 Map。
     *
     * @param log 工具日志
     * @return Map
     */
    private Map<String, Object> toolInvocationMap(ToolInvocationLogEntity log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("toolId", log.getToolId());
        map.put("toolCode", log.getToolCode());
        map.put("agentId", log.getAgentId());
        map.put("runId", log.getRunId());
        map.put("stepId", log.getStepId());
        map.put("inputParams", parseJson(log.getInputParams()));
        map.put("outputResult", parseJson(log.getOutputResult()));
        map.put("success", log.getSuccess());
        map.put("riskLevel", log.getRiskLevel());
        map.put("latencyMs", log.getLatencyMs());
        map.put("errorMessage", log.getErrorMessage());
        map.put("createdAt", log.getCreatedAt());
        return map;
    }

    /**
     * 转换 LLM 调用日志 Map。
     *
     * @param call LLM 调用日志
     * @return Map
     */
    private Map<String, Object> llmCallMap(RuntimeLlmCallEntity call) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", call.getId());
        map.put("runId", call.getRunId());
        map.put("stepId", call.getStepId());
        map.put("providerId", call.getProviderId());
        map.put("modelId", call.getModelId());
        map.put("requestMessages", parseJson(call.getRequestMessages()));
        map.put("responseMessage", parseJson(call.getResponseMessage()));
        map.put("stream", call.getStream());
        map.put("promptTokens", call.getPromptTokens());
        map.put("completionTokens", call.getCompletionTokens());
        map.put("totalTokens", call.getTotalTokens());
        map.put("costAmount", call.getCostAmount());
        map.put("latencyMs", call.getLatencyMs());
        map.put("success", call.getSuccess());
        map.put("errorMessage", call.getErrorMessage());
        map.put("createdAt", call.getCreatedAt());
        return map;
    }

    /**
     * 查询运行记录。
     *
     * @param runId 运行 ID
     * @return 运行记录
     */
    private RuntimeRunEntity requireRun(String runId) {
        RuntimeRunEntity run = runtimeRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException("RUN_NOT_FOUND", "运行记录不存在");
        }
        return run;
    }

    /**
     * 校验当前用户可查看运行。
     *
     * @param run 运行记录
     */
    private void assertCanViewRun(RuntimeRunEntity run) {
        if (!canViewRun(run)) {
            throw new BusinessException("RUN_FORBIDDEN", "没有查看该运行记录的权限");
        }
    }

    /**
     * 判断当前用户是否可查看运行。
     *
     * @param run 运行记录
     * @return 是否可查看
     */
    private boolean canViewRun(RuntimeRunEntity run) {
        if (run == null) {
            return false;
        }
        if (isSystemManager()) {
            return true;
        }
        if (StringUtils.hasText(run.getUserId()) && run.getUserId().equals(currentUserId())) {
            return true;
        }
        if (StringUtils.hasText(run.getAgentId())) {
            AgentEntity agent = agentMapper.selectById(run.getAgentId());
            return agent != null && agentAccessService.canView(agent);
        }
        return false;
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
                .anyMatch(authority -> List.of("ROLE_super_admin", "ROLE_admin", "trace:manage", "runtime:manage").contains(authority));
    }

    /**
     * 当前用户 ID。
     *
     * @return 用户 ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 复制摘要字段到详情。
     *
     * @param source 源摘要
     * @param target 目标详情
     */
    private void copySummary(RunSummary source, RunDetail target) {
        target.setId(source.getId());
        target.setRunNo(source.getRunNo());
        target.setRunType(source.getRunType());
        target.setAgentId(source.getAgentId());
        target.setAgentName(source.getAgentName());
        target.setUserId(source.getUserId());
        target.setUserName(source.getUserName());
        target.setInputText(source.getInputText());
        target.setOutputText(source.getOutputText());
        target.setStatus(source.getStatus());
        target.setStatusLabel(source.getStatusLabel());
        target.setTotalTokens(source.getTotalTokens());
        target.setPromptTokens(source.getPromptTokens());
        target.setCompletionTokens(source.getCompletionTokens());
        target.setTotalCost(source.getTotalCost());
        target.setLatencyMs(source.getLatencyMs());
        target.setErrorMessage(source.getErrorMessage());
        target.setStepCount(source.getStepCount());
        target.setStartedAt(source.getStartedAt());
        target.setFinishedAt(source.getFinishedAt());
    }

    /**
     * 查询 Agent 名称。
     *
     * @param agentId Agent ID
     * @return Agent 名称
     */
    private String findAgentName(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return "";
        }
        AgentEntity agent = agentMapper.selectById(agentId);
        return agent == null ? "" : agent.getAgentName();
    }

    /**
     * 查询用户展示名。
     *
     * @param userId 用户 ID
     * @return 用户名
     */
    private String findUserName(String userId) {
        if (!StringUtils.hasText(userId)) {
            return "";
        }
        IamUserEntity user = iamUserMapper.selectById(userId);
        return user == null ? "" : user.getDisplayName();
    }

    /**
     * 查询步骤数量。
     *
     * @param runId 运行 ID
     * @return 步骤数量
     */
    private Integer stepCount(String runId) {
        Long count = runtimeTraceStepMapper.selectCount(new LambdaQueryWrapper<RuntimeTraceStepEntity>()
                .eq(RuntimeTraceStepEntity::getRunId, runId));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 状态中文标签。
     *
     * @param status 状态编码
     * @return 中文标签
     */
    private String statusLabel(String status) {
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return "成功";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "失败";
        }
        if ("RUNNING".equalsIgnoreCase(status)) {
            return "运行中";
        }
        return StringUtils.hasText(status) ? status : "未知";
    }

    /**
     * 查询单个数字。
     *
     * @param sql SQL
     * @return 数字
     */
    private Long number(String sql) {
        Number number = jdbcTemplate.queryForObject(sql, Number.class);
        return number == null ? 0L : number.longValue();
    }

    /**
     * 解析 JSON 字符串。
     *
     * @param json JSON 字符串
     * @return JSON 对象或原始字符串
     */
    private Object parseJson(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<Object>() {
            });
        } catch (Exception exception) {
            return json;
        }
    }

    /**
     * 整数空值转零。
     *
     * @param value 原始值
     * @return 非空整数
     */
    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
