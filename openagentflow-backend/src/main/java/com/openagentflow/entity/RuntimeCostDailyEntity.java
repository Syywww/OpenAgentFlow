package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运行时成本每日表。
 * <p>对应数据库表：runtime_cost_daily。</p>
 */
@TableName("runtime_cost_daily")
public class RuntimeCostDailyEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 字段说明：STATDATE。 */
    @TableField("stat_date")
    private LocalDate statDate;

    /** 服务商ID。 */
    @TableField("provider_id")
    private String providerId;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 运行数量。 */
    @TableField("run_count")
    private Long runCount;

    /** 成功数量。 */
    @TableField("success_count")
    private Long successCount;

    /** 失败数量。 */
    @TableField("failure_count")
    private Long failureCount;

    /** 总Token数。 */
    @TableField("total_tokens")
    private Long totalTokens;

    /** 总成本。 */
    @TableField("total_cost")
    private BigDecimal totalCost;

    /** AVG耗时毫秒。 */
    @TableField("avg_latency_ms")
    private BigDecimal avgLatencyMs;

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

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
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

    public Long getRunCount() {
        return runCount;
    }

    public void setRunCount(Long runCount) {
        this.runCount = runCount;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(BigDecimal avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
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
