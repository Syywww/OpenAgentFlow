package com.openagentflow.domain.knowledge;

import java.util.List;

/**
 * Agent 知识库绑定保存请求。
 */
public class AgentKnowledgeBindingRequest {

    /** 要启用绑定的知识库 ID 列表。 */
    private List<String> knowledgeBaseIds;

    /** 检索返回条数。 */
    private Integer topK;

    /** 相似度阈值。 */
    private Double scoreThreshold;

    /** 是否启用可信回答模式。 */
    private Boolean trustedAnswerMode;

    /** 是否要求回答必须引用来源。 */
    private Boolean citationRequired;

    /** 最少引用来源数量。 */
    private Integer minCitationCount;

    /** 低置信阈值，低于该阈值时可信回答模式会拒答。 */
    private Double lowConfidenceThreshold;

    public List<String> getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }

    public void setKnowledgeBaseIds(List<String> knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    public Boolean getTrustedAnswerMode() {
        return trustedAnswerMode;
    }

    public void setTrustedAnswerMode(Boolean trustedAnswerMode) {
        this.trustedAnswerMode = trustedAnswerMode;
    }

    public Boolean getCitationRequired() {
        return citationRequired;
    }

    public void setCitationRequired(Boolean citationRequired) {
        this.citationRequired = citationRequired;
    }

    public Integer getMinCitationCount() {
        return minCitationCount;
    }

    public void setMinCitationCount(Integer minCitationCount) {
        this.minCitationCount = minCitationCount;
    }

    public Double getLowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(Double lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }
}
