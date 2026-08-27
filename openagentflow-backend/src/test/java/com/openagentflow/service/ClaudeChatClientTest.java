package com.openagentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.chat.ToolCallRequest;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Anthropic Claude 原生适配器协议构造与解析单元测试。
 *
 * <p>不依赖真实密钥与网络，固定入参校验 Messages API 请求体契约（顶层 system 合并、
 * tool_result user 块、assistant tool_use 块、Anthropic 工具定义格式）、请求头、以及
 * 非流式/流式响应的解析（content blocks → content + toolCalls、SSE partial_json 缓冲模式）。</p>
 */
class ClaudeChatClientTest {

    private final ClaudeChatClient client = new ClaudeChatClient(new ObjectMapper());

    private final ModelConfigEntity model = model("claude-sonnet-5");

    private final ModelProviderEntity provider = provider("https://api.anthropic.com");

    private ChatRunContext context(List<ChatMessage> messages) {
        ChatRunContext context = new ChatRunContext();
        context.setModel(model);
        context.setProvider(provider);
        context.setApiKey("sk-ant-test");
        context.setMessages(messages);
        return context;
    }

    /** 多条 system 消息（首条系统提示 + 工具结果后置指令）按出现顺序合并到顶层 system 字段。 */
    @Test
    void shouldMergeSystemMessagesToTopLevelSystemField() throws Exception {
        ChatRunContext context = context(List.of(
                new ChatMessage("system", "你是智能助手"),
                new ChatMessage("user", "查一下订单"),
                new ChatMessage("system", "以上是工具执行结果，请基于工具结果回答")));

        JsonNode payload = client.buildPayload(context, false, null, null, null);

        assertThat(payload.path("system").asText())
                .isEqualTo("你是智能助手\n\n以上是工具执行结果，请基于工具结果回答");
        // 顶层 system 之外的 messages 只保留 user 消息。
        assertThat(payload.path("messages")).hasSize(1);
        assertThat(payload.path("messages").get(0).path("role").asText()).isEqualTo("user");
    }

