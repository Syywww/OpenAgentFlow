package com.openagentflow.domain.iam;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * IAM 后台管理数据传输对象。
 */
public final class IamAdminDtos {

    private IamAdminDtos() {
    }

    /**
     * IAM 管理概览。
     */
    public static class IamOverview {
        /** 用户总数。 */
        private long userCount;
        /** 部门总数。 */
        private long departmentCount;
        /** 角色总数。 */
        private long roleCount;
        /** 权限总数。 */
        private long permissionCount;

        public long getUserCount() {
            return userCount;
        }

        public void setUserCount(long userCount) {
            this.userCount = userCount;
        }

        public long getDepartmentCount() {
            return departmentCount;
        }

        public void setDepartmentCount(long departmentCount) {
            this.departmentCount = departmentCount;
        }

        public long getRoleCount() {
            return roleCount;
        }

        public void setRoleCount(long roleCount) {
            this.roleCount = roleCount;
        }

        public long getPermissionCount() {
            return permissionCount;
        }

        public void setPermissionCount(long permissionCount) {
            this.permissionCount = permissionCount;
        }
    }

    /**
     * 用户摘要。
     */
    public static class UserSummary {
        /** 用户 ID。 */
        private String id;
        /** 所属部门 ID。 */
        private String departmentId;
        /** 所属部门名称。 */
        private String departmentName;
        /** 登录用户名。 */
        private String username;
        /** 邮箱。 */
        private String email;
        /** 手机号。 */
        private String phone;
        /** 显示名称。 */
        private String displayName;
        /** 用户状态。 */
        private String status;
        /** 来源类型。 */
        private String sourceType;
        /** 最近登录时间。 */
        private LocalDateTime lastLoginAt;
        /** 创建时间。 */
        private LocalDateTime createdAt;
        /** 已绑定角色 ID 列表。 */
        private List<String> roleIds = new ArrayList<>();
        /** 已绑定角色编码列表。 */
        private List<String> roleCodes = new ArrayList<>();
        /** 已绑定角色名称列表。 */
        private List<String> roleNames = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(String departmentId) {
            this.departmentId = departmentId;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public LocalDateTime getLastLoginAt() {
            return lastLoginAt;
        }

        public void setLastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public List<String> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<String> roleIds) {
            this.roleIds = roleIds;
        }

        public List<String> getRoleCodes() {
            return roleCodes;
        }

        public void setRoleCodes(List<String> roleCodes) {
            this.roleCodes = roleCodes;
        }

        public List<String> getRoleNames() {
            return roleNames;
        }

        public void setRoleNames(List<String> roleNames) {
            this.roleNames = roleNames;
        }
    }

    /**
     * 用户保存请求。
     */
    public static class UserRequest {
        /** 所属部门 ID。 */
        private String departmentId;
        /** 登录用户名。 */
        @NotBlank(message = "用户名不能为空")
        private String username;
        /** 邮箱。 */
        private String email;
        /** 手机号。 */
        private String phone;
        /** 登录密码，创建时必填，更新时为空表示不修改。 */
        private String password;
        /** 显示名称。 */
        @NotBlank(message = "显示名称不能为空")
        private String displayName;
        /** 用户状态。 */
        private String status;
        /** 系统角色 ID 列表。 */
        private List<String> roleIds = new ArrayList<>();

        public String getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(String departmentId) {
            this.departmentId = departmentId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<String> roleIds) {
            this.roleIds = roleIds;
        }
    }

    /**
     * 部门树节点。
     */
    public static class DepartmentNode {
        /** 部门 ID。 */
        private String id;
        /** 父部门 ID。 */
        private String parentId;
        /** 部门编码。 */
        private String deptCode;
        /** 部门名称。 */
        private String deptName;
        /** 排序值。 */
        private Integer sortOrder;
        /** 部门状态。 */
        private String status;
        /** 部门直属用户数量。 */
        private long userCount;
        /** 子部门列表。 */
        private List<DepartmentNode> children = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getDeptCode() {
            return deptCode;
        }

        public void setDeptCode(String deptCode) {
            this.deptCode = deptCode;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getUserCount() {
            return userCount;
        }

        public void setUserCount(long userCount) {
            this.userCount = userCount;
        }

        public List<DepartmentNode> getChildren() {
            return children;
        }

        public void setChildren(List<DepartmentNode> children) {
            this.children = children;
        }
    }

