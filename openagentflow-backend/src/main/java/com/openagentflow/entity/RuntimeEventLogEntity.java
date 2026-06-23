package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 运行时事件日志表。
 * <p>对应数据库表：runtime_event_log。</p>
 */
@TableName("runtime_event_log")
public class RuntimeEventLogEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 步骤ID。 */
    @TableField("step_id")
    private String stepId;

    /** 事件级别。 */
    @TableField("event_level")
    private String eventLevel;

    /** 事件类型。 */
    @TableField("event_type")
    private String eventType;

    /** 字段说明：MESSAGE。 */
    @TableField("message")
    private String message;

    /** 载荷。 */
    @TableField("payload")
    private String payload;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getEventLevel() {
        return eventLevel;
    }

    public void setEventLevel(String eventLevel) {
        this.eventLevel = eventLevel;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
