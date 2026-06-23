package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型USAGE配额表。
 * <p>对应数据库表：model_usage_quota。</p>
 */
@TableName("model_usage_quota")
public class ModelUsageQuotaEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** SUBJECT类型。 */
    @TableField("subject_type")
    private String subjectType;

    /** 字段说明：SUBJECTID。 */
    @TableField("subject_id")
    private String subjectId;

    /** 服务商ID。 */
    @TableField("provider_id")
    private String providerId;

    /** 模型ID。 */
    @TableField("model_id")
    private String modelId;

    /** 配额PERIOD。 */
    @TableField("quota_period")
    private String quotaPeriod;

    /** 令牌LIMIT。 */
    @TableField("token_limit")
    private Long tokenLimit;

    /** 成本LIMIT。 */
    @TableField("cost_limit")
    private BigDecimal costLimit;

    /** 令牌USED。 */
    @TableField("token_used")
    private Long tokenUsed;

    /** 成本USED。 */
    @TableField("cost_used")
    private BigDecimal costUsed;

    /** RESET时间。 */
    @TableField("reset_at")
    private LocalDateTime resetAt;

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

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
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

    public String getQuotaPeriod() {
        return quotaPeriod;
    }

    public void setQuotaPeriod(String quotaPeriod) {
        this.quotaPeriod = quotaPeriod;
    }

    public Long getTokenLimit() {
        return tokenLimit;
    }

    public void setTokenLimit(Long tokenLimit) {
        this.tokenLimit = tokenLimit;
    }

    public BigDecimal getCostLimit() {
        return costLimit;
    }

    public void setCostLimit(BigDecimal costLimit) {
        this.costLimit = costLimit;
    }

    public Long getTokenUsed() {
        return tokenUsed;
    }

    public void setTokenUsed(Long tokenUsed) {
        this.tokenUsed = tokenUsed;
    }

    public BigDecimal getCostUsed() {
        return costUsed;
    }

    public void setCostUsed(BigDecimal costUsed) {
        this.costUsed = costUsed;
    }

    public LocalDateTime getResetAt() {
        return resetAt;
    }

    public void setResetAt(LocalDateTime resetAt) {
        this.resetAt = resetAt;
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
