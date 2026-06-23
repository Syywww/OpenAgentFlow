package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 向量记录映射表。
 * <p>对应数据库表：vector_record_mapping。</p>
 */
@TableName("vector_record_mapping")
public class VectorRecordMappingEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 集合ID。 */
    @TableField("collection_id")
    private String collectionId;

    /** 分区ID。 */
    @TableField("partition_id")
    private String partitionId;

    /** 向量主键。 */
    @TableField("vector_primary_key")
    private String vectorPrimaryKey;

    /** 资源类型。 */
    @TableField("resource_type")
    private String resourceType;

    /** 资源ID。 */
    @TableField("resource_id")
    private String resourceId;

    /** 向量模型ID。 */
    @TableField("embedding_model_id")
    private String embeddingModelId;

    /** 内容哈希。 */
    @TableField("content_hash")
    private String contentHash;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 最后同步时间。 */
    @TableField("last_synced_at")
    private LocalDateTime lastSyncedAt;

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

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public String getVectorPrimaryKey() {
        return vectorPrimaryKey;
    }

    public void setVectorPrimaryKey(String vectorPrimaryKey) {
        this.vectorPrimaryKey = vectorPrimaryKey;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getEmbeddingModelId() {
        return embeddingModelId;
    }

    public void setEmbeddingModelId(String embeddingModelId) {
        this.embeddingModelId = embeddingModelId;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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
