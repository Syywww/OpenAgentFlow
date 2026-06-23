package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 评测报告表。
 * <p>对应数据库表：eval_report。</p>
 */
@TableName("eval_report")
public class EvalReportEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 任务ID。 */
    @TableField("task_id")
    private String taskId;

    /** 报告名称。 */
    @TableField("report_name")
    private String reportName;

    /** 字段说明：SUMMARY。 */
    @TableField("summary")
    private String summary;

    /** 模型COMPARE。 */
    @TableField("model_compare")
    private String modelCompare;

    /** ARTIFACT存储桶。 */
    @TableField("artifact_bucket")
    private String artifactBucket;

    /** ARTIFACT密钥。 */
    @TableField("artifact_key")
    private String artifactKey;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getModelCompare() {
        return modelCompare;
    }

    public void setModelCompare(String modelCompare) {
        this.modelCompare = modelCompare;
    }

    public String getArtifactBucket() {
        return artifactBucket;
    }

    public void setArtifactBucket(String artifactBucket) {
        this.artifactBucket = artifactBucket;
    }

    public String getArtifactKey() {
        return artifactKey;
    }

    public void setArtifactKey(String artifactKey) {
        this.artifactKey = artifactKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
