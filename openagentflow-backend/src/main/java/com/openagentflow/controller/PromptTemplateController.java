package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.prompt.PromptDtos;
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

    public PromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
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
}
