package com.openagentflow.domain.chat;

import java.util.List;

/**
 * LLM 调用结果。
 */
public class LlmCallResult {

    /** 模型输出内容。 */
    private String content;

    /** 原始响应 JSON。 */
    private String rawResponse;

    /** 提示词 Token 数。 */
    private Integer promptTokens = 0;

    /** 完成 Token 数。 */
    private Integer completionTokens = 0;

    /** 总 Token 数。 */
    private Integer totalTokens = 0;

    /** 调用耗时毫秒。 */
    private Integer latencyMs = 0;

    /** 流式调用首个有效分片到达耗时。 */
    private Integer firstTokenLatencyMs = 0;

    /** 模型请求调用的工具列表。 */
    private List<ToolCallRequest> toolCalls = List.of();

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
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

    public Integer getFirstTokenLatencyMs() {
        return firstTokenLatencyMs;
    }

    public void setFirstTokenLatencyMs(Integer firstTokenLatencyMs) {
        this.firstTokenLatencyMs = firstTokenLatencyMs;
    }

    public List<ToolCallRequest> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallRequest> toolCalls) {
        this.toolCalls = toolCalls;
    }
}
