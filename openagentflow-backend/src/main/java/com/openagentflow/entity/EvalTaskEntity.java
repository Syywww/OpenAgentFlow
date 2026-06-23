package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 评测任务表。
 * <p>对应数据库表：eval_task。</p>
 */
@TableName("eval_task")
public class EvalTaskEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 任务编码。 */
    @TableField("task_code")
    private String taskCode;

    /** 任务名称。 */
    @TableField("task_name")
    private String taskName;

    /** 数据集ID。 */
    @TableField("dataset_id")
    private String datasetId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** BASELINE模型ID。 */
    @TableField("baseline_model_id")
    private String baselineModelId;

    /** COMPARE模型IDS。 */
    @TableField("compare_model_ids")
    private String compareModelIds;

    /** 提示词模板ID。 */
    @TableField("prompt_template_id")
    private String promptTemplateId;

    /** 评测配置。 */
    @TableField("eval_config")
    private String evalConfig;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 总SAMPLES。 */
    @TableField("total_samples")
    private Integer totalSamples;

    /** 完成SAMPLES。 */
    @TableField("finished_samples")
    private Integer finishedSamples;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 完成时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

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

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getBaselineModelId() {
        return baselineModelId;
    }

    public void setBaselineModelId(String baselineModelId) {
        this.baselineModelId = baselineModelId;
    }

    public String getCompareModelIds() {
        return compareModelIds;
    }

    public void setCompareModelIds(String compareModelIds) {
        this.compareModelIds = compareModelIds;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public void setPromptTemplateId(String promptTemplateId) {
        this.promptTemplateId = promptTemplateId;
    }

    public String getEvalConfig() {
        return evalConfig;
    }

    public void setEvalConfig(String evalConfig) {
        this.evalConfig = evalConfig;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalSamples() {
        return totalSamples;
    }

    public void setTotalSamples(Integer totalSamples) {
        this.totalSamples = totalSamples;
    }

    public Integer getFinishedSamples() {
        return finishedSamples;
    }

    public void setFinishedSamples(Integer finishedSamples) {
        this.finishedSamples = finishedSamples;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
