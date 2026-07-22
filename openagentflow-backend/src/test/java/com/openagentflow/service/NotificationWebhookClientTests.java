package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 外部通知Webhook客户端测试。
 */
class NotificationWebhookClientTests {

    /** 测试HTTP服务。 */
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Webhook发送应携带JSON正文和HMAC签名请求头。 */
    @Test
    void shouldSendSignedWebhookPayload() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> signature = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notify", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            signature.set(exchange.getRequestHeaders().getFirst("X-OAF-Signature"));
            exchange.sendResponseHeaders(200, 2);
            exchange.getResponseBody().write("ok".getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();
        String config = new ObjectMapper().writeValueAsString(Map.of(
                "url", "http://127.0.0.1:" + server.getAddress().getPort() + "/notify",
                "secret", "test-secret"));

        NotificationWebhookClient.SendResult result = new NotificationWebhookClient(new ObjectMapper()).send(
                "webhook", config, Map.of("title", "测试", "content", "通知正文", "severity", "warning"));

        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(body.get()).contains("测试", "通知正文", "warning");
        assertThat(signature.get()).isNotBlank();
    }

    /** Webhook地址只允许HTTP或HTTPS。 */
    @Test
    void shouldRejectUnsupportedWebhookScheme() throws Exception {
        String config = new ObjectMapper().writeValueAsString(Map.of("url", "file:///tmp/notify"));
        NotificationWebhookClient client = new NotificationWebhookClient(new ObjectMapper());

        assertThatThrownBy(() -> client.send("webhook", config, Map.of("title", "测试")))
                .isInstanceOf(NotificationWebhookClient.ChannelDeliveryException.class)
                .hasMessageContaining("HTTP或HTTPS");
    }
}
