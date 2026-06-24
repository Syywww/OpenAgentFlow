package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 运营监控巡检项表。
 * <p>对应数据库表：ops_health_check。</p>
 */
@TableName("ops_health_check")
public class OpsHealthCheckEntity {

    /** 巡检项主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 巡检项编码。 */
    @TableField("check_code")
    private String checkCode;

    /** 巡检项名称。 */
    @TableField("check_name")
    private String checkName;

    /** 目标类型。 */
    @TableField("target_type")
    private String targetType;

    /** 目标编码。 */
    @TableField("target_code")
    private String targetCode;

    /** 巡检状态。 */
    @TableField("status")
    private String status;

    /** 巡检消息。 */
    @TableField("message")
    private String message;

    /** 最近耗时毫秒。 */
    @TableField("latency_ms")
    private Integer latencyMs;

    /** 最近巡检时间。 */
    @TableField("last_checked_at")
    private LocalDateTime lastCheckedAt;

    /** 下一次巡检时间。 */
    @TableField("next_check_at")
    private LocalDateTime nextCheckAt;

    /** 巡检间隔秒数。 */
    @TableField("check_interval_seconds")
    private Integer checkIntervalSeconds;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 元数据JSON。 */
    @TableField("metadata_json")
    private String metadataJson;

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

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public String getCheckName() {
        return checkName;
    }

    public void setCheckName(String checkName) {
        this.checkName = checkName;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public LocalDateTime getNextCheckAt() {
        return nextCheckAt;
    }

    public void setNextCheckAt(LocalDateTime nextCheckAt) {
        this.nextCheckAt = nextCheckAt;
    }

    public Integer getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
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
