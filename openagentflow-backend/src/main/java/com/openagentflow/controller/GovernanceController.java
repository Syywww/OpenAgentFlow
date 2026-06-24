package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.governance.GovernanceDtos;
import com.openagentflow.service.GovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审计与风险治理中心接口。
 */
@RestController
@RequestMapping("/governance")
public class GovernanceController {

    /** 审计与风险治理中心服务。 */
    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    /**
     * 查询治理概览。
     *
     * @return 治理概览
     */
    @GetMapping("/overview")
    public ApiResponse<GovernanceDtos.Overview> overview() {
        return ApiResponse.ok(governanceService.overview());
    }

    /**
     * 查询审计日志列表。
     *
     * @param success 成功状态
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 审计日志分页
     */
    @GetMapping("/audits")
    public ApiResponse<PageResult<GovernanceDtos.AuditItem>> listAudits(@RequestParam(required = false) Boolean success,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(defaultValue = "1") Integer pageNo,
                                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(governanceService.listAudits(success, keyword, pageNo, pageSize));
    }

    /**
     * 查询风险治理事件列表。
     *
     * @param status 处置状态
     * @param riskLevel 风险级别
     * @param eventType 事件类型
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 风险事件分页
     */
    @GetMapping("/risks")
    public ApiResponse<PageResult<GovernanceDtos.RiskItem>> listRisks(@RequestParam(required = false) String status,
                                                                      @RequestParam(required = false) String riskLevel,
                                                                      @RequestParam(required = false) String eventType,
                                                                      @RequestParam(required = false) String keyword,
                                                                      @RequestParam(defaultValue = "1") Integer pageNo,
                                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(governanceService.listRisks(status, riskLevel, eventType, keyword, pageNo, pageSize));
    }

    /**
     * 查询高风险确认请求。
     *
     * @param status 确认状态
     * @return 确认请求列表
     */
    @GetMapping("/confirmations")
    public ApiResponse<List<GovernanceDtos.ConfirmationItem>> listConfirmations(@RequestParam(required = false) String status) {
        return ApiResponse.ok(governanceService.listConfirmations(status));
    }

    /**
     * 处置风险事件。
     *
     * @param id 风险事件ID
     * @param request 处置请求
     * @return 处置后的风险事件
     */
    @PostMapping("/risks/{id}/handle")
    public ApiResponse<GovernanceDtos.RiskItem> handleRisk(@PathVariable String id,
                                                           @RequestBody GovernanceDtos.HandleRiskRequest request) {
        return ApiResponse.ok(governanceService.handleRisk(id, request));
    }

    /**
     * 通过高风险确认请求。
     *
     * @param id 确认请求ID
     * @param payload 请求体
     * @return 确认请求
     */
    @PostMapping("/confirmations/{id}/approve")
    public ApiResponse<GovernanceDtos.ConfirmationItem> approveConfirmation(@PathVariable String id,
                                                                            @RequestBody(required = false) Map<String, String> payload) {
        return ApiResponse.ok(governanceService.decideConfirmation(id, true, payload == null ? "" : payload.get("note")));
    }

    /**
     * 拒绝高风险确认请求。
     *
     * @param id 确认请求ID
     * @param payload 请求体
     * @return 确认请求
     */
    @PostMapping("/confirmations/{id}/reject")
    public ApiResponse<GovernanceDtos.ConfirmationItem> rejectConfirmation(@PathVariable String id,
                                                                           @RequestBody(required = false) Map<String, String> payload) {
        return ApiResponse.ok(governanceService.decideConfirmation(id, false, payload == null ? "" : payload.get("note")));
    }
}
