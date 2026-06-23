package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 权限用户偏好表。
 * <p>对应数据库表：iam_user_preference。</p>
 */
@TableName("iam_user_preference")
public class IamUserPreferenceEntity {

    /** 用户ID。 */
    @TableId(value = "user_id")
    private String userId;

    /** 字段说明：LOCALE。 */
    @TableField("locale")
    private String locale;

    /** 字段说明：TIMEZONE。 */
    @TableField("timezone")
    private String timezone;

    /** 字段说明：THEME。 */
    @TableField("theme")
    private String theme;

    /** 字段说明：SETTINGS。 */
    @TableField("settings")
    private String settings;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
