package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 权限角色权限表。
 * <p>对应数据库表：iam_role_permission。</p>
 */
@TableName("iam_role_permission")
public class IamRolePermissionEntity {

    /** 角色ID。 */
    @TableField("role_id")
    private String roleId;

    /** 权限ID。 */
    @TableField("permission_id")
    private String permissionId;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
