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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 模型网关灰度/加权路由与会话粘性测试。
 *
 * <p>pickWeightedIndex（按权重比例分发）与 selectWeightedIndex（会话粘性 + 权重）为 package-private
 * 方法，随机值作为显式参数注入，测试可直接传边界值；smoke 接线用例验证 resolveAgentChatRoute 在同一
 * runId 下连续路由复用首次选定的模型。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelGatewayWeightedRoutingTests {

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

    // ---- pickWeightedIndex：权重分发边界 ----

    @Test
    void shouldTreatNullWeightAsOne() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, null), candidate("b", 1, null));

        assertThat(service.pickWeightedIndex(pool, 0.0)).isZero();
        assertThat(service.pickWeightedIndex(pool, 0.99)).isEqualTo(1);
    }

    @Test
    void shouldDistributeByWeightProportion() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 7), candidate("b", 1, 3));

        assertThat(service.pickWeightedIndex(pool, 0.0)).isZero();
        assertThat(service.pickWeightedIndex(pool, 0.69)).isZero();
        assertThat(service.pickWeightedIndex(pool, 0.70)).isEqualTo(1);
        assertThat(service.pickWeightedIndex(pool, 0.99)).isEqualTo(1);
    }

    @Test
    void shouldSkipZeroWeightCandidatesInDistribution() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 1), candidate("b", 1, 0), candidate("c", 1, 1));

        assertThat(service.pickWeightedIndex(pool, 0.0)).isZero();
        assertThat(service.pickWeightedIndex(pool, 0.49)).isZero();
        assertThat(service.pickWeightedIndex(pool, 0.51)).isEqualTo(2);
    }

    @Test
    void shouldNotPickZeroWeightFirstCandidateAtRZero() {
        // 零权重候选在前时，r=0 也必须落到第一个正权重候选
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 0), candidate("b", 1, 1));

        assertThat(service.pickWeightedIndex(pool, 0.0)).isEqualTo(1);
    }

    @Test
    void shouldFallbackToFirstWhenTotalWeightNonPositive() {
        assertThat(service.pickWeightedIndex(List.of(), 0.5)).isZero();
        assertThat(service.pickWeightedIndex(List.of(candidate("a", 1, 0), candidate("b", 1, 0)), 0.5)).isZero();
        assertThat(service.pickWeightedIndex(List.of(candidate("only", 1, 0)), 0.5)).isZero();
    }

    @Test
    void shouldPickSingleCandidate() {
        assertThat(service.pickWeightedIndex(List.of(candidate("only", 1, 5)), 0.99)).isZero();
    }

    // ---- selectWeightedIndex：会话粘性 ----

    @Test
    void shouldPickByWeightWhenNoStickyKey() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 7), candidate("b", 1, 3));

        ModelGatewayService.WeightedSelection selection = service.selectWeightedIndex(pool, List.of("a", "b"), null, 0.0);

        assertThat(selection.index()).isZero();
        assertThat(selection.stickyHit()).isFalse();
    }

    @Test
    void shouldReusePinnedModelOnSecondRoute() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 7), candidate("b", 1, 3));

        ModelGatewayService.WeightedSelection first = service.selectWeightedIndex(pool, List.of("a", "b"), "AGENT_CHAT:run-1", 0.0);
        assertThat(first.index()).isZero();
        assertThat(first.stickyHit()).isFalse();

        // 第二次路由随机值取最大也不跳动 → 粘性短路径，不消耗随机值
        ModelGatewayService.WeightedSelection second = service.selectWeightedIndex(pool, List.of("a", "b"), "AGENT_CHAT:run-1", 0.99);
        assertThat(second.index()).isZero();
        assertThat(second.stickyHit()).isTrue();
    }

    @Test
    void shouldRepickWhenPinnedModelLeftThePool() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 7), candidate("b", 1, 3));
        service.selectWeightedIndex(pool, List.of("a", "b"), "AGENT_CHAT:run-1", 0.0); // pin = a

        // a 离开放量池（不再健康），pool 只剩 b → 重选重写
        ModelGatewayService.WeightedSelection selection = service.selectWeightedIndex(
                List.of(candidate("b", 2, 1)), List.of("b"), "AGENT_CHAT:run-1", 0.99);

        assertThat(selection.index()).isZero();
        assertThat(selection.stickyHit()).isFalse();
    }

    @Test
    void shouldKeepStickyIndependentAcrossRuns() {
        List<ModelRouteCandidateEntity> pool = List.of(candidate("a", 1, 7), candidate("b", 1, 3));
        service.selectWeightedIndex(pool, List.of("a", "b"), "AGENT_CHAT:run-1", 0.0);  // run-1 → a
        ModelGatewayService.WeightedSelection run2 = service.selectWeightedIndex(pool, List.of("a", "b"), "AGENT_CHAT:run-2", 0.99); // run-2 → b
        assertThat(run2.index()).isEqualTo(1);
        assertThat(run2.stickyHit()).isFalse();

        ModelGatewayService.WeightedSelection run1Again = service.selectWeightedIndex(pool, List.of("a", "b"), "AGENT_CHAT:run-1", 0.99);
        assertThat(run1Again.index()).isZero();
        assertThat(run1Again.stickyHit()).isTrue();
    }

    // ---- smoke：resolveAgentChatRoute 全链路 ----

    @Test
    void shouldReuseSameModelWithinSameRunId() {
        ModelRoutePolicyEntity globalPolicy = policy("gl-policy", "{\"scope\":\"GLOBAL\"}");
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
        lenient().when(providerService.requireModel("model-a")).thenReturn(modelA);
        lenient().when(providerService.requireModel("model-b")).thenReturn(modelB);
        lenient().when(providerService.requireProviderByModel(modelA)).thenReturn(enabledProvider("prov-1"));
        lenient().when(providerService.requireProviderByModel(modelB)).thenReturn(enabledProvider("prov-1"));
        when(providerService.findApiKeyValue("prov-1")).thenReturn("sk-test");

        ModelRouteDecision first = service.resolveAgentChatRoute(null, null, "run-1");
        ModelRouteDecision second = service.resolveAgentChatRoute(null, null, "run-1");

        assertThat(first.getReason()).contains("命中模型路由策略").contains("按权重分发命中");
        assertThat(first.getCandidateModelIds()).containsExactly("model-a", "model-b");
        assertThat(first.getApiKey()).isEqualTo("sk-test");
        // 第二次路由必然复用首次选定的模型（与随机值无关，确定性断言）
        assertThat(second.getModel().getId()).isEqualTo(first.getModel().getId());
        assertThat(second.getReason()).contains("命中模型路由策略").contains("会话粘性复用");
    }

    // ---- helpers ----

    private ModelRoutePolicyEntity policy(String id, String matchRule) {
        ModelRoutePolicyEntity policy = new ModelRoutePolicyEntity();
        policy.setId(id);
        policy.setPolicyName("策略-" + id);
        policy.setMatchRule(matchRule);
        policy.setStatus("enabled");
        return policy;
    }

    private ModelRouteCandidateEntity candidate(String modelId, int priority, Integer weight) {
        ModelRouteCandidateEntity candidate = new ModelRouteCandidateEntity();
        candidate.setId(UUID.randomUUID().toString());
        candidate.setModelId(modelId);
        candidate.setPriority(priority);
        candidate.setWeight(weight == null ? null : BigDecimal.valueOf(weight));
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
