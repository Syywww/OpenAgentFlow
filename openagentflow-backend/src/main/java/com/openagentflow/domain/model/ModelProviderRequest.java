package com.openagentflow.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型服务商保存请求。
 */
public class ModelProviderRequest {

    /** 模型服务商编码。 */
    @NotBlank(message = "服务商编码不能为空")
    private String providerCode;

    /** 模型服务商名称。 */
    @NotBlank(message = "服务商名称不能为空")
    private String providerName;

    /** 模型服务商类型。 */
    @NotBlank(message = "服务商类型不能为空")
    private String providerType;

    /** 服务基础地址。 */
    @NotBlank(message = "服务地址不能为空")
    private String baseUrl;

    /** 认证类型，默认 api_key。 */
    private String authType = "api_key";

    /** 默认请求头 JSON 字符串。 */
    private String defaultHeaders;

    /** API Key 明文，仅用于写入或替换，不会在响应中返回。 */
    private String apiKey;

    /** 服务商状态。 */
    private String status = "enabled";

    /** 排序值。 */
    private Integer sortOrder = 0;

    /** 模型配置列表。 */
    @Valid
    private List<ModelConfigRequest> models = new ArrayList<>();

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

    public String getDefaultHeaders() {
        return defaultHeaders;
    }

    public void setDefaultHeaders(String defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<ModelConfigRequest> getModels() {
        return models;
    }

    public void setModels(List<ModelConfigRequest> models) {
        this.models = models;
    }
}
