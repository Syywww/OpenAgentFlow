package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.iam.IamAdminDtos;
import com.openagentflow.domain.iam.PermissionGovernanceDtos;
import com.openagentflow.service.IamAdminService;
import com.openagentflow.service.ResourceAclService;
import com.openagentflow.service.WorkspaceAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

/**
 * IAM 用户、部门、角色与权限管理接口。
 */
@RestController
@RequestMapping("/iam-admin")
public class IamAdminController {

    /** IAM 管理服务。 */
    private final IamAdminService iamAdminService;

    /** 工作空间授权服务。 */
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    /** 资源ACL服务。 */
    private final ResourceAclService resourceAclService;

    public IamAdminController(IamAdminService iamAdminService,
                              WorkspaceAuthorizationService workspaceAuthorizationService,
                              ResourceAclService resourceAclService) {
        this.iamAdminService = iamAdminService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.resourceAclService = resourceAclService;
    }

    /**
     * 查询 IAM 管理概览。
     *
     * @return IAM 概览
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<IamAdminDtos.IamOverview> overview() {
        return ApiResponse.ok(iamAdminService.overview());
    }

    /**
     * 查询用户列表。
     *
     * @return 用户摘要列表
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<List<IamAdminDtos.UserSummary>> listUsers() {
        return ApiResponse.ok(iamAdminService.listUsers());
    }

    /**
     * 创建用户。
     *
     * @param request 用户保存请求
     * @return 用户摘要
     */
    @PostMapping("/users")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<IamAdminDtos.UserSummary> createUser(@Valid @RequestBody IamAdminDtos.UserRequest request) {
        return ApiResponse.ok(iamAdminService.createUser(request));
    }

    /**
     * 更新用户。
     *
     * @param id 用户 ID
     * @param request 用户保存请求
     * @return 用户摘要
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<IamAdminDtos.UserSummary> updateUser(@PathVariable String id,
                                                            @Valid @RequestBody IamAdminDtos.UserRequest request) {
        return ApiResponse.ok(iamAdminService.updateUser(id, request));
    }

    /**
     * 删除用户。
     *
     * @param id 用户 ID
     * @return 空响应
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<Void> deleteUser(@PathVariable String id) {
        iamAdminService.deleteUser(id);
        return ApiResponse.ok(null);
    }

    /**
     * 查询部门树。
     *
     * @return 部门树
     */
    @GetMapping("/departments")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<List<IamAdminDtos.DepartmentNode>> listDepartments() {
        return ApiResponse.ok(iamAdminService.listDepartments());
    }

    /**
     * 创建部门。
     *
     * @param request 部门保存请求
     * @return 部门树
     */
    @PostMapping("/departments")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<List<IamAdminDtos.DepartmentNode>> createDepartment(@Valid @RequestBody IamAdminDtos.DepartmentRequest request) {
        return ApiResponse.ok(iamAdminService.createDepartment(request));
    }

    /**
     * 更新部门。
     *
     * @param id 部门 ID
     * @param request 部门保存请求
     * @return 部门树
     */
    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<List<IamAdminDtos.DepartmentNode>> updateDepartment(@PathVariable String id,
                                                                           @Valid @RequestBody IamAdminDtos.DepartmentRequest request) {
        return ApiResponse.ok(iamAdminService.updateDepartment(id, request));
    }

    /**
     * 删除部门。
     *
     * @param id 部门 ID
     * @return 空响应
     */
    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<Void> deleteDepartment(@PathVariable String id) {
        iamAdminService.deleteDepartment(id);
        return ApiResponse.ok(null);
    }

    /**
     * 查询系统角色列表。
     *
     * @return 角色摘要列表
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<List<IamAdminDtos.RoleSummary>> listRoles() {
        return ApiResponse.ok(iamAdminService.listRoles());
    }

    /**
     * 创建系统角色。
     *
     * @param request 角色保存请求
     * @return 角色摘要
     */
    @PostMapping("/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<IamAdminDtos.RoleSummary> createRole(@Valid @RequestBody IamAdminDtos.RoleRequest request) {
        return ApiResponse.ok(iamAdminService.createRole(request));
    }

    /**
     * 更新系统角色。
     *
     * @param id 角色 ID
     * @param request 角色保存请求
     * @return 角色摘要
     */
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<IamAdminDtos.RoleSummary> updateRole(@PathVariable String id,
                                                            @Valid @RequestBody IamAdminDtos.RoleRequest request) {
        return ApiResponse.ok(iamAdminService.updateRole(id, request));
    }

    /**
     * 删除系统角色。
     *
     * @param id 角色 ID
     * @return 空响应
     */
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<Void> deleteRole(@PathVariable String id) {
        iamAdminService.deleteRole(id);
        return ApiResponse.ok(null);
    }

    /**
     * 单独更新角色权限。
     *
     * @param id 角色 ID
     * @param request 角色权限保存请求
     * @return 角色摘要
     */
    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<IamAdminDtos.RoleSummary> updateRolePermissions(@PathVariable String id,
                                                                       @RequestBody IamAdminDtos.RolePermissionRequest request) {
        return ApiResponse.ok(iamAdminService.updateRolePermissions(id, request.getPermissionIds()));
    }

    /**
     * 查询权限树。
     *
     * @return 权限树
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage')")
    public ApiResponse<List<IamAdminDtos.PermissionNode>> listPermissions() {
        return ApiResponse.ok(iamAdminService.listPermissions());
    }

    /** 查询指定工作空间的权限治理概览。 */
    @GetMapping("/governance/overview")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:view','iam:governance:manage'})")
    public ApiResponse<PermissionGovernanceDtos.GovernanceOverview> governanceOverview(@RequestParam String workspaceId) {
        return ApiResponse.ok(workspaceAuthorizationService.overview(workspaceId));
    }

