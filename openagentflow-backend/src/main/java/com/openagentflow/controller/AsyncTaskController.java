package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.task.AsyncTaskDtos;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.service.AsyncTaskService;
import com.openagentflow.service.KnowledgeDocumentProcessingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/**
 * 异步任务中心接口。
 */
@RestController
@RequestMapping("/tasks")
public class AsyncTaskController {

    /** 异步任务中心服务。 */
    private final AsyncTaskService asyncTaskService;

    /** 知识文档处理服务，用于文档类任务重试。 */
    private final KnowledgeDocumentProcessingService knowledgeDocumentProcessingService;

    public AsyncTaskController(AsyncTaskService asyncTaskService,
                               KnowledgeDocumentProcessingService knowledgeDocumentProcessingService) {
        this.asyncTaskService = asyncTaskService;
        this.knowledgeDocumentProcessingService = knowledgeDocumentProcessingService;
    }

    /**
     * 查询异步任务统计。
     *
     * @return 任务统计
     */
    @GetMapping("/overview")
    public ApiResponse<AsyncTaskDtos.Overview> overview() {
        return ApiResponse.ok(asyncTaskService.overview());
    }

    /**
     * 分页查询异步任务列表。
     *
     * @param status 任务状态
     * @param taskType 任务类型
     * @param workspaceId 工作空间ID
     * @param keyword 搜索关键字
     * @param pageNo 当前页
     * @param pageSize 每页大小
     * @return 分页任务列表
     */
    @GetMapping
    public ApiResponse<PageResult<AsyncTaskDtos.Summary>> listTasks(@RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) String taskType,
                                                                    @RequestParam(required = false) String workspaceId,
                                                                    @RequestParam(required = false) String keyword,
                                                                    @RequestParam(defaultValue = "1") Integer pageNo,
                                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        AsyncTaskDtos.Query query = new AsyncTaskDtos.Query();
        query.setStatus(status);
        query.setTaskType(taskType);
        query.setWorkspaceId(workspaceId);
        query.setKeyword(keyword);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return ApiResponse.ok(asyncTaskService.listTasks(query));
    }

    /**
     * 查询异步任务详情。
     *
     * @param id 任务ID
     * @return 任务详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AsyncTaskDtos.Detail> getTask(@PathVariable String id) {
        return ApiResponse.ok(asyncTaskService.getTask(id));
    }

    /**
     * 请求取消异步任务。
     *
     * @param id 任务ID
     * @return 取消后的任务详情
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<AsyncTaskDtos.Detail> cancelTask(@PathVariable String id) {
        return ApiResponse.ok(asyncTaskService.cancelTask(id));
    }

    /**
     * 重试异步任务。
     *
     * @param id 任务ID
     * @return 重试后的任务详情
     */
    @PostMapping("/{id}/retry")
    public ApiResponse<AsyncTaskDtos.Detail> retryTask(@PathVariable String id) {
        AsyncTaskEntity task = asyncTaskService.findById(id);
        if (task == null) {
            throw new BusinessException("TASK_NOT_FOUND", "异步任务不存在");
        }
        if (!StringUtils.hasText(task.getTaskType()) || !java.util.List.of(
                "DOCUMENT_PROCESS",
                "KNOWLEDGE_VECTOR_REBUILD",
                "EVALUATION_RUN",
                "MCP_DISCOVERY",
                "KNOWLEDGE_GOVERNANCE_SCAN",
                "MEMORY_CLEANUP",
                "MEMORY_CAPTURE",
                "MEMORY_VECTOR_REBUILD",
                "MEMORY_GOVERNANCE_SCAN",
                "USAGE_COST_RECALCULATION").contains(task.getTaskType())) {
            throw new BusinessException("TASK_RETRY_UNSUPPORTED", "当前任务类型暂不支持重试");
        }
        if ("DOCUMENT_PROCESS".equals(task.getTaskType())) {
            knowledgeDocumentProcessingService.retryTask(id);
        } else {
            asyncTaskService.prepareRetry(id);
        }
        return ApiResponse.ok(asyncTaskService.getTask(id));
    }
}
