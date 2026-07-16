package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.prompt.PromptDtos;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;
import com.openagentflow.service.PromptExperimentService;
import com.openagentflow.service.PromptOpsService;
import com.openagentflow.service.PromptTemplateService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prompt 模板中心接口。
 */
@RestController
@RequestMapping("/prompt-templates")
public class PromptTemplateController {

    /** Prompt 模板中心服务。 */
    private final PromptTemplateService promptTemplateService;
    /** PromptOps 治理服务。 */
    private final PromptOpsService promptOpsService;
    /** Prompt 实验服务。 */
    private final PromptExperimentService promptExperimentService;

    public PromptTemplateController(PromptTemplateService promptTemplateService,
                                    PromptOpsService promptOpsService,
                                    PromptExperimentService promptExperimentService) {
        this.promptTemplateService = promptTemplateService;
        this.promptOpsService = promptOpsService;
        this.promptExperimentService = promptExperimentService;
    }

    /**
     * 查询 Prompt 模板中心概览。
     *
     * @return 概览指标
     */
    @GetMapping("/overview")
    public ApiResponse<PromptDtos.Overview> getOverview() {
        return ApiResponse.ok(promptTemplateService.getOverview());
    }

    /**
     * 分页查询 Prompt 模板。
     *
     * @param promptType Prompt 类型
     * @param status 模板状态
     * @param keyword 搜索关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 模板分页
     */
    @GetMapping
    public ApiResponse<PageResult<PromptDtos.TemplateSummary>> listTemplates(@RequestParam(defaultValue = "all") String promptType,
                                                                              @RequestParam(defaultValue = "all") String status,
                                                                              @RequestParam(required = false) String keyword,
                                                                              @RequestParam(defaultValue = "1") Integer pageNo,
                                                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(promptTemplateService.listTemplates(promptType, status, keyword, pageNo, pageSize));
    }

    /**
     * 查询 Prompt 模板详情。
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PromptDtos.TemplateDetail> getTemplate(@PathVariable String id) {
        return ApiResponse.ok(promptTemplateService.getTemplate(id));
    }

    /**
     * 创建 Prompt 模板。
     *
     * @param request 保存请求
     * @return 模板详情
     */
    @PostMapping
    public ApiResponse<PromptDtos.TemplateDetail> createTemplate(@RequestBody PromptDtos.TemplateRequest request) {
        return ApiResponse.ok(promptTemplateService.createTemplate(request));
    }

    /**
     * 更新 Prompt 模板。
     *
     * @param id 模板ID
     * @param request 保存请求
     * @return 模板详情
     */
    @PutMapping("/{id}")
    public ApiResponse<PromptDtos.TemplateDetail> updateTemplate(@PathVariable String id,
                                                                 @RequestBody PromptDtos.TemplateRequest request) {
        return ApiResponse.ok(promptTemplateService.updateTemplate(id, request));
    }

    /**
     * 删除 Prompt 模板。
     *
     * @param id 模板ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable String id) {
        promptTemplateService.deleteTemplate(id);
        return ApiResponse.ok(null);
    }

    /**
     * 发布 Prompt 模板版本。
     *
     * @param id 模板ID
     * @param request 发布请求
     * @return 模板详情
     */
    @PostMapping("/{id}/publish")
    public ApiResponse<PromptDtos.TemplateDetail> publishTemplate(@PathVariable String id,
                                                                  @RequestBody(required = false) PromptDtos.PublishRequest request) {
        return ApiResponse.ok(promptTemplateService.publishTemplate(id, request));
    }

    /**
     * 复制 Prompt 模板。
     *
     * @param id 模板ID
     * @param request 复制请求
     * @return 新模板详情
     */
    @PostMapping("/{id}/copy")
    public ApiResponse<PromptDtos.TemplateDetail> copyTemplate(@PathVariable String id,
                                                               @RequestBody(required = false) PromptDtos.CopyRequest request) {
        return ApiResponse.ok(promptTemplateService.copyTemplate(id, request));
    }

    /**
     * 回滚 Prompt 模板到指定版本。
     *
     * @param id 模板ID
     * @param versionId 版本ID
     * @return 模板详情
     */
    @PostMapping("/{id}/versions/{versionId}/rollback")
    public ApiResponse<PromptDtos.TemplateDetail> rollbackTemplate(@PathVariable String id,
                                                                   @PathVariable String versionId) {
        return ApiResponse.ok(promptTemplateService.rollbackTemplate(id, versionId));
    }

    /** 使用统一 Prompt Runtime 预览最终装配内容。 */
    @PostMapping("/{id}/preview")
    public ApiResponse<PromptRuntimeDtos.CompileResult> preview(@PathVariable String id,
                                                                @RequestBody(required = false) PromptRuntimeDtos.PreviewRequest request) {
        return ApiResponse.ok(promptOpsService.preview(id, request));
    }

