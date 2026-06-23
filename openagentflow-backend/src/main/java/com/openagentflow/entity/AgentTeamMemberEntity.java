package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent团队成员表。
 * <p>对应数据库表：agent_team_member。</p>
 */
@TableName("agent_team_member")
public class AgentTeamMemberEntity {

    /** 团队ID。 */
    @TableField("team_id")
    private String teamId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 成员角色。 */
    @TableField("member_role")
    private String memberRole;

    /** HANDOFF策略。 */
    @TableField("handoff_policy")
    private String handoffPolicy;

    /** 排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否启用。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public String getHandoffPolicy() {
        return handoffPolicy;
    }

    public void setHandoffPolicy(String handoffPolicy) {
        this.handoffPolicy = handoffPolicy;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
