package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.eval.EvaluationDtos;
import com.openagentflow.service.EvaluationService;
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
 * 模型评测接口。
 *
 * <p>提供评测集、样本导入、评测任务运行和结果查询能力。</p>
 */
@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

    /** 模型评测业务服务。 */
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * 查询评测集列表。
     *
     * @return 评测集摘要列表
     */
    @GetMapping("/datasets")
    public ApiResponse<List<EvaluationDtos.DatasetSummary>> listDatasets() {
        return ApiResponse.ok(evaluationService.listDatasets());
    }

    /**
     * 创建评测集。
     *
     * @param request 保存请求
     * @return 评测集详情
     */
    @PostMapping("/datasets")
    public ApiResponse<EvaluationDtos.DatasetDetail> createDataset(@Valid @RequestBody EvaluationDtos.DatasetRequest request) {
        return ApiResponse.ok(evaluationService.createDataset(request));
    }

    /**
     * 查询评测集详情。
     *
     * @param id 评测集 ID
     * @return 评测集详情
     */
    @GetMapping("/datasets/{id}")
    public ApiResponse<EvaluationDtos.DatasetDetail> getDataset(@PathVariable String id) {
        return ApiResponse.ok(evaluationService.getDataset(id));
    }

    /**
     * 更新评测集。
     *
     * @param id 评测集 ID
     * @param request 保存请求
     * @return 评测集详情
     */
    @PutMapping("/datasets/{id}")
    public ApiResponse<EvaluationDtos.DatasetDetail> updateDataset(@PathVariable String id,
                                                                    @Valid @RequestBody EvaluationDtos.DatasetRequest request) {
        return ApiResponse.ok(evaluationService.updateDataset(id, request));
    }

    /**
     * 删除评测集。
     *
     * @param id 评测集 ID
     * @return 空响应
     */
    @DeleteMapping("/datasets/{id}")
    public ApiResponse<Void> deleteDataset(@PathVariable String id) {
        evaluationService.deleteDataset(id);
        return ApiResponse.ok(null);
    }

    /**
     * 导入评测样本。
     *
     * @param id 评测集 ID
     * @param request 导入请求
     * @return 评测集详情
     */
    @PostMapping("/datasets/{id}/samples/import")
    public ApiResponse<EvaluationDtos.DatasetDetail> importSamples(@PathVariable String id,
                                                                    @Valid @RequestBody EvaluationDtos.SampleImportRequest request) {
        return ApiResponse.ok(evaluationService.importSamples(id, request));
    }

    /**
     * 创建并运行评测任务。
     *
     * @param request 运行请求
     * @return 评测任务详情
     */
    @PostMapping("/tasks/run")
    public ApiResponse<EvaluationDtos.TaskDetail> runTask(@Valid @RequestBody EvaluationDtos.RunTaskRequest request) {
        return ApiResponse.ok(evaluationService.runTask(request));
    }

    /**
     * 查询评测任务列表。
     *
     * @return 任务摘要列表
     */
    @GetMapping("/tasks")
    public ApiResponse<List<EvaluationDtos.TaskSummary>> listTasks() {
        return ApiResponse.ok(evaluationService.listTasks());
    }

    /**
     * 查询评测任务详情。
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    @GetMapping("/tasks/{id}")
    public ApiResponse<EvaluationDtos.TaskDetail> getTask(@PathVariable String id) {
        return ApiResponse.ok(evaluationService.getTask(id));
    }
}
