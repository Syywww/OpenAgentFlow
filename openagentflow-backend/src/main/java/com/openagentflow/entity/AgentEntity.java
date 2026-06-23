package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent表。
 * <p>对应数据库表：agent。</p>
 */
@TableName("agent")
public class AgentEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** Agent编码。 */
    @TableField("agent_code")
    private String agentCode;

    /** Agent名称。 */
    @TableField("agent_name")
    private String agentName;

    /** 头像URL。 */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 字段说明：CATEGORY。 */
    @TableField("category")
    private String category;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** Agent类型。 */
    @TableField("agent_type")
    private String agentType;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 所属工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** SYSTEM提示词模板ID。 */
    @TableField("system_prompt_template_id")
    private String systemPromptTemplateId;

    /** SYSTEM提示词。 */
    @TableField("system_prompt")
    private String systemPrompt;

    /** 模型参数。 */
    @TableField("model_params")
    private String modelParams;

    /** 记忆STRATEGY。 */
    @TableField("memory_strategy")
    private String memoryStrategy;

    /** 字段说明：VISIBILITY。 */
    @TableField("visibility")
    private String visibility;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** PUBLISHED版本。 */
    @TableField("published_version")
    private String publishedVersion;

    /** 所有者用户ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 删除时间。 */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /** 版本。 */
    @TableField("version")
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getSystemPromptTemplateId() {
        return systemPromptTemplateId;
    }

    public void setSystemPromptTemplateId(String systemPromptTemplateId) {
        this.systemPromptTemplateId = systemPromptTemplateId;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getModelParams() {
        return modelParams;
    }

    public void setModelParams(String modelParams) {
        this.modelParams = modelParams;
    }

    public String getMemoryStrategy() {
        return memoryStrategy;
    }

    public void setMemoryStrategy(String memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPublishedVersion() {
        return publishedVersion;
    }

    public void setPublishedVersion(String publishedVersion) {
        this.publishedVersion = publishedVersion;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
