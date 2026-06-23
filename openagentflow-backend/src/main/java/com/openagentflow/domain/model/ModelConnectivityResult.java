package com.openagentflow.domain.model;

/**
 * 模型连通性测试结果。
 */
public class ModelConnectivityResult {

    /** 是否测试成功。 */
    private Boolean success;

    /** 健康状态。 */
    private String healthStatus;

    /** 测试耗时毫秒。 */
    private Integer latencyMs;

    /** 测试响应文本。 */
    private String responseText;

    /** 错误信息。 */
    private String errorMessage;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
