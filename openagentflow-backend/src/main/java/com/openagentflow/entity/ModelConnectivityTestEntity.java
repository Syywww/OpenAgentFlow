package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 模型CONNECTIVITY测试表。
 * <p>对应数据库表：model_connectivity_test。</p>
 */
@TableName("model_connectivity_test")
public class ModelConnectivityTestEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 服务商ID。 */
    @TableField("provider_id")
    private String providerId;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 测试类型。 */
    @TableField("test_type")
    private String testType;

    /** 成功。 */
    @TableField("success")
    private Boolean success;

    /** 耗时毫秒。 */
    @TableField("latency_ms")
    private Integer latencyMs;

    /** 请求载荷。 */
    @TableField("request_payload")
    private String requestPayload;

    /** 响应载荷。 */
    @TableField("response_payload")
    private String responsePayload;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** TESTED人。 */
    @TableField("tested_by")
    private String testedBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTestedBy() {
        return testedBy;
    }

    public void setTestedBy(String testedBy) {
        this.testedBy = testedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
