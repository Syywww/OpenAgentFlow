package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** P73 Prompt 编译器核心规则测试。 */
class PromptCompilerTests {

    /** 强类型变量应支持默认值、枚举和敏感值掩码。 */
    @Test
    void shouldCompileTypedVariablesAndMaskSensitiveValues() {
        PromptCompiler compiler = new PromptCompiler(new ObjectMapper());
        String schema = """
                [
                  {"name":"tone","type":"string","required":true,"defaultValue":"专业","enumValues":["专业","友好"]},
                  {"name":"customer_name","type":"string","required":true,"sensitive":true}
                ]
                """;

        PromptRuntimeDtos.CompileResult result = compiler.compile(
                "请以{{tone}}语气服务{{customer_name}}",
                schema,
                Map.of("customer_name", "张三"),
                List.of(),
                false
        );

        assertThat(result.renderedPrompt).isEqualTo("请以专业语气服务张三");
        assertThat(result.missingVariables).isEmpty();
        assertThat(result.variableSources).containsEntry("tone", "default");
        assertThat(result.variableSources).containsEntry("customer_name", "request:sensitive");
        assertThat(result.sensitiveVariableNames).containsExactly("customer_name");
        assertThat(result.contentHash).hasSize(64);
        assertThat(result.estimatedTokens).isPositive();
    }

    /** 缺少必填变量时应保留缺失项，严格模式不得静默生成。 */
    @Test
    void shouldReportMissingRequiredVariables() {
        PromptCompiler compiler = new PromptCompiler(new ObjectMapper());
        String schema = "[{\"name\":\"question\",\"type\":\"string\",\"required\":true}]";

        PromptRuntimeDtos.CompileResult result = compiler.compile(
                "用户问题：{{question}}",
                schema,
                Map.of(),
                List.of(),
                false
        );

        assertThat(result.missingVariables).containsExactly("question");
        assertThat(result.renderedPrompt).contains("{{question}}");
    }

    /** Prompt 分层应按顺序装配角色、任务、RAG、工具和安全约束。 */
    @Test
    void shouldComposePromptLayersInStableOrder() {
        PromptCompiler compiler = new PromptCompiler(new ObjectMapper());
        List<PromptRuntimeDtos.PromptLayer> layers = List.of(
                new PromptRuntimeDtos.PromptLayer("safety", "安全约束", "不得泄露密钥", 90),
                new PromptRuntimeDtos.PromptLayer("role", "角色", "你是企业客服", 10),
                new PromptRuntimeDtos.PromptLayer("rag", "知识证据", "退款需要人工确认", 50)
        );

        PromptRuntimeDtos.CompileResult result = compiler.compile(
                "回答用户问题",
                "[]",
                Map.of(),
                layers,
                false
        );

        assertThat(result.renderedPrompt).containsSubsequence("[角色]", "[知识证据]", "[安全约束]", "回答用户问题");
        assertThat(result.layers).extracting(layer -> layer.layerCode)
                .containsExactly("role", "rag", "safety", "template");
    }
}
