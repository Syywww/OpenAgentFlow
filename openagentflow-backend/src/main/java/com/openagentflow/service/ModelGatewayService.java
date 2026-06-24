package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.model.ModelGatewayDtos;
import com.openagentflow.domain.model.ModelRouteDecision;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.entity.ModelRouteCandidateEntity;
import com.openagentflow.entity.ModelRoutePolicyEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.mapper.ModelProviderMapper;
import com.openagentflow.mapper.ModelRouteCandidateMapper;
import com.openagentflow.mapper.ModelRoutePolicyMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 模型网关与模型治理服务。
 *
 * <p>统一负责模型路由策略、候选模型管理、健康评分、失败回退和治理看板查询。</p>
 */
@Service
public class ModelGatewayService {

    /** 默认 Agent 对话场景。 */
    public static final String SCENE_AGENT_CHAT = "AGENT_CHAT";

    /** 模型不可用判定的失败率阈值。 */
    private static final BigDecimal UNHEALTHY_FAILURE_RATE = BigDecimal.valueOf(80);

    /** 路由策略 Mapper。 */
    private final ModelRoutePolicyMapper modelRoutePolicyMapper;

    /** 路由候选 Mapper。 */
    private final ModelRouteCandidateMapper modelRouteCandidateMapper;

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** 模型服务商 Mapper。 */
    private final ModelProviderMapper modelProviderMapper;

    /** 模型服务商应用服务。 */
    private final ModelProviderService modelProviderService;

