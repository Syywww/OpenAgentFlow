package com.openagentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

/**
 * Webhook类通知渠道HTTP客户端。
 */
@Component
public class NotificationWebhookClient {

    /** JSON序列化工具。 */
    private final ObjectMapper objectMapper;
    /** 复用连接池的HTTP客户端。 */
    private final HttpClient httpClient;

    public NotificationWebhookClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /**
     * 向渠道发送通知并返回状态、耗时和响应摘要。
     */
    public SendResult send(String channelType, String configJson, Map<String, Object> message) {
        if ("station".equalsIgnoreCase(channelType)) {
            return new SendResult(200, 0L, "内置站内通知可用");
        }
        try {
            JsonNode config = objectMapper.readTree(configJson);
            URI uri = validateUri(config.path("url").asText(config.path("webhookUrl").asText()));
            int timeoutSeconds = Math.min(30, Math.max(1, config.path("timeoutSeconds").asInt(10)));
            String body = objectMapper.writeValueAsString(formatPayload(channelType, message));
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            appendHeaders(builder, config.path("headers"));
            appendSignature(builder, config.path("secret").asText(), body);
            long startedAt = System.nanoTime();
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000L;
            String summary = limit(response.body(), 1000);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ChannelDeliveryException(response.statusCode(), latencyMs,
                        "HTTP " + response.statusCode() + ": " + summary);
            }
            return new SendResult(response.statusCode(), latencyMs, summary);
        } catch (ChannelDeliveryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ChannelDeliveryException(null, 0L, exception.getMessage());
        }
    }

    /** 校验Webhook地址。 */
    private URI validateUri(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("NOTIFY_CHANNEL_URL_EMPTY", "通知渠道未配置Webhook URL");
        }
        URI uri = URI.create(value.trim());
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || !StringUtils.hasText(uri.getHost()) || uri.getUserInfo() != null) {
            throw new BusinessException("NOTIFY_CHANNEL_URL_INVALID", "Webhook只支持无用户凭证的HTTP或HTTPS地址");
        }
        return uri;
    }

    /** 根据渠道类型生成兼容的消息载荷。 */
    private Object formatPayload(String channelType, Map<String, Object> message) {
        String title = String.valueOf(message.getOrDefault("title", "OpenAgentFlow通知"));
        String content = String.valueOf(message.getOrDefault("content", ""));
        String severity = String.valueOf(message.getOrDefault("severity", "info"));
        if ("dingtalk".equalsIgnoreCase(channelType)) {
            return Map.of("msgtype", "markdown", "markdown",
                    Map.of("title", title, "text", "### " + title + "\n\n" + content + "\n\n级别：" + severity));
        }
        if ("wechat".equalsIgnoreCase(channelType)) {
            return Map.of("msgtype", "markdown", "markdown",
                    Map.of("content", "**" + title + "**\n>" + content + "\n>级别：" + severity));
        }
        return message;
    }

    /** 附加用户配置的非敏感请求头。 */
    private void appendHeaders(HttpRequest.Builder builder, JsonNode headers) {
        if (!headers.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = headers.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            if (!"authorization".equalsIgnoreCase(name) && !"host".equalsIgnoreCase(name)
                    && !"content-length".equalsIgnoreCase(name)) {
                builder.header(name, field.getValue().asText());
            }
        }
    }

    /** 使用共享密钥附加HMAC-SHA256签名。 */
    private void appendSignature(HttpRequest.Builder builder, String secret, String body) throws Exception {
        if (!StringUtils.hasText(secret)) {
            return;
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(
                mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
        builder.header("X-OAF-Timestamp", timestamp).header("X-OAF-Signature", signature);
    }

    /** 截断外部响应，避免大响应进入数据库。 */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /** 成功发送结果。 */
    public record SendResult(Integer statusCode, Long latencyMs, String responseSummary) {
    }

    /** 可携带HTTP状态和耗时的渠道异常。 */
    public static class ChannelDeliveryException extends RuntimeException {
        /** HTTP状态码。 */
        private final Integer statusCode;
        /** 请求耗时毫秒。 */
        private final Long latencyMs;

        public ChannelDeliveryException(Integer statusCode, Long latencyMs, String message) {
            super(message);
            this.statusCode = statusCode;
            this.latencyMs = latencyMs;
        }

        public Integer getStatusCode() { return statusCode; }
        public Long getLatencyMs() { return latencyMs; }
    }
}
