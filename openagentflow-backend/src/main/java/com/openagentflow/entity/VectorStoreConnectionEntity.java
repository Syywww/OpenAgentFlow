package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 向量存储连接表。
 * <p>对应数据库表：vector_store_connection。</p>
 */
@TableName("vector_store_connection")
public class VectorStoreConnectionEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 连接编码。 */
    @TableField("connection_code")
    private String connectionCode;

    /** 连接名称。 */
    @TableField("connection_name")
    private String connectionName;

    /** 存储类型。 */
    @TableField("store_type")
    private String storeType;

    /** 端点。 */
    @TableField("endpoint")
    private String endpoint;

    /** DATABASE名称。 */
    @TableField("database_name")
    private String databaseName;

    /** 认证类型。 */
    @TableField("auth_type")
    private String authType;

    /** 用户名。 */
    @TableField("username")
    private String username;

    /** 密码CIPHER。 */
    @TableField("password_cipher")
    private String passwordCipher;

    /** 令牌CIPHER。 */
    @TableField("token_cipher")
    private String tokenCipher;

    /** 安全连接。 */
    @TableField("secure")
    private Boolean secure;

    /** 默认一致性级别。 */
    @TableField("default_consistency_level")
    private String defaultConsistencyLevel;

    /** 默认距离度量类型。 */
    @TableField("default_metric_type")
    private String defaultMetricType;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 健康状态。 */
    @TableField("health_status")
    private String healthStatus;

    /** LAST健康CHECK时间。 */
    @TableField("last_health_check_at")
    private LocalDateTime lastHealthCheckAt;

    /** 配置JSON。 */
    @TableField("config_json")
    private String configJson;

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

    public String getConnectionCode() {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode) {
        this.connectionCode = connectionCode;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordCipher() {
        return passwordCipher;
    }

    public void setPasswordCipher(String passwordCipher) {
        this.passwordCipher = passwordCipher;
    }

    public String getTokenCipher() {
        return tokenCipher;
    }

    public void setTokenCipher(String tokenCipher) {
        this.tokenCipher = tokenCipher;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    public String getDefaultConsistencyLevel() {
        return defaultConsistencyLevel;
    }

    public void setDefaultConsistencyLevel(String defaultConsistencyLevel) {
        this.defaultConsistencyLevel = defaultConsistencyLevel;
    }

    public String getDefaultMetricType() {
        return defaultMetricType;
    }

    public void setDefaultMetricType(String defaultMetricType) {
        this.defaultMetricType = defaultMetricType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public LocalDateTime getLastHealthCheckAt() {
        return lastHealthCheckAt;
    }

    public void setLastHealthCheckAt(LocalDateTime lastHealthCheckAt) {
        this.lastHealthCheckAt = lastHealthCheckAt;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
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