    /** 查询指定工作空间的角色列表。 */
    @GetMapping("/governance/workspace-roles")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:view','iam:governance:manage'})")
    public ApiResponse<List<PermissionGovernanceDtos.WorkspaceRoleSummary>> listWorkspaceRoles(@RequestParam String workspaceId) {
        return ApiResponse.ok(workspaceAuthorizationService.listRoles(workspaceId));
    }

    /** 新建工作空间角色。 */
    @PostMapping("/governance/workspace-roles")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:manage'})")
    public ApiResponse<PermissionGovernanceDtos.WorkspaceRoleSummary> createWorkspaceRole(
            @Valid @RequestBody PermissionGovernanceDtos.WorkspaceRoleRequest request) {
        return ApiResponse.ok(workspaceAuthorizationService.saveRole(null, request));
    }

    /** 更新工作空间角色。 */
    @PutMapping("/governance/workspace-roles/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:manage'})")
    public ApiResponse<PermissionGovernanceDtos.WorkspaceRoleSummary> updateWorkspaceRole(
            @PathVariable String id, @Valid @RequestBody PermissionGovernanceDtos.WorkspaceRoleRequest request) {
        return ApiResponse.ok(workspaceAuthorizationService.saveRole(id, request));
    }

    /** 删除非内置工作空间角色。 */
    @DeleteMapping("/governance/workspace-roles/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:manage'})")
    public ApiResponse<Void> deleteWorkspaceRole(@PathVariable String id, @RequestParam String workspaceId) {
        workspaceAuthorizationService.deleteRole(workspaceId, id);
        return ApiResponse.ok(null);
    }

    /** 重新分配工作空间成员角色。 */
    @GetMapping("/governance/workspaces/{workspaceId}/members/{userId}/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:view','iam:governance:manage'})")
    public ApiResponse<List<String>> listWorkspaceMemberRoles(@PathVariable String workspaceId,
                                                              @PathVariable String userId) {
        return ApiResponse.ok(workspaceAuthorizationService.listMemberRoleIds(workspaceId, userId));
    }

    /** 重新分配工作空间成员角色。 */
    @PutMapping("/governance/workspaces/{workspaceId}/members/{userId}/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:manage'})")
    public ApiResponse<Void> assignWorkspaceMemberRoles(@PathVariable String workspaceId,
                                                        @PathVariable String userId,
                                                        @RequestBody PermissionGovernanceDtos.MemberRoleRequest request) {
        workspaceAuthorizationService.assignMemberRoles(workspaceId, userId, request);
        return ApiResponse.ok(null);
    }

    /** 查询用户在工作空间内的数据范围。 */
    @GetMapping("/governance/workspaces/{workspaceId}/members/{userId}/data-scope")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:view','iam:governance:manage'})")
    public ApiResponse<PermissionGovernanceDtos.DataScopeResult> resolveDataScope(@PathVariable String workspaceId,
                                                                                  @PathVariable String userId) {
        return ApiResponse.ok(workspaceAuthorizationService.resolveDataScope(workspaceId, userId));
    }

    /** 查询工作空间资源授权。 */
    @GetMapping("/resource-acls")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:acl:manage'})")
    public ApiResponse<List<PermissionGovernanceDtos.ResourceAclSummary>> listResourceAcls(@RequestParam String workspaceId) {
        return ApiResponse.ok(resourceAclService.list(workspaceId));
    }

    /** 创建或替换资源授权。 */
    @PostMapping("/resource-acls")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:acl:manage'})")
    public ApiResponse<PermissionGovernanceDtos.ResourceAclSummary> grantResourceAcl(
            @Valid @RequestBody PermissionGovernanceDtos.ResourceAclRequest request) {
        return ApiResponse.ok(resourceAclService.grant(request));
    }

    /** 撤销资源授权。 */
    @DeleteMapping("/resource-acls/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:acl:manage'})")
    public ApiResponse<Void> revokeResourceAcl(@PathVariable String id,
                                               @RequestParam String workspaceId,
                                               @RequestParam(required = false) String reason) {
        resourceAclService.revoke(workspaceId, id, reason);
        return ApiResponse.ok(null);
    }

    /** 查询授权变更审计。 */
    @GetMapping("/governance/audits")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage') or @workspaceAuthorizationService.currentUserHasAnyPermission({'iam:governance:view','iam:governance:manage'})")
    public ApiResponse<List<PermissionGovernanceDtos.AuthorizationAuditSummary>> listAuthorizationAudits(@RequestParam String workspaceId) {
        return ApiResponse.ok(workspaceAuthorizationService.listAudits(workspaceId));
    }

    /** 强制撤销用户全部登录会话。 */
    @PostMapping("/users/{id}/revoke-sessions")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','iam:manage','iam:session:revoke')")
    public ApiResponse<Long> revokeUserSessions(@PathVariable String id,
                                                @RequestBody(required = false) PermissionGovernanceDtos.RevokeSessionRequest request) {
        long revoked = iamAdminService.revokeUserSessions(id);
        workspaceAuthorizationService.writeAudit(null, "revoke_session", "user", id, "user", id,
                request == null ? null : request.reason(), Map.of(), Map.of("revokedTokenCount", revoked));
        return ApiResponse.ok(revoked);
    }
}
