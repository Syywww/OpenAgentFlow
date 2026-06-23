package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent协作运行表。
 * <p>对应数据库表：agent_collaboration_run。</p>
 */
@TableName("agent_collaboration_run")
public class AgentCollaborationRunEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 团队ID。 */
    @TableField("team_id")
    private String teamId;

    /** 运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 字段说明：OBJECTIVE。 */
    @TableField("objective")
    private String objective;

    /** SHARED上下文。 */
    @TableField("shared_context")
    private String sharedContext;

    /** FINAL结果。 */
    @TableField("final_result")
    private String finalResult;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 完成时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getSharedContext() {
        return sharedContext;
    }

    public void setSharedContext(String sharedContext) {
        this.sharedContext = sharedContext;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
