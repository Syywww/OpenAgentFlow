package com.openagentflow.domain.knowledge;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识库治理中心传输对象集合。
 */
public final class KnowledgeGovernanceDtos {

    private KnowledgeGovernanceDtos() {
    }

    /**
     * 知识库治理概览指标。
     */
    public static class Overview {

        /** 知识库总数。 */
        private Long knowledgeBaseCount;

        /** 文档总数。 */
        private Long documentCount;

        /** 已解析文档数。 */
        private Long parsedDocumentCount;

        /** 解析失败文档数。 */
        private Long failedDocumentCount;

        /** 处理中或排队中文档数。 */
        private Long processingDocumentCount;

        /** 分片总数。 */
        private Long chunkCount;

        /** 向量总数。 */
        private Long embeddingCount;

        /** 未同步到Milvus或降级存储的向量数。 */
        private Long milvusFallbackCount;

        /** 打开的治理问题数。 */
        private Long openIssueCount;

        /** 高风险治理问题数。 */
        private Long highRiskIssueCount;

        /** 陈旧文档数。 */
        private Long staleDocumentCount;

        /** 未绑定智能体的知识库数。 */
        private Long unboundKnowledgeBaseCount;

        public Long getKnowledgeBaseCount() {
            return knowledgeBaseCount;
        }

        public void setKnowledgeBaseCount(Long knowledgeBaseCount) {
            this.knowledgeBaseCount = knowledgeBaseCount;
        }

        public Long getDocumentCount() {
            return documentCount;
        }

        public void setDocumentCount(Long documentCount) {
            this.documentCount = documentCount;
        }

        public Long getParsedDocumentCount() {
            return parsedDocumentCount;
        }

        public void setParsedDocumentCount(Long parsedDocumentCount) {
            this.parsedDocumentCount = parsedDocumentCount;
        }

        public Long getFailedDocumentCount() {
            return failedDocumentCount;
        }

        public void setFailedDocumentCount(Long failedDocumentCount) {
            this.failedDocumentCount = failedDocumentCount;
        }

        public Long getProcessingDocumentCount() {
            return processingDocumentCount;
        }

        public void setProcessingDocumentCount(Long processingDocumentCount) {
            this.processingDocumentCount = processingDocumentCount;
        }

        public Long getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(Long chunkCount) {
            this.chunkCount = chunkCount;
        }

        public Long getEmbeddingCount() {
            return embeddingCount;
        }

        public void setEmbeddingCount(Long embeddingCount) {
            this.embeddingCount = embeddingCount;
        }

        public Long getMilvusFallbackCount() {
            return milvusFallbackCount;
        }

        public void setMilvusFallbackCount(Long milvusFallbackCount) {
            this.milvusFallbackCount = milvusFallbackCount;
        }

        public Long getOpenIssueCount() {
            return openIssueCount;
        }

        public void setOpenIssueCount(Long openIssueCount) {
            this.openIssueCount = openIssueCount;
        }

        public Long getHighRiskIssueCount() {
            return highRiskIssueCount;
        }

        public void setHighRiskIssueCount(Long highRiskIssueCount) {
            this.highRiskIssueCount = highRiskIssueCount;
        }

        public Long getStaleDocumentCount() {
            return staleDocumentCount;
        }

        public void setStaleDocumentCount(Long staleDocumentCount) {
            this.staleDocumentCount = staleDocumentCount;
        }

        public Long getUnboundKnowledgeBaseCount() {
            return unboundKnowledgeBaseCount;
        }

        public void setUnboundKnowledgeBaseCount(Long unboundKnowledgeBaseCount) {
            this.unboundKnowledgeBaseCount = unboundKnowledgeBaseCount;
        }
    }

    /**
     * 知识库质量行。
     */
    public static class QualityRow {

        /** 知识库ID。 */
        private String kbId;

        /** 知识库名称。 */
        private String kbName;

        /** 文档数量。 */
        private Long documentCount;

