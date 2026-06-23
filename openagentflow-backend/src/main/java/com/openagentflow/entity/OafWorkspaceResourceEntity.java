package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 工作空间资源归属实体。
 *
 * <p>对应数据库表：oaf_workspace_resource，用于统一记录资源属于哪个工作空间。</p>
 */
@TableName("oaf_workspace_resource")
public class OafWorkspaceResourceEntity {

    /** 资源归属主键 ID。 */
    @TableId("id")
    private String id;

    /** 工作空间 ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 资源类型。 */
    @TableField("resource_type")
    private String resourceType;

    /** 资源 ID。 */
    @TableField("resource_id")
    private String resourceId;

    /** 资源所有者用户 ID。 */
    @TableField("owner_user_id")
    private String ownerUserId;

    /** 创建人 ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
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
}
