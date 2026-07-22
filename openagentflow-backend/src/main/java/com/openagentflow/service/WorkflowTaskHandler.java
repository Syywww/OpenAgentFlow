package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.task.AsyncTaskDtos;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.WorkflowDefinitionEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Kafka 工作流运行任务处理器。 */
@Service
public class WorkflowTaskHandler implements DistributedTaskHandler {

    /** 工作流服务，用于校验资源权限。 */
    private final WorkflowService workflowService;
    /** 工作流执行引擎。 */
    private final WorkflowExecutionService workflowExecutionService;
    /** 异步任务中心。 */
    private final AsyncTaskService asyncTaskService;
    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    public WorkflowTaskHandler(WorkflowService workflowService,
                               WorkflowExecutionService workflowExecutionService,
                               AsyncTaskService asyncTaskService,
                               ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.workflowExecutionService = workflowExecutionService;
        this.asyncTaskService = asyncTaskService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String taskType() {
        return "WORKFLOW_RUN";
    }

    /** 将工作流运行请求提交到现有 Kafka Outbox 链路。 */
    public AsyncTaskDtos.Detail submit(String workflowId, WorkflowDtos.RunRequest request) {
        WorkflowDefinitionEntity workflow = workflowService.requireWorkflow(workflowId);
        if (!workflowService.canView(workflow)) {
            throw new com.openagentflow.exception.BusinessException("WORKFLOW_FORBIDDEN", "没有运行该工作流的权限");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workflowId", workflowId);
        payload.put("request", objectMapper.convertValue(request == null ? new WorkflowDtos.RunRequest() : request,
                new TypeReference<Map<String, Object>>() { }));
        AsyncTaskEntity task = asyncTaskService.createTask("异步运行工作流：" + workflow.getWorkflowName(),
                taskType(), "workflow", workflowId, "workflow_definition", workflowId,
                workflow.getWorkspaceId(), payload);
        return asyncTaskService.getTask(task.getId());
    }

    /** Kafka Worker 中恢复请求并执行完整工作流。 */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        try {
            Map<String, Object> payload = objectMapper.readValue(task.getRequestPayload(), new TypeReference<>() { });
            String workflowId = String.valueOf(payload.get("workflowId"));
            WorkflowDtos.RunRequest request = objectMapper.convertValue(payload.get("request"), WorkflowDtos.RunRequest.class);
            asyncTaskService.updateProgress(task.getId(), "workflow_running", "工作流节点正在执行", 20,
                    Map.of("workflowId", workflowId));
            WorkflowDtos.RunResult result = workflowExecutionService.runWorkflow(workflowId, request, "async_task");
            if (!"SUCCESS".equalsIgnoreCase(result.getStatus())) {
                throw new IllegalStateException(result.getErrorMessage() == null ? "工作流运行失败" : result.getErrorMessage());
            }
            return Map.of("workflowId", workflowId, "workflowRunId", result.getWorkflowRunId(),
                    "runtimeRunId", result.getRuntimeRunId(), "status", result.getStatus(),
                    "output", result.getOutputText() == null ? "" : result.getOutputText(),
                    "totalTokens", result.getTotalTokens() == null ? 0 : result.getTotalTokens());
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("恢复工作流异步请求失败：" + exception.getMessage(), exception);
        }
    }
}