        /** 分片数量。 */
        private Long chunkCount;

        /** 向量数量。 */
        private Long embeddingCount;

        /** 失败文档数量。 */
        private Long failedDocumentCount;

        /** 向量同步异常数量。 */
        private Long fallbackEmbeddingCount;

        /** 绑定智能体数量。 */
        private Long agentBindingCount;

        /** 最后上传时间。 */
        private LocalDateTime lastUploadedAt;

        /** 质量得分，0到100。 */
        private Integer qualityScore;

        /** 风险级别。 */
        private String riskLevel;

        public String getKbId() {
            return kbId;
        }

        public void setKbId(String kbId) {
            this.kbId = kbId;
        }

        public String getKbName() {
            return kbName;
        }

        public void setKbName(String kbName) {
            this.kbName = kbName;
        }

        public Long getDocumentCount() {
            return documentCount;
        }

        public void setDocumentCount(Long documentCount) {
            this.documentCount = documentCount;
        }

        public Long getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(Long chunkCount) {
            this.chunkCount = chunkCount;
        }

        public Long getEmbeddingCount() {
            return embeddingCount;
        }

        public void setEmbeddingCount(Long embeddingCount) {
            this.embeddingCount = embeddingCount;
        }

        public Long getFailedDocumentCount() {
            return failedDocumentCount;
        }

        public void setFailedDocumentCount(Long failedDocumentCount) {
            this.failedDocumentCount = failedDocumentCount;
        }

        public Long getFallbackEmbeddingCount() {
            return fallbackEmbeddingCount;
        }

        public void setFallbackEmbeddingCount(Long fallbackEmbeddingCount) {
            this.fallbackEmbeddingCount = fallbackEmbeddingCount;
        }

        public Long getAgentBindingCount() {
            return agentBindingCount;
        }

        public void setAgentBindingCount(Long agentBindingCount) {
            this.agentBindingCount = agentBindingCount;
        }

        public LocalDateTime getLastUploadedAt() {
            return lastUploadedAt;
        }

        public void setLastUploadedAt(LocalDateTime lastUploadedAt) {
            this.lastUploadedAt = lastUploadedAt;
        }

        public Integer getQualityScore() {
            return qualityScore;
        }

        public void setQualityScore(Integer qualityScore) {
            this.qualityScore = qualityScore;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }
    }

    /**
     * 知识库治理策略摘要。
     */
    public static class PolicySummary extends PolicyRequest {

        /** 策略ID。 */
        private String id;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        /** 更新时间。 */
        private LocalDateTime updatedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

    /**
     * 知识库治理策略保存请求。
     */
    public static class PolicyRequest {

        /** 策略编码。 */
        private String policyCode;

        /** 策略名称。 */
        private String policyName;

        /** 限定知识库ID。 */
        private String kbId;

        /** 陈旧天数阈值。 */
        private Integer staleDays;

        /** 最小分片Token。 */
        private Integer minChunkTokens;

        /** 最大分片Token。 */
        private Integer maxChunkTokens;

        /** 允许的最大失败文档数。 */
        private Integer maxFailedDocuments;

        /** 是否要求智能体绑定。 */
        private Boolean requireAgentBinding;

        /** 是否要求Milvus同步。 */
        private Boolean requireMilvusSync;

        /** 是否自动生成问题。 */
        private Boolean autoIssueEnabled;

        /** 策略状态。 */
        private String status;

        public String getPolicyCode() {
            return policyCode;
        }

