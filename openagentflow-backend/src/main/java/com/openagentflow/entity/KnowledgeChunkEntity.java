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

    /** 父分片ID。 */
    @TableField("parent_chunk_id")
    private String parentChunkId;

    /** 分片层级，parent/child。 */
    @TableField("chunk_level")
    private String chunkLevel;

    /** 标题。 */
    @TableField("title")
    private String title;

    /** 章节标题。 */
    @TableField("section_title")
    private String sectionTitle;

    /** 章节路径。 */
    @TableField("section_path")
    private String sectionPath;

    /** 段落序号。 */
    @TableField("paragraph_no")
    private Integer paragraphNo;

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

    /** 切片策略版本。 */
    @TableField("strategy_version")
    private String strategyVersion;

    /** 分片内容哈希。 */
    @TableField("content_hash")
    private String contentHash;

    /** 来源文档哈希。 */
    @TableField("source_hash")
    private String sourceHash;

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

    public String getParentChunkId() {
        return parentChunkId;
    }

    public void setParentChunkId(String parentChunkId) {
        this.parentChunkId = parentChunkId;
    }

    public String getChunkLevel() {
        return chunkLevel;
    }

    public void setChunkLevel(String chunkLevel) {
        this.chunkLevel = chunkLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public String getSectionPath() {
        return sectionPath;
    }

    public void setSectionPath(String sectionPath) {
        this.sectionPath = sectionPath;
    }

    public Integer getParagraphNo() {
        return paragraphNo;
    }

    public void setParagraphNo(Integer paragraphNo) {
        this.paragraphNo = paragraphNo;
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

    public String getStrategyVersion() {
        return strategyVersion;
    }

    public void setStrategyVersion(String strategyVersion) {
        this.strategyVersion = strategyVersion;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
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
