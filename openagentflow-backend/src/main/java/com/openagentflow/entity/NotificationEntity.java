package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 通知主表。
 * <p>对应数据库表：notification。</p>
 */
@TableName("notification")
public class NotificationEntity {

    /** 主键ID。 */
    @TableId("id")
    private String id;

    /** 所属工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 通知类型。 */
    @TableField("notification_type")
    private String notificationType;

    /** 通知标题。 */
    @TableField("title")
    private String title;

    /** 通知正文。 */
    @TableField("content")
    private String content;

    /** 严重级别。 */
    @TableField("severity")
    private String severity;

    /** 关联资源类型。 */
    @TableField("resource_type")
    private String resourceType;

    /** 关联资源ID。 */
    @TableField("resource_id")
    private String resourceId;

    /** 前端跳转地址。 */
    @TableField("action_url")
    private String actionUrl;

    /** 业务去重键。 */
    @TableField("dedupe_key")
    private String dedupeKey;

    /** 通知失效时间。 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /** 扩展载荷JSON。 */
    @TableField("payload")
    private String payload;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
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
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
