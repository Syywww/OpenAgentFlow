package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.iam.IamAdminDtos;
import com.openagentflow.service.IamAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IAM 用户、部门、角色与权限管理接口。
 */
@RestController
@RequestMapping("/iam-admin")
public class IamAdminController {

    /** IAM 管理服务。 */
    private final IamAdminService iamAdminService;

    public IamAdminController(IamAdminService iamAdminService) {
        this.iamAdminService = iamAdminService;
    }

    /**
     * 查询 IAM 管理概览。
     *
     * @return IAM 概览
     */
    @GetMapping("/overview")
    public ApiResponse<IamAdminDtos.IamOverview> overview() {
        return ApiResponse.ok(iamAdminService.overview());
    }

    /**
     * 查询用户列表。
     *
     * @return 用户摘要列表
     */
    @GetMapping("/users")
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
    public ApiResponse<List<IamAdminDtos.PermissionNode>> listPermissions() {
        return ApiResponse.ok(iamAdminService.listPermissions());
    }
}