        public void setPolicyCode(String policyCode) {
            this.policyCode = policyCode;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public String getKbId() {
            return kbId;
        }

        public void setKbId(String kbId) {
            this.kbId = kbId;
        }

        public Integer getStaleDays() {
            return staleDays;
        }

        public void setStaleDays(Integer staleDays) {
            this.staleDays = staleDays;
        }

        public Integer getMinChunkTokens() {
            return minChunkTokens;
        }

        public void setMinChunkTokens(Integer minChunkTokens) {
            this.minChunkTokens = minChunkTokens;
        }

        public Integer getMaxChunkTokens() {
            return maxChunkTokens;
        }

        public void setMaxChunkTokens(Integer maxChunkTokens) {
            this.maxChunkTokens = maxChunkTokens;
        }

        public Integer getMaxFailedDocuments() {
            return maxFailedDocuments;
        }

        public void setMaxFailedDocuments(Integer maxFailedDocuments) {
            this.maxFailedDocuments = maxFailedDocuments;
        }

        public Boolean getRequireAgentBinding() {
            return requireAgentBinding;
        }

        public void setRequireAgentBinding(Boolean requireAgentBinding) {
            this.requireAgentBinding = requireAgentBinding;
        }

        public Boolean getRequireMilvusSync() {
            return requireMilvusSync;
        }

        public void setRequireMilvusSync(Boolean requireMilvusSync) {
            this.requireMilvusSync = requireMilvusSync;
        }

        public Boolean getAutoIssueEnabled() {
            return autoIssueEnabled;
        }

        public void setAutoIssueEnabled(Boolean autoIssueEnabled) {
            this.autoIssueEnabled = autoIssueEnabled;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 知识库治理问题摘要。
     */
    public static class IssueSummary {

        /** 问题ID。 */
        private String id;

        /** 知识库ID。 */
        private String kbId;

        /** 知识库名称。 */
        private String kbName;

        /** 文档ID。 */
        private String documentId;

        /** 文档名称。 */
        private String documentName;

        /** 分片ID。 */
        private String chunkId;

        /** 问题类型。 */
        private String issueType;

        /** 严重级别。 */
        private String severity;

        /** 问题标题。 */
        private String issueTitle;

        /** 问题详情。 */
        private String issueDetail;

        /** 证据对象。 */
        private Map<String, Object> evidence;

        /** 处理状态。 */
        private String status;

        /** 处理人用户ID。 */
        private String handlerUserId;

        /** 处理时间。 */
        private LocalDateTime handledAt;

        /** 处理备注。 */
        private String handleNote;

        /** 创建时间。 */
        private LocalDateTime createdAt;

        /** 更新时间。 */
        private LocalDateTime updatedAt;

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

        public String getKbName() {
            return kbName;
        }

        public void setKbName(String kbName) {
            this.kbName = kbName;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public String getDocumentName() {
            return documentName;
        }

        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }

        public String getChunkId() {
            return chunkId;
        }

        public void setChunkId(String chunkId) {
            this.chunkId = chunkId;
        }

        public String getIssueType() {
            return issueType;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getIssueTitle() {
            return issueTitle;
        }

        public void setIssueTitle(String issueTitle) {
            this.issueTitle = issueTitle;
        }

        public String getIssueDetail() {
            return issueDetail;
        }

        public void setIssueDetail(String issueDetail) {
            this.issueDetail = issueDetail;
        }

        public Map<String, Object> getEvidence() {
            return evidence;
        }

        public void setEvidence(Map<String, Object> evidence) {
            this.evidence = evidence;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getHandlerUserId() {
            return handlerUserId;
        }

        public void setHandlerUserId(String handlerUserId) {
            this.handlerUserId = handlerUserId;
        }

        public LocalDateTime getHandledAt() {
            return handledAt;
        }

        public void setHandledAt(LocalDateTime handledAt) {
            this.handledAt = handledAt;
        }

        public String getHandleNote() {
            return handleNote;
        }

        public void setHandleNote(String handleNote) {
            this.handleNote = handleNote;
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

    /**
     * 知识库治理问题处理请求。
     */
    public static class IssueHandleRequest {

        /** 目标状态：resolved或ignored。 */
        private String status;

        /** 处理备注。 */
        private String handleNote;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getHandleNote() {
            return handleNote;
        }

        public void setHandleNote(String handleNote) {
            this.handleNote = handleNote;
        }
    }
}
