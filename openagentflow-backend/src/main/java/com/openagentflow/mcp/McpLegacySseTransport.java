package com.openagentflow.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** MCP 传统 SSE 原生传输实现。 */
@Component
public class McpLegacySseTransport implements McpTransport {

    /** JDK 原生 HTTP 客户端。 */
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** 认证请求头辅助器。 */
    private final McpHttpHeaderSupport headerSupport;

    /** 每个 MCP Server 对应一个持久 SSE 会话。 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public McpLegacySseTransport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.headerSupport = new McpHttpHeaderSupport(objectMapper);
    }

    @Override
    public boolean supports(String transportType) {
        return "sse".equals(transportType == null ? "" : transportType.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public JsonNode request(McpServerEntity server, ObjectNode payload, Duration timeout) {
        Session session = requireSession(server, timeout);
        String requestId = payload.path("id").asText();
        CompletableFuture<JsonNode> pending = new CompletableFuture<>();
        session.pending().put(requestId, pending);
        try {
            post(server, session.endpoint().get(timeout.toMillis(), TimeUnit.MILLISECONDS), payload, timeout);
            return pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            session.pending().remove(requestId);
            throw wrap("MCP SSE 请求失败", exception);
        }
    }

    @Override
    public void notify(McpServerEntity server, ObjectNode payload, Duration timeout) {
        Session session = requireSession(server, timeout);
        try {
            post(server, session.endpoint().get(timeout.toMillis(), TimeUnit.MILLISECONDS), payload, timeout);
        } catch (Exception exception) {
            throw wrap("MCP SSE 通知失败", exception);
        }
    }

    /** 获取或建立持久 SSE 会话。 */
    private Session requireSession(McpServerEntity server, Duration timeout) {
        if (server == null || !StringUtils.hasText(server.getEndpointUrl())) {
            throw new BusinessException("MCP_ENDPOINT_EMPTY", "SSE MCP Server 需要配置事件流 URL");
        }
        Session session = sessions.compute(server.getId(), (serverId, current) -> {
            if (current != null && !current.closed()) {
                return current;
            }
            return openSession(server, timeout);
        });
        try {
            session.endpoint().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return session;
        } catch (Exception exception) {
            close(server.getId());
            throw wrap("MCP SSE 会话建立失败", exception);
        }
    }

    /** 打开事件流并在虚拟线程中持续分发服务端消息。 */
    private Session openSession(McpServerEntity server, Duration timeout) {
        Session session = new Session(new CompletableFuture<>(), new ConcurrentHashMap<>());
        Thread.ofVirtual().name("mcp-sse-" + server.getId()).start(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(server.getEndpointUrl()))
                        .GET();
                headerSupport.apply(builder, server, "text/event-stream");
                HttpResponse<InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("SSE 状态码异常：" + response.statusCode());
                }
                session.inputStream(response.body());
                consumeEvents(server, session, response.body());
            } catch (Exception exception) {
                session.endpoint().completeExceptionally(exception);
                session.pending().values().forEach(future -> future.completeExceptionally(exception));
            } finally {
                session.markClosed();
                sessions.remove(server.getId(), session);
            }
        });
        return session;
    }

    /** 消费 SSE endpoint 与 message 事件。 */
    private void consumeEvents(McpServerEntity server, Session session, InputStream stream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while (!session.closed() && (line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                    continue;
                }
                dispatchEvent(server, session, lines);
                lines.clear();
            }
            dispatchEvent(server, session, lines);
        }
    }

    /** 将 endpoint 事件保存为 POST 地址，将 message 事件按 JSON-RPC ID 分发。 */
    private void dispatchEvent(McpServerEntity server, Session session, List<String> lines) throws Exception {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        McpSseEventParser.Event event = McpSseEventParser.parse(lines);
        if ("endpoint".equals(event.type())) {
            URI base = URI.create(server.getEndpointUrl());
            URI endpoint = base.resolve(event.data());
            if (!sameOrigin(base, endpoint)) {
                throw new BusinessException("MCP_SSE_ENDPOINT_FORBIDDEN", "MCP SSE 会话端点必须与事件流地址同源");
            }
            session.endpoint().complete(endpoint);
            return;
        }
        if (event.data() == null || event.data().isBlank()) {
            return;
        }
        JsonNode message = objectMapper.readTree(event.data());
        CompletableFuture<JsonNode> future = session.pending().remove(message.path("id").asText());
        if (future != null) {
            future.complete(message);
        }
    }

    /** 防止 SSE endpoint 事件把认证信息重定向到其他来源。 */
    private boolean sameOrigin(URI source, URI target) {
        int sourcePort = source.getPort() >= 0 ? source.getPort() : defaultPort(source.getScheme());
        int targetPort = target.getPort() >= 0 ? target.getPort() : defaultPort(target.getScheme());
        return java.util.Objects.equals(source.getScheme(), target.getScheme())
                && java.util.Objects.equals(source.getHost(), target.getHost())
                && sourcePort == targetPort;
    }

    private int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    /** 将 JSON-RPC 消息发送到服务端给出的会话端点。 */
    private void post(McpServerEntity server, URI endpoint, ObjectNode payload, Duration timeout) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
        headerSupport.apply(builder, server, "application/json");
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException("MCP_SSE_POST_ERROR", "MCP SSE POST 状态码异常：" + response.statusCode() + "，响应：" + response.body());
        }
    }

    private BusinessException wrap(String prefix, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return new BusinessException("MCP_SSE_FAILED", prefix + "：" + cause.getMessage());
    }

    @Override
    public void close(String serverId) {
        Session session = sessions.remove(serverId);
        if (session != null) {
            session.close();
        }
    }

    /** 应用关闭时释放全部事件流。 */
    @PreDestroy
    public void shutdown() {
        List.copyOf(sessions.keySet()).forEach(this::close);
    }

    /** 传统 SSE 会话状态。 */
    private static final class Session {

        /** 服务端下发的 JSON-RPC POST 地址。 */
        private final CompletableFuture<URI> endpoint;

        /** 等待响应的 JSON-RPC 请求。 */
        private final Map<String, CompletableFuture<JsonNode>> pending;

        /** 当前事件流。 */
        private volatile InputStream inputStream;

        /** 会话是否已关闭。 */
        private volatile boolean closed;

        private Session(CompletableFuture<URI> endpoint, Map<String, CompletableFuture<JsonNode>> pending) {
            this.endpoint = endpoint;
            this.pending = pending;
        }

        private CompletableFuture<URI> endpoint() {
            return endpoint;
        }

        private Map<String, CompletableFuture<JsonNode>> pending() {
            return pending;
        }

        private boolean closed() {
            return closed;
        }

        private void inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        private void markClosed() {
            this.closed = true;
        }

        private void close() {
            closed = true;
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ignored) {
                // 关闭阶段不覆盖原业务异常。
            }
        }
    }
}
