package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 向量分区表。
 * <p>对应数据库表：vector_partition。</p>
 */
@TableName("vector_partition")
public class VectorPartitionEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 集合ID。 */
    @TableField("collection_id")
    private String collectionId;

    /** 分区名称。 */
    @TableField("partition_name")
    private String partitionName;

    /** 分区密钥。 */
    @TableField("partition_key")
    private String partitionKey;

    /** 业务类型。 */
    @TableField("business_type")
    private String businessType;

    /** 所有者资源ID。 */
    @TableField("owner_resource_id")
    private String ownerResourceId;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** ROW数量。 */
    @TableField("row_count")
    private Long rowCount;

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

    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getOwnerResourceId() {
        return ownerResourceId;
    }

    public void setOwnerResourceId(String ownerResourceId) {
        this.ownerResourceId = ownerResourceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
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
