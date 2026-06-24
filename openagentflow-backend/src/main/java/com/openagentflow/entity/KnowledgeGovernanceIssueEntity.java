package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识库治理问题实体。
 * <p>对应数据库表：knowledge_governance_issue。</p>
 */
@TableName("knowledge_governance_issue")
public class KnowledgeGovernanceIssueEntity {

    /** 主键ID。 */
    @TableId("id")
    private String id;

    /** 知识库ID。 */
    @TableField("kb_id")
    private String kbId;

    /** 关联文档ID。 */
    @TableField("document_id")
    private String documentId;

    /** 关联分片ID。 */
    @TableField("chunk_id")
    private String chunkId;

    /** 问题类型。 */
    @TableField("issue_type")
    private String issueType;

    /** 严重级别。 */
    @TableField("severity")
    private String severity;

    /** 问题标题。 */
    @TableField("issue_title")
    private String issueTitle;

    /** 问题详情。 */
    @TableField("issue_detail")
    private String issueDetail;

    /** 问题证据JSON。 */
    @TableField("evidence_json")
    private String evidenceJson;

    /** 处理状态。 */
    @TableField("status")
    private String status;

    /** 处理人用户ID。 */
    @TableField("handler_user_id")
    private String handlerUserId;

    /** 处理时间。 */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /** 处理备注。 */
    @TableField("handle_note")
    private String handleNote;

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

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getIssueTitle() {
        return issueTitle;
    }

    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }

    public String getIssueDetail() {
        return issueDetail;
    }

    public void setIssueDetail(String issueDetail) {
        this.issueDetail = issueDetail;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandlerUserId() {
        return handlerUserId;
    }

    public void setHandlerUserId(String handlerUserId) {
        this.handlerUserId = handlerUserId;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
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
