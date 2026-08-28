package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.model.ModelRouteDecision;
import com.openagentflow.entity.AgentEntity;
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
import static org.mockito.Mockito.when;

/**
 * 模型网关空间级策略路由测试。
 *
 * <p>match_rule 解析（parseMatchRule）与空间命中选择（selectPolicyForWorkspace）为 package-private
 * 纯方法，无需 mock mapper 直接断言；smoke 接线用例验证 resolveAgentChatRoute 把工作空间ID传入
 * 策略命中链路（WORKSPACE 命中 + 平台级 GLOBAL 兜底）。</p>
 */
@ExtendWith(MockitoExtension.class)
class ModelGatewayPolicyRoutingTests {

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

    private ModelGatewayService service;

    @BeforeEach
    void setUp() {
        service = new ModelGatewayService(policyMapper, candidateMapper, configMapper, providerMapper,
                providerService, jdbcTemplate, new ObjectMapper());
    }

    // ---- parseMatchRule：三态解析 ----

    @Test
    void shouldParseExplicitGlobalRule() {
        ModelGatewayService.MatchRuleParsed parsed = service.parseMatchRule("{\"scope\":\"GLOBAL\"}");

        assertThat(parsed.scope()).isEqualTo("GLOBAL");
        assertThat(parsed.workspaceIds()).isEmpty();
    }

    @Test
    void shouldParseWorkspaceRuleWithTrimmedIds() {
        ModelGatewayService.MatchRuleParsed parsed = service.parseMatchRule(
                "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-1\",\" ws-2 \",\"\"]}");

        assertThat(parsed.scope()).isEqualTo("WORKSPACE");
        assertThat(parsed.workspaceIds()).containsExactly("ws-1", "ws-2");
    }

    @Test
    void shouldDefaultMissingScopeToGlobal() {
        assertThat(service.parseMatchRule("{\"description\":\"全局兜底\"}").scope()).isEqualTo("GLOBAL");
    }

    @Test
    void shouldSkipIllegalJson() {
        assertThat(service.parseMatchRule("not-json{{").scope()).isEqualTo("SKIP");
    }

    @Test
    void shouldSkipNonObjectJson() {
        assertThat(service.parseMatchRule("[1,2,3]").scope()).isEqualTo("SKIP");
    }

    @Test
    void shouldDefaultEmptyWorkspaceListToGlobal() {
        assertThat(service.parseMatchRule("{\"scope\":\"WORKSPACE\",\"workspaceIds\":[]}").scope())
                .isEqualTo("GLOBAL");
    }

    @Test
    void shouldParseNullAsGlobal() {
        assertThat(service.parseMatchRule(null).scope()).isEqualTo("GLOBAL");
    }

    // ---- selectPolicyForWorkspace：空间命中选择 ----

    @Test
    void shouldPickGlobalWhenWorkspaceIdIsNull() {
        List<ModelRoutePolicyEntity> policies = List.of(
                policy("ws-p", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-1\"]}"),
                policy("gl-p", "{\"scope\":\"GLOBAL\"}"));

        assertThat(service.selectPolicyForWorkspace(policies, null))
                .extracting(ModelRoutePolicyEntity::getId).isEqualTo("gl-p");
        assertThat(service.selectPolicyForWorkspace(policies, ""))
                .extracting(ModelRoutePolicyEntity::getId).isEqualTo("gl-p");
    }

    @Test
    void shouldFallBackToGlobalWhenWorkspaceMatchesNothing() {
        List<ModelRoutePolicyEntity> policies = List.of(
                policy("ws-p", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-2\"]}"),
                policy("gl-p", "{\"scope\":\"GLOBAL\"}"));

        assertThat(service.selectPolicyForWorkspace(policies, "ws-1"))
                .extracting(ModelRoutePolicyEntity::getId).isEqualTo("gl-p");
    }

    @Test
    void shouldPickWorkspacePolicyWhenItMatches() {
        List<ModelRoutePolicyEntity> policies = List.of(
                policy("ws-other", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-2\"]}"),
                policy("ws-hit", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-1\"]}"));

        assertThat(service.selectPolicyForWorkspace(policies, "ws-1"))
                .extracting(ModelRoutePolicyEntity::getId).isEqualTo("ws-hit");
    }

