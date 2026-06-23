package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识来源引用表。
 * <p>对应数据库表：knowledge_source_citation。</p>
 */
@TableName("knowledge_source_citation")
public class KnowledgeSourceCitationEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 字段说明：MESSAGEID。 */
    @TableField("message_id")
    private String messageId;

    /** 检索日志ID。 */
    @TableField("retrieval_log_id")
    private String retrievalLogId;

    /** 分片ID。 */
    @TableField("chunk_id")
    private String chunkId;

    /** 文档ID。 */
    @TableField("document_id")
    private String documentId;

    /** QUOTE文本。 */
    @TableField("quote_text")
    private String quoteText;

    /** 得分。 */
    @TableField("score")
    private BigDecimal score;

    /** PAGE序号。 */
    @TableField("page_no")
    private Integer pageNo;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRetrievalLogId() {
        return retrievalLogId;
    }

    public void setRetrievalLogId(String retrievalLogId) {
        this.retrievalLogId = retrievalLogId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public void setQuoteText(String quoteText) {
        this.quoteText = quoteText;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
