package com.openagentflow.domain.auth;

import java.util.List;

/**
 * 当前登录用户对象。
 */
public class CurrentUser {

    /** 用户主键ID。 */
    private String id;

    /** 用户名。 */
    private String username;

    /** 显示名称。 */
    private String displayName;

    /** 邮箱。 */
    private String email;

    /** 头像地址。 */
    private String avatarUrl;

    /** 角色编码列表。 */
    private List<String> roles;

    /** 权限编码列表。 */
    private List<String> permissions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
