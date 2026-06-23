package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 本地模型运行时表。
 * <p>对应数据库表：local_model_runtime。</p>
 */
@TableName("local_model_runtime")
public class LocalModelRuntimeEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 运行时编码。 */
    @TableField("runtime_code")
    private String runtimeCode;

    /** 运行时名称。 */
    @TableField("runtime_name")
    private String runtimeName;

    /** 运行时类型。 */
    @TableField("runtime_type")
    private String runtimeType;

    /** 端点URL。 */
    @TableField("endpoint_url")
    private String endpointUrl;

    /** 字段说明：HOSTINFO。 */
    @TableField("host_info")
    private String hostInfo;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** LASTHEARTBEAT时间。 */
    @TableField("last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

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

    public String getRuntimeCode() {
        return runtimeCode;
    }

    public void setRuntimeCode(String runtimeCode) {
        this.runtimeCode = runtimeCode;
    }

    public String getRuntimeName() {
        return runtimeName;
    }

    public void setRuntimeName(String runtimeName) {
        this.runtimeName = runtimeName;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
        this.runtimeType = runtimeType;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getHostInfo() {
        return hostInfo;
    }

    public void setHostInfo(String hostInfo) {
        this.hostInfo = hostInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
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
