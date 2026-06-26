package com.openagentflow.domain.chat;

import com.openagentflow.domain.knowledge.KnowledgeSource;
import com.openagentflow.domain.memory.MemoryDtos;

import java.util.List;
import java.util.Map;

/**
 * 聊天补全响应对象。
 */
public class ChatCompletionResponse {

    /** 运行 ID，可用于跳转 Trace 详情。 */
    private String runId;

    /** 历史会话 ID，可用于继续对话。 */
    private String sessionId;

    /** 模型输出内容。 */
    private String content;

    /** 模型服务商名称。 */
    private String providerName;

    /** 模型名称。 */
    private String modelName;

    /** 运行状态。 */
    private String status;

    /** 提示词 Token 数。 */
    private Integer promptTokens;

    /** 完成 Token 数。 */
    private Integer completionTokens;

    /** 总 Token 数。 */
    private Integer totalTokens;

    /** 本次调用耗时毫秒。 */
    private Integer latencyMs;

    /** 错误信息。 */
    private String errorMessage;

    /** RAG 引用来源列表。 */
    private List<KnowledgeSource> sources;

    /** Memory 召回来源列表。 */
    private List<MemoryDtos.RecallItem> memories;

    /** 工具调用结果列表。 */
    private List<Map<String, Object>> toolResults;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<KnowledgeSource> getSources() {
        return sources;
    }

    public void setSources(List<KnowledgeSource> sources) {
        this.sources = sources;
    }

    public List<MemoryDtos.RecallItem> getMemories() {
        return memories;
    }

    public void setMemories(List<MemoryDtos.RecallItem> memories) {
        this.memories = memories;
    }

    public List<Map<String, Object>> getToolResults() {
        return toolResults;
    }

    public void setToolResults(List<Map<String, Object>> toolResults) {
        this.toolResults = toolResults;
    }
}
