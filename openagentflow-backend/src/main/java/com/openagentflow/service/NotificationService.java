package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.notification.NotificationDtos;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 个人通知收件箱、偏好和统一发布服务。
 */
@Service
public class NotificationService {

    /** 支持的通知级别顺序。 */
    private static final List<String> SEVERITIES = List.of("info", "warning", "critical");
    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** 支持命名参数的数据访问工具。 */
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    /** JSON序列化工具。 */
    private final ObjectMapper objectMapper;

    public NotificationService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询当前用户通知。
     */
    public PageResult<NotificationDtos.NotificationItem> list(String status, String notificationType,
                                                               String severity, String keyword,
                                                               Integer pageNo, Integer pageSize) {
        String userId = currentUserIdOrThrow();
        int safePageNo = Math.max(1, pageNo == null ? 1 : pageNo);
        int safePageSize = Math.min(100, Math.max(1, pageSize == null ? 10 : pageSize));
        List<Object> args = new ArrayList<>();
        String where = buildFilter(status, notificationType, severity, keyword, userId, args);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) " + baseJoin() + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safePageSize);
        pageArgs.add((safePageNo - 1) * safePageSize);
        List<NotificationDtos.NotificationItem> items = jdbcTemplate.query("""
                SELECT n.id,n.notification_type,n.title,n.content,n.severity,n.resource_type,n.resource_id,
                       n.action_url,n.payload,n.expires_at,n.created_at,r.read_at,r.archived_at
                """ + baseJoin() + where + " ORDER BY n.created_at DESC LIMIT ? OFFSET ?",
                this::mapItem, pageArgs.toArray());
        return new PageResult<>(items, total == null ? 0L : total, safePageNo, safePageSize);
    }

    /**
     * 查询当前用户通知数量汇总。
     */
    public NotificationDtos.NotificationOverview overview() {
        String userId = currentUserIdOrThrow();
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                  SUM(CASE WHEN r.archived_at IS NULL THEN 1 ELSE 0 END) total_count,
                  SUM(CASE WHEN r.archived_at IS NULL AND r.read_at IS NULL THEN 1 ELSE 0 END) unread_count,
                  SUM(CASE WHEN r.archived_at IS NULL AND r.read_at IS NULL AND n.severity='critical' THEN 1 ELSE 0 END) critical_count,
                  SUM(CASE WHEN r.archived_at IS NULL AND r.read_at IS NULL AND n.severity='warning' THEN 1 ELSE 0 END) warning_count,
                  SUM(CASE WHEN r.archived_at IS NOT NULL THEN 1 ELSE 0 END) archived_count
                FROM notification_recipient r JOIN notification n ON n.id=r.notification_id
                WHERE r.user_id=? AND (n.expires_at IS NULL OR n.expires_at>NOW(3))
                """, userId);
        NotificationDtos.NotificationOverview result = new NotificationDtos.NotificationOverview();
        result.setTotalCount(longValue(row.get("total_count")));
        result.setUnreadCount(longValue(row.get("unread_count")));
        result.setCriticalUnreadCount(longValue(row.get("critical_count")));
        result.setWarningUnreadCount(longValue(row.get("warning_count")));
        result.setArchivedCount(longValue(row.get("archived_count")));
        return result;
    }

    /** 标记单条通知已读。 */
    public void markRead(String notificationId) {
        updateOwned("read_at=COALESCE(read_at,NOW(3))", List.of(notificationId));
    }

    /** 批量标记通知已读。 */
    public void markReadBatch(List<String> notificationIds) {
        updateOwned("read_at=COALESCE(read_at,NOW(3))", notificationIds);
    }

    /** 标记当前用户全部未归档通知已读。 */
    public void markAllRead() {
        jdbcTemplate.update("""
                UPDATE notification_recipient r JOIN notification n ON n.id=r.notification_id
                SET r.read_at=COALESCE(r.read_at,NOW(3))
                WHERE r.user_id=? AND r.archived_at IS NULL AND (n.expires_at IS NULL OR n.expires_at>NOW(3))
                """, currentUserIdOrThrow());
    }

    /** 归档单条通知。 */
    public void archive(String notificationId) {
        updateOwned("archived_at=COALESCE(archived_at,NOW(3)),read_at=COALESCE(read_at,NOW(3))", List.of(notificationId));
    }

    /** 批量归档通知。 */
    public void archiveBatch(List<String> notificationIds) {
        updateOwned("archived_at=COALESCE(archived_at,NOW(3)),read_at=COALESCE(read_at,NOW(3))", notificationIds);
    }

    /**
     * 查询当前用户通知偏好，不存在时返回平台默认值。
     */
    public NotificationDtos.Preference preference() {
        String userId = currentUserIdOrThrow();
        List<NotificationDtos.Preference> rows = jdbcTemplate.query("""
                SELECT enabled_types,min_severity,station_enabled,email_enabled,webhook_enabled,
                       quiet_start,quiet_end,digest_mode
                FROM notification_preference WHERE user_id=?
                """, (rs, rowNum) -> mapPreference(rs), userId);
        return rows.isEmpty() ? defaultPreference() : rows.getFirst();
    }

    /**
     * 保存当前用户通知偏好。
     */
    public NotificationDtos.Preference savePreference(NotificationDtos.Preference request) {
        String userId = currentUserIdOrThrow();
        validatePreference(request);
        jdbcTemplate.update("""
                INSERT INTO notification_preference
                  (id,user_id,enabled_types,min_severity,station_enabled,email_enabled,webhook_enabled,quiet_start,quiet_end,digest_mode)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE enabled_types=VALUES(enabled_types),min_severity=VALUES(min_severity),
                  station_enabled=VALUES(station_enabled),email_enabled=VALUES(email_enabled),webhook_enabled=VALUES(webhook_enabled),
                  quiet_start=VALUES(quiet_start),quiet_end=VALUES(quiet_end),digest_mode=VALUES(digest_mode)
                """, UUID.randomUUID().toString(), userId, toJson(request.getEnabledTypes()), request.getMinSeverity(),
                Boolean.TRUE.equals(request.getStationEnabled()), Boolean.TRUE.equals(request.getEmailEnabled()),
                Boolean.TRUE.equals(request.getWebhookEnabled()), request.getQuietStart(), request.getQuietEnd(),
                request.getDigestMode());
        return preference();
    }

    /**
     * 发布通知并按用户偏好生成接收关系。
     */
    @Transactional
    public NotificationDtos.PublishResult publish(NotificationDtos.PublishRequest request) {
        return publishInternal(request, currentUserIdOrThrow());
    }

    /**
     * 由平台内部模块发布系统通知，适用于调度线程没有登录上下文的场景。
     */
    @Transactional
    public NotificationDtos.PublishResult publishSystem(NotificationDtos.PublishRequest request, String createdBy) {
        return publishInternal(request, createdBy);
    }

    /** 执行统一的通知持久化、偏好过滤和接收关系创建。 */
    private NotificationDtos.PublishResult publishInternal(NotificationDtos.PublishRequest request, String createdBy) {
        validatePublish(request);
        String existingId = findDeduplicated(request.getNotificationType(), request.getDedupeKey());
        if (existingId != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM notification_recipient WHERE notification_id=?", Integer.class, existingId);
            return new NotificationDtos.PublishResult(existingId, count == null ? 0 : count, true);
        }
        Set<String> recipients = resolveRecipients(request);
        List<String> accepted = filterByPreference(recipients, request.getNotificationType(), normalizeSeverity(request.getSeverity()));
        if (accepted.isEmpty()) {
            throw new BusinessException("NOTIFICATION_RECIPIENT_EMPTY", "没有符合通知偏好的接收用户");
        }
        String notificationId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO notification
                  (id,notification_type,title,content,severity,resource_type,resource_id,action_url,dedupe_key,expires_at,payload,created_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, notificationId, request.getNotificationType().trim(), request.getTitle().trim(), request.getContent().trim(),
                normalizeSeverity(request.getSeverity()), trimToNull(request.getResourceType()), trimToNull(request.getResourceId()),
                trimToNull(request.getActionUrl()), trimToNull(request.getDedupeKey()), request.getExpiresAt(),
                toJson(request.getPayload() == null ? Map.of() : request.getPayload()), createdBy);
        jdbcTemplate.batchUpdate("INSERT INTO notification_recipient(id,notification_id,user_id) VALUES (?,?,?)",
                accepted, 500, (statement, userId) -> {
                    statement.setString(1, UUID.randomUUID().toString());
                    statement.setString(2, notificationId);
                    statement.setString(3, userId);
                });
        return new NotificationDtos.PublishResult(notificationId, accepted.size(), false);
    }

    /** 构建通知关联查询。 */
    private String baseJoin() {
        return "FROM notification_recipient r JOIN notification n ON n.id=r.notification_id ";
    }

    /** 按请求构建参数化筛选条件。 */
    private String buildFilter(String status, String type, String severity, String keyword,
                               String userId, List<Object> args) {
        StringBuilder where = new StringBuilder("WHERE r.user_id=? AND (n.expires_at IS NULL OR n.expires_at>NOW(3)) ");
        args.add(userId);
        String normalizedStatus = status == null ? "all" : status.toLowerCase(Locale.ROOT);
        switch (normalizedStatus) {
            case "unread" -> where.append("AND r.archived_at IS NULL AND r.read_at IS NULL ");
            case "read" -> where.append("AND r.archived_at IS NULL AND r.read_at IS NOT NULL ");
            case "archived" -> where.append("AND r.archived_at IS NOT NULL ");
            default -> where.append("AND r.archived_at IS NULL ");
        }
        if (StringUtils.hasText(type)) {
            where.append("AND n.notification_type=? ");
            args.add(type.trim());
        }
        if (StringUtils.hasText(severity)) {
            where.append("AND n.severity=? ");
            args.add(normalizeSeverity(severity));
        }
        if (StringUtils.hasText(keyword)) {
            where.append("AND (n.title LIKE ? OR n.content LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        return where.toString();
    }

    /** 把查询行转换为前端通知对象。 */
    private NotificationDtos.NotificationItem mapItem(ResultSet rs, int rowNum) throws SQLException {
        NotificationDtos.NotificationItem item = new NotificationDtos.NotificationItem();
        item.setId(rs.getString("id"));
        item.setNotificationType(rs.getString("notification_type"));
        item.setTitle(rs.getString("title"));
        item.setContent(rs.getString("content"));
        item.setSeverity(rs.getString("severity"));
        item.setResourceType(rs.getString("resource_type"));
        item.setResourceId(rs.getString("resource_id"));
        item.setActionUrl(rs.getString("action_url"));
        item.setPayload(readMap(rs.getString("payload")));
        item.setRead(rs.getTimestamp("read_at") != null);
        item.setArchived(rs.getTimestamp("archived_at") != null);
        item.setCreatedAt(localDateTime(rs.getTimestamp("created_at")));
        item.setExpiresAt(localDateTime(rs.getTimestamp("expires_at")));
        return item;
    }

    /** 更新属于当前用户的通知接收状态。 */
    private void updateOwned(String setClause, List<String> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new BusinessException("NOTIFICATION_IDS_EMPTY", "请选择需要处理的通知");
        }
        namedJdbcTemplate.update("UPDATE notification_recipient SET " + setClause
                        + " WHERE user_id=:userId AND notification_id IN (:ids)",
                new MapSqlParameterSource("userId", currentUserIdOrThrow()).addValue("ids", notificationIds));
    }

    /** 解析数据库中的通知偏好。 */
    private NotificationDtos.Preference mapPreference(ResultSet rs) throws SQLException {
        NotificationDtos.Preference value = new NotificationDtos.Preference();
        value.setEnabledTypes(readList(rs.getString("enabled_types")));
        value.setMinSeverity(rs.getString("min_severity"));
        value.setStationEnabled(rs.getBoolean("station_enabled"));
        value.setEmailEnabled(rs.getBoolean("email_enabled"));
        value.setWebhookEnabled(rs.getBoolean("webhook_enabled"));
        value.setQuietStart(rs.getObject("quiet_start", LocalTime.class));
        value.setQuietEnd(rs.getObject("quiet_end", LocalTime.class));
        value.setDigestMode(rs.getString("digest_mode"));
        return value;
    }

    /** 返回平台默认通知偏好。 */
    private NotificationDtos.Preference defaultPreference() {
        NotificationDtos.Preference value = new NotificationDtos.Preference();
        value.setEnabledTypes(List.of());
        value.setMinSeverity("info");
        value.setStationEnabled(true);
        value.setEmailEnabled(false);
        value.setWebhookEnabled(false);
        value.setDigestMode("realtime");
        return value;
    }

    /** 校验通知偏好。 */
    private void validatePreference(NotificationDtos.Preference request) {
        if (request == null) {
            throw new BusinessException("NOTIFICATION_PREFERENCE_INVALID", "通知偏好不能为空");
        }
        request.setMinSeverity(normalizeSeverity(request.getMinSeverity()));
        if (!List.of("realtime", "hourly", "daily").contains(request.getDigestMode())) {
            throw new BusinessException("NOTIFICATION_DIGEST_INVALID", "通知频率只支持 realtime、hourly 或 daily");
        }
        if (request.getEnabledTypes() == null) {
            request.setEnabledTypes(List.of());
        }
    }

    /** 校验通知发布参数。 */
    private void validatePublish(NotificationDtos.PublishRequest request) {
        if (request == null || !StringUtils.hasText(request.getNotificationType())
                || !StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent())) {
            throw new BusinessException("NOTIFICATION_PUBLISH_INVALID", "通知类型、标题和正文不能为空");
        }
        normalizeSeverity(request.getSeverity());
        boolean noUsers = request.getRecipientUserIds() == null || request.getRecipientUserIds().isEmpty();
        boolean noRoles = request.getRecipientRoleCodes() == null || request.getRecipientRoleCodes().isEmpty();
        if (!Boolean.TRUE.equals(request.getBroadcast()) && noUsers && noRoles) {
            throw new BusinessException("NOTIFICATION_RECIPIENT_EMPTY", "必须指定接收用户、角色或全员广播");
        }
    }

    /** 按去重键查询已有通知。 */
    private String findDeduplicated(String type, String dedupeKey) {
        if (!StringUtils.hasText(dedupeKey)) {
            return null;
        }
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM notification WHERE notification_type=? AND dedupe_key=? LIMIT 1",
                String.class, type.trim(), dedupeKey.trim());
        return ids.isEmpty() ? null : ids.getFirst();
    }

    /** 解析用户ID、角色和广播范围。 */
    private Set<String> resolveRecipients(NotificationDtos.PublishRequest request) {
        Set<String> users = new LinkedHashSet<>();
        if (request.getRecipientUserIds() != null) {
            users.addAll(request.getRecipientUserIds().stream().filter(StringUtils::hasText).toList());
        }
        if (request.getRecipientRoleCodes() != null && !request.getRecipientRoleCodes().isEmpty()) {
            users.addAll(namedJdbcTemplate.queryForList("""
                    SELECT DISTINCT ur.user_id FROM iam_user_role ur
                    JOIN iam_role role ON role.id=ur.role_id
                    JOIN iam_user u ON u.id=ur.user_id
                    WHERE role.role_code IN (:roles) AND role.status='enabled'
                      AND u.status='enabled' AND u.deleted_at IS NULL
                    """, Map.of("roles", request.getRecipientRoleCodes()), String.class));
        }
        if (Boolean.TRUE.equals(request.getBroadcast())) {
            users.addAll(jdbcTemplate.queryForList(
                    "SELECT id FROM iam_user WHERE status='enabled' AND deleted_at IS NULL", String.class));
        }
        return users;
    }

    /** 根据站内开关、类型和最低级别过滤接收用户。 */
    private List<String> filterByPreference(Set<String> userIds, String type, String severity) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<String, NotificationDtos.Preference> preferences = namedJdbcTemplate.query("""
                SELECT user_id,enabled_types,min_severity,station_enabled,email_enabled,webhook_enabled,
                       quiet_start,quiet_end,digest_mode
                FROM notification_preference WHERE user_id IN (:ids)
                """, Map.of("ids", userIds), rs -> {
            java.util.HashMap<String, NotificationDtos.Preference> values = new java.util.HashMap<>();
            while (rs.next()) {
                values.put(rs.getString("user_id"), mapPreference(rs));
            }
            return values;
        });
        return userIds.stream().filter(userId -> {
            NotificationDtos.Preference preference = preferences.getOrDefault(userId, defaultPreference());
            boolean typeEnabled = preference.getEnabledTypes().isEmpty() || preference.getEnabledTypes().contains(type);
            return Boolean.TRUE.equals(preference.getStationEnabled()) && typeEnabled
                    && severityRank(severity) >= severityRank(preference.getMinSeverity());
        }).toList();
    }

    /** 标准化严重级别。 */
    private String normalizeSeverity(String value) {
        String severity = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "info";
        if (!SEVERITIES.contains(severity)) {
            throw new BusinessException("NOTIFICATION_SEVERITY_INVALID", "通知级别只支持 info、warning 或 critical");
        }
        return severity;
    }

    /** 返回严重级别排序值。 */
    private int severityRank(String value) {
        int index = SEVERITIES.indexOf(value == null ? "info" : value.toLowerCase(Locale.ROOT));
        return Math.max(0, index);
    }

    /** 获取当前登录用户ID。 */
    private String currentUserIdOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new BusinessException("UNAUTHORIZED", "请先登录");
    }

    /** JSON序列化。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException("NOTIFICATION_JSON_INVALID", "通知扩展数据不是有效JSON");
        }
    }

    /** JSON对象反序列化。 */
    private Map<String, Object> readMap(String value) {
        try {
            return !StringUtils.hasText(value) ? Map.of() : objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /** JSON数组反序列化。 */
    private List<String> readList(String value) {
        try {
            return !StringUtils.hasText(value) ? List.of() : objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /** 读取可空时间。 */
    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    /** 把聚合值安全转换为长整型。 */
    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /** 去除字符串首尾空白并把空串转换为空值。 */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
