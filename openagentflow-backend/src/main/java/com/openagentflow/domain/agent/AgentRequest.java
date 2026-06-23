package com.openagentflow.domain.agent;

import jakarta.validation.constraints.NotBlank;

/**
 * Agent 保存请求。
 */
public class AgentRequest {

    /** Agent 编码，不传时后端会根据名称自动生成。 */
    private String agentCode;

    /** Agent 名称。 */
    @NotBlank(message = "Agent 名称不能为空")
    private String agentName;

    /** Agent 头像地址。 */
    private String avatarUrl;

    /** Agent 分类。 */
    private String category;

    /** Agent 描述。 */
    private String description;

    /** Agent 类型。 */
    private String agentType;

    /** 绑定模型 ID。 */
    private String modelId;

    /** 所属工作空间 ID。 */
    private String workspaceId;

    /** System Prompt 模板 ID。 */
    private String systemPromptTemplateId;

    /** System Prompt 内容。 */
    private String systemPrompt;

    /** 模型参数 JSON 字符串。 */
    private String modelParams;

    /** 记忆策略。 */
    private String memoryStrategy;

    /** 可见范围。 */
    private String visibility;

    /** Agent 状态。 */
    private String status;

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
}
