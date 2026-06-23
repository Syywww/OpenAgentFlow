package com.openagentflow.domain.trace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运行记录列表摘要。
 */
public class RunSummary {

    /** 运行 ID。 */
    private String id;

    /** 运行编号。 */
    private String runNo;

    /** 运行类型。 */
    private String runType;

    /** Agent ID。 */
    private String agentId;

    /** Agent 名称。 */
    private String agentName;

    /** 用户 ID。 */
    private String userId;

    /** 用户展示名称。 */
    private String userName;

    /** 输入文本。 */
    private String inputText;

    /** 输出文本。 */
    private String outputText;

    /** 状态编码。 */
    private String status;

    /** 状态中文标签。 */
    private String statusLabel;

    /** 总 Token 数。 */
    private Integer totalTokens;

    /** 提示词 Token 数。 */
    private Integer promptTokens;

    /** 完成 Token 数。 */
    private Integer completionTokens;

    /** 总成本。 */
    private BigDecimal totalCost;

    /** 耗时毫秒。 */
    private Integer latencyMs;

    /** 错误信息。 */
    private String errorMessage;

    /** 步骤数量。 */
    private Integer stepCount;

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

    public String getRunNo() {
        return runNo;
    }

    public void setRunNo(String runNo) {
        this.runNo = runNo;
    }

    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getOutputText() {
        return outputText;
    }

    public void setOutputText(String outputText) {
        this.outputText = outputText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
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

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
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

    public Integer getStepCount() {
        return stepCount;
    }

    public void setStepCount(Integer stepCount) {
        this.stepCount = stepCount;
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
