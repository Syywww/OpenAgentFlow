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
}
