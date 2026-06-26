package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.workflow.WorkflowAdvancedDtos;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.service.WorkflowAdvancedService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作流生产级增强接口。
 */
@RestController
@RequestMapping
public class WorkflowAdvancedController {

    /** 工作流增强服务。 */
    private final WorkflowAdvancedService workflowAdvancedService;

    public WorkflowAdvancedController(WorkflowAdvancedService workflowAdvancedService) {
        this.workflowAdvancedService = workflowAdvancedService;
    }

    /**
     * 查询工作流增强总览。
     *
     * @return 总览数据
     */
    @GetMapping("/workflows/advanced/overview")
    public ApiResponse<WorkflowAdvancedDtos.Overview> overview() {
        return ApiResponse.ok(workflowAdvancedService.overview());
    }

    /**
     * 查询工作流增强能力清单。
     *
     * @return 能力清单
     */
    @GetMapping("/workflows/advanced/capabilities")
    public ApiResponse<List<WorkflowAdvancedDtos.Capability>> capabilities() {
        return ApiResponse.ok(workflowAdvancedService.capabilities());
    }

    /**
     * 查询工作流模板。
     *
     * @return 模板列表
     */
    @GetMapping("/workflows/templates")
    public ApiResponse<List<WorkflowAdvancedDtos.TemplateSummary>> templates() {
        return ApiResponse.ok(workflowAdvancedService.listTemplates());
    }

    /**
     * 查询工作流 API 端点。
     *
     * @return API 端点列表
     */
    @GetMapping("/workflows/api-endpoints")
    public ApiResponse<List<WorkflowAdvancedDtos.ApiEndpointSummary>> apiEndpoints() {
        return ApiResponse.ok(workflowAdvancedService.listApiEndpoints());
    }

    /**
     * 发布或更新工作流 API 端点。
     *
     * @param workflowId 工作流 ID
     * @param request 发布请求
     * @return API 端点
     */
    @PutMapping("/workflows/{workflowId}/api-endpoint")
    public ApiResponse<WorkflowAdvancedDtos.ApiEndpointSummary> publishApiEndpoint(@PathVariable String workflowId,
                                                                                   @RequestBody WorkflowAdvancedDtos.ApiPublishRequest request) {
        return ApiResponse.ok(workflowAdvancedService.publishApiEndpoint(workflowId, request));
    }

    /**
     * 通过发布的 API 端点调用工作流。
     *
     * @param endpointCode 端点编码
     * @param request 运行请求
     * @return 运行结果
     */
    @PostMapping("/workflow-api/{endpointCode}")
    public ApiResponse<WorkflowDtos.RunResult> invokeApi(@PathVariable String endpointCode,
                                                         @RequestBody WorkflowDtos.RunRequest request) {
        return ApiResponse.ok(workflowAdvancedService.invokeApiEndpoint(endpointCode, request));
    }

    /**
     * 查询人工确认任务。
     *
     * @return 任务列表
     */
    @GetMapping("/workflows/human-tasks")
    public ApiResponse<List<WorkflowAdvancedDtos.HumanTaskSummary>> humanTasks() {
        return ApiResponse.ok(workflowAdvancedService.listHumanTasks());
    }

    /**
     * 处理人工确认任务。
     *
     * @param taskId 任务 ID
     * @param request 决策请求
     * @return 处理后的任务
     */
    @PostMapping("/workflows/human-tasks/{taskId}/decision")
    public ApiResponse<WorkflowAdvancedDtos.HumanTaskSummary> decideHumanTask(@PathVariable String taskId,
                                                                              @RequestBody WorkflowAdvancedDtos.HumanTaskDecisionRequest request) {
        return ApiResponse.ok(workflowAdvancedService.decideHumanTask(taskId, request));
    }

    /**
     * 对比两个发布版本。
     *
     * @param workflowId 工作流 ID
     * @param leftVersion 左版本号
     * @param rightVersion 右版本号
     * @return 版本差异
     */
    @GetMapping("/workflows/{workflowId}/versions/diff")
    public ApiResponse<WorkflowAdvancedDtos.VersionDiff> diffVersions(@PathVariable String workflowId,
                                                                      @RequestParam String leftVersion,
                                                                      @RequestParam String rightVersion) {
        return ApiResponse.ok(workflowAdvancedService.diffVersions(workflowId, leftVersion, rightVersion));
    }
}
