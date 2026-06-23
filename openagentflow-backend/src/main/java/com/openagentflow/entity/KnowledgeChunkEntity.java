package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识分片表。
 * <p>对应数据库表：knowledge_chunk。</p>
 */
@TableName("knowledge_chunk")
public class KnowledgeChunkEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 字段说明：KBID。 */
    @TableField("kb_id")
    private String kbId;

    /** 文档ID。 */
    @TableField("document_id")
    private String documentId;

    /** 分片序号。 */
    @TableField("chunk_no")
    private Integer chunkNo;

    /** 标题。 */
    @TableField("title")
    private String title;

    /** 内容。 */
    @TableField("content")
    private String content;

    /** Token数量。 */
    @TableField("token_count")
    private Integer tokenCount;

    /** PAGE序号。 */
    @TableField("page_no")
    private Integer pageNo;

    /** 开始OFFSET。 */
    @TableField("start_offset")
    private Integer startOffset;

    /** 字段说明：ENDOFFSET。 */
    @TableField("end_offset")
    private Integer endOffset;

    /** 元数据JSON。 */
    @TableField("metadata")
    private String metadata;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

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

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Integer startOffset) {
        this.startOffset = startOffset;
    }

    public Integer getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Integer endOffset) {
        this.endOffset = endOffset;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
