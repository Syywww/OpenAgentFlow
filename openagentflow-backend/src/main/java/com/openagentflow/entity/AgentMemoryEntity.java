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

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 用户ID。 */
    @TableField("user_id")
    private String userId;

    /** 字段说明：SESSIONID。 */
    @TableField("session_id")
    private String sessionId;

    /** 记忆类型。 */
    @TableField("memory_type")
    private String memoryType;

    /** 记忆密钥。 */
    @TableField("memory_key")
    private String memoryKey;

    /** 记忆文本。 */
    @TableField("memory_text")
    private String memoryText;

    /** 记忆值。 */
    @TableField("memory_value")
    private String memoryValue;

    /** 向量JSON。 */
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

    /** 同步状态。 */
    @TableField("sync_status")
    private String syncStatus;

    /** 最后同步时间。 */
    @TableField("last_synced_at")
    private LocalDateTime lastSyncedAt;

    /** 外部向量ID。 */
    @TableField("external_vector_id")
    private String externalVectorId;

    /** IMPORTANCE得分。 */
    @TableField("importance_score")
    private BigDecimal importanceScore;

    /** 过期时间。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

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