    /** 连续 tool 消息合并为一条 user 消息的多个 tool_result block（Anthropic 无 role=tool）。 */
    @Test
    void shouldMergeConsecutiveToolMessagesIntoUserToolResultBlocks() throws Exception {
        ChatMessage tool1 = new ChatMessage("tool", "{\"success\":true,\"orderStatus\":\"已发货\"}");
        tool1.setToolCallId("toolu_1");
        tool1.setName("query_order");
        ChatMessage tool2 = new ChatMessage("tool", "{\"success\":true,\"stock\":12}");
        tool2.setToolCallId("toolu_2");
        tool2.setName("query_stock");
        ChatRunContext context = context(List.of(
                new ChatMessage("user", "查订单和库存"),
                tool1, tool2));

        JsonNode payload = client.buildPayload(context, false, null, null, null);
        JsonNode messages = payload.path("messages");

        assertThat(messages).hasSize(2);
        JsonNode userBlock = messages.get(1);
        assertThat(userBlock.path("role").asText()).isEqualTo("user");
        JsonNode content = userBlock.path("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).path("type").asText()).isEqualTo("tool_result");
        assertThat(content.get(0).path("tool_use_id").asText()).isEqualTo("toolu_1");
        assertThat(content.get(0).path("content").asText()).contains("orderStatus");
        assertThat(content.get(1).path("tool_use_id").asText()).isEqualTo("toolu_2");
    }

    /** assistant 携带工具调用时还原为 content blocks：text block + tool_use block（argumentsJson → input 对象）。 */
    @Test
    void shouldBuildAssistantToolUseBlocksFromToolCalls() throws Exception {
        ChatMessage assistant = new ChatMessage("assistant", "我来查询天气");
        ToolCallRequest call = new ToolCallRequest();
        call.setId("toolu_weather");
        call.setName("get_weather");
        call.setArgumentsJson("{\"city\":\"合肥\"}");
        assistant.setToolCalls(List.of(call));
        ChatRunContext context = context(List.of(
                new ChatMessage("user", "合肥天气"),
                assistant));

        JsonNode payload = client.buildPayload(context, false, null, null, null);
        JsonNode assistantNode = payload.path("messages").get(1);

        assertThat(assistantNode.path("role").asText()).isEqualTo("assistant");
        JsonNode blocks = assistantNode.path("content");
        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).path("type").asText()).isEqualTo("text");
        assertThat(blocks.get(0).path("text").asText()).isEqualTo("我来查询天气");
        assertThat(blocks.get(1).path("type").asText()).isEqualTo("tool_use");
        assertThat(blocks.get(1).path("id").asText()).isEqualTo("toolu_weather");
        assertThat(blocks.get(1).path("name").asText()).isEqualTo("get_weather");
        assertThat(blocks.get(1).path("input").path("city").asText()).isEqualTo("合肥");
    }

    /** 工具定义采用 Anthropic 格式：name/description/input_schema（非 OpenAI 的嵌套 function）。 */
    @Test
    void shouldBuildAnthropicToolDefinitions() throws Exception {
        ToolDefinitionForModel tool = new ToolDefinitionForModel();
        tool.setName("get_weather");
        tool.setDescription("查询天气");
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("type", "object");
        params.putObject("properties").putObject("city").put("type", "string");
        params.putArray("required").add("city");
        tool.setParameters(params);
        ChatRunContext context = context(List.of(new ChatMessage("user", "合肥天气")));

        JsonNode payload = client.buildPayload(context, false, null, null, List.of(tool));
        JsonNode toolNode = payload.path("tools").get(0);

        assertThat(toolNode.path("name").asText()).isEqualTo("get_weather");
        assertThat(toolNode.path("description").asText()).isEqualTo("查询天气");
        assertThat(toolNode.path("input_schema").path("type").asText()).isEqualTo("object");
        assertThat(toolNode.path("input_schema").path("required")).hasSize(1);
        assertThat(toolNode.path("function").isMissingNode()).isTrue();
    }

    /** 工具决策后的最终回答轮未显式传工具时，从 context.getTools() 兜底携带（Anthropic tool_use 必须有定义）。 */
    @Test
    void shouldAttachToolsFromContextWhenCallerOmits() throws Exception {
        ToolDefinitionForModel tool = new ToolDefinitionForModel();
        tool.setName("get_weather");
        tool.setDescription("查询天气");
        tool.setParameters(new ObjectMapper().createObjectNode().put("type", "object"));
        ChatRunContext context = context(List.of(new ChatMessage("user", "合肥天气")));
        context.setTools(List.of(tool));

        JsonNode payload = client.buildPayload(context, false, null, null, null);

        assertThat(payload.path("tools")).hasSize(1);
        assertThat(payload.path("tools").get(0).path("name").asText()).isEqualTo("get_weather");
    }

    /** max_tokens 必填缺省 2048；temperature 仅非空写入；非流式请求不带 stream 字段。 */
    @Test
    void shouldApplyDefaultsForMaxTokensAndTemperature() throws Exception {
        ChatRunContext context = context(List.of(new ChatMessage("user", "hi")));

        JsonNode payload = client.buildPayload(context, false, null, null, null);
        assertThat(payload.path("max_tokens").asInt()).isEqualTo(2048);
        assertThat(payload.path("temperature").isMissingNode()).isTrue();
        assertThat(payload.path("stream").isMissingNode()).isTrue();
        assertThat(payload.path("model").asText()).isEqualTo("claude-sonnet-5");

        JsonNode streamPayload = client.buildPayload(context, true, 0.3, 1024, null);
        assertThat(streamPayload.path("stream").asBoolean()).isTrue();
        assertThat(streamPayload.path("temperature").asDouble()).isEqualTo(0.3);
        assertThat(streamPayload.path("max_tokens").asInt()).isEqualTo(1024);
    }

    /** 非流式响应：text block 拼接为 content，tool_use block 解析为 toolCalls，usage 双字段对齐。 */
    @Test
    void shouldParseNormalResponseTextAndToolUse() throws Exception {
        String body = """
                {"id":"msg_1","type":"message","role":"assistant",
                 "content":[
                   {"type":"text","text":"正在查询。"},
                   {"type":"tool_use","id":"toolu_1","name":"get_weather","input":{"city":"合肥"}},
                   {"type":"text","text":" 结果如下。"}
                 ],
                 "stop_reason":"tool_use","usage":{"input_tokens":12,"output_tokens":34}}
                """;

        LlmCallResult result = client.parseNormalResponse(body);

        assertThat(result.getContent()).isEqualTo("正在查询。 结果如下。");
        assertThat(result.getToolCalls()).hasSize(1);
        assertThat(result.getToolCalls().get(0).getName()).isEqualTo("get_weather");
        assertThat(result.getToolCalls().get(0).getArgumentsJson()).contains("\"city\":\"合肥\"");
        assertThat(result.getPromptTokens()).isEqualTo(12);
        assertThat(result.getCompletionTokens()).isEqualTo(34);
        assertThat(result.getTotalTokens()).isEqualTo(46);
    }

    /** 非流式错误响应（error 对象）抛业务异常。 */
    @Test
    void shouldRejectErrorObjectInNormalResponse() {
        String body = """
                {"type":"error","error":{"type":"invalid_request_error","message":"max_tokens is required"}}
                """;

        assertThatThrownBy(() -> client.parseNormalResponse(body))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("CLAUDE_API_ERROR");
    }

    /** 流式事件序列：token 累计 + text_delta 推送 + 多帧 partial_json 拼装 tool_use + message_stop 收口。 */
    @Test
    void shouldParseStreamEventsWithToolUseJsonBuffering() throws Exception {
        LlmCallResult result = new LlmCallResult();
        ClaudeChatClient.ToolJsonBuffers buffers = new ClaudeChatClient.ToolJsonBuffers();
        // content 由调用方（completeStream 外层）累计，这里模拟外层行为。
        StringBuilder content = new StringBuilder();

        String delta1 = client.parseStreamDelta("""
                {"type":"message_start","message":{"id":"msg_1","usage":{"input_tokens":20,"output_tokens":1}}}
                """, result, buffers);
        assertThat(delta1).isEmpty();
        assertThat(result.getPromptTokens()).isEqualTo(20);

        String delta2 = client.parseStreamDelta("""
                {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}
                """, result, buffers);
        assertThat(delta2).isEmpty();

        String delta3 = client.parseStreamDelta("""
                {"type":"content_block_start","index":1,"content_block":{"type":"tool_use","id":"toolu_x","name":"get_weather","input":{}}}
                """, result, buffers);
        assertThat(delta3).isEmpty();

        content.append(client.parseStreamDelta("""
                {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"今天"}}
                """, result, buffers));

        // partial_json 按任意字符边界切分（含首片空串），必须逐帧缓冲后在 stop 一次性解析。
        client.parseStreamDelta("""
                {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":""}}
                """, result, buffers);
        client.parseStreamDelta("""
                {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"{\\"city\\":"}}
                """, result, buffers);
        client.parseStreamDelta("""
                {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"\\"合肥\\"}"}}
                """, result, buffers);

        client.parseStreamDelta("""
                {"type":"content_block_stop","index":1}
                """, result, buffers);
        client.parseStreamDelta("""
                {"type":"message_delta","delta":{"stop_reason":"tool_use"},"usage":{"output_tokens":28}}
                """, result, buffers);

        assertThat(content.toString()).isEqualTo("今天");
        assertThat(result.getCompletionTokens()).isEqualTo(28);
        assertThat(result.getToolCalls()).hasSize(1);
        assertThat(result.getToolCalls().get(0).getId()).isEqualTo("toolu_x");
        assertThat(result.getToolCalls().get(0).getName()).isEqualTo("get_weather");
        assertThat(result.getToolCalls().get(0).getArgumentsJson()).contains("\"city\":\"合肥\"");
    }

    /** 流式 error 事件抛业务异常。 */
    @Test
    void shouldThrowOnStreamErrorEvent() {
        LlmCallResult result = new LlmCallResult();
        ClaudeChatClient.ToolJsonBuffers buffers = new ClaudeChatClient.ToolJsonBuffers();

        assertThatThrownBy(() -> client.parseStreamDelta("""
                {"type":"error","error":{"type":"overloaded_error","message":"服务过载"}}
                """, result, buffers))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("CLAUDE_API_ERROR");
    }

    /** base_url 拼接 /v1/messages；已带则不再重复。 */
    @Test
    void shouldBuildMessagesUrl() {
        assertThat(client.messagesUrl("https://api.anthropic.com")).isEqualTo("https://api.anthropic.com/v1/messages");
        assertThat(client.messagesUrl("https://api.anthropic.com/")).isEqualTo("https://api.anthropic.com/v1/messages");
        assertThat(client.messagesUrl("https://gateway.example/v1/messages")).isEqualTo("https://gateway.example/v1/messages");
    }

    /** 请求头：x-api-key + anthropic-version + content-type（Messages API 鉴权三件套）。 */
    @Test
    void shouldBuildRequestWithAnthropicHeaders() throws Exception {
        ChatRunContext context = context(List.of(new ChatMessage("user", "hi")));
        context.setApiKey("sk-ant-test-abc");

        HttpRequest request = client.buildRequest(context, client.buildPayload(context, false, null, null, null));

        assertThat(request.uri().toString()).isEqualTo("https://api.anthropic.com/v1/messages");
        assertThat(request.headers().firstValue("x-api-key")).contains("sk-ant-test-abc");
        assertThat(request.headers().firstValue("anthropic-version")).contains("2023-06-01");
        assertThat(request.headers().firstValue("Content-Type")).contains("application/json");
    }

    /** 空闲态取消与活动运行快照。 */
    @Test
    void shouldExposeIdleCancellationState() {
        assertThat(client.activeRunIds()).isEmpty();
        assertThat(client.cancel("run-not-active")).isFalse();
    }

    private ModelConfigEntity model(String modelCode) {
        ModelConfigEntity model = new ModelConfigEntity();
        model.setModelCode(modelCode);
        return model;
    }

    private ModelProviderEntity provider(String baseUrl) {
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setBaseUrl(baseUrl);
        provider.setAuthType("api_key");
        return provider;
    }
}
