package com.openagentflow.domain.notification;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通知中心请求与响应对象集合。
 */
public final class NotificationDtos {

    private NotificationDtos() {
    }

    /** 用户通知列表项。 */
    public static class NotificationItem {
        /** 通知ID。 */
        private String id;
        /** 通知类型。 */
        private String notificationType;
        /** 通知标题。 */
        private String title;
        /** 通知正文。 */
        private String content;
        /** 严重级别。 */
        private String severity;
        /** 关联资源类型。 */
        private String resourceType;
        /** 关联资源ID。 */
        private String resourceId;
        /** 前端跳转地址。 */
        private String actionUrl;
        /** 扩展载荷。 */
        private Map<String, Object> payload;
        /** 是否已读。 */
        private Boolean read;
        /** 是否已归档。 */
        private Boolean archived;
        /** 创建时间。 */
        private LocalDateTime createdAt;
        /** 失效时间。 */
        private LocalDateTime expiresAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNotificationType() { return notificationType; }
        public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
        public Boolean getRead() { return read; }
        public void setRead(Boolean read) { this.read = read; }
        public Boolean getArchived() { return archived; }
        public void setArchived(Boolean archived) { this.archived = archived; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    }

    /** 通知数量汇总。 */
    public static class NotificationOverview {
        /** 全部未归档通知数。 */
        private Long totalCount;
        /** 未读通知数。 */
        private Long unreadCount;
        /** 严重通知未读数。 */
        private Long criticalUnreadCount;
        /** 警告通知未读数。 */
        private Long warningUnreadCount;
        /** 已归档通知数。 */
        private Long archivedCount;

        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getUnreadCount() { return unreadCount; }
        public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
        public Long getCriticalUnreadCount() { return criticalUnreadCount; }
        public void setCriticalUnreadCount(Long criticalUnreadCount) { this.criticalUnreadCount = criticalUnreadCount; }
        public Long getWarningUnreadCount() { return warningUnreadCount; }
        public void setWarningUnreadCount(Long warningUnreadCount) { this.warningUnreadCount = warningUnreadCount; }
        public Long getArchivedCount() { return archivedCount; }
        public void setArchivedCount(Long archivedCount) { this.archivedCount = archivedCount; }
    }

    /** 批量通知操作请求。 */
    public static class BatchActionRequest {
        /** 需要处理的通知ID集合。 */
        private List<String> notificationIds = new ArrayList<>();

        public List<String> getNotificationIds() { return notificationIds; }
        public void setNotificationIds(List<String> notificationIds) { this.notificationIds = notificationIds; }
    }

    /** 用户通知偏好。 */
    public static class Preference {
        /** 允许接收的通知类型，空集合表示全部。 */
        private List<String> enabledTypes = new ArrayList<>();
        /** 最低接收严重级别。 */
        private String minSeverity;
        /** 是否启用站内通知。 */
        private Boolean stationEnabled;
        /** 是否启用邮件通知。 */
        private Boolean emailEnabled;
        /** 是否启用Webhook通知。 */
        private Boolean webhookEnabled;
        /** 免打扰开始时间。 */
        private LocalTime quietStart;
        /** 免打扰结束时间。 */
        private LocalTime quietEnd;
        /** 通知摘要频率。 */
        private String digestMode;

        public List<String> getEnabledTypes() { return enabledTypes; }
        public void setEnabledTypes(List<String> enabledTypes) { this.enabledTypes = enabledTypes; }
        public String getMinSeverity() { return minSeverity; }
        public void setMinSeverity(String minSeverity) { this.minSeverity = minSeverity; }
        public Boolean getStationEnabled() { return stationEnabled; }
        public void setStationEnabled(Boolean stationEnabled) { this.stationEnabled = stationEnabled; }
        public Boolean getEmailEnabled() { return emailEnabled; }
        public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
        public Boolean getWebhookEnabled() { return webhookEnabled; }
        public void setWebhookEnabled(Boolean webhookEnabled) { this.webhookEnabled = webhookEnabled; }
        public LocalTime getQuietStart() { return quietStart; }
        public void setQuietStart(LocalTime quietStart) { this.quietStart = quietStart; }
        public LocalTime getQuietEnd() { return quietEnd; }
        public void setQuietEnd(LocalTime quietEnd) { this.quietEnd = quietEnd; }
        public String getDigestMode() { return digestMode; }
        public void setDigestMode(String digestMode) { this.digestMode = digestMode; }
    }

    /** 管理员统一发布通知请求。 */
    public static class PublishRequest {
        /** 通知类型。 */
        private String notificationType;
        /** 通知标题。 */
        private String title;
        /** 通知正文。 */
        private String content;
        /** 严重级别。 */
        private String severity;
        /** 关联资源类型。 */
        private String resourceType;
        /** 关联资源ID。 */
        private String resourceId;
        /** 前端跳转地址。 */
        private String actionUrl;
        /** 业务去重键。 */
        private String dedupeKey;
        /** 通知失效时间。 */
        private LocalDateTime expiresAt;
        /** 扩展载荷。 */
        private Map<String, Object> payload;
        /** 明确指定的接收用户ID。 */
        private List<String> recipientUserIds = new ArrayList<>();
        /** 接收角色编码。 */
        private List<String> recipientRoleCodes = new ArrayList<>();
        /** 是否发送给全部启用用户。 */
        private Boolean broadcast;

        public String getNotificationType() { return notificationType; }
        public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
        public String getDedupeKey() { return dedupeKey; }
        public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
        public List<String> getRecipientUserIds() { return recipientUserIds; }
        public void setRecipientUserIds(List<String> recipientUserIds) { this.recipientUserIds = recipientUserIds; }
        public List<String> getRecipientRoleCodes() { return recipientRoleCodes; }
        public void setRecipientRoleCodes(List<String> recipientRoleCodes) { this.recipientRoleCodes = recipientRoleCodes; }
        public Boolean getBroadcast() { return broadcast; }
        public void setBroadcast(Boolean broadcast) { this.broadcast = broadcast; }
    }

    /** 通知发布结果。 */
    public static class PublishResult {
        /** 通知ID。 */
        private String notificationId;
        /** 实际接收人数。 */
        private Integer recipientCount;
        /** 是否命中已有去重通知。 */
        private Boolean deduplicated;

        public PublishResult(String notificationId, Integer recipientCount, Boolean deduplicated) {
            this.notificationId = notificationId;
            this.recipientCount = recipientCount;
            this.deduplicated = deduplicated;
        }

        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
        public Integer getRecipientCount() { return recipientCount; }
        public void setRecipientCount(Integer recipientCount) { this.recipientCount = recipientCount; }
        public Boolean getDeduplicated() { return deduplicated; }
        public void setDeduplicated(Boolean deduplicated) { this.deduplicated = deduplicated; }
    }
}
