package com.openagentflow.domain.knowledge;

import java.util.List;

/**
 * 知识库检索结果。
 */
public class KnowledgeRetrievalResult {

    /** 检索日志 ID。 */
    private String retrievalLogId;

    /** 检索结果来源。 */
    private List<KnowledgeSource> sources;

    /** 本次检索耗时毫秒。 */
    private Integer latencyMs;

    public String getRetrievalLogId() {
        return retrievalLogId;
    }

    public void setRetrievalLogId(String retrievalLogId) {
        this.retrievalLogId = retrievalLogId;
    }

    public List<KnowledgeSource> getSources() {
        return sources;
    }

    public void setSources(List<KnowledgeSource> sources) {
        this.sources = sources;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }
}
