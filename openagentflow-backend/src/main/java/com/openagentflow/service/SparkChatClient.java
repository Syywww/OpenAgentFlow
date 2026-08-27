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
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * 讯飞星火（iFlytek Spark）模型聊天客户端。
 *
 * <p>星火原生协议与 OpenAI-compatible 完全不同：<b>WebSocket + HMAC-SHA256 动态签名鉴权</b>。
 * 鉴权参数（authorization/date/host）拼接到 WSS URL 查询串；建连后发送
 * {@code header/parameter/payload} 三段的 JSON 请求帧；服务端以
 * {@code payload.choices.text[0].content} 逐帧返回流式文本，{@code choices.status==2}
 * 表示最后一段。</p>
 *
 * <p><b>凭证约定</b>：星火需要 APPID/APIKey/APISecret 三项，沿用单加密 Key 字段，
 * 以 {@code {appId}:{apiKey}:{apiSecret}} 复合串形式存入并解密，按前两个冒号拆分。
 * <b>模型版本</b>：domain 放在模型配置 default_params 的 {@code domain} 字段
 * （如 {@code 4.0Ultra}/generalv3.5），provider 的 base_url 直接指向对应版本端点
 * （如 {@code wss://spark-api.xf-yun.com/v4.0/chat}）。</p>
 *
 * <p><b>主动取消</b>：星火协议没有专门的 cancel 帧，官方做法是直接发送 WebSocket Close，
 * 服务端随之停止生成。客户端以 runId 登记进行中的调用（{@link #activeCalls}），
 * 取消时先标记响应 future 中断等待，再关闭连接；由 {@link ModelChatClientRouter} 扇出到
 * Runtime 控制链路（{@code RuntimeControlService}/{@code RuntimeCancellationWatcher}）。</p>
 */
@Service
public class SparkChatClient implements ModelChatClient {

    /** 星火鉴权时间戳格式：RFC1123 GMT。 */
    private static final DateTimeFormatter RFC1123_FORMAT =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));

    /** 最后一段响应的 choices.status 标志。 */
    private static final int DONE_STATUS = 2;

    /** 星火服务默认 domain（Spark Ultra，2026 官方推荐版本）。 */
    private static final String DEFAULT_DOMAIN = "4.0Ultra";

    /** 单次调用超时，与 OpenAI 客户端对齐。 */
    private static final int CALL_TIMEOUT_SECONDS = 120;

    /** HTTP/WebSocket 客户端，复用连接超时配置。 */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** 活动星火调用，runId -> 进行中的 WebSocket 调用句柄（建连前即登记）。 */
    private final Map<String, SparkCallHandle> activeCalls = new ConcurrentHashMap<>();

    public SparkChatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmCallResult complete(ChatRunContext context, Double temperature, Integer maxTokens) {
        return call(context, temperature, maxTokens, null, null);
    }

    @Override
    public LlmCallResult completeWithTools(ChatRunContext context, Double temperature, Integer maxTokens) {
        // 星火 function calling：请求 payload.functions 注册函数，响应帧 function_call 返回 name + arguments。
        return call(context, temperature, maxTokens, null,
                context == null ? null : context.getTools());
    }

    @Override
    public LlmCallResult completeStream(ChatRunContext context, Double temperature, Integer maxTokens,
                                        Consumer<String> onDelta) {
        // 与 OpenAI 客户端对齐：流式调用暂不携带工具定义。
        return call(context, temperature, maxTokens, onDelta, null);
    }

    /**
     * 主动取消指定 Runtime 运行关联的星火 WebSocket 调用。
     *
     * <p>星火协议没有专门的 cancel 帧：主动停止即向服务端发送 WebSocket Close，
     * 服务端随之停止生成；同时把响应 future 标记为已取消，避免调用方等待完整响应帧。</p>
     *
     * @param runId 运行 ID
     * @return 是否找到活动调用
     */
    public boolean cancel(String runId) {
        SparkCallHandle handle = activeCalls.remove(runId);
        if (handle == null) {
            return false;
        }
        handle.listener.cancel();
        WebSocket webSocket = handle.webSocket;
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "cancelled").join();
            } catch (Exception ignored) {
                // 服务端可能已自行关闭。
            }
        }
        return true;
    }

    /** 返回当前 JVM 正在调用星火的运行 ID 快照。 */
    public Set<String> activeRunIds() {
        return Set.copyOf(activeCalls.keySet());
    }

    /** 星火调用句柄：listener 建连前即登记以便取消，webSocket 建连后回填。 */
    private static final class SparkCallHandle {
        private final SparkWebSocketListener listener;
        private volatile WebSocket webSocket;

        SparkCallHandle(SparkWebSocketListener listener) {
            this.listener = listener;
        }
    }

    /**
     * 发起一次星火 WebSocket 对话：拼鉴权 URL → 建连 → 发请求帧 → 逐帧收集直到 status=2。
     *
     * @param context 聊天运行上下文
     * @param temperature 温度参数
     * @param maxTokens 最大输出 Token 数
     * @param onDelta 流式片段回调，非流式传 null
     * @return LLM 调用结果
     */
    private LlmCallResult call(ChatRunContext context, Double temperature, Integer maxTokens,
                               Consumer<String> onDelta, List<ToolDefinitionForModel> tools) {
        Instant startedAt = Instant.now();
        ModelProviderEntity provider = requireProvider(context);
        String[] credentials = parseCredentials(context.getApiKey());
        String authUrl = buildAuthUrl(provider.getBaseUrl(), credentials[1], credentials[2]);
        String requestJson = buildRequestJson(context, credentials[0], temperature, maxTokens, tools);

        SparkWebSocketListener listener = new SparkWebSocketListener(requestJson, onDelta, objectMapper);
        String runId = context == null ? null : context.getRunId();
        SparkCallHandle handle = new SparkCallHandle(listener);
        if (StringUtils.hasText(runId)) {
            activeCalls.put(runId, handle);
        }
        try {
            CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .buildAsync(URI.create(authUrl), listener);
            WebSocket webSocket = wsFuture.join();
            handle.webSocket = webSocket;
            try {
                String content = listener.done().get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                LlmCallResult result = new LlmCallResult();
                result.setContent(content);
                result.setRawResponse(listener.rawResponse());
                result.setToolCalls(listener.toolCalls());
                result.setPromptTokens(listener.promptTokens());
                result.setCompletionTokens(listener.completionTokens());
                result.setTotalTokens(listener.totalTokens());
                result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
                return result;
            } catch (TimeoutException exception) {
                throw new BusinessException("SPARK_TIMEOUT", "星火模型调用超时");
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                if (cause instanceof BusinessException businessException) {
                    throw businessException;
                }
                throw new BusinessException("SPARK_CALL_FAILED", cause.getMessage());
            } finally {
                if (StringUtils.hasText(runId)) {
                    activeCalls.remove(runId);
                }
                try {
                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
                } catch (Exception ignored) {
                    // 服务端可能已自行关闭。
                }
            }
        } catch (BusinessException exception) {
            if (StringUtils.hasText(runId)) {
                activeCalls.remove(runId);
            }
            throw exception;
        } catch (CompletionException exception) {
            if (StringUtils.hasText(runId)) {
                activeCalls.remove(runId);
            }
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new BusinessException("SPARK_CONNECT_FAILED", "星火连接失败：" + cause.getMessage());
        } catch (Exception exception) {
            if (StringUtils.hasText(runId)) {
                activeCalls.remove(runId);
            }
            throw new BusinessException("SPARK_CALL_FAILED", exception.getMessage());
        }
    }

    /**
     * 拼接星火鉴权 URL。
     *
     * <p>签名规则（参考官方 WebSocket 文档）：以 APISecret 为密钥对
     * {@code host/date/request-line} 三段做 HMAC-SHA256，再对 authorization 字符串做 Base64，
     * 最终 authorization/date/host 作为查询参数拼入 WSS 地址。</p>
     *
     * @param baseUrl 服务商 base_url，如 {@code wss://spark-api.xf-yun.com/v4.0/chat}
     * @param apiKey 星火 APIKey
     * @param apiSecret 星火 APISecret
     * @return 带鉴权参数的 WSS URL
     */
    String buildAuthUrl(String baseUrl, String apiKey, String apiSecret) {
        try {
            URI base = URI.create(baseUrl);
            String host = base.getHost();
            String path = base.getPath();
            if (!StringUtils.hasText(host)) {
                throw new BusinessException("SPARK_BASE_URL_INVALID", "星火 base_url 缺少 host：" + baseUrl);
            }
            String date = RFC1123_FORMAT.format(Instant.now());
            String signatureOrigin = "host: " + host + "\n"
                    + "date: " + date + "\n"
                    + "GET " + path + " HTTP/1.1";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(
                    mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));
            String authorizationOrigin = "api_key=\"" + apiKey
                    + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\""
                    + signature + "\"";
            String authorization = Base64.getEncoder().encodeToString(
                    authorizationOrigin.getBytes(StandardCharsets.UTF_8));
            String query = "authorization=" + urlEncode(authorization)
                    + "&date=" + urlEncode(date)
                    + "&host=" + urlEncode(host);
            return "wss://" + host + path + "?" + query;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("SPARK_SIGN_FAILED", "星火鉴权签名失败：" + exception.getMessage());
        }
    }

    /**
     * 构造星火请求 JSON 帧。
     *
     * @param context 聊天运行上下文
     * @param appId 星火 APPID
     * @param temperature 温度参数
     * @param maxTokens 最大输出 Token 数
     * @param tools 可用工具定义，非空时注册到 payload.functions
     * @return 请求帧 JSON 字符串
     */
    String buildRequestJson(ChatRunContext context, String appId,
                            Double temperature, Integer maxTokens,
                            List<ToolDefinitionForModel> tools) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode header = root.putObject("header");
            header.put("app_id", appId);
            header.put("uid", "oaf");
            ObjectNode chat = root.putObject("parameter").putObject("chat");
            chat.put("domain", resolveDomain(context.getModel()));
            chat.put("temperature", temperature == null ? 0.5D : temperature);
            chat.put("max_tokens", maxTokens == null || maxTokens <= 0 ? 2048 : maxTokens);
            // payload 只创建一次：message 与 functions 是兄弟节点，复用避免 putObject 覆盖。
            ObjectNode payload = root.putObject("payload");
            ArrayNode text = payload.putObject("message").putArray("text");
            for (ChatMessage message : context.getMessages()) {
                ObjectNode item = text.addObject();
                item.put("role", mapRole(message.getRole()));
                item.put("content", message.getContent() == null ? "" : message.getContent());
            }
            appendFunctions(payload, tools);
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new BusinessException("SPARK_PAYLOAD_FAILED", "星火请求报文构造失败：" + exception.getMessage());
        }
    }

    /**
     * 向请求帧追加星火 function calling 函数定义。
     *
     * <p>请求结构 {@code payload.functions.text}（数组，元素含 name/description/parameters JSON Schema）。
     * 星火参数需标 required 才能稳定触发传递。</p>
     *
     * @param payload 已创建的 payload 节点（复用，避免覆盖 message）
     * @param tools 工具定义
     */
    private void appendFunctions(ObjectNode payload, List<ToolDefinitionForModel> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }
        ArrayNode functionsText = payload.putObject("functions").putArray("text");
        for (ToolDefinitionForModel tool : tools) {
            ObjectNode fn = functionsText.addObject();
            fn.put("name", tool.getName());
            fn.put("description", tool.getDescription() == null ? "" : tool.getDescription());
            fn.set("parameters", tool.getParameters() == null
                    ? objectMapper.createObjectNode().put("type", "object") : tool.getParameters());
        }
    }

    /**
     * 读取模型配置中的星火 domain，缺省回退 Ultra。
     *
     * @param model 模型配置
     * @return 星火 domain
     */
    String resolveDomain(ModelConfigEntity model) {
        if (model != null && StringUtils.hasText(model.getDefaultParams())) {
            try {
                String domain = objectMapper.readTree(model.getDefaultParams()).path("domain").asText("");
                if (StringUtils.hasText(domain)) {
                    return domain;
                }
            } catch (Exception ignored) {
                // default_params 解析失败时按缺省 domain 处理。
            }
        }
        return DEFAULT_DOMAIN;
    }

    /**
     * 拆分星火三凭证。约定复合串 {@code {appId}:{apiKey}:{apiSecret}}。
     *
     * @param compound 复合凭证串
     * @return [appId, apiKey, apiSecret]
     */
    String[] parseCredentials(String compound) {
        if (!StringUtils.hasText(compound)) {
            throw new BusinessException("SPARK_CREDENTIAL_MISSING", "星火服务商未配置 APPID/APIKey/APISecret");
        }
        String[] parts = compound.split(":", 3);
        if (parts.length != 3
                || !StringUtils.hasText(parts[0])
                || !StringUtils.hasText(parts[1])
                || !StringUtils.hasText(parts[2])) {
            throw new BusinessException("SPARK_CREDENTIAL_INVALID", "星火凭证格式应为 {appId}:{apiKey}:{apiSecret}");
        }
        return parts;
    }

    /**
     * 映射 OpenAI 角色到星火角色，tool 等不支持的角色回退为 user。
     *
     * @param role 原始角色
     * @return 星火角色
     */
    String mapRole(String role) {
        String normalized = role == null ? "" : role.toLowerCase(java.util.Locale.ROOT);
        if ("system".equals(normalized) || "user".equals(normalized) || "assistant".equals(normalized)) {
            return normalized;
        }
        return "user";
    }

    /**
     * 查询服务商实体。
     *
     * @param context 聊天运行上下文
     * @return 模型服务商
     */
    private ModelProviderEntity requireProvider(ChatRunContext context) {
        ModelProviderEntity provider = context == null ? null : context.getProvider();
        if (provider == null) {
            throw new BusinessException("MODEL_PROVIDER_NOT_FOUND", "星火调用缺少模型服务商");
        }
        return provider;
    }

    /**
     * URL 编码查询参数，空格编码为 %20 而非 form 编码的 +。
     *
     * @param value 原始值
     * @return URL 编码结果
     */
    String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * 星火 WebSocket 监听器：发请求帧、收集响应帧、按 status=2 收口。
     *
     * <p>包可见以便 {@code SparkWebSocketListenerTests} 直接喂帧验证解析逻辑
     * （status=2 收口、错误码、usage 统计、onClose 兜底）。</p>
     */
    static final class SparkWebSocketListener implements WebSocket.Listener {

        private final String requestJson;
        private final Consumer<String> onDelta;
        private final ObjectMapper objectMapper;
        private final StringBuilder contentBuilder = new StringBuilder();
        private final StringBuilder rawBuilder = new StringBuilder();
        private final StringBuilder frameBuffer = new StringBuilder();
        private final CompletableFuture<String> done = new CompletableFuture<>();
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        private ToolCallRequest functionCall;
        private int frameSeq;

        SparkWebSocketListener(String requestJson, Consumer<String> onDelta, ObjectMapper objectMapper) {
            this.requestJson = requestJson;
            this.onDelta = onDelta;
            this.objectMapper = objectMapper;
        }

        CompletableFuture<String> done() {
            return done;
        }

        /** 本次调用解析到的函数调用（星火单次对话至多一个 function_call，未命中返回空列表）。 */
        List<ToolCallRequest> toolCalls() {
            return functionCall == null ? List.of() : List.of(functionCall);
        }

        /** 标记本次调用被用户取消：异常完成响应 future，中断对完整响应帧的等待。 */
        void cancel() {
            done.completeExceptionally(new BusinessException("SPARK_CANCELLED", "调用已被用户取消"));
        }

        String rawResponse() {
            return rawBuilder.toString();
        }

        int promptTokens() {
            return promptTokens;
        }

        int completionTokens() {
            return completionTokens;
        }

        int totalTokens() {
            return totalTokens;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
            webSocket.sendText(requestJson, true).thenRun(() -> webSocket.request(1));
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            frameBuffer.append(data);
            if (last) {
                String frame = frameBuffer.toString();
                frameBuffer.setLength(0);
                rawBuilder.append(frame).append('\n');
                processFrame(frame);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (!done.isDone()) {
                done.completeExceptionally(error);
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            // 服务端可能不发送 status=2 直接关闭，此时按已收集内容收口。
            if (!done.isDone()) {
                done.complete(contentBuilder.toString());
            }
            return null;
        }

        private void processFrame(String frame) {
            try {
                JsonNode root = objectMapper.readTree(frame);
                int code = root.path("header").path("code").asInt(0);
                if (code != 0) {
                    done.completeExceptionally(new BusinessException("SPARK_API_ERROR",
                            "星火接口错误 code=" + code + "：" + root.path("header").path("message").asText("")));
                    return;
                }
                JsonNode choices = root.path("payload").path("choices");
                JsonNode textItem = choices.path("text").path(0);
                String delta = textItem.path("content").asText("");
                if (StringUtils.hasText(delta)) {
                    contentBuilder.append(delta);
                    if (onDelta != null) {
                        onDelta.accept(delta);
                    }
                }
                // function_call 与 content 并列在 text 项内：命中时 content 为空、arguments 为 JSON 字符串。
                JsonNode functionCallNode = textItem.path("function_call");
                if (!functionCallNode.isMissingNode() && StringUtils.hasText(functionCallNode.path("name").asText(""))) {
                    ToolCallRequest toolCall = new ToolCallRequest();
                    toolCall.setId("spark-fc-" + frameSeq++);
                    toolCall.setName(functionCallNode.path("name").asText(""));
                    toolCall.setArgumentsJson(functionCallNode.path("arguments").asText(""));
                    functionCall = toolCall;
                }
                JsonNode usage = root.path("payload").path("usage").path("text");
                promptTokens = usage.path("prompt_tokens").asInt(promptTokens);
                completionTokens = usage.path("completion_tokens").asInt(completionTokens);
                totalTokens = usage.path("total_tokens").asInt(totalTokens);
                if (choices.path("status").asInt(0) == DONE_STATUS) {
                    done.complete(contentBuilder.toString());
                }
            } catch (BusinessException exception) {
                done.completeExceptionally(exception);
            } catch (Exception exception) {
                done.completeExceptionally(new BusinessException("SPARK_RESPONSE_PARSE_FAILED",
                        "星火响应解析失败：" + exception.getMessage()));
            }
        }
    }
}