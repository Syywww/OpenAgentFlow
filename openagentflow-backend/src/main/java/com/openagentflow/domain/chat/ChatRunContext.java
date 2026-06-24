package com.openagentflow.domain.chat;

import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.domain.model.ModelRouteDecision;

import java.util.List;

/**
 * 聊天运行上下文。
 */
public class ChatRunContext {

    /** 当前 Agent。 */
    private AgentEntity agent;

    /** 当前模型。 */
    private ModelConfigEntity model;

    /** 当前模型服务商。 */
    private ModelProviderEntity provider;

    /** 调用模型使用的 API Key。 */
    private String apiKey;

    /** 当前历史会话 ID。 */
    private String sessionId;

    /** 本次发送给模型的消息列表。 */
    private List<ChatMessage> messages;

    /** 本次 RAG 检索命中的引用来源。 */
    private List<KnowledgeSource> sources;

    /** 当前 Agent 可用的工具定义。 */
    private List<ToolDefinitionForModel> tools;

    /** 模型网关路由决策。 */
    private ModelRouteDecision routeDecision;

    public AgentEntity getAgent() {
        return agent;
    }

    public void setAgent(AgentEntity agent) {
        this.agent = agent;
    }

    public ModelConfigEntity getModel() {
        return model;
    }

    public void setModel(ModelConfigEntity model) {
        this.model = model;
    }

    public ModelProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ModelProviderEntity provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public List<KnowledgeSource> getSources() {
        return sources;
    }

    public void setSources(List<KnowledgeSource> sources) {
        this.sources = sources;
    }

    public List<ToolDefinitionForModel> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinitionForModel> tools) {
        this.tools = tools;
    }

    public ModelRouteDecision getRouteDecision() {
        return routeDecision;
    }

    public void setRouteDecision(ModelRouteDecision routeDecision) {
        this.routeDecision = routeDecision;
    }
}