    @Test
    void shouldPickFirstGlobalInListOrder() {
        List<ModelRoutePolicyEntity> policies = List.of(
                policy("gl-new", "{\"scope\":\"GLOBAL\"}"),
                policy("gl-old", "{\"scope\":\"GLOBAL\"}"));

        assertThat(service.selectPolicyForWorkspace(policies, "ws-1"))
                .extracting(ModelRoutePolicyEntity::getId).isEqualTo("gl-new");
    }

    @Test
    void shouldReturnNullWhenNothingMatchesAndNoGlobal() {
        List<ModelRoutePolicyEntity> policies = List.of(
                policy("ws-other", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-2\"]}"));

        assertThat(service.selectPolicyForWorkspace(policies, "ws-1")).isNull();
    }

    @Test
    void shouldSkipIllegalRuleAndContinue() {
        List<ModelRoutePolicyEntity> policies = List.of(
                policy("bad-p", "not-json"),
                policy("ws-hit", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-1\"]}"));

        assertThat(service.selectPolicyForWorkspace(policies, "ws-1"))
                .extracting(ModelRoutePolicyEntity::getId).isEqualTo("ws-hit");
    }

    @Test
    void shouldTrimWorkspaceIdWhenMatching() {
        List<ModelRoutePolicyEntity> policies = List.of(
                policy("ws-hit", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-1\"]}"));

        assertThat(service.selectPolicyForWorkspace(policies, " ws-1 "))
                .extracting(ModelRoutePolicyEntity::getId).isEqualTo("ws-hit");
    }

    // ---- smoke：resolveAgentChatRoute 接线 ----

    @Test
    void shouldRouteAgentChatToWorkspacePolicy() {
        ModelRoutePolicyEntity workspacePolicy = policy("ws-policy", "{\"scope\":\"WORKSPACE\",\"workspaceIds\":[\"ws-9\"]}");
        workspacePolicy.setFallbackEnabled(true);
        when(policyMapper.selectList(any())).thenReturn(List.of(workspacePolicy));
        when(candidateMapper.selectList(any())).thenReturn(List.of(candidate("model-a")));

        ModelConfigEntity model = model("model-a", "gpt-4o-mini");
        when(configMapper.selectById("model-a")).thenReturn(model);
        when(providerMapper.selectById("prov-1")).thenReturn(enabledProvider("prov-1"));
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("call_count", 0L, "failure_count", 0L, "avg_latency_ms", BigDecimal.ZERO));
        when(providerService.requireModel("model-a")).thenReturn(model);
        when(providerService.requireProviderByModel(model)).thenReturn(enabledProvider("prov-1"));
        when(providerService.findApiKeyValue("prov-1")).thenReturn("sk-test");

        AgentEntity agent = new AgentEntity();
        agent.setWorkspaceId("ws-9");

        ModelRouteDecision decision = service.resolveAgentChatRoute(null, agent, null);

        assertThat(decision.getModel().getModelCode()).isEqualTo("gpt-4o-mini");
        assertThat(decision.getRoutePolicyName()).isEqualTo("策略-ws-policy");
        assertThat(decision.getReason()).contains("命中模型路由策略");
        assertThat(decision.getApiKey()).isEqualTo("sk-test");
    }

    @Test
    void shouldRoutePlatformAgentToGlobalPolicy() {
        ModelRoutePolicyEntity globalPolicy = policy("gl-policy", "{\"scope\":\"GLOBAL\"}");
        globalPolicy.setFallbackEnabled(true);
        when(policyMapper.selectList(any())).thenReturn(List.of(globalPolicy));
        when(candidateMapper.selectList(any())).thenReturn(List.of(candidate("model-b")));

        ModelConfigEntity model = model("model-b", "claude-sonnet-5");
        when(configMapper.selectById("model-b")).thenReturn(model);
        when(providerMapper.selectById("prov-1")).thenReturn(enabledProvider("prov-1"));
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("call_count", 0L, "failure_count", 0L, "avg_latency_ms", BigDecimal.ZERO));
        when(providerService.requireModel("model-b")).thenReturn(model);
        when(providerService.requireProviderByModel(model)).thenReturn(enabledProvider("prov-1"));
        when(providerService.findApiKeyValue("prov-1")).thenReturn("sk-test");

        ModelRouteDecision decision = service.resolveAgentChatRoute(null, null, null);

        assertThat(decision.getRoutePolicyName()).isEqualTo("策略-gl-policy");
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

    private ModelRouteCandidateEntity candidate(String modelId) {
        ModelRouteCandidateEntity candidate = new ModelRouteCandidateEntity();
        candidate.setId(UUID.randomUUID().toString());
        candidate.setModelId(modelId);
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
