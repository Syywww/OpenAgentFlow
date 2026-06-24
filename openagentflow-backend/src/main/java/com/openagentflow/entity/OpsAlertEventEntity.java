package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运营监控告警事件表。
 * <p>对应数据库表：ops_alert_event。</p>
 */
@TableName("ops_alert_event")
public class OpsAlertEventEntity {

    /** 告警事件主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 告警事件编码。 */
    @TableField("event_code")
    private String eventCode;

    /** 告警规则ID。 */
    @TableField("rule_id")
    private String ruleId;

    /** 告警规则编码。 */
    @TableField("rule_code")
    private String ruleCode;

    /** 告警标题。 */
    @TableField("alert_title")
    private String alertTitle;

    /** 告警级别。 */
    @TableField("severity")
    private String severity;

    /** 监控指标编码。 */
    @TableField("metric_code")
    private String metricCode;

    /** 指标来源模块。 */
    @TableField("metric_source")
    private String metricSource;

    /** 当前指标值。 */
    @TableField("metric_value")
    private BigDecimal metricValue;

    /** 阈值。 */
    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    /** 告警详情。 */
    @TableField("alert_detail")
    private String alertDetail;

    /** 证据JSON。 */
    @TableField("evidence_json")
    private String evidenceJson;

    /** 告警状态。 */
    @TableField("status")
    private String status;

    /** 通知状态。 */
    @TableField("notify_status")
    private String notifyStatus;

    /** 处理人用户ID。 */
    @TableField("handled_by")
    private String handledBy;

    /** 处理时间。 */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /** 处理备注。 */
    @TableField("handle_note")
    private String handleNote;

    /** 首次触发时间。 */
    @TableField("first_triggered_at")
    private LocalDateTime firstTriggeredAt;

    /** 最近触发时间。 */
    @TableField("last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    /** 触发次数。 */
    @TableField("trigger_count")
    private Integer triggerCount;

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

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getAlertTitle() {
        return alertTitle;
    }

    public void setAlertTitle(String alertTitle) {
        this.alertTitle = alertTitle;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getAlertDetail() {
        return alertDetail;
    }

    public void setAlertDetail(String alertDetail) {
        this.alertDetail = alertDetail;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotifyStatus() {
        return notifyStatus;
    }

    public void setNotifyStatus(String notifyStatus) {
        this.notifyStatus = notifyStatus;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public LocalDateTime getFirstTriggeredAt() {
        return firstTriggeredAt;
    }

    public void setFirstTriggeredAt(LocalDateTime firstTriggeredAt) {
        this.firstTriggeredAt = firstTriggeredAt;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
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
