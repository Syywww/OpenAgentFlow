package com.openagentflow.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型服务商摘要对象。
 */
public class ModelProviderSummary {

    /** 模型服务商主键 ID。 */
    private String id;

    /** 模型服务商编码。 */
    private String providerCode;

    /** 模型服务商名称。 */
    private String providerName;

    /** 模型服务商类型。 */
    private String providerType;

    /** 服务基础地址。 */
    private String baseUrl;

    /** 认证类型。 */
    private String authType;

    /** 启用状态。 */
    private String status;

    /** 健康状态。 */
    private String healthStatus;

    /** 脱敏后的 API Key。 */
    private String keyMask;

    /** 该服务商下的模型列表。 */
    private List<ModelConfigSummary> models = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getKeyMask() {
        return keyMask;
    }

    public void setKeyMask(String keyMask) {
        this.keyMask = keyMask;
    }

    public List<ModelConfigSummary> getModels() {
        return models;
    }

    public void setModels(List<ModelConfigSummary> models) {
        this.models = models;
    }
}
