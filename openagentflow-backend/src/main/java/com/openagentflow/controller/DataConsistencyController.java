package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.DataConsistencyService;
import com.openagentflow.service.AsyncTaskService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** 数据一致性治理接口。 */
@RestController
@RequestMapping("/governance/consistency")
public class DataConsistencyController {
    private final DataConsistencyService service;
    private final AsyncTaskService asyncTaskService;
    public DataConsistencyController(DataConsistencyService service, AsyncTaskService asyncTaskService) {
        this.service = service;
        this.asyncTaskService = asyncTaskService;
    }
    /** 查询跨存储一致性问题。 */
    @GetMapping public ApiResponse<List<Map<String,Object>>> issues() { return ApiResponse.ok(service.issues()); }
    /** 立即执行一致性巡检。 */
    @PostMapping("/scan") public ApiResponse<List<Map<String,Object>>> scan() { service.scan(); return ApiResponse.ok(service.issues()); }
    /** 将一致性修复投递到Kafka Worker。 */
    @PostMapping("/issues/{issueId}/repair")
    public ApiResponse<Map<String,Object>> repair(@PathVariable String issueId) {
        var task = asyncTaskService.createTask("修复数据一致性问题", "DATA_CONSISTENCY_REPAIR", "consistency_issue",
                issueId, "data_consistency_issue", issueId, null, Map.of("issueId", issueId));
        return ApiResponse.ok(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }
}
