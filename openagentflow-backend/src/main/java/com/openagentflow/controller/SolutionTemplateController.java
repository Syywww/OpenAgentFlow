package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.template.TemplateDtos;
import com.openagentflow.service.SolutionTemplateService;
import com.openagentflow.service.TemplateInstallService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 企业解决方案模板广场、发布审核、安装升级和运营接口。 */
@RestController
@RequestMapping("/templates")
public class SolutionTemplateController {

    /** 模板发布与运营服务。 */ private final SolutionTemplateService templateService;
    /** 模板安装与升级服务。 */ private final TemplateInstallService installService;

    public SolutionTemplateController(SolutionTemplateService templateService,
                                      TemplateInstallService installService) {
        this.templateService = templateService;
        this.installService = installService;
    }

    /** 查询模板广场运营概览。 */
    @GetMapping("/overview")
    public ApiResponse<TemplateDtos.Overview> overview() {
        return ApiResponse.ok(templateService.overview());
    }

    /** 分页查询公开模板。 */
    @GetMapping
    public ApiResponse<PageResult<TemplateDtos.TemplateSummary>> list(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "false") boolean favoriteOnly,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(templateService.listPublic(category, keyword, sort, favoriteOnly, pageNo, pageSize));
    }

    /** 查询模板完整详情。 */
    @GetMapping("/{id}")
    public ApiResponse<TemplateDtos.TemplateDetail> detail(@PathVariable String id) {
        return ApiResponse.ok(templateService.detail(id));
    }

    /** 查询模板作者公开主页。 */
    @GetMapping("/authors/{userId}")
    public ApiResponse<TemplateDtos.AuthorProfile> author(@PathVariable String userId) {
        return ApiResponse.ok(templateService.authorProfile(userId));
    }

    /** 收藏或取消收藏模板。 */
    @PostMapping("/{id}/favorite")
    public ApiResponse<Boolean> toggleFavorite(@PathVariable String id) {
        return ApiResponse.ok(templateService.toggleFavorite(id));
    }

    /** 成功安装用户提交评分评论。 */
    @PutMapping("/{id}/rating")
    public ApiResponse<Void> rate(@PathVariable String id, @RequestBody TemplateDtos.RatingRequest request) {
        templateService.rate(id, request);
        return ApiResponse.ok(null);
    }

    /** 模板作者或管理员回复评论。 */
    @PostMapping("/{id}/comments/{commentId}/reply")
    public ApiResponse<Void> reply(@PathVariable String id,
                                   @PathVariable String commentId,
                                   @RequestBody TemplateDtos.ReplyRequest request) {
        templateService.reply(id, commentId, request);
        return ApiResponse.ok(null);
    }

    /** 举报公开模板。 */
    @PostMapping("/{id}/reports")
    public ApiResponse<Void> report(@PathVariable String id, @RequestBody TemplateDtos.ReportRequest request) {
        templateService.report(id, request);
        return ApiResponse.ok(null);
    }

    /** 创建Kafka异步安装任务。 */
    @PostMapping("/{id}/install")
    public ApiResponse<TemplateDtos.InstallSummary> install(@PathVariable String id,
                                                            @RequestBody TemplateDtos.InstallRequest request) {
        return ApiResponse.ok(installService.install(id, request));
    }

    /** 查询当前用户安装实例。 */
    @GetMapping("/installs/mine")
    public ApiResponse<List<TemplateDtos.InstallSummary>> installs() {
        return ApiResponse.ok(installService.listMine());
    }

    /** 查询安装进度。 */
    @GetMapping("/installs/{installId}")
    public ApiResponse<TemplateDtos.InstallSummary> installDetail(@PathVariable String installId) {
        return ApiResponse.ok(installService.getInstall(installId));
    }

    /** 生成三方升级差异。 */
    @PostMapping("/installs/{installId}/upgrade-preview")
    public ApiResponse<List<TemplateDtos.UpgradeConflict>> prepareUpgrade(
            @PathVariable String installId,
            @RequestParam String targetVersionId) {
        return ApiResponse.ok(installService.prepareUpgrade(installId, targetVersionId));
    }

    /** 查询已生成的三方升级冲突。 */
    @GetMapping("/installs/{installId}/conflicts")
    public ApiResponse<List<TemplateDtos.UpgradeConflict>> conflicts(
            @PathVariable String installId,
            @RequestParam String targetVersionId) {
        return ApiResponse.ok(installService.conflicts(installId, targetVersionId));
    }

    /** 提交冲突选择并创建异步升级任务。 */
    @PostMapping("/installs/{installId}/upgrade")
    public ApiResponse<TemplateDtos.InstallSummary> upgrade(
            @PathVariable String installId,
            @RequestBody TemplateDtos.UpgradeRequest request) {
        return ApiResponse.ok(installService.upgrade(installId, request));
    }

    /** 解除模板关联，可选删除未修改资源。 */
    @PostMapping("/installs/{installId}/uninstall")
    public ApiResponse<Void> uninstall(@PathVariable String installId,
                                       @RequestBody(required = false) TemplateDtos.UninstallRequest request) {
        installService.uninstall(installId, request == null ? new TemplateDtos.UninstallRequest() : request);
        return ApiResponse.ok(null);
    }

    /** 查询当前用户或工作空间管理的模板。 */
    @GetMapping("/manage/mine")
    public ApiResponse<List<TemplateDtos.TemplateSummary>> managed() {
        return ApiResponse.ok(templateService.listManaged());
    }

    /** 创建工作空间私有模板。 */
    @PostMapping("/manage")
    public ApiResponse<TemplateDtos.TemplateDetail> create(@RequestBody TemplateDtos.TemplateRequest request) {
        return ApiResponse.ok(templateService.create(request));
    }

    /** 更新模板基础信息。 */
    @PutMapping("/manage/{id}")
    public ApiResponse<TemplateDtos.TemplateDetail> update(@PathVariable String id,
                                                           @RequestBody TemplateDtos.TemplateRequest request) {
        return ApiResponse.ok(templateService.update(id, request));
    }

    /** 删除未公开模板。 */
    @DeleteMapping("/manage/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        templateService.delete(id);
        return ApiResponse.ok(null);
    }

    /** 分析入口Agent和团队的完整依赖。 */
    @PostMapping("/manage/dependencies")
    public ApiResponse<List<TemplateDtos.ResourceReference>> dependencies(@RequestBody TemplateDtos.PublishRequest request) {
        return ApiResponse.ok(templateService.analyzeDependencies(request));
    }

    /** 创建不可变模板版本并执行自动发布检查。 */
    @PostMapping("/manage/{id}/versions")
    public ApiResponse<TemplateDtos.VersionSummary> publishVersion(@PathVariable String id,
                                                                   @RequestBody TemplateDtos.PublishRequest request) {
        return ApiResponse.ok(templateService.publishVersion(id, request));
    }

    /** 查询待人工审核版本。 */
    @GetMapping("/reviews/pending")
    public ApiResponse<List<TemplateDtos.VersionSummary>> pendingReviews() {
        return ApiResponse.ok(templateService.pendingReviews());
    }

    /** 管理员人工审核公开版本。 */
    @PostMapping("/reviews/{versionId}")
    public ApiResponse<TemplateDtos.VersionSummary> review(@PathVariable String versionId,
                                                           @RequestBody TemplateDtos.ReviewRequest request) {
        return ApiResponse.ok(templateService.review(versionId, request));
    }

    /** 运营设置推荐或上下架。 */
    @PutMapping("/operations/{id}")
    public ApiResponse<TemplateDtos.TemplateDetail> operate(@PathVariable String id,
                                                            @RequestBody Map<String, Object> request) {
        Boolean recommended = request.get("recommended") instanceof Boolean value ? value : null;
        String status = request.get("status") == null ? null : String.valueOf(request.get("status"));
        return ApiResponse.ok(templateService.operate(id, recommended, status));
    }

    /** 查询模板举报治理队列。 */
    @GetMapping("/operations/reports")
    public ApiResponse<List<TemplateDtos.ReportSummary>> reports(
            @RequestParam(defaultValue = "pending") String status) {
        return ApiResponse.ok(templateService.reports(status));
    }

    /** 完成举报处置，并可同步下架风险模板。 */
    @PostMapping("/operations/reports/{reportId}/resolve")
    public ApiResponse<TemplateDtos.ReportSummary> resolveReport(
            @PathVariable String reportId,
            @RequestBody TemplateDtos.ReportResolutionRequest request) {
        return ApiResponse.ok(templateService.resolveReport(reportId, request));
    }
}