    /** 对比两个 Prompt 版本的内容和变量 Schema。 */
    @GetMapping("/{id}/diff")
    public ApiResponse<PromptRuntimeDtos.VersionDiff> diff(@PathVariable String id,
                                                           @RequestParam String fromVersionId,
                                                           @RequestParam String toVersionId) {
        return ApiResponse.ok(promptOpsService.diff(id, fromVersionId, toVersionId));
    }

    /** 查询 Prompt 模板的资源绑定影响面。 */
    @GetMapping("/{id}/impacts")
    public ApiResponse<java.util.List<PromptRuntimeDtos.ImpactItem>> impacts(@PathVariable String id) {
        return ApiResponse.ok(promptOpsService.impacts(id));
    }

    /** 将 Prompt 版本晋级到指定环境。 */
    @PostMapping("/{id}/releases")
    public ApiResponse<PromptRuntimeDtos.EnvironmentRelease> promote(@PathVariable String id,
                                                                     @RequestBody PromptRuntimeDtos.PromotionRequest request) {
        return ApiResponse.ok(promptOpsService.promote(id, request));
    }

    /** 查询 Prompt 多环境发布历史。 */
    @GetMapping("/{id}/releases")
    public ApiResponse<java.util.List<PromptRuntimeDtos.EnvironmentRelease>> releases(@PathVariable String id) {
        return ApiResponse.ok(promptOpsService.releases(id));
    }

    /** 查询 Prompt 版本在线运行指标。 */
    @GetMapping("/{id}/metrics")
    public ApiResponse<java.util.List<PromptRuntimeDtos.VersionMetric>> metrics(@PathVariable String id) {
        return ApiResponse.ok(promptOpsService.metrics(id));
    }

    /** 查询模板下的 Prompt 实验。 */
    @GetMapping("/{id}/experiments")
    public ApiResponse<java.util.List<PromptRuntimeDtos.ExperimentSummary>> listExperiments(@PathVariable String id) {
        return ApiResponse.ok(promptExperimentService.list(id));
    }

    /** 创建 Prompt 实验草稿。 */
    @PostMapping("/{id}/experiments")
    public ApiResponse<PromptRuntimeDtos.ExperimentSummary> createExperiment(@PathVariable String id,
                                                                             @RequestBody PromptRuntimeDtos.ExperimentRequest request) {
        return ApiResponse.ok(promptExperimentService.create(id, request));
    }

    /** 更新 Prompt 实验草稿。 */
    @PutMapping("/{id}/experiments/{experimentId}")
    public ApiResponse<PromptRuntimeDtos.ExperimentSummary> updateExperiment(@PathVariable String id,
                                                                             @PathVariable String experimentId,
                                                                             @RequestBody PromptRuntimeDtos.ExperimentRequest request) {
        return ApiResponse.ok(promptExperimentService.update(id, experimentId, request));
    }

    /** 启动 Prompt 实验。 */
    @PostMapping("/{id}/experiments/{experimentId}/start")
    public ApiResponse<PromptRuntimeDtos.ExperimentSummary> startExperiment(@PathVariable String id,
                                                                            @PathVariable String experimentId) {
        return ApiResponse.ok(promptExperimentService.start(id, experimentId));
    }

    /** 停止 Prompt 实验。 */
    @PostMapping("/{id}/experiments/{experimentId}/stop")
    public ApiResponse<PromptRuntimeDtos.ExperimentSummary> stopExperiment(@PathVariable String id,
                                                                           @PathVariable String experimentId) {
        return ApiResponse.ok(promptExperimentService.stop(id, experimentId));
    }

    /** 手动指定 Prompt 实验胜出变体。 */
    @PostMapping("/{id}/experiments/{experimentId}/winner")
    public ApiResponse<PromptRuntimeDtos.ExperimentSummary> chooseWinner(@PathVariable String id,
                                                                         @PathVariable String experimentId,
                                                                         @RequestParam String variantId) {
        return ApiResponse.ok(promptExperimentService.chooseWinner(id, experimentId, variantId));
    }

    /** 根据样本量和质量指标自动选择 Prompt 实验胜出变体。 */
    @PostMapping("/{id}/experiments/{experimentId}/auto-winner")
    public ApiResponse<PromptRuntimeDtos.ExperimentSummary> autoChooseWinner(@PathVariable String id,
                                                                             @PathVariable String experimentId) {
        return ApiResponse.ok(promptExperimentService.autoChooseWinner(id, experimentId));
    }

    /** 删除已停止的 Prompt 实验。 */
    @DeleteMapping("/{id}/experiments/{experimentId}")
    public ApiResponse<Void> deleteExperiment(@PathVariable String id, @PathVariable String experimentId) {
        promptExperimentService.delete(id, experimentId);
        return ApiResponse.ok(null);
    }
}
