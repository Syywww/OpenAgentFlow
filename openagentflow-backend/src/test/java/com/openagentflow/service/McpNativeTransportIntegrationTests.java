package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.mcp.McpLegacySseTransport;
import com.openagentflow.mcp.McpStdioTransport;
import com.openagentflow.mcp.McpStreamableHttpTransport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** MCP 三种原生传输的本地协议测试。 */
class McpNativeTransportIntegrationTests {

    /** Streamable HTTP 应解析 JSON/SSE 响应并复用服务端会话 ID。 */
    @Test
    void shouldUseStreamableHttpSessionAndSseResponse() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> secondSession = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String id = objectMapper.readTree(request).path("id").asText();
            secondSession.set(exchange.getRequestHeaders().getFirst("Mcp-Session-Id"));
            exchange.getResponseHeaders().add("Mcp-Session-Id", "session-1");
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            byte[] body = ("event: message\ndata: {\"jsonrpc\":\"2.0\",\"id\":\"" + id
                    + "\",\"result\":{\"transport\":\"http\"}}\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            McpServerEntity entity = httpServer("http-1", "http://127.0.0.1:" + server.getAddress().getPort() + "/mcp", "http");
            McpStreamableHttpTransport transport = new McpStreamableHttpTransport(objectMapper);
            assertThat(transport.request(entity, request(objectMapper, "1"), Duration.ofSeconds(3))
                    .path("result").path("transport").asText()).isEqualTo("http");
            assertThat(transport.request(entity, request(objectMapper, "2"), Duration.ofSeconds(3))
                    .path("result").path("transport").asText()).isEqualTo("http");
            assertThat(secondSession.get()).isEqualTo("session-1");
        } finally {
            server.stop(0);
        }
    }

    /** 传统 SSE 应先读取 endpoint，再把 POST 请求响应分发回原调用线程。 */
    @Test
    void shouldCallLegacySseEndpoint() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<OutputStream> eventStream = new AtomicReference<>();
        CountDownLatch streamReady = new CountDownLatch(1);
        CountDownLatch streamClosed = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/sse", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            OutputStream output = exchange.getResponseBody();
            eventStream.set(output);
            output.write("event: endpoint\ndata: /message\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
            streamReady.countDown();
            try {
                streamClosed.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.createContext("/message", exchange -> writeLegacyResponse(exchange, objectMapper, eventStream, streamReady));
        server.start();
        McpLegacySseTransport transport = new McpLegacySseTransport(objectMapper);
        try {
            McpServerEntity entity = httpServer("sse-1", "http://127.0.0.1:" + server.getAddress().getPort() + "/sse", "sse");
            assertThat(transport.request(entity, request(objectMapper, "legacy-1"), Duration.ofSeconds(3))
                    .path("result").path("transport").asText()).isEqualTo("sse");
            transport.close(entity.getId());
        } finally {
            streamClosed.countDown();
            transport.shutdown();
            server.stop(0);
        }
    }

    /** stdio 应直接启动白名单子进程并按请求 ID 关联响应。 */
    @Test
    void shouldCallStdioProcess() {
        ObjectMapper objectMapper = new ObjectMapper();
        OpenAgentFlowProperties properties = new OpenAgentFlowProperties();
        properties.getMcp().setStdioAllowedCommands("java");
        McpStdioTransport transport = new McpStdioTransport(objectMapper, properties);
        McpServerEntity entity = new McpServerEntity();
        entity.setId("stdio-1");
        entity.setTransportType("stdio");
        entity.setCommand(PathSupport.javaCommand());
        String testClasses = java.nio.file.Path.of("target", "test-classes").toAbsolutePath().toString();
        entity.setArgs(toJson(objectMapper, new String[]{"-cp", testClasses,
                McpStdioEchoServer.class.getName()}));
        entity.setEnvVars("{}");
        try {
            assertThat(transport.request(entity, request(objectMapper, "stdio-request"), Duration.ofSeconds(5))
                    .path("result").path("transport").asText()).isEqualTo("stdio");
        } finally {
            transport.shutdown();
        }
    }

    private static void writeLegacyResponse(HttpExchange exchange,
                                            ObjectMapper objectMapper,
                                            AtomicReference<OutputStream> eventStream,
                                            CountDownLatch streamReady) throws java.io.IOException {
        try {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String id = objectMapper.readTree(request).path("id").asText();
            streamReady.await(3, TimeUnit.SECONDS);
            OutputStream output = eventStream.get();
            synchronized (output) {
                output.write(("event: message\ndata: {\"jsonrpc\":\"2.0\",\"id\":\"" + id
                        + "\",\"result\":{\"transport\":\"sse\"}}\n\n").getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
            exchange.sendResponseHeaders(202, -1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            exchange.sendResponseHeaders(500, -1);
        } finally {
            exchange.close();
        }
    }

    private static McpServerEntity httpServer(String id, String endpoint, String transportType) {
        McpServerEntity entity = new McpServerEntity();
        entity.setId(id);
        entity.setEndpointUrl(endpoint);
        entity.setTransportType(transportType);
        entity.setAuthType("none");
        entity.setAuthConfig("{}");
        return entity;
    }

    private static ObjectNode request(ObjectMapper objectMapper, String id) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", "tools/list");
        request.set("params", objectMapper.createObjectNode());
        return request;
    }

    private static String toJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** 测试环境 Java 可执行文件定位。 */
    private static final class PathSupport {

        private static String javaCommand() {
            String executable = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
            return java.nio.file.Path.of(System.getProperty("java.home"), "bin", executable).toString();
        }
    }
}
