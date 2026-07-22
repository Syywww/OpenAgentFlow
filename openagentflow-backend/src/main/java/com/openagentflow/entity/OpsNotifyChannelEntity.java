package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 运营监控通知渠道表。
 * <p>对应数据库表：ops_notify_channel。</p>
 */
@TableName("ops_notify_channel")
public class OpsNotifyChannelEntity {

    /** 通知渠道主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 通知渠道编码。 */
    @TableField("channel_code")
    private String channelCode;

    /** 通知渠道名称。 */
    @TableField("channel_name")
    private String channelName;

    /** 通知渠道类型。 */
    @TableField("channel_type")
    private String channelType;

    /** 渠道配置JSON。 */
    @TableField("config_json")
    private String configJson;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 最近测试状态。 */
    @TableField("last_test_status")
    private String lastTestStatus;

    /** 最近测试消息。 */
    @TableField("last_test_message")
    private String lastTestMessage;

    /** 最近测试时间。 */
    @TableField("last_test_at")
    private LocalDateTime lastTestAt;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 最近成功投递时间。 */
    @TableField("last_success_at")
    private LocalDateTime lastSuccessAt;

    /** 连续失败次数。 */
    @TableField("failure_count")
    private Integer failureCount;

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

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getLastTestStatus() {
        return lastTestStatus;
    }

    public void setLastTestStatus(String lastTestStatus) {
        this.lastTestStatus = lastTestStatus;
    }

    public String getLastTestMessage() {
        return lastTestMessage;
    }

    public void setLastTestMessage(String lastTestMessage) {
        this.lastTestMessage = lastTestMessage;
    }

    public LocalDateTime getLastTestAt() {
        return lastTestAt;
    }

    public void setLastTestAt(LocalDateTime lastTestAt) {
        this.lastTestAt = lastTestAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(LocalDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
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
