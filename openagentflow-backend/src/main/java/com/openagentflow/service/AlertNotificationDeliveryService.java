package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 告警外部通知的失败补偿、指数退避与死信收敛服务。
 */
@Service
public class AlertNotificationDeliveryService {

    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON序列化工具。 */
    private final ObjectMapper objectMapper;
    /** 统一Webhook通知客户端。 */
    private final NotificationWebhookClient webhookClient;

    public AlertNotificationDeliveryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                                            NotificationWebhookClient webhookClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.webhookClient = webhookClient;
    }

    /**
     * 定时抢占到期投递，最多重试五次并按指数退避安排下次执行。
     */
    @Scheduled(fixedDelayString = "${openagentflow.ops.notification-retry-ms:10000}")
    public void retryDueDeliveries() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.*,c.config_json,c.channel_type configured_channel_type,
                       e.alert_title,e.alert_detail,e.severity
                FROM ops_notification_delivery d
                LEFT JOIN ops_notify_channel c ON c.id=d.channel_id
                JOIN ops_alert_event e ON e.id=d.alert_event_id
                WHERE d.status IN ('pending','failed') AND (d.next_retry_at IS NULL OR d.next_retry_at<=NOW(3))
                ORDER BY d.created_at LIMIT 50
                """);
        rows.forEach(this::deliver);
    }

    /** 执行单次投递并持久化成功或失败状态。 */
    private void deliver(Map<String, Object> row) {
        String id = String.valueOf(row.get("id"));
        String channelId = row.get("channel_id") == null ? null : String.valueOf(row.get("channel_id"));
        int attempts = ((Number) row.get("attempt_count")).intValue() + 1;
        try {
            String channelType = row.get("configured_channel_type") == null
                    ? String.valueOf(row.get("channel_type")) : String.valueOf(row.get("configured_channel_type"));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", row.get("alert_title"));
            payload.put("content", row.get("alert_detail"));
            payload.put("severity", row.get("severity"));
            payload.put("eventId", row.get("alert_event_id"));
            String payloadJson = objectMapper.writeValueAsString(payload);
            NotificationWebhookClient.SendResult response = webhookClient.send(
                    channelType, String.valueOf(row.get("config_json")), payload);
            jdbcTemplate.update("""
                    UPDATE ops_notification_delivery
                    SET status='sent',attempt_count=?,request_payload=?,response_summary=?,error_message=NULL,
                        sent_at=NOW(3),updated_at=NOW(3) WHERE id=?
                    """, attempts, payloadJson, limit(response.responseSummary(), 1000), id);
            if (channelId != null) {
                jdbcTemplate.update("""
                        UPDATE ops_notify_channel
                        SET last_success_at=NOW(3),failure_count=0 WHERE id=?
                        """, channelId);
            }
        } catch (Exception exception) {
            String status = attempts >= 5 ? "dead" : "failed";
            int delayMinutes = Math.min(60, 1 << Math.min(attempts, 6));
            jdbcTemplate.update("""
                    UPDATE ops_notification_delivery SET status=?,attempt_count=?,error_message=?,
                      next_retry_at=DATE_ADD(NOW(3),INTERVAL ? MINUTE),updated_at=NOW(3) WHERE id=?
                    """, status, attempts, limit(exception.getMessage(), 2000), delayMinutes, id);
            if (channelId != null) {
                jdbcTemplate.update("UPDATE ops_notify_channel SET failure_count=failure_count+1 WHERE id=?", channelId);
            }
        }
    }

    /** 截断外部响应和错误，避免超长内容写入状态字段。 */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
