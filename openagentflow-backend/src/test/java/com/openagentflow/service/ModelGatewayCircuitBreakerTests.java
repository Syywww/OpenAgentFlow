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

import static com.openagentflow.service.ModelGatewayService.BreakerStatus.CLOSED;
import static com.openagentflow.service.ModelGatewayService.BreakerStatus.HALF_OPEN;
import static com.openagentflow.service.ModelGatewayService.BreakerStatus.OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 模型网关熔断器测试（Phase 10）。
 *
 * <p>纯状态机方法（afterFailure / afterSuccess / isBreakerExcluded / advanceOnRead）为
 * package-private static，时间戳显式注入可直接断言时间边界；集成用例通过
 * recordLlmFailure / recordLlmSuccess 驱动内存熔断 map，再经 resolveAgentChatRoute /
 * nextFallbackDecision 验证路由与回退链对 OPEN 候选的排除。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelGatewayCircuitBreakerTests {

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

    // ---- 纯状态机：阈值与打开 ----

    @Test
    void shouldStayClosedBelowThreshold() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        state = ModelGatewayService.afterFailure(state, 5, 60, 1_000);
        state = ModelGatewayService.afterFailure(state, 5, 60, 2_000);

        assertThat(state.status()).isEqualTo(CLOSED);
        assertThat(state.consecutiveFailures()).isEqualTo(2);
        assertThat(ModelGatewayService.isBreakerExcluded(state, 60, 3_000)).isFalse();
    }

    @Test
    void shouldOpenAtThreshold() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        for (int i = 0; i < 5; i++) {
            state = ModelGatewayService.afterFailure(state, 5, 60, 1_000L * i);
        }

        assertThat(state.status()).isEqualTo(OPEN);
        assertThat(state.consecutiveFailures()).isEqualTo(5);
        // 达到阈值的时刻起即被排除
        assertThat(ModelGatewayService.isBreakerExcluded(state, 60, state.openedAtMillis())).isTrue();
    }

    // ---- 纯状态机：超时与半开 ----

    @Test
    void shouldExcludeOnlyWithinTimeoutWindow() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        for (int i = 0; i < 5; i++) {
            state = ModelGatewayService.afterFailure(state, 5, 60, 1_000L * i);
        }
        long openedAt = state.openedAtMillis();

        assertThat(ModelGatewayService.isBreakerExcluded(state, 60, openedAt + 59_999)).isTrue();
        // 边界：达到超时时长即不再排除
        assertThat(ModelGatewayService.isBreakerExcluded(state, 60, openedAt + 60_000)).isFalse();
    }

    @Test
    void shouldLazilyAdvanceExpiredOpenToHalfOpenOnRead() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        for (int i = 0; i < 5; i++) {
            state = ModelGatewayService.afterFailure(state, 5, 60, 1_000L * i);
        }

        // 未超时读取：保持 OPEN
        assertThat(ModelGatewayService.advanceOnRead(state, 60, state.openedAtMillis() + 59_999).status())
                .isEqualTo(OPEN);
        // 超时读取：惰性转半开，放行探测流量
        assertThat(ModelGatewayService.advanceOnRead(state, 60, state.openedAtMillis() + 60_000).status())
                .isEqualTo(HALF_OPEN);
    }

    @Test
    void shouldCloseWhenHalfOpenSucceeds() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        for (int i = 0; i < 5; i++) {
            state = ModelGatewayService.afterFailure(state, 5, 60, 1_000L * i);
        }
        state = ModelGatewayService.advanceOnRead(state, 60, state.openedAtMillis() + 60_000);

        ModelGatewayService.BreakerState recovered = ModelGatewayService.afterSuccess(state, state.openedAtMillis() + 61_000);

        assertThat(recovered.status()).isEqualTo(CLOSED);
        assertThat(recovered.consecutiveFailures()).isZero();
    }

    @Test
    void shouldReopenWhenHalfOpenProbeFails() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        for (int i = 0; i < 5; i++) {
            state = ModelGatewayService.afterFailure(state, 5, 60, 1_000L * i);
        }
        long openedAt = state.openedAtMillis();
        state = ModelGatewayService.advanceOnRead(state, 60, openedAt + 60_000);

        // 半开探测失败：回 OPEN 并刷新 openedAt，重新计时
        ModelGatewayService.BreakerState reopened = ModelGatewayService.afterFailure(state, 5, 60, openedAt + 61_000);

        assertThat(reopened.status()).isEqualTo(OPEN);
        assertThat(reopened.openedAtMillis()).isEqualTo(openedAt + 61_000);
    }

    // ---- 纯状态机：OPEN 吸收与成功语义 ----

    @Test
    void shouldAbsorbFailureWhileOpenWithoutExtendingTimeout() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        for (int i = 0; i < 5; i++) {
            state = ModelGatewayService.afterFailure(state, 5, 60, 1_000L * i);
        }
        long openedAt = state.openedAtMillis();

        // OPEN 内的零星失败：吸收不动，不刷新 openedAt（否则熔断永不到期）
        ModelGatewayService.BreakerState absorbed = ModelGatewayService.afterFailure(state, 5, 60, openedAt + 30_000);

        assertThat(absorbed.status()).isEqualTo(OPEN);
        assertThat(absorbed.openedAtMillis()).isEqualTo(openedAt);
        assertThat(absorbed.consecutiveFailures()).isEqualTo(5);
    }

    @Test
    void shouldResetCountOnSuccessWhileClosed() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        state = ModelGatewayService.afterFailure(state, 5, 60, 1_000);

        ModelGatewayService.BreakerState reset = ModelGatewayService.afterSuccess(state, 2_000);

        assertThat(reset.status()).isEqualTo(CLOSED);
        assertThat(reset.consecutiveFailures()).isZero();
    }

    @Test
    void shouldIgnoreSuccessWhileOpen() {
        ModelGatewayService.BreakerState state = ModelGatewayService.BreakerState.closed();
        for (int i = 0; i < 5; i++) {
            state = ModelGatewayService.afterFailure(state, 5, 60, 1_000L * i);
        }

        ModelGatewayService.BreakerState after = ModelGatewayService.afterSuccess(state, 1_000L * 5);

        // 单一成功不足抵消失败模式：OPEN 状态成功调用不改变状态
        assertThat(after.status()).isEqualTo(OPEN);
        assertThat(after.consecutiveFailures()).isEqualTo(5);
    }

    // ---- recordLlmFailure / recordLlmSuccess 与路由集成 ----

    @Test
    void shouldExcludeTrippedCandidateAndRouteToRemaining() {
        ModelRoutePolicyEntity policy = trippingPolicy();
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(policyMapper.selectById("gl-policy")).thenReturn(policy);
        when(candidateMapper.selectList(any()))
                .thenReturn(List.of(candidate("model-a", 1, 1), candidate("model-b", 1, 1)));
        stubHealthyModels();

        // A 连续 2 次失败 → OPEN
        ModelRouteDecision tripA = tripDecision(model("model-a", "gpt-4o-mini"));
        service.recordLlmFailure(tripA);
        service.recordLlmFailure(tripA);

        stubProvider(model("model-b", "claude-sonnet-5"));
        ModelRouteDecision decision = service.resolveAgentChatRoute(null, null, null);

        // 熔断候选被剔除，且只出现在剩余候选 B
        assertThat(decision.getModel().getId()).isEqualTo("model-b");
        assertThat(decision.getCandidateModelIds()).containsExactly("model-b");
        assertThat(decision.getReason()).contains("命中模型路由策略");
    }

    @Test
    void shouldUseDefaultChatModelWhenAllCandidatesTripped() {
        ModelRoutePolicyEntity policy = trippingPolicy();
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(policyMapper.selectById("gl-policy")).thenReturn(policy);
        when(candidateMapper.selectList(any()))
                .thenReturn(List.of(candidate("model-a", 1, 1), candidate("model-b", 1, 1)));
        stubHealthyModels();

        // 两个候选都连续 2 次失败 → 全部 OPEN
        service.recordLlmFailure(tripDecision(model("model-a", "gpt-4o-mini")));
        service.recordLlmFailure(tripDecision(model("model-a", "gpt-4o-mini")));
        service.recordLlmFailure(tripDecision(model("model-b", "claude-sonnet-5")));
        service.recordLlmFailure(tripDecision(model("model-b", "claude-sonnet-5")));

        ModelConfigEntity defaultModel = model("model-default", "gpt-4o");
        when(configMapper.selectList(any())).thenReturn(List.of(defaultModel));
        stubProvider(defaultModel);

        ModelRouteDecision decision = service.resolveAgentChatRoute(null, null, null);

        assertThat(decision.getModel().getId()).isEqualTo("model-default");
        assertThat(decision.getReason()).contains("候选全部熔断");
    }

    @Test
    void shouldKeepTrippedCandidateExcludedAfterSingleSuccess() {
        ModelRoutePolicyEntity policy = trippingPolicy();
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(policyMapper.selectById("gl-policy")).thenReturn(policy);
        when(candidateMapper.selectList(any()))
                .thenReturn(List.of(candidate("model-a", 1, 1), candidate("model-b", 1, 1)));
        stubHealthyModels();

        ModelRouteDecision tripA = tripDecision(model("model-a", "gpt-4o-mini"));
        service.recordLlmFailure(tripA);
        service.recordLlmFailure(tripA);
        // OPEN 状态的成功调用不会恢复熔断
        service.recordLlmSuccess(tripA);

        stubProvider(model("model-b", "claude-sonnet-5"));
        ModelRouteDecision decision = service.resolveAgentChatRoute(null, null, null);

        assertThat(decision.getModel().getId()).isEqualTo("model-b");
    }

    // ---- nextFallbackDecision：跳过 OPEN 候选 ----

    @Test
    void shouldSkipOpenCandidateInFallbackChain() {
        when(policyMapper.selectById("gl-policy")).thenReturn(trippingPolicy());

        // B 已熔断
        service.recordLlmFailure(tripDecision(model("model-b", "claude-sonnet-5")));
        service.recordLlmFailure(tripDecision(model("model-b", "claude-sonnet-5")));

        ModelRouteDecision current = new ModelRouteDecision();
        current.setModel(model("model-a", "gpt-4o-mini"));
        current.setSceneType("AGENT_CHAT");
        current.setRoutePolicyId("gl-policy");
        current.setRoutePolicyName("策略-gl");
        current.setCandidateModelIds(List.of("model-a", "model-b", "model-c"));
        current.setCandidateIndex(0);
        current.setFallbackEnabled(true);
        current.setExplicitModel(false);

        ModelConfigEntity modelC = model("model-c", "claude-opus-5");
        stubProvider(modelC);

        ModelRouteDecision fallback = service.nextFallbackDecision(current, "上游模型调用失败");

        assertThat(fallback).isNotNull();
        assertThat(fallback.getModel().getId()).isEqualTo("model-c");
        assertThat(fallback.getCandidateIndex()).isEqualTo(2);
        assertThat(fallback.getFallbackUsed()).isTrue();
    }

    // ---- helpers ----

    /** 熔断阈值 2、时长 60 秒的全局策略。 */
    private ModelRoutePolicyEntity trippingPolicy() {
        ModelRoutePolicyEntity policy = new ModelRoutePolicyEntity();
        policy.setId("gl-policy");
        policy.setPolicyName("策略-gl");
        policy.setSceneType("AGENT_CHAT");
        policy.setMatchRule("{\"scope\":\"GLOBAL\"}");
        policy.setStatus("enabled");
        policy.setFallbackEnabled(true);
        policy.setBreakerFailureThreshold(2);
        policy.setBreakerTimeoutSeconds(60);
        return policy;
    }

    /** 带指定模型的失败决策（routePolicyId 指向熔断策略）。 */
    private ModelRouteDecision tripDecision(ModelConfigEntity model) {
        ModelRouteDecision decision = new ModelRouteDecision();
        decision.setModel(model);
        decision.setRoutePolicyId("gl-policy");
        return decision;
    }

    /** 让 healthyCandidate 的静态健康检查全部放行（无失败率、延迟、成本约束）。 */
    private void stubHealthyModels() {
        when(configMapper.selectById("model-a")).thenReturn(model("model-a", "gpt-4o-mini"));
        when(configMapper.selectById("model-b")).thenReturn(model("model-b", "claude-sonnet-5"));
        when(providerMapper.selectById("prov-1")).thenReturn(enabledProvider("prov-1"));
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("call_count", 0L, "failure_count", 0L, "avg_latency_ms", BigDecimal.ZERO));
    }

    /** 让模型解析/密钥查找放行（lenient：随机选路时仅其中一个候选被命中）。 */
    private void stubProvider(ModelConfigEntity model) {
        lenient().when(providerService.requireModel(model.getId())).thenReturn(model);
        lenient().when(providerService.requireProviderByModel(model)).thenReturn(enabledProvider("prov-1"));
        lenient().when(providerService.findApiKeyValue("prov-1")).thenReturn("sk-test");
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
