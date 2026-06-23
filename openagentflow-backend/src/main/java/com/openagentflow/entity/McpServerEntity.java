package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * MCP服务表。
 * <p>对应数据库表：mcp_server。</p>
 */
@TableName("mcp_server")
public class McpServerEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 服务编码。 */
    @TableField("server_code")
    private String serverCode;

    /** 服务名称。 */
    @TableField("server_name")
    private String serverName;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** TRANSPORT类型。 */
    @TableField("transport_type")
    private String transportType;

    /** 字段说明：COMMAND。 */
    @TableField("command")
    private String command;

    /** 字段说明：ARGS。 */
    @TableField("args")
    private String args;

    /** 端点URL。 */
    @TableField("endpoint_url")
    private String endpointUrl;

    /** 认证类型。 */
    @TableField("auth_type")
    private String authType;

    /** 认证配置。 */
    @TableField("auth_config")
    private String authConfig;

    /** 字段说明：ENVVARS。 */
    @TableField("env_vars")
    private String envVars;

    /** 字段说明：ALLOWEDPATHS。 */
    @TableField("allowed_paths")
    private String allowedPaths;

    /** 风险策略。 */
    @TableField("risk_policy")
    private String riskPolicy;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** LASTHEARTBEAT时间。 */
    @TableField("last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    /** 所有者用户ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 删除时间。 */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    /** 版本。 */
    @TableField("version")
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServerCode() {
        return serverCode;
    }

    public void setServerCode(String serverCode) {
        this.serverCode = serverCode;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(String authConfig) {
        this.authConfig = authConfig;
    }

    public String getEnvVars() {
        return envVars;
    }

    public void setEnvVars(String envVars) {
        this.envVars = envVars;
    }

    public String getAllowedPaths() {
        return allowedPaths;
    }

    public void setAllowedPaths(String allowedPaths) {
        this.allowedPaths = allowedPaths;
    }

    public String getRiskPolicy() {
        return riskPolicy;
    }

    public void setRiskPolicy(String riskPolicy) {
        this.riskPolicy = riskPolicy;
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

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
