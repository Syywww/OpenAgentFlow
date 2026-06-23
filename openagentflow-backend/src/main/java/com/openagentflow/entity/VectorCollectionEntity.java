package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 向量集合表。
 * <p>对应数据库表：vector_collection。</p>
 */
@TableName("vector_collection")
public class VectorCollectionEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 连接ID。 */
    @TableField("connection_id")
    private String connectionId;

    /** 集合名称。 */
    @TableField("collection_name")
    private String collectionName;

    /** 集合别名。 */
    @TableField("collection_alias")
    private String collectionAlias;

    /** 业务类型。 */
    @TableField("business_type")
    private String businessType;

    /** 所有者资源类型。 */
    @TableField("owner_resource_type")
    private String ownerResourceType;

    /** 所有者资源ID。 */
    @TableField("owner_resource_id")
    private String ownerResourceId;

    /** 向量模型ID。 */
    @TableField("embedding_model_id")
    private String embeddingModelId;

    /** 维度。 */
    @TableField("dimension")
    private Integer dimension;

    /** 距离度量类型。 */
    @TableField("metric_type")
    private String metricType;

    /** 索引类型。 */
    @TableField("index_type")
    private String indexType;

    /** 索引参数。 */
    @TableField("index_params")
    private String indexParams;

    /** 字段说明：Schema JSON。 */
    @TableField("schema_json")
    private String schemaJson;

    /** 分片NUM。 */
    @TableField("shard_num")
    private Integer shardNum;

    /** 副本编号。 */
    @TableField("replica_number")
    private Integer replicaNumber;

    /** 一致性级别。 */
    @TableField("consistency_level")
    private String consistencyLevel;

    /** AUTOCREATE分区。 */
    @TableField("auto_create_partition")
    private Boolean autoCreatePartition;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 已加载。 */
    @TableField("loaded")
    private Boolean loaded;

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

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionAlias() {
        return collectionAlias;
    }

    public void setCollectionAlias(String collectionAlias) {
        this.collectionAlias = collectionAlias;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getOwnerResourceType() {
        return ownerResourceType;
    }

    public void setOwnerResourceType(String ownerResourceType) {
        this.ownerResourceType = ownerResourceType;
    }

    public String getOwnerResourceId() {
        return ownerResourceId;
    }

    public void setOwnerResourceId(String ownerResourceId) {
        this.ownerResourceId = ownerResourceId;
    }

    public String getEmbeddingModelId() {
        return embeddingModelId;
    }

    public void setEmbeddingModelId(String embeddingModelId) {
        this.embeddingModelId = embeddingModelId;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getIndexParams() {
        return indexParams;
    }

    public void setIndexParams(String indexParams) {
        this.indexParams = indexParams;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public Integer getShardNum() {
        return shardNum;
    }

    public void setShardNum(Integer shardNum) {
        this.shardNum = shardNum;
    }

    public Integer getReplicaNumber() {
        return replicaNumber;
    }

    public void setReplicaNumber(Integer replicaNumber) {
        this.replicaNumber = replicaNumber;
    }

    public String getConsistencyLevel() {
        return consistencyLevel;
    }

    public void setConsistencyLevel(String consistencyLevel) {
        this.consistencyLevel = consistencyLevel;
    }

    public Boolean getAutoCreatePartition() {
        return autoCreatePartition;
    }

    public void setAutoCreatePartition(Boolean autoCreatePartition) {
        this.autoCreatePartition = autoCreatePartition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getLoaded() {
        return loaded;
    }

    public void setLoaded(Boolean loaded) {
        this.loaded = loaded;
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
