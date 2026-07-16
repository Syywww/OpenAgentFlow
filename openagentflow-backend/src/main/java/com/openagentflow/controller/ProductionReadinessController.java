package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.ProductionReadinessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 生产容量与灾备目标接口。 */
@RestController
@RequestMapping("/production-readiness")
@PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','ops:manage')")
public class ProductionReadinessController {
    private final ProductionReadinessService service;
    public ProductionReadinessController(ProductionReadinessService service) { this.service = service; }
    /** 保存容量基线。 */
    @PostMapping("/capacity-baselines")
    public ApiResponse<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(service.saveCapacityBaseline(body));
    }
    /** 查询容量基线。 */
    @GetMapping("/capacity-baselines")
    public ApiResponse<List<Map<String, Object>>> capacity() { return ApiResponse.ok(service.capacityBaselines()); }
    /** 查询灾备 RPO/RTO 目标。 */
    @GetMapping("/disaster-recovery-targets")
    public ApiResponse<List<Map<String, Object>>> drTargets() { return ApiResponse.ok(service.disasterRecoveryTargets()); }
}
