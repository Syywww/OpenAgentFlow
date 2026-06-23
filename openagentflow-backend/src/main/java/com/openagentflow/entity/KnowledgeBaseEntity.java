package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识库表。
 * <p>对应数据库表：knowledge_base。</p>
 */
@TableName("knowledge_base")
public class KnowledgeBaseEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** KB编码。 */
    @TableField("kb_code")
    private String kbCode;

    /** KB名称。 */
    @TableField("kb_name")
    private String kbName;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** 所属工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 向量模型ID。 */
    @TableField("embedding_model_id")
    private String embeddingModelId;

    /** RERANK模型ID。 */
    @TableField("rerank_model_id")
    private String rerankModelId;

    /** 向量存储连接ID。 */
    @TableField("vector_connection_id")
    private String vectorConnectionId;

    /** 向量集合ID。 */
    @TableField("vector_collection_id")
    private String vectorCollectionId;

    /** Milvus集合名称。 */
    @TableField("milvus_collection_name")
    private String milvusCollectionName;

    /** Milvus分区名称。 */
    @TableField("milvus_partition_name")
    private String milvusPartitionName;

    /** 分片STRATEGY。 */
    @TableField("chunk_strategy")
    private String chunkStrategy;

    /** 分片大小。 */
    @TableField("chunk_size")
    private Integer chunkSize;

    /** 分片OVERLAP。 */
    @TableField("chunk_overlap")
    private Integer chunkOverlap;

    /** 字段说明：VISIBILITY。 */
    @TableField("visibility")
    private String visibility;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 所有者用户ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 删除时间。 */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /** 版本。 */
    @TableField("version")
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbCode() {
        return kbCode;
    }

    public void setKbCode(String kbCode) {
        this.kbCode = kbCode;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getEmbeddingModelId() {
        return embeddingModelId;
    }

    public void setEmbeddingModelId(String embeddingModelId) {
        this.embeddingModelId = embeddingModelId;
    }

    public String getRerankModelId() {
        return rerankModelId;
    }

    public void setRerankModelId(String rerankModelId) {
        this.rerankModelId = rerankModelId;
    }

    public String getVectorConnectionId() {
        return vectorConnectionId;
    }

    public void setVectorConnectionId(String vectorConnectionId) {
        this.vectorConnectionId = vectorConnectionId;
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

    public String getMilvusPartitionName() {
        return milvusPartitionName;
    }

    public void setMilvusPartitionName(String milvusPartitionName) {
        this.milvusPartitionName = milvusPartitionName;
    }

    public String getChunkStrategy() {
        return chunkStrategy;
    }

    public void setChunkStrategy(String chunkStrategy) {
        this.chunkStrategy = chunkStrategy;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
