package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent团队表。
 * <p>对应数据库表：agent_team。</p>
 */
@TableName("agent_team")
public class AgentTeamEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 团队编码。 */
    @TableField("team_code")
    private String teamCode;

    /** 团队名称。 */
    @TableField("team_name")
    private String teamName;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** 协作MODE。 */
    @TableField("collaboration_mode")
    private String collaborationMode;

    /** 字段说明：COORDINATORAgentID。 */
    @TableField("coordinator_agent_id")
    private String coordinatorAgentId;

    /** 状态。 */
    @TableField("status")
    private String status;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamCode() {
        return teamCode;
    }

    public void setTeamCode(String teamCode) {
        this.teamCode = teamCode;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCollaborationMode() {
        return collaborationMode;
    }

    public void setCollaborationMode(String collaborationMode) {
        this.collaborationMode = collaborationMode;
    }

    public String getCoordinatorAgentId() {
        return coordinatorAgentId;
    }

    public void setCoordinatorAgentId(String coordinatorAgentId) {
        this.coordinatorAgentId = coordinatorAgentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