    /** JDBC 工具，用于聚合运行指标。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    public ModelGatewayService(ModelRoutePolicyMapper modelRoutePolicyMapper,
                               ModelRouteCandidateMapper modelRouteCandidateMapper,
                               ModelConfigMapper modelConfigMapper,
                               ModelProviderMapper modelProviderMapper,
                               ModelProviderService modelProviderService,
                               JdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper) {
        this.modelRoutePolicyMapper = modelRoutePolicyMapper;
        this.modelRouteCandidateMapper = modelRouteCandidateMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.modelProviderMapper = modelProviderMapper;
        this.modelProviderService = modelProviderService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询模型网关概览。
     *
     * @return 概览数据
     */
    public ModelGatewayDtos.Overview getOverview() {
        ModelGatewayDtos.Overview overview = new ModelGatewayDtos.Overview();
        overview.setEnabledPolicyCount(countLong("SELECT COUNT(1) FROM model_route_policy WHERE status = 'enabled'"));
        overview.setEnabledModelCount(countLong("SELECT COUNT(1) FROM model_config WHERE status = 'enabled'"));
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COUNT(1) call_count,
                       COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END), 0) failure_count,
                       COALESCE(AVG(latency_ms), 0) avg_latency_ms,
                       COALESCE(SUM(CASE WHEN fallback_used = 1 THEN 1 ELSE 0 END), 0) fallback_count
                FROM runtime_llm_call
                WHERE created_at >= ?
                """, Timestamp.valueOf(LocalDateTime.now().minusHours(24)));
        long callCount = longValue(row.get("call_count"));
        long failureCount = longValue(row.get("failure_count"));
        overview.setCallCount24h(callCount);
        overview.setFailureCount24h(failureCount);
        overview.setFailureRate24h(rate(failureCount, callCount));
        overview.setAvgLatencyMs24h(decimalValue(row.get("avg_latency_ms")));
        overview.setFallbackCount24h(longValue(row.get("fallback_count")));
        return overview;
    }

    /**
     * 查询模型路由策略列表。
     *
     * @return 策略摘要列表
     */
    public List<ModelGatewayDtos.PolicySummary> listPolicies() {
        List<ModelRoutePolicyEntity> policies = modelRoutePolicyMapper.selectList(new LambdaQueryWrapper<ModelRoutePolicyEntity>()
                .orderByAsc(ModelRoutePolicyEntity::getSceneType)
                .orderByDesc(ModelRoutePolicyEntity::getCreatedAt));
        return policies.stream().map(this::toPolicySummary).toList();
    }

    /**
     * 创建模型路由策略。
     *
     * @param request 保存请求
     * @return 策略摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelGatewayDtos.PolicySummary createPolicy(ModelGatewayDtos.PolicyRequest request) {
        ModelRoutePolicyEntity entity = new ModelRoutePolicyEntity();
        entity.setId(newId());
        fillPolicy(entity, request);
        entity.setCreatedBy(currentUserId());
        modelRoutePolicyMapper.insert(entity);
        saveCandidates(entity.getId(), request.getCandidates());
        return toPolicySummary(entity);
    }

    /**
     * 更新模型路由策略。
     *
     * @param id 策略ID
     * @param request 保存请求
     * @return 策略摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelGatewayDtos.PolicySummary updatePolicy(String id, ModelGatewayDtos.PolicyRequest request) {
        ModelRoutePolicyEntity entity = requirePolicy(id);
        fillPolicy(entity, request);
        modelRoutePolicyMapper.updateById(entity);
        if (request.getCandidates() != null) {
            saveCandidates(entity.getId(), request.getCandidates());
        }
        return toPolicySummary(entity);
    }

    /**
     * 删除模型路由策略。
     *
     * @param id 策略ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(String id) {
        requirePolicy(id);
        modelRouteCandidateMapper.delete(new LambdaQueryWrapper<ModelRouteCandidateEntity>()
                .eq(ModelRouteCandidateEntity::getPolicyId, id));
        modelRoutePolicyMapper.deleteById(id);
    }

    /**
     * 查询模型健康列表。
     *
     * @return 模型健康摘要列表
     */
    public List<ModelGatewayDtos.ModelHealthSummary> listModelHealth() {
        String sql = """
                SELECT m.id model_id,
                       m.model_name,
                       m.model_code,
                       m.status,
                       p.provider_name,
                       p.health_status,
                       COUNT(c.id) recent_call_count,
                       COALESCE(SUM(CASE WHEN c.success = 0 THEN 1 ELSE 0 END), 0) recent_failure_count,
                       COALESCE(AVG(c.latency_ms), 0) recent_avg_latency_ms,
                       COALESCE(SUM(c.cost_amount), 0) recent_cost
                FROM model_config m
                JOIN model_provider p ON p.id = m.provider_id
                LEFT JOIN runtime_llm_call c ON c.model_id = m.id
                  AND c.created_at >= ?
                GROUP BY m.id, m.model_name, m.model_code, m.status, p.provider_name, p.health_status
                ORDER BY recent_failure_count DESC, recent_call_count DESC, m.created_at DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ModelGatewayDtos.ModelHealthSummary item = new ModelGatewayDtos.ModelHealthSummary();
            long callCount = rs.getLong("recent_call_count");
            long failureCount = rs.getLong("recent_failure_count");
            item.setModelId(rs.getString("model_id"));
            item.setModelName(rs.getString("model_name"));
            item.setModelCode(rs.getString("model_code"));
            item.setProviderName(rs.getString("provider_name"));
            item.setStatus(rs.getString("status"));
            item.setHealthStatus(rs.getString("health_status"));
            item.setRecentCallCount(callCount);
            item.setRecentFailureCount(failureCount);
            item.setRecentFailureRate(rate(failureCount, callCount));
            item.setRecentAvgLatencyMs(rs.getBigDecimal("recent_avg_latency_ms"));
            item.setRecentCost(rs.getBigDecimal("recent_cost"));
            return item;
        }, Timestamp.valueOf(LocalDateTime.now().minusHours(24)));
    }

    /**
     * 查询最近模型网关调用。
     *
     * @param limit 返回数量
     * @return 调用摘要列表
     */
    public List<ModelGatewayDtos.GatewayCallSummary> listRecentCalls(Integer limit) {
        int size = limit == null ? 30 : Math.min(Math.max(limit, 1), 100);
        String sql = """
                SELECT c.id,
                       c.run_id,
                       c.gateway_scene_type,
                       c.route_policy_id,
                       rp.policy_name,
                       p.provider_name,
                       m.model_name,
                       c.fallback_used,
                       c.success,
                       c.total_tokens,
                       c.cost_amount,
                       c.latency_ms,
                       c.error_message,
                       c.created_at
                FROM runtime_llm_call c
                LEFT JOIN model_route_policy rp ON rp.id = c.route_policy_id
                LEFT JOIN model_provider p ON p.id = c.provider_id
                LEFT JOIN model_config m ON m.id = c.model_id
                ORDER BY c.created_at DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ModelGatewayDtos.GatewayCallSummary item = new ModelGatewayDtos.GatewayCallSummary();
            Timestamp createdAt = rs.getTimestamp("created_at");
            item.setId(rs.getString("id"));
            item.setRunId(rs.getString("run_id"));
            item.setGatewaySceneType(rs.getString("gateway_scene_type"));
            item.setRoutePolicyId(rs.getString("route_policy_id"));
            item.setPolicyName(rs.getString("policy_name"));
            item.setProviderName(rs.getString("provider_name"));
            item.setModelName(rs.getString("model_name"));
            item.setFallbackUsed(rs.getBoolean("fallback_used"));
            item.setSuccess(rs.getBoolean("success"));
            item.setTotalTokens(rs.getInt("total_tokens"));
            item.setCostAmount(rs.getBigDecimal("cost_amount"));
            item.setLatencyMs(rs.getInt("latency_ms"));
            item.setErrorMessage(rs.getString("error_message"));
            item.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return item;
        }, size);
    }

    /**
     * 解析 Agent 对话模型路由。
     *
     * @param explicitModelId 显式指定模型ID
     * @param agent 当前 Agent
     * @return 路由决策
     */
    public ModelRouteDecision resolveAgentChatRoute(String explicitModelId, AgentEntity agent) {
        if (StringUtils.hasText(explicitModelId)) {
            return directDecision(modelProviderService.requireModel(explicitModelId), SCENE_AGENT_CHAT, "请求显式指定模型");
        }
        if (agent != null && StringUtils.hasText(agent.getModelId())) {
            return directDecision(modelProviderService.requireModel(agent.getModelId()), SCENE_AGENT_CHAT, "Agent 已绑定模型");
        }
        return resolvePolicyRoute(SCENE_AGENT_CHAT);
    }

    /**
     * 尝试切换到下一个可用候选模型。
     *
     * @param current 当前决策
     * @param errorMessage 触发回退的错误信息
     * @return 新决策，无法回退时返回 null
     */
    public ModelRouteDecision nextFallbackDecision(ModelRouteDecision current, String errorMessage) {
        if (current == null
                || Boolean.TRUE.equals(current.getExplicitModel())
                || !Boolean.TRUE.equals(current.getFallbackEnabled())
                || current.getCandidateModelIds() == null) {
            return null;
        }
        int nextIndex = current.getCandidateIndex() == null ? 1 : current.getCandidateIndex() + 1;
        if (nextIndex >= current.getCandidateModelIds().size()) {
            return null;
        }
        String nextModelId = current.getCandidateModelIds().get(nextIndex);
        ModelConfigEntity model = modelProviderService.requireModel(nextModelId);
        if (!"enabled".equalsIgnoreCase(safeText(model.getStatus()))) {
            return null;
        }
        ModelRouteDecision decision = buildDecision(model, current.getSceneType(), current.getRoutePolicyId(), current.getRoutePolicyName(), current.getCandidateModelIds(), nextIndex);
        decision.setFallbackEnabled(true);
        decision.setFallbackUsed(true);
        decision.setReason("上游模型调用失败，已切换到候选模型：" + safeText(errorMessage));
        return decision;
    }

    /**
     * 把路由决策转为 JSON，便于写入运行日志。
     *
     * @param decision 路由决策
     * @return JSON 字符串
     */
    public String toDecisionJson(ModelRouteDecision decision) {
        if (decision == null) {
            return "{}";
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routePolicyId", decision.getRoutePolicyId());
        payload.put("routePolicyName", decision.getRoutePolicyName());
        payload.put("sceneType", decision.getSceneType());
        payload.put("modelId", decision.getModel() == null ? null : decision.getModel().getId());
        payload.put("modelName", decision.getModel() == null ? null : decision.getModel().getModelName());
        payload.put("providerId", decision.getProvider() == null ? null : decision.getProvider().getId());
        payload.put("providerName", decision.getProvider() == null ? null : decision.getProvider().getProviderName());
        payload.put("candidateModelIds", decision.getCandidateModelIds());
        payload.put("candidateIndex", decision.getCandidateIndex());
        payload.put("fallbackEnabled", decision.getFallbackEnabled());
        payload.put("explicitModel", decision.getExplicitModel());
        payload.put("fallbackUsed", decision.getFallbackUsed());
        payload.put("reason", decision.getReason());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private ModelRouteDecision resolvePolicyRoute(String sceneType) {
        ModelRoutePolicyEntity policy = modelRoutePolicyMapper.selectList(new LambdaQueryWrapper<ModelRoutePolicyEntity>()
                        .eq(ModelRoutePolicyEntity::getSceneType, sceneType)
                        .eq(ModelRoutePolicyEntity::getStatus, "enabled")
                        .orderByDesc(ModelRoutePolicyEntity::getUpdatedAt)
                        .orderByDesc(ModelRoutePolicyEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElse(null);
        if (policy == null) {
            return directDecision(defaultChatModel(), sceneType, "未配置路由策略，使用默认聊天模型");
        }

        List<ModelRouteCandidateEntity> candidates = modelRouteCandidateMapper.selectList(new LambdaQueryWrapper<ModelRouteCandidateEntity>()
                        .eq(ModelRouteCandidateEntity::getPolicyId, policy.getId())
                        .eq(ModelRouteCandidateEntity::getEnabled, true)
                        .orderByAsc(ModelRouteCandidateEntity::getPriority)
                        .orderByDesc(ModelRouteCandidateEntity::getWeight)
                        .orderByAsc(ModelRouteCandidateEntity::getCreatedAt))
                .stream()
                .filter(candidate -> healthyCandidate(candidate, policy))
                .toList();
        if (candidates.isEmpty()) {
            return directDecision(defaultChatModel(), sceneType, "路由策略无可用候选，使用默认聊天模型");
        }

        List<String> modelIds = candidates.stream().map(ModelRouteCandidateEntity::getModelId).toList();
        ModelRouteDecision decision = buildDecision(modelProviderService.requireModel(modelIds.get(0)),
                sceneType,
                policy.getId(),
                policy.getPolicyName(),
                modelIds,
                0);
        decision.setFallbackEnabled(Boolean.TRUE.equals(policy.getFallbackEnabled()));
        decision.setReason("命中模型路由策略：" + policy.getPolicyName());
        return decision;
    }

    private ModelRouteDecision directDecision(ModelConfigEntity model, String sceneType, String reason) {
        ModelRouteDecision decision = buildDecision(model, sceneType, null, null, List.of(model.getId()), 0);
        decision.setExplicitModel(true);
        decision.setFallbackEnabled(false);
        decision.setReason(reason);
        return decision;
    }

    private ModelRouteDecision buildDecision(ModelConfigEntity model,
                                             String sceneType,
                                             String routePolicyId,
                                             String routePolicyName,
                                             List<String> candidateModelIds,
                                             Integer candidateIndex) {
        ModelProviderEntity provider = modelProviderService.requireProviderByModel(model);
        ModelRouteDecision decision = new ModelRouteDecision();
        decision.setModel(model);
        decision.setProvider(provider);
        decision.setApiKey(modelProviderService.findApiKeyValue(provider.getId()));
        decision.setSceneType(sceneType);
        decision.setRoutePolicyId(routePolicyId);
        decision.setRoutePolicyName(routePolicyName);
        decision.setCandidateModelIds(candidateModelIds);
        decision.setCandidateIndex(candidateIndex);
        return decision;
    }

    private boolean healthyCandidate(ModelRouteCandidateEntity candidate, ModelRoutePolicyEntity policy) {
        ModelConfigEntity model = modelConfigMapper.selectById(candidate.getModelId());
        if (model == null || !"enabled".equalsIgnoreCase(safeText(model.getStatus()))) {
            return false;
        }
        ModelProviderEntity provider = modelProviderMapper.selectById(model.getProviderId());
        if (provider == null || !"enabled".equalsIgnoreCase(safeText(provider.getStatus()))) {
            return false;
        }
        ModelStats stats = modelStats(model.getId(), 60);
        if (stats.callCount() >= 3 && stats.failureRate().compareTo(UNHEALTHY_FAILURE_RATE) >= 0) {
            return false;
        }
        if (candidate.getMaxLatencyMs() != null
                && stats.avgLatencyMs().compareTo(BigDecimal.ZERO) > 0
                && stats.avgLatencyMs().compareTo(BigDecimal.valueOf(candidate.getMaxLatencyMs())) > 0) {
            return false;
        }
        if (candidate.getMaxCostPer1k() != null
                && averageCostPer1k(model).compareTo(candidate.getMaxCostPer1k()) > 0) {
            return false;
        }
        return "enabled".equalsIgnoreCase(safeText(policy.getStatus()));
    }

    private ModelConfigEntity defaultChatModel() {
        return modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getModelType, "chat")
                        .eq(ModelConfigEntity::getStatus, "enabled")
                        .orderByDesc(ModelConfigEntity::getIsDefault)
                        .orderByDesc(ModelConfigEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "请先配置可用的 Chat 模型"));
    }

    private void fillPolicy(ModelRoutePolicyEntity entity, ModelGatewayDtos.PolicyRequest request) {
        entity.setPolicyCode(requiredText(request.getPolicyCode(), "策略编码不能为空"));
        entity.setPolicyName(requiredText(request.getPolicyName(), "策略名称不能为空"));
        entity.setSceneType(StringUtils.hasText(request.getSceneType()) ? request.getSceneType().trim().toUpperCase() : SCENE_AGENT_CHAT);
        entity.setMatchRule(StringUtils.hasText(request.getMatchRule()) ? request.getMatchRule() : "{}");
        entity.setFallbackEnabled(!Boolean.FALSE.equals(request.getFallbackEnabled()));
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
    }

    private void saveCandidates(String policyId, List<ModelGatewayDtos.CandidateRequest> requests) {
        modelRouteCandidateMapper.delete(new LambdaQueryWrapper<ModelRouteCandidateEntity>()
                .eq(ModelRouteCandidateEntity::getPolicyId, policyId));
        if (requests == null) {
            return;
        }
        int index = 1;
        for (ModelGatewayDtos.CandidateRequest request : requests) {
            if (!StringUtils.hasText(request.getModelId())) {
                continue;
            }
            modelProviderService.requireModel(request.getModelId());
            ModelRouteCandidateEntity entity = new ModelRouteCandidateEntity();
            entity.setId(newId());
            entity.setPolicyId(policyId);
            entity.setModelId(request.getModelId());
            entity.setPriority(request.getPriority() == null ? index : request.getPriority());
            entity.setWeight(request.getWeight() == null ? BigDecimal.ONE : request.getWeight());
            entity.setMaxLatencyMs(request.getMaxLatencyMs());
            entity.setMaxCostPer1k(request.getMaxCostPer1k());
            entity.setEnabled(!Boolean.FALSE.equals(request.getEnabled()));
            modelRouteCandidateMapper.insert(entity);
            index++;
        }
    }

    private ModelGatewayDtos.PolicySummary toPolicySummary(ModelRoutePolicyEntity entity) {
        ModelGatewayDtos.PolicySummary summary = new ModelGatewayDtos.PolicySummary();
        summary.setId(entity.getId());
        summary.setPolicyCode(entity.getPolicyCode());
        summary.setPolicyName(entity.getPolicyName());
        summary.setSceneType(entity.getSceneType());
        summary.setMatchRule(entity.getMatchRule());
        summary.setFallbackEnabled(entity.getFallbackEnabled());
        summary.setStatus(entity.getStatus());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        summary.setCandidates(listCandidates(entity.getId()));
        return summary;
    }

    private List<ModelGatewayDtos.CandidateSummary> listCandidates(String policyId) {
        List<ModelRouteCandidateEntity> candidates = modelRouteCandidateMapper.selectList(new LambdaQueryWrapper<ModelRouteCandidateEntity>()
                .eq(ModelRouteCandidateEntity::getPolicyId, policyId)
                .orderByAsc(ModelRouteCandidateEntity::getPriority)
                .orderByDesc(ModelRouteCandidateEntity::getWeight));
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<String, ModelConfigEntity> modelMap = modelConfigMapper.selectBatchIds(candidates.stream()
                        .map(ModelRouteCandidateEntity::getModelId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ModelConfigEntity::getId, model -> model));
        Map<String, ModelProviderEntity> providerMap = modelMap.isEmpty()
                ? Map.of()
                : modelProviderMapper.selectBatchIds(modelMap.values().stream()
                                .map(ModelConfigEntity::getProviderId)
                                .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(ModelProviderEntity::getId, provider -> provider));
        return candidates.stream()
                .map(candidate -> toCandidateSummary(candidate, modelMap, providerMap))
                .sorted(Comparator.comparing(ModelGatewayDtos.CandidateSummary::getPriority, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private ModelGatewayDtos.CandidateSummary toCandidateSummary(ModelRouteCandidateEntity entity,
                                                                 Map<String, ModelConfigEntity> modelMap,
                                                                 Map<String, ModelProviderEntity> providerMap) {
        ModelConfigEntity model = modelMap.get(entity.getModelId());
        ModelProviderEntity provider = model == null ? null : providerMap.get(model.getProviderId());
        ModelStats stats = modelStats(entity.getModelId(), 60);
        ModelGatewayDtos.CandidateSummary summary = new ModelGatewayDtos.CandidateSummary();
        summary.setId(entity.getId());
        summary.setPolicyId(entity.getPolicyId());
        summary.setModelId(entity.getModelId());
        summary.setModelName(model == null ? "" : model.getModelName());
        summary.setModelCode(model == null ? "" : model.getModelCode());
        summary.setProviderName(provider == null ? "" : provider.getProviderName());
        summary.setPriority(entity.getPriority());
        summary.setWeight(entity.getWeight());
        summary.setMaxLatencyMs(entity.getMaxLatencyMs());
        summary.setMaxCostPer1k(entity.getMaxCostPer1k());
        summary.setEnabled(entity.getEnabled());
        summary.setRecentFailureRate(stats.failureRate());
        summary.setRecentAvgLatencyMs(stats.avgLatencyMs());
        return summary;
    }

    private ModelRoutePolicyEntity requirePolicy(String id) {
        ModelRoutePolicyEntity entity = modelRoutePolicyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("MODEL_ROUTE_POLICY_NOT_FOUND", "模型路由策略不存在");
        }
        return entity;
    }

    private ModelStats modelStats(String modelId, int minutes) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COUNT(1) call_count,
                       COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END), 0) failure_count,
                       COALESCE(AVG(latency_ms), 0) avg_latency_ms
                FROM runtime_llm_call
                WHERE model_id = ?
                  AND created_at >= ?
                """, modelId, Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutes)));
        long callCount = longValue(row.get("call_count"));
        long failureCount = longValue(row.get("failure_count"));
        return new ModelStats(callCount, failureCount, rate(failureCount, callCount), decimalValue(row.get("avg_latency_ms")));
    }

    private BigDecimal averageCostPer1k(ModelConfigEntity model) {
        return safeDecimal(model.getInputPricePer1k()).add(safeDecimal(model.getOutputPricePer1k()));
    }

    private Long countLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private String requiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("VALIDATION_FAILED", message);
        }
        return value.trim();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    private BigDecimal rate(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }

    private record ModelStats(long callCount, long failureCount, BigDecimal failureRate, BigDecimal avgLatencyMs) {
    }
}
