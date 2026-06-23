package com.openagentflow.domain.tool;

/**
 * 工具执行结果。
 */
public class ToolExecutionResult {

    /** 是否执行成功。 */
    private Boolean success;

    /** HTTP 状态码或模拟状态码。 */
    private Integer statusCode;

    /** 执行耗时毫秒。 */
    private Integer latencyMs;

    /** 响应体文本。 */
    private String responseBody;

    /** 错误信息。 */
    private String errorMessage;

    /** 是否因为高风险确认被拦截。 */
    private Boolean confirmationRequired;

    /** 确认请求 ID。 */
    private String confirmationId;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public String getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(String confirmationId) {
        this.confirmationId = confirmationId;
    }
}
