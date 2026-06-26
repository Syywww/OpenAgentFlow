package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识检索日志表。
 * <p>对应数据库表：knowledge_retrieval_log。</p>
 */
@TableName("knowledge_retrieval_log")
public class KnowledgeRetrievalLogEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 字段说明：KBID。 */
    @TableField("kb_id")
    private String kbId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 字段说明：SESSIONID。 */
    @TableField("session_id")
    private String sessionId;

    /** 运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 向量集合ID。 */
    @TableField("vector_collection_id")
    private String vectorCollectionId;

    /** Milvus集合名称。 */
    @TableField("milvus_collection_name")
    private String milvusCollectionName;

    /** 查询文本。 */
    @TableField("query_text")
    private String queryText;

    /** 查询向量JSON。 */
    @TableField("query_embedding_json")
    private String queryEmbeddingJson;

    /** 查询外部向量ID。 */
    @TableField("query_external_vector_id")
    private String queryExternalVectorId;

    /** Milvus搜索参数JSON。 */
    @TableField("milvus_search_params")
    private String milvusSearchParams;

    /** 检索模式：vector、keyword、hybrid。 */
    @TableField("search_mode")
    private String searchMode;

    /** 候选召回数量。 */
    @TableField("candidate_k")
    private Integer candidateK;

    /** 元数据过滤条件JSON。 */
    @TableField("metadata_filter")
    private String metadataFilter;

    /** 字段说明：TopK。 */
    @TableField("top_k")
    private Integer topK;

    /** 得分阈值。 */
    @TableField("score_threshold")
    private BigDecimal scoreThreshold;

    /** RERANK是否启用。 */
    @TableField("rerank_enabled")
    private Boolean rerankEnabled;

    /** 结果数量。 */
    @TableField("result_count")
    private Integer resultCount;

    /** 最佳置信得分。 */
    @TableField("confidence_score")
    private BigDecimal confidenceScore;

    /** 是否低置信。 */
    @TableField("low_confidence")
    private Boolean lowConfidence;

    /** 检索质量建议。 */
    @TableField("quality_advice")
    private String qualityAdvice;

    /** 耗时毫秒。 */
    @TableField("latency_ms")
    private Integer latencyMs;

    /** 结果。 */
    @TableField("results")
    private String results;

    /** Milvus结果ID列表JSON。 */
    @TableField("milvus_result_ids")
    private String milvusResultIds;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getVectorCollectionId() {
        return vectorCollectionId;
    }

    public void setVectorCollectionId(String vectorCollectionId) {
        this.vectorCollectionId = vectorCollectionId;
    }

    public String getMilvusCollectionName() {
        return milvusCollectionName;
    }

    public void setMilvusCollectionName(String milvusCollectionName) {
        this.milvusCollectionName = milvusCollectionName;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getQueryEmbeddingJson() {
        return queryEmbeddingJson;
    }

    public void setQueryEmbeddingJson(String queryEmbeddingJson) {
        this.queryEmbeddingJson = queryEmbeddingJson;
    }

    public String getQueryExternalVectorId() {
        return queryExternalVectorId;
    }

    public void setQueryExternalVectorId(String queryExternalVectorId) {
        this.queryExternalVectorId = queryExternalVectorId;
    }

    public String getMilvusSearchParams() {
        return milvusSearchParams;
    }

    public void setMilvusSearchParams(String milvusSearchParams) {
        this.milvusSearchParams = milvusSearchParams;
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

    public String getMetadataFilter() {
        return metadataFilter;
    }

    public void setMetadataFilter(String metadataFilter) {
        this.metadataFilter = metadataFilter;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public BigDecimal getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(BigDecimal scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    public Boolean getRerankEnabled() {
        return rerankEnabled;
    }

    public void setRerankEnabled(Boolean rerankEnabled) {
        this.rerankEnabled = rerankEnabled;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Boolean getLowConfidence() {
        return lowConfidence;
    }

    public void setLowConfidence(Boolean lowConfidence) {
        this.lowConfidence = lowConfidence;
    }

    public String getQualityAdvice() {
        return qualityAdvice;
    }

    public void setQualityAdvice(String qualityAdvice) {
        this.qualityAdvice = qualityAdvice;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getMilvusResultIds() {
        return milvusResultIds;
    }

    public void setMilvusResultIds(String milvusResultIds) {
        this.milvusResultIds = milvusResultIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
