package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.workspace.WorkspaceDtos;
import com.openagentflow.service.WorkspaceGovernanceService;
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
 * 组织和工作空间治理接口。
 */
@RestController
@RequestMapping
public class WorkspaceController {

    /** 工作空间治理服务。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    public WorkspaceController(WorkspaceGovernanceService workspaceGovernanceService) {
        this.workspaceGovernanceService = workspaceGovernanceService;
    }

    /**
     * 查询当前用户可见组织。
     *
     * @return 组织摘要列表
     */
    @GetMapping("/organizations")
    public ApiResponse<List<WorkspaceDtos.OrganizationSummary>> listOrganizations() {
        return ApiResponse.ok(workspaceGovernanceService.listOrganizations());
    }

    /**
     * 创建组织。
     *
     * @param request 组织请求
     * @return 组织摘要
     */
    @PostMapping("/organizations")
    public ApiResponse<WorkspaceDtos.OrganizationSummary> createOrganization(@Valid @RequestBody WorkspaceDtos.OrganizationRequest request) {
        return ApiResponse.ok(workspaceGovernanceService.createOrganization(request));
    }

    /**
     * 查询当前用户可见工作空间。
     *
     * @return 工作空间摘要列表
     */
    @GetMapping("/workspaces")
    public ApiResponse<List<WorkspaceDtos.WorkspaceSummary>> listWorkspaces() {
        return ApiResponse.ok(workspaceGovernanceService.listWorkspaces());
    }

    /**
     * 创建工作空间。
     *
     * @param request 工作空间请求
     * @return 工作空间详情
     */
    @PostMapping("/workspaces")
    public ApiResponse<WorkspaceDtos.WorkspaceDetail> createWorkspace(@Valid @RequestBody WorkspaceDtos.WorkspaceRequest request) {
        return ApiResponse.ok(workspaceGovernanceService.createWorkspace(request));
    }

    /**
     * 查询工作空间详情。
     *
     * @param id 工作空间 ID
     * @return 工作空间详情
     */
    @GetMapping("/workspaces/{id}")
    public ApiResponse<WorkspaceDtos.WorkspaceDetail> getWorkspace(@PathVariable String id) {
        return ApiResponse.ok(workspaceGovernanceService.getWorkspace(id));
    }

    /**
     * 更新工作空间。
     *
     * @param id 工作空间 ID
     * @param request 工作空间请求
     * @return 工作空间详情
     */
    @PutMapping("/workspaces/{id}")
    public ApiResponse<WorkspaceDtos.WorkspaceDetail> updateWorkspace(@PathVariable String id,
                                                                      @Valid @RequestBody WorkspaceDtos.WorkspaceRequest request) {
        return ApiResponse.ok(workspaceGovernanceService.updateWorkspace(id, request));
    }

    /**
     * 新增或更新工作空间成员。
     *
     * @param id 工作空间 ID
     * @param request 成员请求
     * @return 工作空间详情
     */
    @PostMapping("/workspaces/{id}/members")
    public ApiResponse<WorkspaceDtos.WorkspaceDetail> saveWorkspaceMember(@PathVariable String id,
                                                                          @Valid @RequestBody WorkspaceDtos.MemberRequest request) {
        return ApiResponse.ok(workspaceGovernanceService.saveWorkspaceMember(id, request));
    }

    /**
     * 移除工作空间成员。
     *
     * @param id 工作空间 ID
     * @param userId 用户 ID
     * @return 工作空间详情
     */
    @DeleteMapping("/workspaces/{id}/members/{userId}")
    public ApiResponse<WorkspaceDtos.WorkspaceDetail> removeWorkspaceMember(@PathVariable String id,
                                                                            @PathVariable String userId) {
        return ApiResponse.ok(workspaceGovernanceService.removeWorkspaceMember(id, userId));
    }
}
