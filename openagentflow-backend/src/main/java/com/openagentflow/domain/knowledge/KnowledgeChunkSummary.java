package com.openagentflow.domain.knowledge;

import java.time.LocalDateTime;

/**
 * 知识分片摘要对象。
 */
public class KnowledgeChunkSummary {

    /** 分片主键 ID。 */
    private String id;

    /** 文档 ID。 */
    private String documentId;

    /** 分片序号。 */
    private Integer chunkNo;

    /** 分片标题。 */
    private String title;

    /** 分片内容。 */
    private String content;

    /** Token 估算数量。 */
    private Integer tokenCount;

    /** 分片状态。 */
    private String status;

    /** 向量同步状态。 */
    private String syncStatus;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Integer getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(Integer chunkNo) {
        this.chunkNo = chunkNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
