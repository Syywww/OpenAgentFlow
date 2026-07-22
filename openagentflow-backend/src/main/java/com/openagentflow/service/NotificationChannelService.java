package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.notification.NotificationChannelDtos;
import com.openagentflow.entity.OpsNotifyChannelEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.OpsNotifyChannelMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 通知渠道管理、测试和投递明细服务。
 */
@Service
public class NotificationChannelService {

    /** 允许创建的渠道类型。 */
    private static final List<String> CHANNEL_TYPES = List.of("station", "webhook", "dingtalk", "wechat");
    /** 通知渠道Mapper。 */
    private final OpsNotifyChannelMapper channelMapper;
    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON序列化工具。 */
    private final ObjectMapper objectMapper;
    /** 外部Webhook客户端。 */
    private final NotificationWebhookClient webhookClient;

    public NotificationChannelService(OpsNotifyChannelMapper channelMapper, JdbcTemplate jdbcTemplate,
                                      ObjectMapper objectMapper, NotificationWebhookClient webhookClient) {
        this.channelMapper = channelMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.webhookClient = webhookClient;
    }

    /** 查询全部通知渠道，敏感配置只返回是否已配置。 */
    public List<NotificationChannelDtos.ChannelSummary> list() {
        return channelMapper.selectList(null).stream()
                .sorted(Comparator.comparing(OpsNotifyChannelEntity::getChannelType)
                        .thenComparing(OpsNotifyChannelEntity::getChannelCode))
                .map(this::toSummary)
                .toList();
    }

    /** 创建通知渠道。 */
    public NotificationChannelDtos.ChannelSummary create(NotificationChannelDtos.ChannelRequest request) {
        validateRequest(request, null);
        OpsNotifyChannelEntity entity = new OpsNotifyChannelEntity();
        entity.setId(UUID.randomUUID().toString());
        fill(entity, request, null);
        entity.setCreatedBy(currentUserId());
        entity.setFailureCount(0);
        channelMapper.insert(entity);
        return toSummary(channelMapper.selectById(entity.getId()));
    }

    /** 更新通知渠道。 */
    public NotificationChannelDtos.ChannelSummary update(String id, NotificationChannelDtos.ChannelRequest request) {
        OpsNotifyChannelEntity entity = requireChannel(id);
        validateRequest(request, id);
        fill(entity, request, entity.getConfigJson());
        channelMapper.updateById(entity);
        return toSummary(channelMapper.selectById(id));
    }

    /** 启用或停用通知渠道。 */
    public NotificationChannelDtos.ChannelSummary setEnabled(String id, boolean enabled) {
        OpsNotifyChannelEntity entity = requireChannel(id);
        if ("station".equalsIgnoreCase(entity.getChannelType()) && !enabled) {
            throw new BusinessException("NOTIFY_CHANNEL_BUILTIN", "内置站内通知渠道不能停用");
        }
        entity.setEnabled(enabled);
        channelMapper.updateById(entity);
        return toSummary(channelMapper.selectById(id));
    }

