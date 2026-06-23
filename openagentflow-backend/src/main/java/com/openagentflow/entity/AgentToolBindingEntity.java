package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent工具BINDING表。
 * <p>对应数据库表：agent_tool_binding。</p>
 */
@TableName("agent_tool_binding")
public class AgentToolBindingEntity {

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 工具ID。 */
    @TableField("tool_id")
    private String toolId;

    /** 工具配置。 */
    @TableField("tool_config")
    private String toolConfig;

    /** REQUIRE确认。 */
    @TableField("require_confirm")
    private Boolean requireConfirm;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getToolConfig() {
        return toolConfig;
    }

    public void setToolConfig(String toolConfig) {
        this.toolConfig = toolConfig;
    }

    public Boolean getRequireConfirm() {
        return requireConfirm;
    }

    public void setRequireConfirm(Boolean requireConfirm) {
        this.requireConfirm = requireConfirm;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
