package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 权限用户角色表。
 * <p>对应数据库表：iam_user_role。</p>
 */
@TableName("iam_user_role")
public class IamUserRoleEntity {

    /** 用户ID。 */
    @TableField("user_id")
    private String userId;

    /** 角色ID。 */
    @TableField("role_id")
    private String roleId;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
