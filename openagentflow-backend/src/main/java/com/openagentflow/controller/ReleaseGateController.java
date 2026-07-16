package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.ReleaseGateService;
import com.openagentflow.service.ContinuousEvaluationService;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.Map;

/** 发布质量门禁接口。 */
@RestController
@RequestMapping("/release-gates")
public class ReleaseGateController {
    private final ReleaseGateService service;
    private final ContinuousEvaluationService continuousEvaluationService;
    public ReleaseGateController(ReleaseGateService service, ContinuousEvaluationService continuousEvaluationService) {
        this.service = service;
        this.continuousEvaluationService = continuousEvaluationService;
    }
    /** 手动执行门禁。 */
    @PostMapping("/check")
    public ApiResponse<Map<String,Object>> check(@RequestParam String resourceType,@RequestParam String resourceId,
                                                  @RequestParam(required=false) String workspaceId,@RequestParam(required=false) String version) {
        String trustedWorkspace = WorkspaceContextHolder.current() == null ? workspaceId : WorkspaceContextHolder.current();
        return ApiResponse.ok(service.assertCanRelease(resourceType,resourceId,trustedWorkspace,version));
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

    /** 将成功评测任务设置为资源黄金基线。 */
    @PostMapping("/baselines")
    @PreAuthorize("hasAnyAuthority('ROLE_super_admin','ROLE_admin','evaluation:manage')")
    public ApiResponse<Map<String,Object>> createBaseline(@RequestBody Map<String,Object> body,
                                                           @AuthenticationPrincipal AuthUserDetails user) {
        return ApiResponse.ok(continuousEvaluationService.createBaseline(
                String.valueOf(body.get("resourceType")), String.valueOf(body.get("resourceId")),
                WorkspaceContextHolder.current(),
                String.valueOf(body.get("evalTaskId")), String.valueOf(body.get("baselineName")),
                body.get("resourceVersion") == null ? null : String.valueOf(body.get("resourceVersion")),
                user == null ? null : user.getUserId()));
    }

    /** 查询资源历次黄金基线。 */
    @GetMapping("/baselines")
    public ApiResponse<List<Map<String,Object>>> baselines(@RequestParam String resourceType,
                                                            @RequestParam String resourceId) {
        return ApiResponse.ok(continuousEvaluationService.listBaselines(resourceType, resourceId));
    }
}
