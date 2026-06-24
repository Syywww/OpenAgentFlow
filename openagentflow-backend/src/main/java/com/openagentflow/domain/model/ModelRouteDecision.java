package com.openagentflow.domain.model;

import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;

import java.util.List;

/**
 * 模型网关路由决策。
 */
public class ModelRouteDecision {

    /** 当前选中的模型。 */
    private ModelConfigEntity model;

    /** 当前选中的服务商。 */
    private ModelProviderEntity provider;

    /** 当前服务商可用 API Key。 */
    private String apiKey;

    /** 命中的路由策略ID。 */
    private String routePolicyId;

    /** 命中的路由策略名称。 */
    private String routePolicyName;

    /** 网关场景类型。 */
    private String sceneType;

    /** 候选模型ID列表。 */
    private List<String> candidateModelIds = List.of();

    /** 当前候选下标。 */
    private Integer candidateIndex = 0;

    /** 是否使用失败回退。 */
    private Boolean fallbackEnabled = false;

    /** 是否为显式指定模型。 */
    private Boolean explicitModel = false;

    /** 是否已经发生回退。 */
    private Boolean fallbackUsed = false;

    /** 决策原因。 */
    private String reason;

    public ModelConfigEntity getModel() {
        return model;
    }

    public void setModel(ModelConfigEntity model) {
        this.model = model;
    }

    public ModelProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ModelProviderEntity provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRoutePolicyId() {
        return routePolicyId;
    }

    public void setRoutePolicyId(String routePolicyId) {
        this.routePolicyId = routePolicyId;
    }

    public String getRoutePolicyName() {
        return routePolicyName;
    }

    public void setRoutePolicyName(String routePolicyName) {
        this.routePolicyName = routePolicyName;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public List<String> getCandidateModelIds() {
        return candidateModelIds;
    }

    public void setCandidateModelIds(List<String> candidateModelIds) {
        this.candidateModelIds = candidateModelIds;
    }

    public Integer getCandidateIndex() {
        return candidateIndex;
    }

    public void setCandidateIndex(Integer candidateIndex) {
        this.candidateIndex = candidateIndex;
    }

    public Boolean getFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(Boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public Boolean getExplicitModel() {
        return explicitModel;
    }

    public void setExplicitModel(Boolean explicitModel) {
        this.explicitModel = explicitModel;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
