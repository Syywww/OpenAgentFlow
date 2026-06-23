package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * MCP能力表。
 * <p>对应数据库表：mcp_capability。</p>
 */
@TableName("mcp_capability")
public class McpCapabilityEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 服务ID。 */
    @TableField("server_id")
    private String serverId;

    /** 能力类型。 */
    @TableField("capability_type")
    private String capabilityType;

    /** 能力名称。 */
    @TableField("capability_name")
    private String capabilityName;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** 字段说明：Schema JSON。 */
    @TableField("schema_json")
    private String schemaJson;

    /** 元数据JSON。 */
    @TableField("metadata")
    private String metadata;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 风险级别。 */
    @TableField("risk_level")
    private String riskLevel;

    /** DISCOVERED时间。 */
    @TableField("discovered_at")
    private LocalDateTime discoveredAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getCapabilityType() {
        return capabilityType;
    }

    public void setCapabilityType(String capabilityType) {
        this.capabilityType = capabilityType;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public void setCapabilityName(String capabilityName) {
        this.capabilityName = capabilityName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(String schemaJson) {
        this.schemaJson = schemaJson;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }
}
