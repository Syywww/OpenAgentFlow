package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.PrivacyComplianceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 隐私同意与数据主体权利接口。 */
@RestController
@RequestMapping("/compliance/privacy")
public class PrivacyComplianceController {

    /** 隐私合规服务。 */
    private final PrivacyComplianceService service;

    public PrivacyComplianceController(PrivacyComplianceService service) { this.service = service; }

    /** 授予隐私同意。 */
    @PostMapping("/consents")
    public ApiResponse<Map<String, Object>> grant(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked") Map<String, Object> evidence = body.get("evidence") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        return ApiResponse.ok(service.grantConsent(String.valueOf(body.get("purposeCode")),
                String.valueOf(body.get("consentVersion")), evidence));
    }

    /** 撤回隐私同意。 */
    @PostMapping("/consents/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable String id) { service.withdrawConsent(id); return ApiResponse.ok(null); }

    /** 创建数据主体申请。 */
    @PostMapping("/subject-requests")
    public ApiResponse<Map<String, Object>> createRequest(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked") Map<String, Object> scope = body.get("scope") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        return ApiResponse.ok(service.createSubjectRequest(String.valueOf(body.get("requestType")), scope));
    }

    /** 查询当前用户的数据主体申请。 */
    @GetMapping("/subject-requests")
    public ApiResponse<List<Map<String, Object>>> requests() { return ApiResponse.ok(service.subjectRequests()); }
}
