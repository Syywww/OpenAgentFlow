package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.memory.MemoryDtos;
import com.openagentflow.domain.task.AsyncTaskDtos;
import com.openagentflow.service.MemoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

import java.util.List;

/**
 * Memory 记忆中心接口。
 */
@RestController
@RequestMapping("/memories")
public class MemoryController {

    /** Memory 记忆中心服务。 */
    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 查询记忆中心概览。
     *
     * @return 记忆概览
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('memory:view','memory:manage')")
    public ApiResponse<MemoryDtos.Overview> overview() {
        return ApiResponse.ok(memoryService.overview());
    }

    /**
     * 分页查询记忆。
     *
     * @param memoryType 记忆类型
     * @param status 记忆状态
     * @param agentId Agent ID
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 记忆分页
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('memory:view','memory:manage')")
    public ApiResponse<PageResult<MemoryDtos.Summary>> list(@RequestParam(required = false) String memoryType,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String agentId,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(defaultValue = "1") Integer pageNo,
                                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(memoryService.listMemories(memoryType, status, agentId, keyword, pageNo, pageSize));
    }

    /**
     * 创建记忆。
     *
     * @param request 创建请求
     * @return 记忆摘要
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('memory:create','memory:manage')")
    public ApiResponse<MemoryDtos.Summary> create(@RequestBody MemoryDtos.SaveRequest request) {
        return ApiResponse.ok(memoryService.createMemory(request));
    }

    /**
     * 更新记忆。
     *
     * @param id 记忆 ID
     * @param request 更新请求
     * @return 记忆摘要
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('memory:update','memory:manage')")
    public ApiResponse<MemoryDtos.Summary> update(@PathVariable String id,
                                                  @RequestBody MemoryDtos.SaveRequest request) {
        return ApiResponse.ok(memoryService.updateMemory(id, request));
    }

    /**
     * 删除记忆。
     *
     * @param id 记忆 ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('memory:delete','memory:manage')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        memoryService.deleteMemory(id);
        return ApiResponse.ok(null);
    }

    /**
     * 测试记忆召回。
     *
     * @param request 召回请求
     * @return 召回结果
     */
    @PostMapping("/recall")
    @PreAuthorize("hasAnyAuthority('memory:recall','memory:manage')")
    public ApiResponse<List<MemoryDtos.RecallItem>> recall(@RequestBody MemoryDtos.RecallRequest request) {
        return ApiResponse.ok(memoryService.recall(request));
    }

    /**
     * 执行记忆清理。
     *
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasAuthority('memory:manage')")
    public ApiResponse<AsyncTaskDtos.Detail> cleanup() {
        return ApiResponse.ok(memoryService.submitCleanupTask());
    }

    /** 查询生产运营指标。 */
    @GetMapping("/production-overview")
    @PreAuthorize("hasAnyAuthority('memory:view','memory:manage')")
    public ApiResponse<Map<String, Object>> productionOverview() {
        return ApiResponse.ok(memoryService.productionOverview());
    }

    /** 查询Memory策略。 */
    @GetMapping("/policies")
    @PreAuthorize("hasAnyAuthority('memory:view','memory:policy','memory:manage')")
    public ApiResponse<List<Map<String, Object>>> policies() {
        return ApiResponse.ok(memoryService.listPolicies());
    }

    /** 保存Memory策略。 */
    @PostMapping("/policies")
    @PreAuthorize("hasAnyAuthority('memory:policy','memory:manage')")
    public ApiResponse<Map<String, Object>> savePolicy(@RequestBody Map<String, Object> request) {
        return ApiResponse.ok(memoryService.savePolicy(request));
    }

    /** 分页查询治理问题。 */
    @GetMapping("/governance/issues")
    @PreAuthorize("hasAnyAuthority('memory:governance','memory:manage')")
    public ApiResponse<PageResult<Map<String, Object>>> issues(@RequestParam(defaultValue = "open") String status,
                                                               @RequestParam(defaultValue = "all") String type,
                                                               @RequestParam(defaultValue = "1") Integer pageNo,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(memoryService.listGovernanceIssues(status, type, pageNo, pageSize));
    }

    /** 处置治理问题。 */
    @PutMapping("/governance/issues/{id}")
    @PreAuthorize("hasAnyAuthority('memory:governance','memory:manage')")
    public ApiResponse<Map<String, Object>> resolveIssue(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(memoryService.resolveGovernanceIssue(id, request));
    }

    /** 提交治理扫描。 */
    @PostMapping("/governance/scan")
    @PreAuthorize("hasAnyAuthority('memory:governance','memory:manage')")
    public ApiResponse<AsyncTaskDtos.Detail> scan() { return ApiResponse.ok(memoryService.submitGovernanceScan()); }

    /** 提交向量补偿重建。 */
    @PostMapping("/vectors/rebuild")
    @PreAuthorize("hasAnyAuthority('memory:governance','memory:manage')")
    public ApiResponse<AsyncTaskDtos.Detail> rebuildVectors() { return ApiResponse.ok(memoryService.submitVectorRebuild()); }

    /** 保存召回反馈。 */
    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasAnyAuthority('memory:feedback','memory:manage')")
    public ApiResponse<Void> feedback(@PathVariable String id, @RequestBody Map<String, Object> request) {
        memoryService.feedback(id, request);
        return ApiResponse.ok(null);
    }

    /** 按用户或业务主体执行一键遗忘。 */
    @DeleteMapping("/subjects/{subjectId}")
    @PreAuthorize("hasAnyAuthority('memory:forget','memory:manage')")
    public ApiResponse<Map<String, Object>> forget(@PathVariable String subjectId) {
        return ApiResponse.ok(Map.of("deleted", memoryService.forgetSubject(subjectId)));
    }
}
