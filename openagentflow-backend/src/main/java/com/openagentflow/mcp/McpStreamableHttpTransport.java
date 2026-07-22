package com.openagentflow.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.exception.BusinessException;
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
import java.util.concurrent.ConcurrentHashMap;

/** MCP Streamable HTTP 原生传输实现。 */
@Component
public class McpStreamableHttpTransport implements McpTransport {

    /** MCP 会话请求头名称。 */
    private static final String SESSION_HEADER = "Mcp-Session-Id";

    /** JDK 原生 HTTP 客户端。 */
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** 认证请求头辅助器。 */
    private final McpHttpHeaderSupport headerSupport;

    /** 服务端 ID 与协议会话 ID 的映射。 */
    private final Map<String, String> sessionIds = new ConcurrentHashMap<>();

    public McpStreamableHttpTransport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.headerSupport = new McpHttpHeaderSupport(objectMapper);
    }

    @Override
    public boolean supports(String transportType) {
        return List.of("http", "streamable_http", "streamable-http").contains(normalize(transportType));
    }

    @Override
    public JsonNode request(McpServerEntity server, ObjectNode payload, Duration timeout) {
        return exchange(server, payload, timeout, true);
    }

    @Override
    public void notify(McpServerEntity server, ObjectNode payload, Duration timeout) {
        exchange(server, payload, timeout, false);
    }

    /** 发送 Streamable HTTP 请求，同时兼容 JSON 与 SSE 两种响应体。 */
    private JsonNode exchange(McpServerEntity server, ObjectNode payload, Duration timeout, boolean responseRequired) {
        requireEndpoint(server);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(server.getEndpointUrl()))
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
            headerSupport.apply(builder, server, "application/json, text/event-stream");
            String sessionId = sessionIds.get(server.getId());
            if (StringUtils.hasText(sessionId)) {
                builder.header(SESSION_HEADER, sessionId);
            }
            HttpResponse<InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            response.headers().firstValue(SESSION_HEADER).ifPresent(value -> sessionIds.put(server.getId(), value));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                if (response.statusCode() == 404) {
                    sessionIds.remove(server.getId());
                }
                throw new BusinessException("MCP_HTTP_ERROR", "MCP HTTP 状态码异常：" + response.statusCode() + "，响应：" + body);
            }
            if (!responseRequired || response.statusCode() == 202) {
                response.body().close();
                return objectMapper.createObjectNode();
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
            if (contentType.contains("text/event-stream")) {
                return readSseResponse(response.body(), payload.path("id").asText(""));
            }
            byte[] bytes = response.body().readAllBytes();
            if (bytes.length == 0) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(bytes);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("MCP_REQUEST_FAILED", "MCP Streamable HTTP 请求失败：" + exception.getMessage());
        }
    }

    /** 从 SSE 响应中读取与当前 JSON-RPC ID 对应的消息。 */
    private JsonNode readSseResponse(InputStream inputStream, String requestId) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                    continue;
                }
                JsonNode message = parseMessageEvent(lines);
                lines.clear();
                if (message != null && (requestId.isBlank() || requestId.equals(message.path("id").asText()))) {
                    return message;
                }
            }
            JsonNode message = parseMessageEvent(lines);
            if (message != null) {
                return message;
            }
            throw new BusinessException("MCP_SSE_EMPTY", "MCP SSE 响应未返回 JSON-RPC 消息");
        }
    }

    /** 将一个 SSE 事件解析为 JSON-RPC 对象。 */
    private JsonNode parseMessageEvent(List<String> lines) throws Exception {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        McpSseEventParser.Event event = McpSseEventParser.parse(lines);
        if (event.data() == null || event.data().isBlank()) {
            return null;
        }
        return objectMapper.readTree(event.data());
    }

    /** 校验 HTTP 服务端点。 */
    private void requireEndpoint(McpServerEntity server) {
        if (server == null || !StringUtils.hasText(server.getEndpointUrl())) {
            throw new BusinessException("MCP_ENDPOINT_EMPTY", "Streamable HTTP MCP Server 需要配置端点 URL");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void close(String serverId) {
        sessionIds.remove(serverId);
    }
}
