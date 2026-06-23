package com.openagentflow.domain.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天补全请求对象。
 */
public class ChatCompletionRequest {

    /** Agent ID，不传时使用默认已发布 Agent。 */
    private String agentId;

    /** 模型 ID，不传时优先使用 Agent 绑定模型。 */
    private String modelId;

    /** 历史会话 ID，不传时后端自动创建新会话。 */
    private String sessionId;

    /** 用户本次输入。 */
    @NotBlank(message = "用户输入不能为空")
    private String input;

    /** 历史消息列表，用于多轮对话上下文。 */
    @Valid
    private List<ChatMessage> history = new ArrayList<>();

    /** 温度参数，不传时使用模型或 Agent 默认参数。 */
    private Double temperature;

    /** 最大输出 Token 数。 */
    private Integer maxTokens;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}
