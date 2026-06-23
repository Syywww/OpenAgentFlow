package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 工作流人工任务表。
 * <p>对应数据库表：workflow_human_task。</p>
 */
@TableName("workflow_human_task")
public class WorkflowHumanTaskEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作流运行ID。 */
    @TableField("workflow_run_id")
    private String workflowRunId;

    /** 步骤运行ID。 */
    @TableField("step_run_id")
    private String stepRunId;

    /** 任务名称。 */
    @TableField("task_name")
    private String taskName;

    /** ASSIGNEE用户ID。 */
    @TableField("assignee_user_id")
    private String assigneeUserId;

    /** 载荷。 */
    @TableField("payload")
    private String payload;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 字段说明：DECISION。 */
    @TableField("decision")
    private String decision;

    /** 字段说明：COMMENT。 */
    @TableField("comment")
    private String comment;

    /** COMPLETED时间。 */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /** 过期时间。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowRunId() {
        return workflowRunId;
    }

    public void setWorkflowRunId(String workflowRunId) {
        this.workflowRunId = workflowRunId;
    }

    public String getStepRunId() {
        return stepRunId;
    }

    public void setStepRunId(String stepRunId) {
        this.stepRunId = stepRunId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(String assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
