package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.service.WorkflowExecutionService;
import com.openagentflow.service.WorkflowService;
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
 * 工作流编排与运行接口。
 */
@RestController
@RequestMapping
public class WorkflowController {

    /** 工作流定义服务。 */
    private final WorkflowService workflowService;

    /** 工作流执行服务。 */
    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowController(WorkflowService workflowService, WorkflowExecutionService workflowExecutionService) {
        this.workflowService = workflowService;
        this.workflowExecutionService = workflowExecutionService;
    }

    /**
     * 查询工作流列表。
     *
     * @return 工作流摘要列表
     */
    @GetMapping("/workflows")
    public ApiResponse<List<WorkflowDtos.Summary>> listWorkflows() {
        return ApiResponse.ok(workflowService.listWorkflows());
    }

    /**
     * 查询工作流详情。
     *
     * @param id 工作流 ID
     * @return 工作流详情
     */
    @GetMapping("/workflows/{id}")
    public ApiResponse<WorkflowDtos.Detail> getWorkflow(@PathVariable String id) {
        return ApiResponse.ok(workflowService.getWorkflow(id));
    }

    /**
     * 创建工作流。
     *
     * @param request 保存请求
     * @return 工作流详情
     */
    @PostMapping("/workflows")
    public ApiResponse<WorkflowDtos.Detail> createWorkflow(@RequestBody WorkflowDtos.Request request) {
        return ApiResponse.ok(workflowService.createWorkflow(request));
    }

    /**
     * 更新工作流。
     *
     * @param id 工作流 ID
     * @param request 保存请求
     * @return 工作流详情
     */
    @PutMapping("/workflows/{id}")
    public ApiResponse<WorkflowDtos.Detail> updateWorkflow(@PathVariable String id,
                                                           @RequestBody WorkflowDtos.Request request) {
        return ApiResponse.ok(workflowService.updateWorkflow(id, request));
    }

    /**
     * 删除工作流。
     *
     * @param id 工作流 ID
     * @return 空响应
     */
    @DeleteMapping("/workflows/{id}")
    public ApiResponse<Void> deleteWorkflow(@PathVariable String id) {
        workflowService.deleteWorkflow(id);
        return ApiResponse.ok(null);
    }

    /**
     * 发布工作流版本。
     *
     * @param id 工作流 ID
     * @param request 发布请求
     * @return 工作流详情
     */
    @PostMapping("/workflows/{id}/publish")
    public ApiResponse<WorkflowDtos.Detail> publishWorkflow(@PathVariable String id,
                                                            @RequestBody WorkflowDtos.PublishRequest request) {
        return ApiResponse.ok(workflowService.publishWorkflow(id, request));
    }

    /**
     * 运行工作流。
     *
     * @param id 工作流 ID
     * @param request 运行请求
     * @return 运行结果
     */
    @PostMapping("/workflows/{id}/run")
    public ApiResponse<WorkflowDtos.RunResult> runWorkflow(@PathVariable String id,
                                                           @RequestBody WorkflowDtos.RunRequest request) {
        return ApiResponse.ok(workflowExecutionService.runWorkflow(id, request, "manual"));
    }

    /**
     * 查询 Agent 绑定的工作流。
     *
     * @param agentId Agent ID
     * @return 绑定列表
     */
    @GetMapping("/agents/{agentId}/workflows")
    public ApiResponse<List<WorkflowDtos.BindingSummary>> listAgentWorkflowBindings(@PathVariable String agentId) {
        return ApiResponse.ok(workflowService.listAgentWorkflowBindings(agentId));
    }

    /**
     * 保存 Agent 工作流绑定。
     *
     * @param agentId Agent ID
     * @param request 绑定请求
     * @return 保存后的绑定列表
     */
    @PutMapping("/agents/{agentId}/workflows")
    public ApiResponse<List<WorkflowDtos.BindingSummary>> saveAgentWorkflowBindings(@PathVariable String agentId,
                                                                                    @RequestBody WorkflowDtos.BindingRequest request) {
        return ApiResponse.ok(workflowService.saveAgentWorkflowBindings(agentId, request));
    }
}
