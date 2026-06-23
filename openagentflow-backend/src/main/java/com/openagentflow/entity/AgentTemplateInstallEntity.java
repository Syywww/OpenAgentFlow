package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent模板INSTALL表。
 * <p>对应数据库表：agent_template_install。</p>
 */
@TableName("agent_template_install")
public class AgentTemplateInstallEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 模板ID。 */
    @TableField("template_id")
    private String templateId;

    /** 字段说明：TARGETAgentID。 */
    @TableField("target_agent_id")
    private String targetAgentId;

    /** INSTALLED人。 */
    @TableField("installed_by")
    private String installedBy;

    /** INSTALL配置。 */
    @TableField("install_config")
    private String installConfig;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTargetAgentId() {
        return targetAgentId;
    }

    public void setTargetAgentId(String targetAgentId) {
        this.targetAgentId = targetAgentId;
    }

    public String getInstalledBy() {
        return installedBy;
    }

    public void setInstalledBy(String installedBy) {
        this.installedBy = installedBy;
    }

    public String getInstallConfig() {
        return installConfig;
    }

    public void setInstallConfig(String installConfig) {
        this.installConfig = installConfig;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
