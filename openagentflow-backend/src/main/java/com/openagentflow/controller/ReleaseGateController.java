package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.ReleaseGateService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;

/** 发布质量门禁接口。 */
@RestController
@RequestMapping("/release-gates")
public class ReleaseGateController {
    private final ReleaseGateService service;
    public ReleaseGateController(ReleaseGateService service) { this.service = service; }
    /** 手动执行门禁。 */
    @PostMapping("/check")
    public ApiResponse<Map<String,Object>> check(@RequestParam String resourceType,@RequestParam String resourceId,
                                                  @RequestParam(required=false) String workspaceId,@RequestParam(required=false) String version) {
        return ApiResponse.ok(service.assertCanRelease(resourceType,resourceId,workspaceId,version));
    }
    /** 查询门禁执行数据。 */
    @GetMapping
    public ApiResponse<List<Map<String,Object>>> executions(@RequestParam String resourceType,@RequestParam String resourceId) {
        return ApiResponse.ok(service.executions(resourceType,resourceId));
    }
    /** 创建限时发布豁免申请。 */
    @PostMapping("/waivers")
    public ApiResponse<Map<String,Object>> createWaiver(@RequestParam String resourceType,@RequestParam String resourceId,
                                                         @RequestParam String reason,@RequestParam(required=false) Integer hours) {
        return ApiResponse.ok(service.createWaiver(resourceType, resourceId, reason, hours));
    }
    /** 审批发布豁免申请。 */
    @PostMapping("/waivers/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','risk:manage')")
    public ApiResponse<Map<String,Object>> approveWaiver(@PathVariable String id) {
        return ApiResponse.ok(service.approveWaiver(id));
    }
}
