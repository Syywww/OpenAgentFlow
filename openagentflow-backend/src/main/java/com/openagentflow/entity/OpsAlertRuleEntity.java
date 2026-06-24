package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运营监控告警规则表。
 * <p>对应数据库表：ops_alert_rule。</p>
 */
@TableName("ops_alert_rule")
public class OpsAlertRuleEntity {

    /** 告警规则主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 告警规则编码。 */
    @TableField("rule_code")
    private String ruleCode;

    /** 告警规则名称。 */
    @TableField("rule_name")
    private String ruleName;

    /** 监控指标编码。 */
    @TableField("metric_code")
    private String metricCode;

    /** 指标来源模块。 */
    @TableField("metric_source")
    private String metricSource;

    /** 比较操作符。 */
    @TableField("operator")
    private String operator;

    /** 阈值。 */
    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    /** 告警级别。 */
    @TableField("severity")
    private String severity;

    /** 统计窗口分钟数。 */
    @TableField("window_minutes")
    private Integer windowMinutes;

    /** 冷却分钟数。 */
    @TableField("cooldown_minutes")
    private Integer cooldownMinutes;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 通知渠道编码，多个渠道使用英文逗号分隔。 */
    @TableField("notify_channels")
    private String notifyChannels;

    /** 规则说明。 */
    @TableField("description")
    private String description;

    /** 创建人用户ID。 */
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

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public String getMetricSource() {
        return metricSource;
    }

    public void setMetricSource(String metricSource) {
        this.metricSource = metricSource;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Integer getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(Integer windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public Integer getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(Integer cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getNotifyChannels() {
        return notifyChannels;
    }

    public void setNotifyChannels(String notifyChannels) {
        this.notifyChannels = notifyChannels;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
