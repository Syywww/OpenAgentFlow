package com.openagentflow.domain.knowledge;

import java.time.LocalDateTime;

/**
 * 知识文档 MinIO 直传凭据。
 */
public class KnowledgeDirectUploadTicket {

    /** 预创建文档ID。 */
    private String documentId;

    /** 存储桶名称。 */
    private String bucket;

    /** 对象键。 */
    private String objectKey;

    /** 预签名 PUT 地址。 */
    private String uploadUrl;

    /** 预签名地址过期时间。 */
    private LocalDateTime expiresAt;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
