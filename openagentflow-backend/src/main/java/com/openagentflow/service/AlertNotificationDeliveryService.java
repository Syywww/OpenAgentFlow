package com.openagentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 告警渠道失败补偿、指数退避与死信收敛服务。 */
@Service
public class AlertNotificationDeliveryService {

    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON 工具。 */
    private final ObjectMapper objectMapper;
    /** 告警 Webhook 客户端。 */
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public AlertNotificationDeliveryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** 每十秒抢占到期投递，最多重试五次并指数退避。 */
    @Scheduled(fixedDelayString = "${openagentflow.ops.notification-retry-ms:10000}")
    public void retryDueDeliveries() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.*,c.config_json,e.alert_title,e.alert_detail,e.severity
                FROM ops_notification_delivery d
                LEFT JOIN ops_notify_channel c ON c.id=d.channel_id
                JOIN ops_alert_event e ON e.id=d.alert_event_id
                WHERE d.status IN ('pending','failed') AND (d.next_retry_at IS NULL OR d.next_retry_at<=NOW(3))
                ORDER BY d.created_at LIMIT 50
                """);
        rows.forEach(this::deliver);
    }

    /** 当前支持 Webhook、钉钉和企业微信兼容 JSON 地址，其余渠道进入可见死信。 */
    private void deliver(Map<String, Object> row) {
        String id = String.valueOf(row.get("id"));
        int attempts = ((Number) row.get("attempt_count")).intValue() + 1;
        try {
            JsonNode config = objectMapper.readTree(String.valueOf(row.get("config_json")));
            String url = config.path("url").asText(config.path("webhookUrl").asText());
            if (url.isBlank()) throw new IllegalStateException("通知渠道未配置 Webhook URL");
            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", row.get("alert_title"), "content", row.get("alert_detail"), "severity", row.get("severity")));
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("HTTP " + response.statusCode());
            jdbcTemplate.update("""
                    UPDATE ops_notification_delivery SET status='sent',attempt_count=?,response_summary=?,sent_at=NOW(3),updated_at=NOW(3) WHERE id=?
                    """, attempts, limit(response.body(), 1000), id);
        } catch (Exception exception) {
            String status = attempts >= 5 ? "dead" : "failed";
            int delayMinutes = Math.min(60, 1 << Math.min(attempts, 6));
            jdbcTemplate.update("""
                    UPDATE ops_notification_delivery SET status=?,attempt_count=?,error_message=?,
                      next_retry_at=DATE_ADD(NOW(3),INTERVAL ? MINUTE),updated_at=NOW(3) WHERE id=?
                    """, status, attempts, limit(exception.getMessage(), 2000), delayMinutes, id);
        }
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
