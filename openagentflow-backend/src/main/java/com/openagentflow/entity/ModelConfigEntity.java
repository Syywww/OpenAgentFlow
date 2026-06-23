package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型配置表。
 * <p>对应数据库表：model_config。</p>
 */
@TableName("model_config")
public class ModelConfigEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 服务商ID。 */
    @TableField("provider_id")
    private String providerId;

    /** 模型编码。 */
    @TableField("model_code")
    private String modelCode;

    /** 模型名称。 */
    @TableField("model_name")
    private String modelName;

    /** 模型类型。 */
    @TableField("model_type")
    private String modelType;

    /** 上下文WINDOW。 */
    @TableField("context_window")
    private Integer contextWindow;

    /** MAX输出TOKENS。 */
    @TableField("max_output_tokens")
    private Integer maxOutputTokens;

    /** 输入PRICEPER1K。 */
    @TableField("input_price_per_1k")
    private BigDecimal inputPricePer1k;

    /** 输出PRICEPER1K。 */
    @TableField("output_price_per_1k")
    private BigDecimal outputPricePer1k;

    /** 字段说明：SUPPORTSTREAM。 */
    @TableField("support_stream")
    private Boolean supportStream;

    /** 字段说明：SUPPORTFUNCTIONCALLING。 */
    @TableField("support_function_calling")
    private Boolean supportFunctionCalling;

    /** 字段说明：SUPPORTVISION。 */
    @TableField("support_vision")
    private Boolean supportVision;

    /** 默认参数。 */
    @TableField("default_params")
    private String defaultParams;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** IS默认。 */
    @TableField("is_default")
    private Boolean isDefault;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

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

    public String getDefaultParams() {
        return defaultParams;
    }

    public void setDefaultParams(String defaultParams) {
        this.defaultParams = defaultParams;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
