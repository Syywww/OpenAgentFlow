package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** Prompt资源版本绑定实体。 */
@TableName("prompt_binding")
public class PromptBindingEntity {
    /** 主键ID。 */
    @TableId("id") private String id;
    /** 工作空间ID。 */
    @TableField("workspace_id") private String workspaceId;
    /** 资源类型。 */
    @TableField("resource_type") private String resourceType;
    /** 资源ID。 */
    @TableField("resource_id") private String resourceId;
    /** Prompt角色。 */
    @TableField("prompt_role") private String promptRole;
    /** 模板ID。 */
    @TableField("template_id") private String templateId;
    /** 锁定版本ID。 */
    @TableField("version_id") private String versionId;
    /** 绑定模式。 */
    @TableField("binding_mode") private String bindingMode;
    /** 变量值JSON。 */
    @TableField("variable_values") private String variableValues;
    /** 是否启用。 */
    @TableField("enabled") private Boolean enabled;
    /** 创建人ID。 */
    @TableField("created_by") private String createdBy;
    /** 创建时间。 */
    @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */
    @TableField("updated_at") private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getPromptRole() { return promptRole; }
    public void setPromptRole(String promptRole) { this.promptRole = promptRole; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }
    public String getBindingMode() { return bindingMode; }
    public void setBindingMode(String bindingMode) { this.bindingMode = bindingMode; }
    public String getVariableValues() { return variableValues; }
    public void setVariableValues(String variableValues) { this.variableValues = variableValues; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
