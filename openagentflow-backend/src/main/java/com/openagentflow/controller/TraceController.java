package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.trace.RunDetail;
import com.openagentflow.domain.trace.RunStats;
import com.openagentflow.domain.trace.RunSummary;
import com.openagentflow.domain.trace.TraceStepDetail;
import com.openagentflow.service.TraceService;
import com.openagentflow.service.RuntimeEventStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 运行日志与 Trace 查询接口。
 */
@RestController
@RequestMapping("/runs")
public class TraceController {

    /** Trace 查询服务。 */
    private final TraceService traceService;

    /** Runtime事件续传服务。 */
    private final RuntimeEventStreamService runtimeEventStreamService;

    public TraceController(TraceService traceService, RuntimeEventStreamService runtimeEventStreamService) {
        this.traceService = traceService;
        this.runtimeEventStreamService = runtimeEventStreamService;
    }

    /**
     * 分页查询运行记录。
     *
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @param status 状态筛选
     * @param agentId Agent 筛选
     * @param keyword 关键词
     * @return 分页运行记录
     */
    @GetMapping
    public ApiResponse<PageResult<RunSummary>> listRuns(@RequestParam(defaultValue = "1") Integer pageNo,
                                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String agentId,
                                                        @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(traceService.listRuns(pageNo, pageSize, status, agentId, keyword));
    }

    /**
     * 查询运行基础统计。
     *
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<RunStats> getRunStats() {
        return ApiResponse.ok(traceService.getRunStats());
    }

    /** 使用签名游标查询海量运行数据。 */
    @GetMapping("/cursor")
    public ApiResponse<Map<String, Object>> listRunsByCursor(@RequestParam(required = false) String cursor,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String agentId) {
        return ApiResponse.ok(traceService.listRunsByCursor(cursor, pageSize, status, agentId));
    }

    /**
     * 查询运行详情。
     *
     * @param runId 运行 ID
     * @return 运行详情
     */
    @GetMapping("/{runId}")
    public ApiResponse<RunDetail> getRunDetail(@PathVariable String runId) {
        return ApiResponse.ok(traceService.getRunDetail(runId));
    }

    /**
     * 查询运行步骤。
     *
     * @param runId 运行 ID
     * @return 步骤列表
     */
    @GetMapping("/{runId}/steps")
    public ApiResponse<List<TraceStepDetail>> listRunSteps(@PathVariable String runId) {
        return ApiResponse.ok(traceService.listRunSteps(runId));
    }

    /**
     * 使用事件序号恢复中断的Runtime SSE流。
     */
    @GetMapping(value = "/{runId}/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter resumeEvents(@PathVariable String runId,
                                   @RequestParam(defaultValue = "0") Long after,
                                   @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId) {
        return runtimeEventStreamService.resume(runId, lastEventId == null ? after : lastEventId);
    }
}
