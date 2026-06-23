package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型路由候选表。
 * <p>对应数据库表：model_route_candidate。</p>
 */
@TableName("model_route_candidate")
public class ModelRouteCandidateEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 策略ID。 */
    @TableField("policy_id")
    private String policyId;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 字段说明：PRIORITY。 */
    @TableField("priority")
    private Integer priority;

    /** 字段说明：WEIGHT。 */
    @TableField("weight")
    private BigDecimal weight;

    /** MAX耗时毫秒。 */
    @TableField("max_latency_ms")
    private Integer maxLatencyMs;

    /** MAX成本PER1K。 */
    @TableField("max_cost_per_1k")
    private BigDecimal maxCostPer1k;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public Integer getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public void setMaxLatencyMs(Integer maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
    }

    public BigDecimal getMaxCostPer1k() {
        return maxCostPer1k;
    }

    public void setMaxCostPer1k(BigDecimal maxCostPer1k) {
        this.maxCostPer1k = maxCostPer1k;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
