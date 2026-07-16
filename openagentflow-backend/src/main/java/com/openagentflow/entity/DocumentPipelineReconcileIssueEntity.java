package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 文档分布式流水线自动对账问题表实体。 */
@TableName("document_pipeline_reconcile_issue")
public class DocumentPipelineReconcileIssueEntity {

    /** 对账问题主键ID。 */
    @TableId("id")
    private String id;

    /** 知识文档ID。 */
    @TableField("document_id")
    private String documentId;

    /** 知识库ID。 */
    @TableField("kb_id")
    private String kbId;

    /** 关联文档DAG根任务ID。 */
    @TableField("root_task_id")
    private String rootTaskId;

    /** 问题所属流水线代次。 */
    @TableField("pipeline_generation")
    private Long pipelineGeneration;

    /** 问题类型。 */
    @TableField("issue_type")
    private String issueType;

    /** 严重级别。 */
    @TableField("severity")
    private String severity;

    /** 预期条目数。 */
    @TableField("expected_count")
    private Long expectedCount;

    /** 实际条目数。 */
    @TableField("actual_count")
    private Long actualCount;

    /** 对账问题详情JSON。 */
    @TableField("detail_json")
    private String detailJson;

    /** 处理状态。 */
    @TableField("status")
    private String status;

    /** 首次发现时间。 */
    @TableField("first_detected_at")
    private LocalDateTime firstDetectedAt;

    /** 最近发现时间。 */
    @TableField("last_detected_at")
    private LocalDateTime lastDetectedAt;

    /** 解决时间。 */
    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getRootTaskId() { return rootTaskId; }
    public void setRootTaskId(String rootTaskId) { this.rootTaskId = rootTaskId; }
    public Long getPipelineGeneration() { return pipelineGeneration; }
    public void setPipelineGeneration(Long pipelineGeneration) { this.pipelineGeneration = pipelineGeneration; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Long getExpectedCount() { return expectedCount; }
    public void setExpectedCount(Long expectedCount) { this.expectedCount = expectedCount; }
    public Long getActualCount() { return actualCount; }
    public void setActualCount(Long actualCount) { this.actualCount = actualCount; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getFirstDetectedAt() { return firstDetectedAt; }
    public void setFirstDetectedAt(LocalDateTime firstDetectedAt) { this.firstDetectedAt = firstDetectedAt; }
    public LocalDateTime getLastDetectedAt() { return lastDetectedAt; }
    public void setLastDetectedAt(LocalDateTime lastDetectedAt) { this.lastDetectedAt = lastDetectedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
