package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户通知接收偏好实体。
 * <p>对应数据库表：notification_preference。</p>
 */
@TableName("notification_preference")
public class NotificationPreferenceEntity {

    /** 主键ID。 */
    @TableId("id")
    private String id;
    /** 用户ID。 */
    @TableField("user_id")
    private String userId;
    /** 允许接收的通知类型JSON数组。 */
    @TableField("enabled_types")
    private String enabledTypes;
    /** 最低接收严重级别。 */
    @TableField("min_severity")
    private String minSeverity;
    /** 是否接收站内通知。 */
    @TableField("station_enabled")
    private Boolean stationEnabled;
    /** 是否接收邮件通知。 */
    @TableField("email_enabled")
    private Boolean emailEnabled;
    /** 是否接收Webhook通知。 */
    @TableField("webhook_enabled")
    private Boolean webhookEnabled;
    /** 免打扰开始时间。 */
    @TableField("quiet_start")
    private LocalTime quietStart;
    /** 免打扰结束时间。 */
    @TableField("quiet_end")
    private LocalTime quietEnd;
    /** 发送频率。 */
    @TableField("digest_mode")
    private String digestMode;
    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEnabledTypes() { return enabledTypes; }
    public void setEnabledTypes(String enabledTypes) { this.enabledTypes = enabledTypes; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
