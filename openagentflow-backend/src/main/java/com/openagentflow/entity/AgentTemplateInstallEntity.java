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

    /** 目标工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 模板版本ID。 */ @TableField("template_version_id") private String templateVersionId;
    /** 异步任务ID。 */ @TableField("install_task_id") private String installTaskId;
    /** 安装请求幂等键。 */ @TableField("idempotency_key") private String idempotencyKey;

    /** 字段说明：TARGETAgentID。 */
    @TableField("target_agent_id")
    private String targetAgentId;

    /** 安装状态。 */ @TableField("install_status") private String installStatus;
    /** 安装进度。 */ @TableField("progress_percent") private Integer progressPercent;
    /** 当前阶段。 */ @TableField("current_stage") private String currentStage;
    /** 当前说明。 */ @TableField("current_message") private String currentMessage;
    /** 资源名称前缀。 */ @TableField("name_prefix") private String namePrefix;
    /** 模型替代映射JSON。 */ @TableField("model_mapping") private String modelMapping;
    /** Embedding模型ID。 */ @TableField("embedding_model_id") private String embeddingModelId;
    /** 外部凭证是否就绪。 */ @TableField("credentials_ready") private Boolean credentialsReady;

    /** INSTALLED人。 */
    @TableField("installed_by")
    private String installedBy;

    /** INSTALL配置。 */
    @TableField("install_config")
    private String installConfig;

    /** 已安装资源清单JSON。 */ @TableField("installed_manifest") private String installedManifest;
    /** 安装错误原因。 */ @TableField("error_message") private String errorMessage;
    /** 是否存在升级。 */ @TableField("upgrade_available") private Boolean upgradeAvailable;
    /** 安装完成时间。 */ @TableField("completed_at") private LocalDateTime completedAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;

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