    /** 删除未被告警规则使用的外部渠道。 */
    public void delete(String id) {
        OpsNotifyChannelEntity entity = requireChannel(id);
        if ("station".equalsIgnoreCase(entity.getChannelType())) {
            throw new BusinessException("NOTIFY_CHANNEL_BUILTIN", "内置站内通知渠道不能删除");
        }
        Integer references = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ops_alert_rule WHERE FIND_IN_SET(?,REPLACE(notify_channels,' ',''))>0",
                Integer.class, entity.getChannelCode());
        if (references != null && references > 0) {
            throw new BusinessException("NOTIFY_CHANNEL_IN_USE", "通知渠道仍被告警规则使用，请先调整规则");
        }
        channelMapper.deleteById(id);
    }

    /** 发送测试消息并保存测试状态。 */
    public NotificationChannelDtos.TestResult test(String id) {
        OpsNotifyChannelEntity entity = requireChannel(id);
        long startedAt = System.nanoTime();
        try {
            NotificationWebhookClient.SendResult result = webhookClient.send(entity.getChannelType(), entity.getConfigJson(),
                    Map.of("title", "OpenAgentFlow渠道测试", "content", "通知渠道连接正常。", "severity", "info"));
            entity.setLastTestStatus("success");
            entity.setLastTestMessage("连接成功，HTTP " + result.statusCode());
            entity.setLastTestAt(LocalDateTime.now());
            entity.setLastSuccessAt(LocalDateTime.now());
            entity.setFailureCount(0);
            channelMapper.updateById(entity);
            return new NotificationChannelDtos.TestResult(true, result.statusCode(), result.latencyMs(), "通知渠道连接成功");
        } catch (Exception exception) {
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000L;
            Integer statusCode = exception instanceof NotificationWebhookClient.ChannelDeliveryException deliveryException
                    ? deliveryException.getStatusCode() : null;
            entity.setLastTestStatus("failed");
            entity.setLastTestMessage(limit(exception.getMessage(), 500));
            entity.setLastTestAt(LocalDateTime.now());
            entity.setFailureCount((entity.getFailureCount() == null ? 0 : entity.getFailureCount()) + 1);
            channelMapper.updateById(entity);
            return new NotificationChannelDtos.TestResult(false, statusCode, latencyMs, entity.getLastTestMessage());
        }
    }

    /** 分页查询告警外部投递明细。 */
    public PageResult<NotificationChannelDtos.DeliveryItem> deliveries(String status, String channelType,
                                                                       Integer pageNo, Integer pageSize) {
        int safePageNo = Math.max(1, pageNo == null ? 1 : pageNo);
        int safePageSize = Math.min(100, Math.max(1, pageSize == null ? 10 : pageSize));
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (StringUtils.hasText(status)) {
            where.append("AND d.status=? ");
            args.add(status.trim());
        }
        if (StringUtils.hasText(channelType)) {
            where.append("AND d.channel_type=? ");
            args.add(channelType.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ops_notification_delivery d" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safePageSize);
        pageArgs.add((safePageNo - 1) * safePageSize);
        List<NotificationChannelDtos.DeliveryItem> rows = jdbcTemplate.query("""
                SELECT d.*,e.alert_title,c.channel_name
                FROM ops_notification_delivery d
                JOIN ops_alert_event e ON e.id=d.alert_event_id
                LEFT JOIN ops_notify_channel c ON c.id=d.channel_id
                """ + where + " ORDER BY d.created_at DESC LIMIT ? OFFSET ?", this::mapDelivery, pageArgs.toArray());
        return new PageResult<>(rows, total == null ? 0L : total, safePageNo, safePageSize);
    }

    /** 把失败或死信投递恢复为待发送。 */
    public void retryDelivery(String deliveryId) {
        int updated = jdbcTemplate.update("""
                UPDATE ops_notification_delivery
                SET status='pending',next_retry_at=NOW(3),error_message=NULL,updated_at=NOW(3)
                WHERE id=? AND status IN ('failed','dead')
                """, deliveryId);
        if (updated != 1) {
            throw new BusinessException("NOTIFY_DELIVERY_NOT_RETRYABLE", "投递不存在或当前状态不允许重投");
        }
    }

    /** 校验渠道保存请求。 */
    private void validateRequest(NotificationChannelDtos.ChannelRequest request, String currentId) {
        if (request == null || !StringUtils.hasText(request.getChannelCode())
                || !StringUtils.hasText(request.getChannelName()) || !StringUtils.hasText(request.getChannelType())) {
            throw new BusinessException("NOTIFY_CHANNEL_INVALID", "渠道编码、名称和类型不能为空");
        }
        String type = request.getChannelType().trim().toLowerCase(Locale.ROOT);
        if (!CHANNEL_TYPES.contains(type)) {
            throw new BusinessException("NOTIFY_CHANNEL_TYPE_INVALID", "渠道类型只支持 station、webhook、dingtalk 或 wechat");
        }
        if (!request.getChannelCode().trim().matches("[A-Za-z0-9_-]{2,120}")) {
            throw new BusinessException("NOTIFY_CHANNEL_CODE_INVALID", "渠道编码只允许字母、数字、下划线和中划线");
        }
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM ops_notify_channel WHERE channel_code=? AND (? IS NULL OR id<>?) LIMIT 1",
                String.class, request.getChannelCode().trim(), currentId, currentId);
        if (!ids.isEmpty()) {
            throw new BusinessException("NOTIFY_CHANNEL_CODE_EXISTS", "通知渠道编码已存在");
        }
        if (!"station".equals(type)) {
            Map<String, Object> config = request.getConfig() == null ? Map.of() : request.getConfig();
            Object url = config.getOrDefault("url", config.get("webhookUrl"));
            if (url == null || !StringUtils.hasText(String.valueOf(url))) {
                throw new BusinessException("NOTIFY_CHANNEL_URL_EMPTY", "外部通知渠道必须配置Webhook URL");
            }
        }
    }

    /** 把请求字段填充到渠道实体，并保留未重新输入的密钥。 */
    private void fill(OpsNotifyChannelEntity entity, NotificationChannelDtos.ChannelRequest request, String oldConfigJson) {
        entity.setChannelCode(request.getChannelCode().trim());
        entity.setChannelName(request.getChannelName().trim());
        entity.setChannelType(request.getChannelType().trim().toLowerCase(Locale.ROOT));
        Map<String, Object> config = new LinkedHashMap<>(request.getConfig() == null ? Map.of() : request.getConfig());
        if ("******".equals(String.valueOf(config.get("secret"))) && StringUtils.hasText(oldConfigJson)) {
            Object oldSecret = readConfig(oldConfigJson).get("secret");
            if (oldSecret != null) {
                config.put("secret", oldSecret);
            }
        }
        entity.setConfigJson(toJson(config));
        entity.setEnabled(!"station".equals(entity.getChannelType()) ? Boolean.TRUE.equals(request.getEnabled()) : true);
    }

    /** 查询渠道，不存在时抛出业务异常。 */
    private OpsNotifyChannelEntity requireChannel(String id) {
        OpsNotifyChannelEntity entity = channelMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("NOTIFY_CHANNEL_NOT_FOUND", "通知渠道不存在");
        }
        return entity;
    }

    /** 把实体转换为脱敏摘要。 */
    private NotificationChannelDtos.ChannelSummary toSummary(OpsNotifyChannelEntity entity) {
        NotificationChannelDtos.ChannelSummary value = new NotificationChannelDtos.ChannelSummary();
        value.setId(entity.getId());
        value.setChannelCode(entity.getChannelCode());
        value.setChannelName(entity.getChannelName());
        value.setChannelType(entity.getChannelType());
        Map<String, Object> config = new LinkedHashMap<>(readConfig(entity.getConfigJson()));
        if (config.containsKey("secret") && StringUtils.hasText(String.valueOf(config.get("secret")))) {
            config.put("secret", "******");
        }
        value.setConfig(config);
        value.setEnabled(entity.getEnabled());
        value.setLastTestStatus(entity.getLastTestStatus());
        value.setLastTestMessage(entity.getLastTestMessage());
        value.setLastTestAt(entity.getLastTestAt());
        value.setLastSuccessAt(entity.getLastSuccessAt());
        value.setFailureCount(entity.getFailureCount() == null ? 0 : entity.getFailureCount());
        return value;
    }

    /** 把投递查询行转换为响应对象。 */
    private NotificationChannelDtos.DeliveryItem mapDelivery(ResultSet rs, int rowNum) throws SQLException {
        NotificationChannelDtos.DeliveryItem item = new NotificationChannelDtos.DeliveryItem();
        item.setId(rs.getString("id"));
        item.setAlertEventId(rs.getString("alert_event_id"));
        item.setAlertTitle(rs.getString("alert_title"));
        item.setChannelName(rs.getString("channel_name"));
        item.setChannelType(rs.getString("channel_type"));
        item.setStatus(rs.getString("status"));
        item.setAttemptCount(rs.getInt("attempt_count"));
        item.setNextRetryAt(localDateTime(rs.getTimestamp("next_retry_at")));
        item.setResponseSummary(rs.getString("response_summary"));
        item.setErrorMessage(rs.getString("error_message"));
        item.setSentAt(localDateTime(rs.getTimestamp("sent_at")));
        item.setCreatedAt(localDateTime(rs.getTimestamp("created_at")));
        return item;
    }

    /** 读取渠道配置JSON。 */
    private Map<String, Object> readConfig(String value) {
        try {
            return !StringUtils.hasText(value) ? Map.of() : objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /** 序列化渠道配置JSON。 */
    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException("NOTIFY_CHANNEL_CONFIG_INVALID", "通知渠道配置不是有效JSON");
        }
    }

    /** 获取当前登录用户ID。 */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails
                ? userDetails.getUserId() : null;
    }

    /** 读取可空时间。 */
    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    /** 截断数据库状态消息。 */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
