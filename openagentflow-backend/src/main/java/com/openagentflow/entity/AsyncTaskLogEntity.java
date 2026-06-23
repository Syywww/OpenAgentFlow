package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异步任务日志表。
 * <p>对应数据库表：async_task_log。</p>
 */
@TableName("async_task_log")
public class AsyncTaskLogEntity {

    /** 异步任务日志主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 异步任务ID。 */
    @TableField("task_id")
    private String taskId;

    /** 日志级别。 */
    @TableField("log_level")
    private String logLevel;

    /** 阶段编码。 */
    @TableField("stage")
    private String stage;

    /** 日志消息。 */
    @TableField("message")
    private String message;

    /** 日志详情JSON。 */
    @TableField("detail_json")
    private String detailJson;

    /** 日志对应进度百分比。 */
    @TableField("progress_percent")
    private BigDecimal progressPercent;

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

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

