package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 插件安装表。
 * <p>对应数据库表：plugin_installation。</p>
 */
@TableName("plugin_installation")
public class PluginInstallationEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 插件ID。 */
    @TableField("plugin_id")
    private String pluginId;

    /** INSTALLED版本。 */
    @TableField("installed_version")
    private String installedVersion;

    /** 字段说明：INSTALLSCOPE。 */
    @TableField("install_scope")
    private String installScope;

    /** 配置JSON。 */
    @TableField("config_json")
    private String configJson;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** INSTALLED人。 */
    @TableField("installed_by")
    private String installedBy;

    /** INSTALLED时间。 */
    @TableField("installed_at")
    private LocalDateTime installedAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public void setInstalledVersion(String installedVersion) {
        this.installedVersion = installedVersion;
    }

    public String getInstallScope() {
        return installScope;
    }

    public void setInstallScope(String installScope) {
        this.installScope = installScope;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getInstalledBy() {
        return installedBy;
    }

    public void setInstalledBy(String installedBy) {
        this.installedBy = installedBy;
    }

    public LocalDateTime getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(LocalDateTime installedAt) {
        this.installedAt = installedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
