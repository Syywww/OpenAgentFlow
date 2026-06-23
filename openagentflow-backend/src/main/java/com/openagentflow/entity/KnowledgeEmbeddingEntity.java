package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识向量表。
 * <p>对应数据库表：knowledge_embedding。</p>
 */
@TableName("knowledge_embedding")
public class KnowledgeEmbeddingEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 分片ID。 */
    @TableField("chunk_id")
    private String chunkId;

    /** 字段说明：KBID。 */
    @TableField("kb_id")
    private String kbId;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 向量集合ID。 */
    @TableField("vector_collection_id")
    private String vectorCollectionId;

    /** 向量分区ID。 */
    @TableField("vector_partition_id")
    private String vectorPartitionId;

    /** Milvus集合名称。 */
    @TableField("milvus_collection_name")
    private String milvusCollectionName;

    /** Milvus分区名称。 */
    @TableField("milvus_partition_name")
    private String milvusPartitionName;

    /** 向量主键。 */
    @TableField("vector_primary_key")
    private String vectorPrimaryKey;

    /** 同步状态。 */
    @TableField("sync_status")
    private String syncStatus;

    /** 最后同步时间。 */
    @TableField("last_synced_at")
    private LocalDateTime lastSyncedAt;

    /** 向量JSON。 */
    @TableField("embedding_json")
    private String embeddingJson;

    /** 向量二进制。 */
    @TableField("embedding_blob")
    private byte[] embeddingBlob;

    /** 外部向量ID。 */
    @TableField("external_vector_id")
    private String externalVectorId;

    /** 向量DIM。 */
    @TableField("embedding_dim")
    private Integer embeddingDim;

    /** 内容哈希。 */
    @TableField("content_hash")
    private String contentHash;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
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

    public String getMilvusPartitionName() {
        return milvusPartitionName;
    }

    public void setMilvusPartitionName(String milvusPartitionName) {
        this.milvusPartitionName = milvusPartitionName;
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

    public String getExternalVectorId() {
        return externalVectorId;
    }

    public void setExternalVectorId(String externalVectorId) {
        this.externalVectorId = externalVectorId;
    }

    public Integer getEmbeddingDim() {
        return embeddingDim;
    }

    public void setEmbeddingDim(Integer embeddingDim) {
        this.embeddingDim = embeddingDim;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
