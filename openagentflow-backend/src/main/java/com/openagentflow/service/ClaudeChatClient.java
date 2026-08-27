package com.openagentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.chat.ModelChatClient;
import com.openagentflow.domain.chat.ToolCallRequest;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import com.openagentflow.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Anthropic Claude 模型聊天客户端（原生 Messages API，第三种协议）。
 *
 * <p>与 OpenAI-compatible（HTTP/JSON）和讯飞星火（WebSocket + HMAC 动态签名）都不同，
 * Claude 走 <b>REST + SSE</b>：鉴权用 {@code x-api-key} + {@code anthropic-version} 头；
 * 请求/响应消息以 <b>content blocks</b> 组织，工具调用是 {@code tool_use}/{@code tool_result}
 * block 多轮回合——assistant 返回 {@code tool_use} block → 执行工具 → 以 user 消息的
 * {@code tool_result} block 续聊，与 OpenAI 的 {@code tool_calls}/{@code role=tool}、
 * 星火的单轮 {@code function_call} 均不同。</p>
 *
 * <p><b>消息映射</b>：内部 {@code ChatMessage.role=system}（含首条系统提示、工具结果后置指令、
 * 订单路由提示）合并到顶层 {@code system} 字段（Anthropic 无对话内 system 角色，按出现顺序拼接）；
 * 连续的 {@code role=tool} 消息合并为一条 user 消息的 {@code tool_result} blocks；
 * assistant 的 {@code toolCalls} 还原为 {@code tool_use} blocks（argumentsJson → input JSON 对象）。</p>
 *
 * <p><b>工具定义兜底</b>：Anthropic 要求消息中出现 {@code tool_use} block 时请求必须携带对应工具定义，
 * 否则接口报错。工具决策后的最终回答轮（{@link #complete}）由 {@code ChatService} 复用时不再显式传工具，
 * 这里在 {@code context.getTools()} 非空时始终带上定义，避免 {@code tool_use 无对应定义} 报错。</p>
 */
@Service
public class ClaudeChatClient implements ModelChatClient {

    /** Anthropic Messages API 版本头，2023-06-01 为当前稳定版本。 */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /** 单次调用超时，与其它协议客户端对齐。 */
    private static final int CALL_TIMEOUT_SECONDS = 120;

    /** Anthropic max_tokens 为必填参数，缺省回退值。 */
    private static final int DEFAULT_MAX_TOKENS = 2048;

    /** HTTP 客户端，复用连接超时配置。 */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** 运行ID到活动HTTP请求的映射，用于用户停止时主动中断阻塞模型调用。 */
    private final Map<String, CompletableFuture<?>> activeRequests = new java.util.concurrent.ConcurrentHashMap<>();

    /** 运行ID到活动流的映射，用于立即关闭SSE模型响应。 */
    private final Map<String, Closeable> activeStreams = new java.util.concurrent.ConcurrentHashMap<>();

    public ClaudeChatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmCallResult complete(ChatRunContext context, Double temperature, Integer maxTokens) {
        return call(context, temperature, maxTokens, null);
    }

    @Override
    public LlmCallResult completeWithTools(ChatRunContext context, Double temperature, Integer maxTokens) {
        return call(context, temperature, maxTokens, context == null ? null : context.getTools());
    }

    @Override
    public LlmCallResult completeStream(ChatRunContext context, Double temperature, Integer maxTokens,
                                        Consumer<String> onDelta) {
        Instant startedAt = Instant.now();
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder rawBuilder = new StringBuilder();
        LlmCallResult result = new LlmCallResult();
        ToolJsonBuffers buffers = new ToolJsonBuffers();
        try {
            ObjectNode payload = buildPayload(context, true, temperature, maxTokens,
                    context == null ? null : context.getTools());
            HttpRequest request = buildRequest(context, payload);
            HttpResponse<InputStream> response = sendCancelable(context, request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException("CLAUDE_API_ERROR", errorBody);
            }
            registerStream(context, response.body());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                // Anthropic SSE：事件行以 data: 前缀，空行为心跳分隔，message_stop 后流结束。
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    rawBuilder.append(data).append('\n');
                    String delta = parseStreamDelta(data, result, buffers);
                    if (StringUtils.hasText(delta)) {
                        contentBuilder.append(delta);
                        if (onDelta != null) {
                            onDelta.accept(delta);
                        }
                    }
                }
            }
            unregisterStream(context);
            result.setContent(contentBuilder.toString());
            result.setRawResponse(rawBuilder.toString());
            result.setTotalTokens(nullToZero(result.getPromptTokens()) + nullToZero(result.getCompletionTokens()));
            result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("CLAUDE_STREAM_FAILED", exception.getMessage());
        } finally {
            unregisterStream(context);
        }
    }

    /**
     * 主动取消指定 Runtime 运行关联的 HTTP 请求和响应流。
     *
     * @param runId 运行ID
     * @return 是否找到活动调用
     */
    public boolean cancel(String runId) {
        boolean found = false;
        CompletableFuture<?> request = activeRequests.remove(runId);
        if (request != null) {
            found = request.cancel(true);
        }
        Closeable stream = activeStreams.remove(runId);
        if (stream != null) {
            try { stream.close(); } catch (Exception ignored) { }
            found = true;
        }
        return found;
    }

    /** 返回当前JVM正在调用Claude的运行ID快照。 */
    public Set<String> activeRunIds() {
        Set<String> result = new HashSet<>(activeRequests.keySet());
        result.addAll(activeStreams.keySet());
        return Set.copyOf(result);
    }

    /** 使用可取消异步请求替代阻塞send。 */
    private <T> HttpResponse<T> sendCancelable(ChatRunContext context,
                                               HttpRequest request,
                                               HttpResponse.BodyHandler<T> bodyHandler) throws Exception {
        CompletableFuture<HttpResponse<T>> future = httpClient.sendAsync(request, bodyHandler);
        String runId = context == null ? null : context.getRunId();
        if (StringUtils.hasText(runId)) activeRequests.put(runId, future);
        try {
            return future.get();
        } finally {
            if (StringUtils.hasText(runId)) activeRequests.remove(runId, future);
        }
    }

    /** 登记活动模型响应流。 */
    private void registerStream(ChatRunContext context, Closeable stream) {
        if (context != null && StringUtils.hasText(context.getRunId())) activeStreams.put(context.getRunId(), stream);
    }

    /** 清理活动模型响应流。 */
    private void unregisterStream(ChatRunContext context) {
        if (context != null && StringUtils.hasText(context.getRunId())) activeStreams.remove(context.getRunId());
    }

    /**
     * 发起一次 Claude Messages API 调用（非流式）。
     *
     * @param context 聊天运行上下文
     * @param temperature 温度参数
     * @param maxTokens 最大输出 Token 数
     * @param tools 工具定义；为空时回退 context.getTools()（覆盖最终回答轮）
     * @return LLM 调用结果
     */
    private LlmCallResult call(ChatRunContext context, Double temperature, Integer maxTokens,
                               List<ToolDefinitionForModel> tools) {
        Instant startedAt = Instant.now();
        ObjectNode payload = buildPayload(context, false, temperature, maxTokens, tools);
        try {
            HttpRequest request = buildRequest(context, payload);
            HttpResponse<String> response = sendCancelable(context, request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int latencyMs = (int) Duration.between(startedAt, Instant.now()).toMillis();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("CLAUDE_API_ERROR", response.body());
            }
            LlmCallResult result = parseNormalResponse(response.body());
            result.setLatencyMs(latencyMs);
            result.setRawResponse(response.body());
            return result;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("CLAUDE_CALL_FAILED", exception.getMessage());
        }
    }

    /**
     * 构建 Claude Messages API 请求体。
     *
     * @param context 聊天运行上下文
     * @param stream 是否流式输出
     * @param temperature 温度参数
     * @param maxTokens 最大输出 Token 数
     * @param tools 工具定义（Anthropic 格式：name/description/input_schema）
     * @return 请求体 JSON 节点
     */
    ObjectNode buildPayload(ChatRunContext context, boolean stream, Double temperature, Integer maxTokens,
                            List<ToolDefinitionForModel> tools) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", context.getModel().getModelCode());
        payload.put("max_tokens", maxTokens == null || maxTokens <= 0 ? DEFAULT_MAX_TOKENS : maxTokens);
        if (temperature != null) {
            payload.put("temperature", temperature);
        }
        if (stream) {
            payload.put("stream", true);
        }

        // 顶层 system 合并 + 消息数组（连续 tool 消息合并为单条 user 的 tool_result blocks）。
        StringBuilder systemBuilder = new StringBuilder();
        ArrayNode messagesNode = payload.putArray("messages");
        List<ChatMessage> pendingTools = new ArrayList<>();
        for (ChatMessage message : context.getMessages() == null ? List.<ChatMessage>of() : context.getMessages()) {
            if ("system".equalsIgnoreCase(message.getRole())) {
                appendSystem(systemBuilder, message.getContent());
                continue;
            }
            if ("tool".equalsIgnoreCase(message.getRole())) {
                pendingTools.add(message);
                continue;
            }
            flushToolMessages(messagesNode, pendingTools);
            messagesNode.add(buildMessageNode(message));
        }
        flushToolMessages(messagesNode, pendingTools);
        if (systemBuilder.length() > 0) {
            payload.put("system", systemBuilder.toString());
        }

        // Anthropic 要求消息中出现 tool_use/tool_result 时请求携带对应工具定义。
        List<ToolDefinitionForModel> effectiveTools = (tools == null || tools.isEmpty())
                ? (context.getTools() == null ? List.of() : context.getTools()) : tools;
        if (!effectiveTools.isEmpty()) {
            ArrayNode toolsNode = payload.putArray("tools");
            for (ToolDefinitionForModel tool : effectiveTools) {
                ObjectNode toolNode = toolsNode.addObject();
                toolNode.put("name", tool.getName());
                toolNode.put("description", tool.getDescription() == null ? "" : tool.getDescription());
                toolNode.set("input_schema", tool.getParameters() == null
                        ? objectMapper.createObjectNode().put("type", "object") : tool.getParameters());
            }
        }
        return payload;
    }

    /**
     * 构建单条模型消息 JSON（内部表示 → Anthropic content blocks）。
     *
     * <p>普通 user/assistant 消息输出字符串 content；assistant 携带工具调用时输出 blocks 数组
     * （可选的 text block + 若干 tool_use block），与 ChatService 工具执行后的回填结构对应。</p>
     *
     * @param message 内部聊天消息
     * @return Anthropic 消息节点
     */
    ObjectNode buildMessageNode(ChatMessage message) {
        ObjectNode node = objectMapper.createObjectNode();
        String role = message.getRole() == null ? "user" : message.getRole().toLowerCase(java.util.Locale.ROOT);
        if (!("user".equals(role) || "assistant".equals(role))) {
            role = "user";
        }
        node.put("role", role);
        if ("assistant".equals(role) && message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            ArrayNode blocks = node.putArray("content");
            if (StringUtils.hasText(message.getContent())) {
                ObjectNode textBlock = blocks.addObject();
                textBlock.put("type", "text");
                textBlock.put("text", message.getContent());
            }
            for (ToolCallRequest call : message.getToolCalls()) {
                ObjectNode toolUse = blocks.addObject();
                toolUse.put("type", "tool_use");
                toolUse.put("id", call.getId() == null ? "" : call.getId());
                toolUse.put("name", call.getName() == null ? "" : call.getName());
                toolUse.set("input", parseArguments(call.getArgumentsJson()));
            }
            return node;
        }
        node.put("content", message.getContent() == null ? "" : message.getContent());
        return node;
    }

    /**
     * 拼接工具结果后置消息到顶层 system 文本（保持出现顺序，双换行分隔）。
     *
     * @param system 顶层 system 累计器
     * @param content 待拼接的 system 内容
     */
    private void appendSystem(StringBuilder system, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        if (system.length() > 0) {
            system.append("\n\n");
        }
        system.append(content);
    }

    /**
     * 把连续收集的 tool 消息冲刷为一条 user 消息的 tool_result blocks。
     *
     * @param messagesNode 目标消息数组
     * @param pendingTools 连续 tool 消息缓冲（冲刷后清空）
     */
    private void flushToolMessages(ArrayNode messagesNode, List<ChatMessage> pendingTools) {
        if (pendingTools.isEmpty()) {
            return;
        }
        ObjectNode userNode = objectMapper.createObjectNode();
        userNode.put("role", "user");
        ArrayNode blocks = userNode.putArray("content");
        for (ChatMessage toolMessage : pendingTools) {
            ObjectNode toolResult = blocks.addObject();
            toolResult.put("type", "tool_result");
            toolResult.put("tool_use_id", toolMessage.getToolCallId() == null ? "" : toolMessage.getToolCallId());
            toolResult.put("content", toolMessage.getContent() == null ? "" : toolMessage.getContent());
        }
        messagesNode.add(userNode);
        pendingTools.clear();
    }

    /**
     * 把工具调用参数 JSON 字符串解析为 JSON 对象；空串或非法 JSON 回退为空对象。
     *
     * @param argumentsJson 工具参数 JSON 字符串
     * @return input JSON 节点
     */
    private JsonNode parseArguments(String argumentsJson) {
        if (!StringUtils.hasText(argumentsJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode parsed = objectMapper.readTree(argumentsJson);
            return parsed == null || !parsed.isObject() ? objectMapper.createObjectNode() : parsed;
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 构建 Claude Messages API 请求。
     *
     * @param context 聊天运行上下文
     * @param payload 请求体 JSON 节点
     * @return HTTP 请求
     */
    HttpRequest buildRequest(ChatRunContext context, ObjectNode payload) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(messagesUrl(context.getProvider().getBaseUrl())))
                .timeout(Duration.ofSeconds(CALL_TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));

        if (!"none".equalsIgnoreCase(context.getProvider().getAuthType()) && StringUtils.hasText(context.getApiKey())) {
            builder.header("x-api-key", context.getApiKey());
        }
        appendDefaultHeaders(builder, context.getProvider().getDefaultHeaders());
        return builder.build();
    }

    /**
     * 追加服务商配置的默认请求头。
     *
     * @param builder HTTP 请求构建器
     * @param headersJson 请求头 JSON
     */
    private void appendDefaultHeaders(HttpRequest.Builder builder, String headersJson) throws Exception {
        if (!StringUtils.hasText(headersJson)) {
            return;
        }
        JsonNode headersNode = objectMapper.readTree(headersJson);
        if (!headersNode.isObject()) {
            return;
        }
        for (Map.Entry<String, JsonNode> entry : headersNode.properties()) {
            if (StringUtils.hasText(entry.getKey()) && entry.getValue().isTextual()) {
                builder.header(entry.getKey(), entry.getValue().asText());
            }
        }
    }

    /**
     * 拼接 Messages API 地址（兼容 base_url 已带 /v1/messages 的情况）。
     *
     * @param baseUrl 服务基础地址
     * @return Messages 接口地址
     */
    String messagesUrl(String baseUrl) {
        String cleaned = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (cleaned.endsWith("/v1/messages")) {
            return cleaned;
        }
        return cleaned + "/v1/messages";
    }

    /**
     * 解析非流式 Messages API 响应。
     *
     * @param body 响应体
     * @return LLM 调用结果（content 为全部 text block 拼接，toolCalls 为全部 tool_use block）
     */
    LlmCallResult parseNormalResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if ("error".equals(root.path("type").asText(""))) {
            throw new BusinessException("CLAUDE_API_ERROR", extractErrorMessage(root));
        }
        LlmCallResult result = new LlmCallResult();
        StringBuilder text = new StringBuilder();
        List<ToolCallRequest> toolCalls = new ArrayList<>();
        JsonNode content = root.path("content");
        if (content.isArray()) {
            for (JsonNode block : content) {
                String blockType = block.path("type").asText("");
                if ("text".equals(blockType)) {
                    text.append(block.path("text").asText(""));
                } else if ("tool_use".equals(blockType)) {
                    ToolCallRequest call = new ToolCallRequest();
                    call.setId(block.path("id").asText(""));
                    call.setName(block.path("name").asText(""));
                    JsonNode input = block.path("input");
                    call.setArgumentsJson(input.isMissingNode() || input.isNull() ? "{}" : input.toString());
                    if (StringUtils.hasText(call.getName())) {
                        toolCalls.add(call);
                    }
                }
            }
        }
        result.setContent(text.toString());
        result.setToolCalls(toolCalls);
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("input_tokens").asInt(0);
        int completionTokens = usage.path("output_tokens").asInt(0);
        result.setPromptTokens(promptTokens);
        result.setCompletionTokens(completionTokens);
        result.setTotalTokens(promptTokens + completionTokens);
        return result;
    }

    /**
     * 解析单帧 SSE 流式事件，返回本次应推送的文本增量。
     *
     * <p>只处理 Anthropic 官方事件：message_start/message_delta 累计 token 用量；
     * content_block_start（tool_use 记录 id/name）、content_block_delta（text_delta 推送文本、
     * input_json_delta 累积 partial_json）、content_block_stop（解析整块 JSON 生成工具调用）；
     * error 抛业务异常；ping/message_stop 等未知事件静默忽略。</p>
     *
     * @param data SSE data 内容（事件 JSON）
     * @param result 累计调用结果
     * @param buffers 按 content block index 累积的 tool_use JSON 缓冲
     * @return 本次新增文本（可能为空）
     */
    String parseStreamDelta(String data, LlmCallResult result, ToolJsonBuffers buffers) {
        try {
            JsonNode root = objectMapper.readTree(data);
            switch (root.path("type").asText("")) {
                case "message_start" -> {
                    JsonNode usage = root.path("message").path("usage");
                    result.setPromptTokens(usage.path("input_tokens").asInt(result.getPromptTokens()));
                    result.setCompletionTokens(usage.path("output_tokens").asInt(result.getCompletionTokens()));
                    return "";
                }
                case "message_delta" -> {
                    result.setCompletionTokens(root.path("usage").path("output_tokens")
                            .asInt(result.getCompletionTokens()));
                    return "";
                }
                case "content_block_start" -> {
                    JsonNode block = root.path("content_block");
                    if ("tool_use".equals(block.path("type").asText(""))) {
                        buffers.start(root.path("index").asInt(0), block);
                    }
                    return "";
                }
                case "content_block_delta" -> {
                    JsonNode delta = root.path("delta");
                    return switch (delta.path("type").asText("")) {
                        case "text_delta" -> delta.path("text").asText("");
                        case "input_json_delta" -> {
                            buffers.appendJson(root.path("index").asInt(0), delta.path("partial_json").asText(""));
                            yield "";
                        }
                        // thinking_delta / signature_delta 等思考块增量不推送给前端。
                        default -> "";
                    };
                }
                case "content_block_stop" -> {
                    buffers.finish(root.path("index").asInt(0), result);
                    return "";
                }
                case "error" -> throw new BusinessException("CLAUDE_API_ERROR", extractErrorMessage(root));
                // ping / message_stop 等事件。
                default -> {
                    return "";
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("CLAUDE_RESPONSE_PARSE_FAILED", "Claude 流式响应解析失败：" + exception.getMessage());
        }
    }

    /** 从 Anthropic 错误体提取人类可读信息。 */
    private String extractErrorMessage(JsonNode root) {
        JsonNode error = root.path("error");
        if (error.isMissingNode() || error.isNull()) {
            return root.toString();
        }
        return error.path("message").asText(error.toString());
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 流式 tool_use block JSON 累积器。
     *
     * <p>partial_json 片段按任意字符边界切分、首片常为空串，只有全部片段按 index 拼接后在
     * content_block_stop 一次性解析才是合法 JSON（官方推荐的 buffer 模式）。</p>
     */
    static final class ToolJsonBuffers {

        private final Map<Integer, Entry> entries = new HashMap<>();

        private static final class Entry {
            private String id;
            private String name;
            private final StringBuilder json = new StringBuilder();
        }

        /** 开启一个 tool_use block 缓冲（记录 id/name，content_block_start 调用）。 */
        void start(int index, JsonNode block) {
            Entry entry = new Entry();
            entry.id = block.path("id").asText("");
            entry.name = block.path("name").asText("");
            entries.put(index, entry);
        }

        /** 追加一段 partial_json（content_block_delta/input_json_delta 调用）。 */
        void appendJson(int index, String partialJson) {
            Entry entry = entries.get(index);
            if (entry != null) {
                entry.json.append(partialJson == null ? "" : partialJson);
            }
        }

        /** 收口 tool_use block：解析整段 JSON 生成工具调用（content_block_stop 调用）。 */
        void finish(int index, LlmCallResult result) {
            Entry entry = entries.remove(index);
            if (entry == null || !StringUtils.hasText(entry.name)) {
                return;
            }
            ToolCallRequest call = new ToolCallRequest();
            call.setId(entry.id);
            call.setName(entry.name);
            String assembled = entry.json.toString();
            call.setArgumentsJson(StringUtils.hasText(assembled) ? assembled : "{}");
            List<ToolCallRequest> calls = result.getToolCalls();
            List<ToolCallRequest> updated = new ArrayList<>(calls == null ? List.of() : calls);
            updated.add(call);
            result.setToolCalls(updated);
        }
    }
}
