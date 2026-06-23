package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 工作空间成员实体。
 *
 * <p>对应数据库表：oaf_workspace_member，用于控制用户在空间内的资源权限。</p>
 */
@TableName("oaf_workspace_member")
public class OafWorkspaceMemberEntity {

    /** 成员主键 ID。 */
    @TableId("id")
    private String id;

    /** 工作空间 ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 成员角色。 */
    @TableField("member_role")
    private String memberRole;

    /** 成员状态。 */
    @TableField("status")
    private String status;

    /** 加入时间。 */
    @TableField("joined_at")
    private LocalDateTime joinedAt;

    /** 创建人 ID。 */
    @TableField("created_by")
    private String createdBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
