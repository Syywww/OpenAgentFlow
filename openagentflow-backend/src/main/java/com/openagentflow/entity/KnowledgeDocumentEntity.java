package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识文档表。
 * <p>对应数据库表：knowledge_document。</p>
 */
@TableName("knowledge_document")
public class KnowledgeDocumentEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 字段说明：KBID。 */
    @TableField("kb_id")
    private String kbId;

    /** 父级文档ID。 */
    @TableField("parent_document_id")
    private String parentDocumentId;

    /** DOC名称。 */
    @TableField("doc_name")
    private String docName;

    /** DOC类型。 */
    @TableField("doc_type")
    private String docType;

    /** 文件EXT。 */
    @TableField("file_ext")
    private String fileExt;

    /** 文件大小。 */
    @TableField("file_size")
    private Long fileSize;

    /** 文件哈希。 */
    @TableField("file_hash")
    private String fileHash;

    /** STORAGE存储桶。 */
    @TableField("storage_bucket")
    private String storageBucket;

    /** STORAGE密钥。 */
    @TableField("storage_key")
    private String storageKey;

    /** 来源类型。 */
    @TableField("source_type")
    private String sourceType;

    /** 来源URL。 */
    @TableField("source_url")
    private String sourceUrl;

    /** 解析状态。 */
    @TableField("parse_status")
    private String parseStatus;

    /** 解析错误。 */
    @TableField("parse_error")
    private String parseError;

    /** 元数据JSON。 */
    @TableField("metadata")
    private String metadata;

    /** UPLOADED人。 */
    @TableField("uploaded_by")
    private String uploadedBy;

    /** UPLOADED时间。 */
    @TableField("uploaded_at")
    private LocalDateTime uploadedAt;

    /** 更新时间。 */
    @TableField("updated_at")
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

    public String getParentDocumentId() {
        return parentDocumentId;
    }

    public void setParentDocumentId(String parentDocumentId) {
        this.parentDocumentId = parentDocumentId;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
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

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseError() {
        return parseError;
    }

    public void setParseError(String parseError) {
        this.parseError = parseError;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
