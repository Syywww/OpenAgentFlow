package com.openagentflow.domain.agent;

import java.time.LocalDateTime;

/**
 * Agent 详情对象。
 *
 * <p>用于详情页编辑、发布前预览和调试入口绑定。</p>
 */
public class AgentDetail extends AgentSummary {

    /** Agent 头像地址。 */
    private String avatarUrl;

    /** System Prompt 模板 ID。 */
    private String systemPromptTemplateId;

    /** System Prompt版本ID。 */
    private String systemPromptVersionId;

    /** Prompt绑定模式。 */
    private String promptBindingMode;

    /** Agent级Prompt变量值JSON。 */
    private String promptVariables;

    /** System Prompt 内容。 */
    private String systemPrompt;

    /** 模型参数 JSON 字符串。 */
    private String modelParams;

    /** 记忆策略。 */
    private String memoryStrategy;

    /** 创建人用户 ID。 */
    private String createdBy;

    /** 删除时间，非空表示软删除。 */
    private LocalDateTime deletedAt;

    /** 乐观锁版本号。 */
    private Long version;

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSystemPromptTemplateId() {
        return systemPromptTemplateId;
    }

    public void setSystemPromptTemplateId(String systemPromptTemplateId) {
        this.systemPromptTemplateId = systemPromptTemplateId;
    }

    public String getSystemPromptVersionId() {
        return systemPromptVersionId;
    }

    public void setSystemPromptVersionId(String systemPromptVersionId) {
        this.systemPromptVersionId = systemPromptVersionId;
    }

    public String getPromptBindingMode() {
        return promptBindingMode;
    }

    public void setPromptBindingMode(String promptBindingMode) {
        this.promptBindingMode = promptBindingMode;
    }

    public String getPromptVariables() {
        return promptVariables;
    }

    public void setPromptVariables(String promptVariables) {
        this.promptVariables = promptVariables;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
