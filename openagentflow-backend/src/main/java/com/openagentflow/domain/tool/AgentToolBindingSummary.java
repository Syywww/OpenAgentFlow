package com.openagentflow.domain.tool;

/**
 * Agent 工具绑定展示摘要。
 */
public class AgentToolBindingSummary {

    /** Agent ID。 */
    private String agentId;

    /** 工具 ID。 */
    private String toolId;

    /** 工具编码。 */
    private String toolCode;

    /** 工具名称。 */
    private String toolName;

    /** 工具类型。 */
    private String toolType;

    /** 风险等级。 */
    private String riskLevel;

    /** 是否需要确认。 */
    private Boolean requireConfirm;

    /** 是否启用。 */
    private Boolean enabled;

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

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
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
}
