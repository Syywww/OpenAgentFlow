package com.openagentflow.domain.trace;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Trace 步骤详情。
 */
public class TraceStepDetail {

    /** 步骤 ID。 */
    private String id;

    /** 运行 ID。 */
    private String runId;

    /** 父步骤 ID。 */
    private String parentStepId;

    /** 步骤编码。 */
    private String stepKey;

    /** 步骤名称。 */
    private String stepName;

    /** 步骤类型，LLM、RAG、TOOL 等。 */
    private String stepType;

    /** 状态。 */
    private String status;

    /** 输入载荷。 */
    private Object inputPayload;

    /** 输出载荷。 */
    private Object outputPayload;

    /** Prompt 文本或消息。 */
    private Object prompt;

    /** Token 使用量。 */
    private Object tokenUsage;

    /** 成本。 */
    private BigDecimal costAmount;

    /** 耗时毫秒。 */
    private Integer latencyMs;

    /** 错误信息。 */
    private String errorMessage;

    /** 关联 LLM 调用。 */
    private Map<String, Object> llmCall;

    /** 关联工具调用。 */
    private Map<String, Object> toolInvocation;

    /** 关联 RAG 检索日志。 */
    private List<Map<String, Object>> retrievalLogs;

    /** 开始时间。 */
    private LocalDateTime startedAt;

    /** 完成时间。 */
    private LocalDateTime finishedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getParentStepId() {
        return parentStepId;
    }

    public void setParentStepId(String parentStepId) {
        this.parentStepId = parentStepId;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(Object inputPayload) {
        this.inputPayload = inputPayload;
    }

    public Object getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(Object outputPayload) {
        this.outputPayload = outputPayload;
    }

    public Object getPrompt() {
        return prompt;
    }

    public void setPrompt(Object prompt) {
        this.prompt = prompt;
    }

    public Object getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(Object tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
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

    public Map<String, Object> getLlmCall() {
        return llmCall;
    }

    public void setLlmCall(Map<String, Object> llmCall) {
        this.llmCall = llmCall;
    }

    public Map<String, Object> getToolInvocation() {
        return toolInvocation;
    }

    public void setToolInvocation(Map<String, Object> toolInvocation) {
        this.toolInvocation = toolInvocation;
    }

    public List<Map<String, Object>> getRetrievalLogs() {
        return retrievalLogs;
    }

    public void setRetrievalLogs(List<Map<String, Object>> retrievalLogs) {
        this.retrievalLogs = retrievalLogs;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
