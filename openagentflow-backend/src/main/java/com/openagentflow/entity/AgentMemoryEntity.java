package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Agent记忆表。
 * <p>对应数据库表：agent_memory。</p>
 */
@TableName("agent_memory")
public class AgentMemoryEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 归属 Agent ID，为空时表示用户级通用记忆。 */
    @TableField("agent_id")
    private String agentId;

    /** 归属用户 ID，用于隔离个人记忆。 */
    @TableField("user_id")
    private String userId;

    /** 归属会话 ID，短期会话记忆通常绑定该字段。 */
    @TableField("session_id")
    private String sessionId;

    /** 记忆类型：short_term、long_term、task、vector。 */
    @TableField("memory_type")
    private String memoryType;

    /** 记忆密钥，用于业务侧去重或定位来源。 */
    @TableField("memory_key")
    private String memoryKey;

    /** 可直接注入 Prompt 或用于召回匹配的记忆文本。 */
    @TableField("memory_text")
    private String memoryText;

    /** 结构化记忆值 JSON，保存来源、摘要、扩展参数等信息。 */
    @TableField("memory_value")
    private String memoryValue;

    /** 向量 JSON，Milvus 不可用时作为 MySQL 兜底召回依据。 */
    @TableField("embedding_json")
    private String embeddingJson;

    /** 向量二进制。 */
    @TableField("embedding_blob")
    private byte[] embeddingBlob;

    /** 向量集合ID。 */
    @TableField("vector_collection_id")
    private String vectorCollectionId;

    /** 向量分区ID。 */
    @TableField("vector_partition_id")
    private String vectorPartitionId;

    /** Milvus集合名称。 */
    @TableField("milvus_collection_name")
    private String milvusCollectionName;

    /** 向量主键。 */
    @TableField("vector_primary_key")
    private String vectorPrimaryKey;

    /** 同步状态：pending、synced、failed、skipped。 */
    @TableField("sync_status")
    private String syncStatus;

    /** 最后同步时间。 */
    @TableField("last_synced_at")
    private LocalDateTime lastSyncedAt;

    /** 外部向量 ID，兼容第三方向量库主键。 */
    @TableField("external_vector_id")
    private String externalVectorId;

    /** 重要度得分，范围 0 到 1，用于召回排序。 */
    @TableField("importance_score")
    private BigDecimal importanceScore;

    /** 过期时间，到期后默认不再参与召回。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /** 记忆状态：active、archived、deleted。 */
    @TableField("status")
    private String status;

    /** 可见范围：private、agent、workspace。 */
    @TableField("privacy_scope")
    private String privacyScope;

    /** 来源运行 ID，用于追溯记忆来自哪次 Agent 执行。 */
    @TableField("source_run_id")
    private String sourceRunId;

    /** 来源消息 ID，用于追溯记忆来自哪条会话消息。 */
    @TableField("source_message_id")
    private String sourceMessageId;

    /** 标签 JSON，用于分类、筛选和治理。 */
    @TableField("tags_json")
    private String tagsJson;

    /** 命中次数，用于判断记忆是否常被召回。 */
    @TableField("hit_count")
    private Integer hitCount;

    /** 最后命中时间，用于记忆治理和清理。 */
    @TableField("last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public String getMemoryKey() {
        return memoryKey;
    }

    public void setMemoryKey(String memoryKey) {
        this.memoryKey = memoryKey;
    }

    public String getMemoryText() {
        return memoryText;
    }

    public void setMemoryText(String memoryText) {
        this.memoryText = memoryText;
    }

    public String getMemoryValue() {
        return memoryValue;
    }

    public void setMemoryValue(String memoryValue) {
        this.memoryValue = memoryValue;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }

    public byte[] getEmbeddingBlob() {
        return embeddingBlob;
    }

    public void setEmbeddingBlob(byte[] embeddingBlob) {
        this.embeddingBlob = embeddingBlob;
    }

    public String getVectorCollectionId() {
        return vectorCollectionId;
    }

    public void setVectorCollectionId(String vectorCollectionId) {
        this.vectorCollectionId = vectorCollectionId;
    }

    public String getVectorPartitionId() {
        return vectorPartitionId;
    }

    public void setVectorPartitionId(String vectorPartitionId) {
        this.vectorPartitionId = vectorPartitionId;
    }

    public String getMilvusCollectionName() {
        return milvusCollectionName;
    }

    public void setMilvusCollectionName(String milvusCollectionName) {
        this.milvusCollectionName = milvusCollectionName;
    }

    public String getVectorPrimaryKey() {
        return vectorPrimaryKey;
    }

    public void setVectorPrimaryKey(String vectorPrimaryKey) {
        this.vectorPrimaryKey = vectorPrimaryKey;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getExternalVectorId() {
        return externalVectorId;
    }

    public void setExternalVectorId(String externalVectorId) {
        this.externalVectorId = externalVectorId;
    }

    public BigDecimal getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(BigDecimal importanceScore) {
        this.importanceScore = importanceScore;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrivacyScope() {
        return privacyScope;
    }

    public void setPrivacyScope(String privacyScope) {
        this.privacyScope = privacyScope;
    }

    public String getSourceRunId() {
        return sourceRunId;
    }

    public void setSourceRunId(String sourceRunId) {
        this.sourceRunId = sourceRunId;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public void setSourceMessageId(String sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
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
