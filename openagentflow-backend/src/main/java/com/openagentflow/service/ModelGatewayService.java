package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
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

    /** 熔断器默认连续失败阈值（策略未配置时兜底）。 */
    private static final int DEFAULT_BREAKER_FAILURE_THRESHOLD = 5;

    /** 熔断器默认熔断时长（秒，策略未配置时兜底）。 */
    private static final int DEFAULT_BREAKER_TIMEOUT_SECONDS = 60;

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

    /** 会话粘性 TTL：同一 run 内模型选定后保持 24 小时。 */
    private static final long STICKY_TTL_MILLIS = Duration.ofHours(24).toMillis();

    /** 按 run 记录已选定的模型，key 形如 sceneType:runId。 */
    private final Map<String, StickyEntry> stickyModelByRun = new ConcurrentHashMap<>();

    /** 写入计数，用于写时抽检清理过期条目。 */
    private final AtomicLong stickyPutCounter = new AtomicLong();

    /** 按模型记录的熔断状态，key 为 modelId（单实例内存态，重启归零，与 sticky 同策略）。 */
    private final Map<String, BreakerState> breakerByModel = new ConcurrentHashMap<>();

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
     * <p>传入 runId 时策略命中会启用会话粘性：同一 run 内多次路由复用首次选定的模型，
     * 避免工作流同一次运行在不同 LLM 节点间跳动模型。路由发生在 createRun 之前的调用方传 null。</p>
     *
     * @param explicitModelId 显式指定模型ID
     * @param agent 当前 Agent
     * @param runId 运行ID，可为 null（此时不启用会话粘性）
     * @return 路由决策
     */
    public ModelRouteDecision resolveAgentChatRoute(String explicitModelId, AgentEntity agent, String runId) {
        if (StringUtils.hasText(explicitModelId)) {
            return directDecision(modelProviderService.requireModel(explicitModelId), SCENE_AGENT_CHAT, "请求显式指定模型");
        }
        if (agent != null && StringUtils.hasText(agent.getModelId())) {
            return directDecision(modelProviderService.requireModel(agent.getModelId()), SCENE_AGENT_CHAT, "Agent 已绑定模型");
        }
        return resolvePolicyRoute(SCENE_AGENT_CHAT, agent == null ? null : agent.getWorkspaceId(), runId);
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
        // 跳过运行中途被其他请求熔断（OPEN）的候选；HALF_OPEN 放行（回退即探测）。
        // disabled 候选保持原语义：遇到即放弃回退（return null），不继续往下找。
        int breakerTimeout = breakerTimeoutSeconds(loadBreakerPolicy(current.getRoutePolicyId()));
        long now = System.currentTimeMillis();
        int startIndex = current.getCandidateIndex() == null ? 1 : current.getCandidateIndex() + 1;
        for (int nextIndex = startIndex; nextIndex < current.getCandidateModelIds().size(); nextIndex++) {
            String nextModelId = current.getCandidateModelIds().get(nextIndex);
            if (breakerExcluded(nextModelId, breakerTimeout, now)) {
                continue;
            }
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
        return null;
    }

    /**
     * 上报一次模型调用失败，驱动熔断状态机。
     *
     * <p>按决策携带的路由策略读取阈值与熔断时长（失败低频，一次 selectById 可接受）；
     * CLOSED 连续失败达到阈值后进入 OPEN。配额类异常（token/cost 超限）不应上报，
     * 由调用方在 {@code recordGatewayFailure} 助手处过滤。</p>
     *
     * @param decision 失败时的路由决策（携带 model 与 routePolicyId）
     */
    public void recordLlmFailure(ModelRouteDecision decision) {
        if (decision == null || decision.getModel() == null) {
            return;
        }
        ModelRoutePolicyEntity policy = loadBreakerPolicy(decision.getRoutePolicyId());
        int threshold = breakerFailureThreshold(policy);
        int timeoutSeconds = breakerTimeoutSeconds(policy);
        String modelId = decision.getModel().getId();
        long now = System.currentTimeMillis();
        breakerByModel.compute(modelId, (key, state) -> afterFailure(
                state == null ? BreakerState.closed() : state, threshold, timeoutSeconds, now));
    }

    /**
     * 上报一次模型调用成功，驱动熔断恢复。
     *
     * <p>仅更新已有状态条目（成功不创建新条目）；CLOSED 重置计数、HALF_OPEN 成功转 CLOSED、OPEN 忽略。</p>
     *
     * @param decision 成功时的路由决策（携带最终成功模型）
     */
    public void recordLlmSuccess(ModelRouteDecision decision) {
        if (decision == null || decision.getModel() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        breakerByModel.computeIfPresent(decision.getModel().getId(), (key, state) -> afterSuccess(state, now));
    }

    /** 熔断状态机状态。 */
    enum BreakerStatus {
        /** 闭合：正常放行。 */
        CLOSED,
        /** 打开：拒绝路由，等待超时后半开。 */
        OPEN,
        /** 半开：放行探测流量，成功恢复、失败重开。 */
        HALF_OPEN
    }

    /** 熔断状态：当前状态、连续失败计数、OPEN 开始时间（仅 OPEN/HALF_OPEN 有效）。 */
    record BreakerState(BreakerStatus status, int consecutiveFailures, long openedAtMillis) {
        /** 初始闭合状态。 */
        static BreakerState closed() {
            return new BreakerState(BreakerStatus.CLOSED, 0, 0);
        }
    }

    /**
     * 失败转移（纯方法，时间由调用方注入保证可测）。
     *
     * <p>CLOSED 连续失败 +1，达到阈值 → OPEN（记录 openedAt）；HALF_OPEN 失败视为探测失败 → 回 OPEN（刷新 openedAt）；
     * OPEN 失败吸收不动（不刷新 openedAt，否则直连零星失败会让熔断无限延长）；
     * 若 OPEN 已超时但从未被路由读取，本次失败视为半开探测失败 → 重新计时 OPEN。</p>
     *
     * @param state 当前状态
     * @param threshold 连续失败阈值
     * @param timeoutSeconds 熔断时长（秒）
     * @param now 当前时间戳
     * @return 转移后的状态
     */
    static BreakerState afterFailure(BreakerState state, int threshold, int timeoutSeconds, long now) {
        if (state.status() == BreakerStatus.OPEN) {
            if (now - state.openedAtMillis() >= timeoutSeconds * 1000L) {
                return new BreakerState(BreakerStatus.OPEN, state.consecutiveFailures() + 1, now);
            }
            return state;
        }
        if (state.status() == BreakerStatus.HALF_OPEN) {
            return new BreakerState(BreakerStatus.OPEN, state.consecutiveFailures() + 1, now);
        }
        int failures = state.consecutiveFailures() + 1;
        if (failures >= threshold) {
            return new BreakerState(BreakerStatus.OPEN, failures, now);
        }
        return new BreakerState(BreakerStatus.CLOSED, failures, 0);
    }

    /**
     * 成功转移（纯方法）。
     *
     * <p>CLOSED 重置计数；HALF_OPEN 成功 → CLOSED（熔断恢复）；OPEN 忽略（单一成功不足抵消失败模式）。</p>
     *
     * @param state 当前状态
     * @param now 当前时间戳
     * @return 转移后的状态
     */
    static BreakerState afterSuccess(BreakerState state, long now) {
        if (state.status() == BreakerStatus.HALF_OPEN) {
            return new BreakerState(BreakerStatus.CLOSED, 0, 0);
        }
        if (state.status() == BreakerStatus.CLOSED) {
            return new BreakerState(BreakerStatus.CLOSED, 0, 0);
        }
        return state;
    }

    /**
     * 判断当前状态是否应被路由排除（纯方法）。
     *
     * <p>仅 OPEN 且未超时 → true；CLOSED / HALF_OPEN / 已超时 OPEN → false（HALF_OPEN 必须有流量流入才能探测）。</p>
     *
     * @param state 当前状态
     * @param timeoutSeconds 熔断时长（秒）
     * @param now 当前时间戳
     * @return true 表示该模型应从候选池排除
     */
    static boolean isBreakerExcluded(BreakerState state, int timeoutSeconds, long now) {
        return state.status() == BreakerStatus.OPEN
                && now - state.openedAtMillis() < timeoutSeconds * 1000L;
    }

    /**
     * OPEN 超时惰性转 HALF_OPEN（纯方法）。
     *
     * @param state 当前状态
     * @param timeoutSeconds 熔断时长（秒）
     * @param now 当前时间戳
     * @return 转移后的状态
     */
    static BreakerState advanceOnRead(BreakerState state, int timeoutSeconds, long now) {
        if (state.status() == BreakerStatus.OPEN
                && now - state.openedAtMillis() >= timeoutSeconds * 1000L) {
            return new BreakerState(BreakerStatus.HALF_OPEN, state.consecutiveFailures(), state.openedAtMillis());
        }
        return state;
    }

    /**
     * 生产入口：判断模型是否因熔断被路由排除，并在读到时惰性推进状态。
     *
     * @param modelId 模型ID
     * @param timeoutSeconds 熔断时长（秒）
     * @param now 当前时间戳
     * @return true 表示应排除
     */
    private boolean breakerExcluded(String modelId, int timeoutSeconds, long now) {
        BreakerState state = breakerByModel.get(modelId);
        if (state == null || state.status() == BreakerStatus.CLOSED) {
            return false;
        }
        if (isBreakerExcluded(state, timeoutSeconds, now)) {
            return true;
        }
        // OPEN 已超时：惰性转半开并放行（半开必须有流量流入才能探测）
        if (state.status() == BreakerStatus.OPEN) {
            breakerByModel.computeIfPresent(modelId, (key, s) -> advanceOnRead(s, timeoutSeconds, now));
        }
        return false;
    }

    /**
     * 读取熔断配置所在的策略，策略ID为空时返回 null（用默认常量兜底）。
     */
    private ModelRoutePolicyEntity loadBreakerPolicy(String routePolicyId) {
        return routePolicyId == null ? null : modelRoutePolicyMapper.selectById(routePolicyId);
    }

    /**
     * 读取策略的熔断失败阈值，未配置或非法时用默认常量兜底。
     */
    private int breakerFailureThreshold(ModelRoutePolicyEntity policy) {
        return policy != null && policy.getBreakerFailureThreshold() != null
                && policy.getBreakerFailureThreshold() > 0
                ? policy.getBreakerFailureThreshold() : DEFAULT_BREAKER_FAILURE_THRESHOLD;
    }

    /**
     * 读取策略的熔断时长（秒），未配置或非法时用默认常量兜底。
     */
    private int breakerTimeoutSeconds(ModelRoutePolicyEntity policy) {
        return policy != null && policy.getBreakerTimeoutSeconds() != null
                && policy.getBreakerTimeoutSeconds() > 0
                ? policy.getBreakerTimeoutSeconds() : DEFAULT_BREAKER_TIMEOUT_SECONDS;
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

    /**
     * 解析场景路由策略：按工作空间命中策略并选择候选模型。
     *
     * <p>策略按更新时间倒序遍历，取第一个命中的策略（空间策略更新晚于全局时自然优先）；
     * 空间未命中时 GLOBAL 兜底；候选模型解析失败时回退默认聊天模型。</p>
     *
     * <p>候选选择：priority 是最低优先级组的硬分组门，weight 只在组内按比例分发（灰度放量）；
     * 传入 runId 时按会话粘性复用该 run 首次选定的模型。weight 为 0 的候选不参与分发，
     * 但仍保留在候选列表供失败回退链使用。</p>
     *
     * @param sceneType 场景类型
     * @param workspaceId 当前工作空间ID，可为 null（平台级调用仅走 GLOBAL）
     * @param runId 运行ID，可为 null（不启用会话粘性）
     * @return 路由决策
     */
    private ModelRouteDecision resolvePolicyRoute(String sceneType, String workspaceId, String runId) {
        List<ModelRoutePolicyEntity> policies = modelRoutePolicyMapper.selectList(new LambdaQueryWrapper<ModelRoutePolicyEntity>()
                .eq(ModelRoutePolicyEntity::getSceneType, sceneType)
                .eq(ModelRoutePolicyEntity::getStatus, "enabled")
                .orderByDesc(ModelRoutePolicyEntity::getUpdatedAt)
                .orderByDesc(ModelRoutePolicyEntity::getCreatedAt));
        ModelRoutePolicyEntity policy = selectPolicyForWorkspace(policies, workspaceId);
        if (policy == null) {
            return directDecision(defaultChatModel(), sceneType, "未命中路由策略，使用默认聊天模型");
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
        // 熔断过滤：瞬时连续失败达阈值的模型在超时前被剔除，与上面的静态健康过滤互补
        int breakerTimeout = breakerTimeoutSeconds(policy);
        long breakerNow = System.currentTimeMillis();
        candidates = candidates.stream()
                .filter(candidate -> !breakerExcluded(candidate.getModelId(), breakerTimeout, breakerNow))
                .toList();
        if (candidates.isEmpty()) {
            return directDecision(defaultChatModel(), sceneType, "候选全部熔断，使用默认聊天模型");
        }

        List<String> modelIds = candidates.stream().map(ModelRouteCandidateEntity::getModelId).toList();
        // 最低优先级组（priority 相等）为放量池，保证既有"主/备"契约恒选主、灰度在同优先级组内按权重分发
        Integer poolPriority = candidates.get(0).getPriority();
        List<ModelRouteCandidateEntity> pool = candidates.stream()
                .filter(candidate -> Objects.equals(candidate.getPriority(), poolPriority))
                .toList();
        String stickyKey = StringUtils.hasText(runId) ? sceneType + ":" + runId : null;
        WeightedSelection selection = selectWeightedIndex(pool, modelIds, stickyKey, ThreadLocalRandom.current().nextDouble());

        ModelRouteDecision decision;
        try {
            decision = buildDecision(modelProviderService.requireModel(modelIds.get(selection.index())),
                    sceneType,
                    policy.getId(),
                    policy.getPolicyName(),
                    modelIds,
                    selection.index());
        } catch (BusinessException exception) {
            return directDecision(defaultChatModel(), sceneType, "路由候选模型不可用，使用默认聊天模型");
        }
        decision.setFallbackEnabled(Boolean.TRUE.equals(policy.getFallbackEnabled()));
        decision.setReason("命中模型路由策略：" + policy.getPolicyName()
                + (selection.stickyHit() ? "，会话粘性复用首次选定的模型" : "，按权重分发命中"));
        return decision;
    }

    /**
     * 按权重在候选池内选择下标。
     *
     * <p>纯方法（P8 先例）：外部注入随机值 {@code r}（[0,1)），测试无需控制随机源。
     * weight 为 null 按 1 计、<=0 按 0 计（不参与分发）；总权重 <=0 或池为空时兜底返回 0 不抛异常。</p>
     *
     * @param candidates 候选池（已按 priority 分组）
     * @param r 随机值 [0,1)
     * @return 命中的候选下标
     */
    int pickWeightedIndex(List<ModelRouteCandidateEntity> candidates, double r) {
        if (candidates.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (ModelRouteCandidateEntity candidate : candidates) {
            BigDecimal weight = candidate.getWeight();
            total += weight == null ? 1 : Math.max(weight.doubleValue(), 0);
        }
        if (total <= 0) {
            return 0;
        }
        double target = r * total;
        double cumulative = 0;
        for (int i = 0; i < candidates.size(); i++) {
            BigDecimal weight = candidates.get(i).getWeight();
            cumulative += weight == null ? 1 : Math.max(weight.doubleValue(), 0);
            // 严格大于：r=0 落在第一个正权重候选，零权重前缀不会被 r=0 误选
            if (cumulative > target) {
                return i;
            }
        }
        return candidates.size() - 1;
    }

    /**
     * 在放量池内选择模型下标，并处理会话粘性。
     *
     * <p>stickyKey 有文本且池内存在未过期的粘性模型时直接复用（stickyHit=true，不消耗随机值）；
     * 否则按权重新选并写入粘性记录。下标均相对全候选列表 {@code allModelIds}（供回退链使用）。</p>
     *
     * @param pool 放量池（最低优先级组）
     * @param allModelIds 全健康候选模型ID列表
     * @param stickyKey 会话粘性 key（sceneType:runId），可为 null
     * @param r 随机值 [0,1)
     * @return 选择结果
     */
    WeightedSelection selectWeightedIndex(List<ModelRouteCandidateEntity> pool,
                                          List<String> allModelIds,
                                          String stickyKey,
                                          double r) {
        if (StringUtils.hasText(stickyKey)) {
            StickyEntry entry = stickyModelByRun.get(stickyKey);
            if (entry != null && entry.expiresAtMillis() > System.currentTimeMillis()
                    && pool.stream().anyMatch(candidate -> candidate.getModelId().equals(entry.modelId()))) {
                return new WeightedSelection(allModelIds.indexOf(entry.modelId()), true);
            }
        }
        String picked = pool.get(pickWeightedIndex(pool, r)).getModelId();
        if (StringUtils.hasText(stickyKey)) {
            stickyModelByRun.put(stickyKey, new StickyEntry(picked, System.currentTimeMillis() + STICKY_TTL_MILLIS));
            if (stickyPutCounter.incrementAndGet() % 256 == 0) {
                evictExpiredSticky();
            }
        }
        return new WeightedSelection(allModelIds.indexOf(picked), false);
    }

    /** 会话粘性条目：已选定模型与过期时间。 */
    record StickyEntry(String modelId, long expiresAtMillis) {
    }

    /** 加权选择结果：全候选列表下标 + 是否命中会话粘性。 */
    record WeightedSelection(int index, boolean stickyHit) {
    }

    /** 抽检清理已过期的会话粘性条目。 */
    private void evictExpiredSticky() {
        long now = System.currentTimeMillis();
        stickyModelByRun.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    /** 匹配规则解析结果三态：GLOBAL / WORKSPACE / SKIP（非法规则，不可参与匹配）。 */
    record MatchRuleParsed(String scope, List<String> workspaceIds) {
        /** 非法 JSON 解析结果。 */
        static MatchRuleParsed skip() {
            return new MatchRuleParsed("SKIP", List.of());
        }
    }

    /**
     * 解析策略匹配规则 JSON。
     *
     * <p>鲁棒解析：非法 JSON → SKIP（策略不可匹配）；缺 scope / scope 非 WORKSPACE → GLOBAL；
     * WORKSPACE 但 workspaceIds 为空 → GLOBAL（避免永不命中）。</p>
     *
     * @param matchRule 匹配规则 JSON，可为 null
     * @return 解析结果
     */
    MatchRuleParsed parseMatchRule(String matchRule) {
        if (!StringUtils.hasText(matchRule)) {
            return new MatchRuleParsed("GLOBAL", List.of());
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(matchRule);
        } catch (Exception exception) {
            return MatchRuleParsed.skip();
        }
        if (!node.isObject()) {
            return MatchRuleParsed.skip();
        }
        if (StringUtils.hasText(node.path("scope").asText(""))
                && "WORKSPACE".equalsIgnoreCase(node.path("scope").asText("").trim())) {
            List<String> workspaceIds = new ArrayList<>();
            JsonNode idsNode = node.path("workspaceIds");
            if (idsNode.isArray()) {
                idsNode.forEach(item -> {
                    String id = item.asText("");
                    if (StringUtils.hasText(id)) {
                        workspaceIds.add(id.trim());
                    }
                });
            }
            if (workspaceIds.isEmpty()) {
                return new MatchRuleParsed("GLOBAL", List.of());
            }
            return new MatchRuleParsed("WORKSPACE", workspaceIds);
        }
        return new MatchRuleParsed("GLOBAL", List.of());
    }

    /**
     * 按工作空间从策略列表中选出第一个命中策略。
     *
     * <p>策略列表须已按更新时间倒序；SKIP（非法规则）跳过；workspaceId 为空时仅 GLOBAL 命中；
     * workspaceId 非空时 GLOBAL 或 WORKSPACE 包含即命中。未命中返回 null。</p>
     *
     * @param policies 候选策略列表（已排序）
     * @param workspaceId 当前工作空间ID，可为 null
     * @return 命中的策略，未命中返回 null
     */
    ModelRoutePolicyEntity selectPolicyForWorkspace(List<ModelRoutePolicyEntity> policies, String workspaceId) {
        if (policies == null || policies.isEmpty()) {
            return null;
        }
        boolean hasWorkspace = StringUtils.hasText(workspaceId);
        String normalizedWorkspaceId = hasWorkspace ? workspaceId.trim() : null;
        for (ModelRoutePolicyEntity policy : policies) {
            if (policy == null) {
                continue;
            }
            MatchRuleParsed parsed = parseMatchRule(policy.getMatchRule());
            if ("SKIP".equals(parsed.scope())) {
                continue;
            }
            if (!hasWorkspace) {
                if ("GLOBAL".equals(parsed.scope())) {
                    return policy;
                }
                continue;
            }
            if ("GLOBAL".equals(parsed.scope())
                    || (parsed.workspaceIds() != null && parsed.workspaceIds().contains(normalizedWorkspaceId))) {
                return policy;
            }
        }
        return null;
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
        entity.setMatchRule(buildMatchRule(request));
        entity.setFallbackEnabled(!Boolean.FALSE.equals(request.getFallbackEnabled()));
        // null 直接落库（列可空），读取侧用 DEFAULT_BREAKER_* 常量兜底
        entity.setBreakerFailureThreshold(request.getBreakerFailureThreshold());
        entity.setBreakerTimeoutSeconds(request.getBreakerTimeoutSeconds());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
    }

    /**
     * 由请求生成匹配规则 JSON。
     *
     * <p>请求显式携带 matchRule 时原样保留（兼容历史裸 JSON 提交）；
     * 否则由结构化 matchScope/workspaceIds 生成；空对象兜底为 GLOBAL。</p>
     *
     * @param request 策略保存请求
     * @return 匹配规则 JSON
     */
    private String buildMatchRule(ModelGatewayDtos.PolicyRequest request) {
        if (request.getMatchRule() != null && StringUtils.hasText(request.getMatchRule())) {
            return request.getMatchRule();
        }
        if ("WORKSPACE".equalsIgnoreCase(safeText(request.getMatchScope()))) {
            List<String> workspaceIds = request.getWorkspaceIds() == null
                    ? List.of()
                    : request.getWorkspaceIds().stream()
                            .filter(StringUtils::hasText)
                            .map(String::trim)
                            .distinct()
                            .toList();
            if (workspaceIds.isEmpty()) {
                return "{\"scope\":\"GLOBAL\"}";
            }
            try {
                ObjectNode rule = objectMapper.createObjectNode();
                rule.put("scope", "WORKSPACE");
                ArrayNode idsNode = rule.putArray("workspaceIds");
                workspaceIds.forEach(idsNode::add);
                return objectMapper.writeValueAsString(rule);
            } catch (Exception exception) {
                return "{\"scope\":\"GLOBAL\"}";
            }
        }
        return "{\"scope\":\"GLOBAL\"}";
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
        MatchRuleParsed parsed = parseMatchRule(entity.getMatchRule());
        summary.setMatchScope("SKIP".equals(parsed.scope()) ? "GLOBAL" : parsed.scope());
        summary.setWorkspaceIds("WORKSPACE".equals(parsed.scope()) ? parsed.workspaceIds() : List.of());
        summary.setFallbackEnabled(entity.getFallbackEnabled());
        summary.setBreakerFailureThreshold(entity.getBreakerFailureThreshold());
        summary.setBreakerTimeoutSeconds(entity.getBreakerTimeoutSeconds());
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
