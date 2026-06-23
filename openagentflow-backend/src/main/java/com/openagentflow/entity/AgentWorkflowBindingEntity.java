package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent工作流BINDING表。
 * <p>对应数据库表：agent_workflow_binding。</p>
 */
@TableName("agent_workflow_binding")
public class AgentWorkflowBindingEntity {

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 工作流ID。 */
    @TableField("workflow_id")
    private String workflowId;

    /** 字段说明：TRIGGERMODE。 */
    @TableField("trigger_mode")
    private String triggerMode;

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

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
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
