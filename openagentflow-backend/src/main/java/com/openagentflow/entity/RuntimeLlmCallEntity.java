package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运行时大模型CALL表。
 * <p>对应数据库表：runtime_llm_call。</p>
 */
@TableName("runtime_llm_call")
public class RuntimeLlmCallEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 步骤ID。 */
    @TableField("step_id")
    private String stepId;

    /** 服务商ID。 */
    @TableField("provider_id")
    private String providerId;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 请求MESSAGES。 */
    @TableField("request_messages")
    private String requestMessages;

    /** 响应MESSAGE。 */
    @TableField("response_message")
    private String responseMessage;

    /** 字段说明：STREAM。 */
    @TableField("stream")
    private Boolean stream;

    /** 提示词Token数。 */
    @TableField("prompt_tokens")
    private Integer promptTokens;

    /** 完成Token数。 */
    @TableField("completion_tokens")
    private Integer completionTokens;

    /** 总Token数。 */
    @TableField("total_tokens")
    private Integer totalTokens;

    /** 成本AMOUNT。 */
    @TableField("cost_amount")
    private BigDecimal costAmount;

    /** 耗时毫秒。 */
    @TableField("latency_ms")
    private Integer latencyMs;

    /** 成功。 */
    @TableField("success")
    private Boolean success;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

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

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getRequestMessages() {
        return requestMessages;
    }

    public void setRequestMessages(String requestMessages) {
        this.requestMessages = requestMessages;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
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

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
