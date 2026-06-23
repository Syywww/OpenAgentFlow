package com.openagentflow.domain.model;

import java.math.BigDecimal;

/**
 * 模型配置摘要对象。
 */
public class ModelConfigSummary {

    /** 模型主键 ID。 */
    private String id;

    /** 所属模型服务商 ID。 */
    private String providerId;

    /** 模型服务商名称。 */
    private String providerName;

    /** 模型编码。 */
    private String modelCode;

    /** 模型名称。 */
    private String modelName;

    /** 模型类型。 */
    private String modelType;

    /** 上下文窗口大小。 */
    private Integer contextWindow;

    /** 最大输出 Token 数。 */
    private Integer maxOutputTokens;

    /** 输入每千 Token 单价。 */
    private BigDecimal inputPricePer1k;

    /** 输出每千 Token 单价。 */
    private BigDecimal outputPricePer1k;

    /** 是否支持流式输出。 */
    private Boolean supportStream;

    /** 是否支持 Function Calling。 */
    private Boolean supportFunctionCalling;

    /** 是否支持视觉输入。 */
    private Boolean supportVision;

    /** 模型状态。 */
    private String status;

    /** 是否默认模型。 */
    private Boolean isDefault;

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

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public Integer getContextWindow() {
        return contextWindow;
    }

    public void setContextWindow(Integer contextWindow) {
        this.contextWindow = contextWindow;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(Integer maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public BigDecimal getInputPricePer1k() {
        return inputPricePer1k;
    }

    public void setInputPricePer1k(BigDecimal inputPricePer1k) {
        this.inputPricePer1k = inputPricePer1k;
    }

    public BigDecimal getOutputPricePer1k() {
        return outputPricePer1k;
    }

    public void setOutputPricePer1k(BigDecimal outputPricePer1k) {
        this.outputPricePer1k = outputPricePer1k;
    }

    public Boolean getSupportStream() {
        return supportStream;
    }

    public void setSupportStream(Boolean supportStream) {
        this.supportStream = supportStream;
    }

    public Boolean getSupportFunctionCalling() {
        return supportFunctionCalling;
    }

    public void setSupportFunctionCalling(Boolean supportFunctionCalling) {
        this.supportFunctionCalling = supportFunctionCalling;
    }

    public Boolean getSupportVision() {
        return supportVision;
    }

    public void setSupportVision(Boolean supportVision) {
        this.supportVision = supportVision;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
