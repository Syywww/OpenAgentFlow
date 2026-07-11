package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.RuntimeControlService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Agent Runtime 控制接口。 */
@RestController
@RequestMapping("/runs")
public class RuntimeControlController {

    /** Runtime控制服务。 */
    private final RuntimeControlService runtimeControlService;

    public RuntimeControlController(RuntimeControlService runtimeControlService) {
        this.runtimeControlService = runtimeControlService;
    }

    /** 请求停止仍在执行的对话或工作流运行。 */
    @PostMapping("/{runId}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable String runId) {
        return ApiResponse.ok(runtimeControlService.cancel(runId));
    }
}
