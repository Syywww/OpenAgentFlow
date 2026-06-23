package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识文档解析任务表。
 * <p>对应数据库表：knowledge_document_parse_task。</p>
 */
@TableName("knowledge_document_parse_task")
public class KnowledgeDocumentParseTaskEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 文档ID。 */
    @TableField("document_id")
    private String documentId;

    /** 任务类型。 */
    @TableField("task_type")
    private String taskType;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 字段说明：PROGRESS。 */
    @TableField("progress")
    private BigDecimal progress;

    /** 配置。 */
    @TableField("config")
    private String config;

    /** 结果。 */
    @TableField("result")
    private String result;

    /** 错误信息。 */
    @TableField("error_message")
    private String errorMessage;

    /** 开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 完成时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /** 创建时间。 */
    @TableField("created_at")
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

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
