package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 异步任务结构化阶段实体。
 */
@TableName("async_task_stage")
public class AsyncTaskStageEntity {

    /** 阶段主键ID。 */
    @TableId("id")
    private String id;

    /** 所属异步任务ID。 */
    @TableField("task_id")
    private String taskId;

    /** 阶段编码。 */
    @TableField("stage_code")
    private String stageCode;

    /** 阶段名称。 */
    @TableField("stage_name")
    private String stageName;

    /** 阶段顺序。 */
    @TableField("stage_order")
    private Integer stageOrder;

    /** 阶段分片序号。 */
    @TableField("shard_no")
    private Integer shardNo;

    /** 阶段执行尝试序号。 */
    @TableField("attempt_no")
    private Integer attemptNo;

    /** 阶段状态。 */
    @TableField("status")
    private String status;

    /** 执行该阶段的 Worker ID。 */
    @TableField("worker_id")
    private String workerId;

    /** 执行代次。 */
    @TableField("lock_version")
    private Long lockVersion;

    /** 阶段输入JSON。 */
    @TableField("input_json")
    private String inputJson;

    /** 阶段输出JSON。 */
    @TableField("output_json")
    private String outputJson;

    /** 阶段错误摘要。 */
    @TableField("error_message")
    private String errorMessage;

    /** 阶段开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 阶段完成时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public Integer getStageOrder() { return stageOrder; }
    public void setStageOrder(Integer stageOrder) { this.stageOrder = stageOrder; }
    public Integer getShardNo() { return shardNo; }
    public void setShardNo(Integer shardNo) { this.shardNo = shardNo; }
    public Integer getAttemptNo() { return attemptNo; }
    public void setAttemptNo(Integer attemptNo) { this.attemptNo = attemptNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long lockVersion) { this.lockVersion = lockVersion; }
    public String getInputJson() { return inputJson; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }
    public String getOutputJson() { return outputJson; }
    public void setOutputJson(String outputJson) { this.outputJson = outputJson; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
