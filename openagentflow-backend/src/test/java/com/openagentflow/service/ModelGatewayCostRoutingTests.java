package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.model.ModelRouteDecision;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.entity.ModelRouteCandidateEntity;
import com.openagentflow.entity.ModelRoutePolicyEntity;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.mapper.ModelProviderMapper;
import com.openagentflow.mapper.ModelRouteCandidateMapper;
import com.openagentflow.mapper.ModelRoutePolicyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 模型网关成本优化路由测试（Phase 11，routing_mode = cost_first）。
 *
 * <p>pickCheapestIndex（按估算成本选最便宜，并列走 weight → createdAt）与
 * selectCostFirstIndex（会话粘性 + 成本优选）为 package-private 纯方法，成本函数显式注入，
 * 无需控制价格表即可直接断言；smoke 接线用例验证 resolveAgentChatRoute 在 cost_first 下
 * 命中便宜模型、candidateModelIds 按成本升序，且 weighted 模式 reason 保持不变。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelGatewayCostRoutingTests {

    @Mock
    private ModelRoutePolicyMapper policyMapper;

    @Mock
    private ModelRouteCandidateMapper candidateMapper;

    @Mock
    private ModelConfigMapper configMapper;

    @Mock
    private ModelProviderMapper providerMapper;

    @Mock
    private ModelProviderService providerService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UsageCostService usageCostService;

    private ModelGatewayService service;

    @BeforeEach
    void setUp() {
        service = new ModelGatewayService(policyMapper, candidateMapper, configMapper, providerMapper,
                providerService, jdbcTemplate, new ObjectMapper(), usageCostService);
    }

    // ---- pickCheapestIndex：成本与并列规则 ----

    @Test
    void shouldPickCheapestCandidate() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, null), candidate("b", 1, null));

        assertThat(ModelGatewayService.pickCheapestIndex(pool, modelId ->
                modelId.equals("a") ? BigDecimal.TEN : BigDecimal.ONE)).isEqualTo(1);
    }

    @Test
    void shouldPickHigherWeightOnCostTie() {
        // 同成本时 weight 高者胜（a），与 pickWeightedIndex 口径一致
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 7), candidate("b", 1, 3));

        assertThat(ModelGatewayService.pickCheapestIndex(pool, modelId -> BigDecimal.ONE)).isZero();
    }

    @Test
    void shouldPickEarlierCreatedAtOnFullTie() {
        // 同成本同权重 → createdAt 早者胜（b）
        List<ModelRouteCandidateEntity> pool = List.of(
                candidateWithTime("a", 1, 7, LocalDateTime.parse("2026-01-02T10:00:00")),
                candidateWithTime("b", 1, 7, LocalDateTime.parse("2026-01-01T10:00:00")));

        assertThat(ModelGatewayService.pickCheapestIndex(pool, modelId -> BigDecimal.ONE)).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroForEmptyPool() {
        assertThat(ModelGatewayService.pickCheapestIndex(List.of(), modelId -> BigDecimal.ONE)).isZero();
    }

    @Test
    void shouldTreatNullCostAsZero() {
        // a 成本缺失按 0 计 → 最便宜；b 成本 1
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, null), candidate("b", 1, null));

        assertThat(ModelGatewayService.pickCheapestIndex(pool, modelId ->
                modelId.equals("a") ? null : BigDecimal.ONE)).isZero();
    }

    // ---- selectCostFirstIndex：会话粘性 ----

    @Test
    void shouldPickCheapestAndWriteSticky() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, null), candidate("b", 1, null));

        ModelGatewayService.WeightedSelection selection = service.selectCostFirstIndex(
                pool, List.of("a", "b"), "AGENT_CHAT:run-1",
                modelId -> modelId.equals("a") ? BigDecimal.TEN : BigDecimal.ONE);

        assertThat(selection.index()).isEqualTo(1);
        assertThat(selection.stickyHit()).isFalse();

        // 再次路由 → 粘性复用 b，即使 a 已变便宜也不跳
        ModelGatewayService.WeightedSelection second = service.selectCostFirstIndex(
                pool, List.of("a", "b"), "AGENT_CHAT:run-1",
                modelId -> modelId.equals("a") ? BigDecimal.ZERO : BigDecimal.ONE);
        assertThat(second.index()).isEqualTo(1);
        assertThat(second.stickyHit()).isTrue();
    }

    @Test
    void shouldReusePinnedNonCheapest() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, null), candidate("b", 1, null));
        // 首次同成本 → 并列回落池内序 → a
        ModelGatewayService.WeightedSelection first = service.selectCostFirstIndex(
                pool, List.of("a", "b"), "AGENT_CHAT:run-1", modelId -> BigDecimal.ONE);
        assertThat(first.index()).isZero();

        // a 成本变贵但 pin 仍在池内 → 仍复用 a
        ModelGatewayService.WeightedSelection second = service.selectCostFirstIndex(
                pool, List.of("a", "b"), "AGENT_CHAT:run-1",
                modelId -> modelId.equals("a") ? BigDecimal.TEN : BigDecimal.ONE);
        assertThat(second.index()).isZero();
        assertThat(second.stickyHit()).isTrue();
    }

    @Test
    void shouldRepickWhenPinnedModelLeftPool() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, null), candidate("b", 1, null));
        service.selectCostFirstIndex(pool, List.of("a", "b"), "AGENT_CHAT:run-1", modelId -> BigDecimal.ONE); // pin = a

        // a 离开放量池（不再健康）→ 重选重写，仅 b 可选
        ModelGatewayService.WeightedSelection selection = service.selectCostFirstIndex(
                List.of(candidate("b", 2, null)), List.of("b"), "AGENT_CHAT:run-1", modelId -> BigDecimal.ONE);

        assertThat(selection.index()).isZero();
        assertThat(selection.stickyHit()).isFalse();
    }

    // ---- smoke：cost_first 全链路 ----

    @Test
    void shouldRouteToCheapestAndSortCandidatesByCost() {
        ModelRoutePolicyEntity globalPolicy = policy("gl-cost", "{\"scope\":\"GLOBAL\"}", "cost_first");
        globalPolicy.setFallbackEnabled(true);
        when(policyMapper.selectList(any())).thenReturn(List.of(globalPolicy));
        when(candidateMapper.selectList(any()))
                .thenReturn(List.of(candidate("model-a", 1, 7), candidate("model-b", 1, 3)));

        ModelConfigEntity modelA = model("model-a", "gpt-4o-mini");
        ModelConfigEntity modelB = model("model-b", "claude-sonnet-5");
        when(configMapper.selectById("model-a")).thenReturn(modelA);
        when(configMapper.selectById("model-b")).thenReturn(modelB);
        when(providerMapper.selectById("prov-1")).thenReturn(enabledProvider("prov-1"));
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("call_count", 0L, "failure_count", 0L, "avg_latency_ms", BigDecimal.ZERO));
        // configMapper 返回同一实例，Mockito equals 匹配命中同一个 model 对象
        when(usageCostService.calculateCost(modelA, 2000, 500)).thenReturn(BigDecimal.TEN);
        when(usageCostService.calculateCost(modelB, 2000, 500)).thenReturn(BigDecimal.ONE);
        lenient().when(providerService.requireModel("model-a")).thenReturn(modelA);
        lenient().when(providerService.requireModel("model-b")).thenReturn(modelB);
        lenient().when(providerService.requireProviderByModel(modelA)).thenReturn(enabledProvider("prov-1"));
        lenient().when(providerService.requireProviderByModel(modelB)).thenReturn(enabledProvider("prov-1"));
        when(providerService.findApiKeyValue("prov-1")).thenReturn("sk-test");

        ModelRouteDecision first = service.resolveAgentChatRoute(null, null, "run-1");

        assertThat(first.getModel().getId()).isEqualTo("model-b");
        assertThat(first.getCandidateModelIds()).containsExactly("model-b", "model-a");
        assertThat(first.getReason()).contains("命中模型路由策略").contains("按成本优化命中");

        // 同 runId 二次路由必然复用首次选定的便宜模型（与价格变化无关）
        ModelRouteDecision second = service.resolveAgentChatRoute(null, null, "run-1");
        assertThat(second.getModel().getId()).isEqualTo("model-b");
        assertThat(second.getReason()).contains("命中模型路由策略").contains("会话粘性复用");
    }

    @Test
    void weightedModeKeepsWeightedReason() {
        ModelRoutePolicyEntity globalPolicy = policy("gl-weight", "{\"scope\":\"GLOBAL\"}", "weighted");
        when(policyMapper.selectList(any())).thenReturn(List.of(globalPolicy));
        when(candidateMapper.selectList(any()))
                .thenReturn(List.of(candidate("model-a", 1, 7), candidate("model-b", 1, 3)));

        ModelConfigEntity modelA = model("model-a", "gpt-4o-mini");
        ModelConfigEntity modelB = model("model-b", "claude-sonnet-5");
        when(configMapper.selectById("model-a")).thenReturn(modelA);
        when(configMapper.selectById("model-b")).thenReturn(modelB);
        when(providerMapper.selectById("prov-1")).thenReturn(enabledProvider("prov-1"));
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("call_count", 0L, "failure_count", 0L, "avg_latency_ms", BigDecimal.ZERO));
        lenient().when(providerService.requireModel("model-a")).thenReturn(modelA);
        lenient().when(providerService.requireModel("model-b")).thenReturn(modelB);
        lenient().when(providerService.requireProviderByModel(modelA)).thenReturn(enabledProvider("prov-1"));
        lenient().when(providerService.requireProviderByModel(modelB)).thenReturn(enabledProvider("prov-1"));
        when(providerService.findApiKeyValue("prov-1")).thenReturn("sk-test");

        ModelRouteDecision decision = service.resolveAgentChatRoute(null, null, "run-1");

        assertThat(decision.getCandidateModelIds()).containsExactly("model-a", "model-b");
        assertThat(decision.getReason()).contains("命中模型路由策略").contains("按权重分发命中");
    }

    // ---- helpers ----

    private ModelRoutePolicyEntity policy(String id, String matchRule, String routingMode) {
        ModelRoutePolicyEntity policy = new ModelRoutePolicyEntity();
        policy.setId(id);
        policy.setPolicyName("策略-" + id);
        policy.setMatchRule(matchRule);
        policy.setRoutingMode(routingMode);
        policy.setStatus("enabled");
        return policy;
    }

    private ModelRouteCandidateEntity candidate(String modelId, int priority, Integer weight) {
        return candidateWithTime(modelId, priority, weight, null);
    }

    private ModelRouteCandidateEntity candidateWithTime(String modelId, int priority, Integer weight,
                                                        LocalDateTime createdAt) {
        ModelRouteCandidateEntity candidate = new ModelRouteCandidateEntity();
        candidate.setId(UUID.randomUUID().toString());
        candidate.setModelId(modelId);
        candidate.setPriority(priority);
        candidate.setWeight(weight == null ? null : BigDecimal.valueOf(weight));
        candidate.setCreatedAt(createdAt);
        candidate.setEnabled(true);
        return candidate;
    }

    private ModelConfigEntity model(String id, String code) {
        ModelConfigEntity model = new ModelConfigEntity();
        model.setId(id);
        model.setModelCode(code);
        model.setStatus("enabled");
        model.setProviderId("prov-1");
        return model;
    }

    private ModelProviderEntity enabledProvider(String id) {
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setId(id);
        provider.setStatus("enabled");
        return provider;
    }
}
