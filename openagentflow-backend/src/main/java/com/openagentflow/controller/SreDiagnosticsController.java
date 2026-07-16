package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.SreDiagnosticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 生产可观测性与 SRE 诊断接口。 */
@RestController
@RequestMapping("/sre")
@PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','ops:view')")
public class SreDiagnosticsController {

    /** SRE 诊断服务。 */
    private final SreDiagnosticsService service;

    public SreDiagnosticsController(SreDiagnosticsService service) { this.service = service; }

    /** 查询一次运行的完整资源消耗画像。 */
    @GetMapping("/runs/{runId}/resources")
    public ApiResponse<Map<String, Object>> runResources(@PathVariable String runId) {
        return ApiResponse.ok(service.runResourceSummary(runId));
    }

    /** 查询最近一小时黄金信号。 */
    @GetMapping("/golden-signals")
    public ApiResponse<Map<String, Object>> goldenSignals() {
        return ApiResponse.ok(service.goldenSignals());
    }
}
