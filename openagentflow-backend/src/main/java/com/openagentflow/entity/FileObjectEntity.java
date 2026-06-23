package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 文件对象表。
 * <p>对应数据库表：file_object。</p>
 */
@TableName("file_object")
public class FileObjectEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 存储桶名称。 */
    @TableField("bucket_name")
    private String bucketName;

    /** 对象密钥。 */
    @TableField("object_key")
    private String objectKey;

    /** ORIGINAL名称。 */
    @TableField("original_name")
    private String originalName;

    /** 内容类型。 */
    @TableField("content_type")
    private String contentType;

    /** 文件EXT。 */
    @TableField("file_ext")
    private String fileExt;

    /** 文件大小。 */
    @TableField("file_size")
    private Long fileSize;

    /** 文件哈希。 */
    @TableField("file_hash")
    private String fileHash;

    /** 所有者用户ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 资源类型。 */
    @TableField("resource_type")
    private String resourceType;

    /** 资源ID。 */
    @TableField("resource_id")
    private String resourceId;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
