package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 通知接收人表。
 * <p>对应数据库表：notification_recipient。</p>
 */
@TableName("notification_recipient")
public class NotificationRecipientEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 通知ID。 */
    @TableField("notification_id")
    private String notificationId;

    /** 用户ID。 */
    @TableField("user_id")
    private String userId;

    /** READ时间。 */
    @TableField("read_at")
    private LocalDateTime readAt;

    /** ARCHIVED时间。 */
    @TableField("archived_at")
    private LocalDateTime archivedAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
