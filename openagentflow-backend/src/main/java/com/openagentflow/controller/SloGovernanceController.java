package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.SloGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 平台SLO治理接口。 */
@RestController
@RequestMapping("/ops/slo")
public class SloGovernanceController {

    /** SLO治理服务。 */
    private final SloGovernanceService service;

    public SloGovernanceController(SloGovernanceService service) { this.service = service; }

    /** 查询SLO总览。 */
    @GetMapping
    public ApiResponse<Map<String, Object>> overview() { return ApiResponse.ok(service.overview()); }

    /** 立即执行一轮SLO计算。 */
    @PostMapping("/evaluate")
    public ApiResponse<Map<String, Object>> evaluate() {
        service.evaluate();
        return ApiResponse.ok(service.overview());
    }
}
