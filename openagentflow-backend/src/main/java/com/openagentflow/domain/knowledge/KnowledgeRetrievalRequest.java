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
}
