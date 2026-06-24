package com.openagentflow.domain.knowledge;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库检索请求。
 */
public class KnowledgeRetrievalRequest {

    /** 检索问题或关键词。 */
    @NotBlank(message = "检索内容不能为空")
    private String query;

    /** 返回条数。 */
    private Integer topK;

    /** 相似度阈值。 */
    private Double scoreThreshold;

    /** 检索模式：vector、keyword、hybrid。 */
    private String searchMode;

    /** 候选召回数量，最终会在候选内重排后截取 topK。 */
    private Integer candidateK;

    /** 是否启用本地规则重排。 */
    private Boolean rerankEnabled;

    /** 向量得分权重，混合检索时生效。 */
    private Double vectorWeight;

    /** 关键词得分权重，混合检索时生效。 */
    private Double keywordWeight;

    /** 低置信阈值，最佳结果低于该值时提示拒答。 */
    private Double lowConfidenceThreshold;

    /** 低置信时是否建议拒答。 */
    private Boolean rejectLowConfidence;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
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

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public Integer getCandidateK() {
        return candidateK;
    }

    public void setCandidateK(Integer candidateK) {
        this.candidateK = candidateK;
    }

    public Boolean getRerankEnabled() {
        return rerankEnabled;
    }

    public void setRerankEnabled(Boolean rerankEnabled) {
        this.rerankEnabled = rerankEnabled;
    }

    public Double getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(Double vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

    public Double getKeywordWeight() {
        return keywordWeight;
    }

    public void setKeywordWeight(Double keywordWeight) {
        this.keywordWeight = keywordWeight;
    }

    public Double getLowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(Double lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    public Boolean getRejectLowConfidence() {
        return rejectLowConfidence;
    }

    public void setRejectLowConfidence(Boolean rejectLowConfidence) {
        this.rejectLowConfidence = rejectLowConfidence;
    }
}
