package com.openagentflow.domain;

/**
 * 首页概览对象。
 */
public class DashboardOverview {

    /** Agent 总数。 */
    private Long agentCount;

    /** 知识库总数。 */
    private Long knowledgeBaseCount;

    /** 工具总数。 */
    private Long toolCount;

    /** MCP 服务总数。 */
    private Long mcpServerCount;

    /** 今日运行次数。 */
    private Long todayRunCount;

    /** 今日调用成本。 */
    private Double todayCost;

    public Long getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(Long agentCount) {
        this.agentCount = agentCount;
    }

    public Long getKnowledgeBaseCount() {
        return knowledgeBaseCount;
    }

    public void setKnowledgeBaseCount(Long knowledgeBaseCount) {
        this.knowledgeBaseCount = knowledgeBaseCount;
    }

    public Long getToolCount() {
        return toolCount;
    }

    public void setToolCount(Long toolCount) {
        this.toolCount = toolCount;
    }

    public Long getMcpServerCount() {
        return mcpServerCount;
    }

    public void setMcpServerCount(Long mcpServerCount) {
        this.mcpServerCount = mcpServerCount;
    }

    public Long getTodayRunCount() {
        return todayRunCount;
    }

    public void setTodayRunCount(Long todayRunCount) {
        this.todayRunCount = todayRunCount;
    }

    public Double getTodayCost() {
        return todayCost;
    }

    public void setTodayCost(Double todayCost) {
        this.todayCost = todayCost;
    }
}
