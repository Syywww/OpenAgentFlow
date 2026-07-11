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
    public ApiResponse<List<MemoryDtos.RecallItem>> recall(@RequestBody MemoryDtos.RecallRequest request) {
        return ApiResponse.ok(memoryService.recall(request));
    }

    /**
     * 执行记忆清理。
     *
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    public ApiResponse<AsyncTaskDtos.Detail> cleanup() {
        return ApiResponse.ok(memoryService.submitCleanupTask());
    }
}