    /**
     * 部门保存请求。
     */
    public static class DepartmentRequest {
        /** 父部门 ID。 */
        private String parentId;
        /** 部门编码。 */
        @NotBlank(message = "部门编码不能为空")
        private String deptCode;
        /** 部门名称。 */
        @NotBlank(message = "部门名称不能为空")
        private String deptName;
        /** 排序值。 */
        private Integer sortOrder;
        /** 部门状态。 */
        private String status;

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getDeptCode() {
            return deptCode;
        }

        public void setDeptCode(String deptCode) {
            this.deptCode = deptCode;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 系统角色摘要。
     */
    public static class RoleSummary {
        /** 角色 ID。 */
        private String id;
        /** 角色编码。 */
        private String roleCode;
        /** 角色名称。 */
        private String roleName;
        /** 角色描述。 */
        private String description;
        /** 是否内置角色。 */
        private Boolean builtIn;
        /** 角色状态。 */
        private String status;
        /** 已授权权限 ID 列表。 */
        private List<String> permissionIds = new ArrayList<>();
        /** 已授权权限编码列表。 */
        private List<String> permissionCodes = new ArrayList<>();
        /** 绑定该角色的用户数量。 */
        private long userCount;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getBuiltIn() {
            return builtIn;
        }

        public void setBuiltIn(Boolean builtIn) {
            this.builtIn = builtIn;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<String> permissionIds) {
            this.permissionIds = permissionIds;
        }

        public List<String> getPermissionCodes() {
            return permissionCodes;
        }

        public void setPermissionCodes(List<String> permissionCodes) {
            this.permissionCodes = permissionCodes;
        }

        public long getUserCount() {
            return userCount;
        }

        public void setUserCount(long userCount) {
            this.userCount = userCount;
        }
    }

    /**
     * 角色保存请求。
     */
    public static class RoleRequest {
        /** 角色编码。 */
        @NotBlank(message = "角色编码不能为空")
        private String roleCode;
        /** 角色名称。 */
        @NotBlank(message = "角色名称不能为空")
        private String roleName;
        /** 角色描述。 */
        private String description;
        /** 角色状态。 */
        private String status;
        /** 权限 ID 列表。 */
        private List<String> permissionIds = new ArrayList<>();

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<String> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }

    /**
     * 角色权限保存请求。
     */
    public static class RolePermissionRequest {
        /** 权限 ID 列表。 */
        private List<String> permissionIds = new ArrayList<>();

        public List<String> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<String> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }

    /**
     * 权限树节点。
     */
    public static class PermissionNode {
        /** 权限 ID。 */
        private String id;
        /** 父权限 ID。 */
        private String parentId;
        /** 权限编码。 */
        private String permissionCode;
        /** 权限名称。 */
        private String permissionName;
        /** 权限类型。 */
        private String permissionType;
        /** 前端路由路径。 */
        private String routePath;
        /** API 方法。 */
        private String apiMethod;
        /** API 路径。 */
        private String apiPath;
        /** 排序值。 */
        private Integer sortOrder;
        /** 是否可见。 */
        private Boolean visible;
        /** 权限状态。 */
        private String status;
        /** 子权限列表。 */
        private List<PermissionNode> children = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getPermissionCode() {
            return permissionCode;
        }

        public void setPermissionCode(String permissionCode) {
            this.permissionCode = permissionCode;
        }

        public String getPermissionName() {
            return permissionName;
        }

        public void setPermissionName(String permissionName) {
            this.permissionName = permissionName;
        }

        public String getPermissionType() {
            return permissionType;
        }

        public void setPermissionType(String permissionType) {
            this.permissionType = permissionType;
        }

        public String getRoutePath() {
            return routePath;
        }

        public void setRoutePath(String routePath) {
            this.routePath = routePath;
        }

        public String getApiMethod() {
            return apiMethod;
        }

        public void setApiMethod(String apiMethod) {
            this.apiMethod = apiMethod;
        }

        public String getApiPath() {
            return apiPath;
        }

        public void setApiPath(String apiPath) {
            this.apiPath = apiPath;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public Boolean getVisible() {
            return visible;
        }

        public void setVisible(Boolean visible) {
            this.visible = visible;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<PermissionNode> getChildren() {
            return children;
        }

        public void setChildren(List<PermissionNode> children) {
            this.children = children;
        }
    }
}
