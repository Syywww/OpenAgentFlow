package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.TenantIsolationAuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 全局租户隔离巡检接口。 */
@RestController
@RequestMapping("/tenant-isolation")
@PreAuthorize("hasAuthority('ROLE_super_admin')")
public class TenantIsolationAuditController {
    private final TenantIsolationAuditService service;
    public TenantIsolationAuditController(TenantIsolationAuditService service) { this.service = service; }
    /** 执行全局租户隔离扫描。 */
    @PostMapping("/scan")
    public ApiResponse<List<Map<String, Object>>> scan() { return ApiResponse.ok(service.scan()); }
    /** 查询未处理隔离问题。 */
    @GetMapping("/issues")
    public ApiResponse<List<Map<String, Object>>> issues() { return ApiResponse.ok(service.openIssues()); }
}
