package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 模型路由策略表。
 * <p>对应数据库表：model_route_policy。</p>
 */
@TableName("model_route_policy")
public class ModelRoutePolicyEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 策略编码。 */
    @TableField("policy_code")
    private String policyCode;

    /** 策略名称。 */
    @TableField("policy_name")
    private String policyName;

    /** SCENE类型。 */
    @TableField("scene_type")
    private String sceneType;

    /** 字段说明：MATCHRULE。 */
    @TableField("match_rule")
    private String matchRule;

    /** FALLBACK是否启用。 */
    @TableField("fallback_enabled")
    private Boolean fallbackEnabled;

    /** 熔断连续失败次数阈值，空值用默认常量兜底。 */
    @TableField("breaker_failure_threshold")
    private Integer breakerFailureThreshold;

    /** 熔断持续时间（秒），空值用默认常量兜底。 */
    @TableField("breaker_timeout_seconds")
    private Integer breakerTimeoutSeconds;

    /** 路由模式：weighted 按权重分发 / cost_first 按估算成本优选。 */
    @TableField("routing_mode")
    private String routingMode;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

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

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getMatchRule() {
        return matchRule;
    }

    public void setMatchRule(String matchRule) {
        this.matchRule = matchRule;
    }

    public Boolean getFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(Boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public Integer getBreakerFailureThreshold() {
        return breakerFailureThreshold;
    }

    public void setBreakerFailureThreshold(Integer breakerFailureThreshold) {
        this.breakerFailureThreshold = breakerFailureThreshold;
    }

    public Integer getBreakerTimeoutSeconds() {
        return breakerTimeoutSeconds;
    }

    public void setBreakerTimeoutSeconds(Integer breakerTimeoutSeconds) {
        this.breakerTimeoutSeconds = breakerTimeoutSeconds;
    }

    public String getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(String routingMode) {
        this.routingMode = routingMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
