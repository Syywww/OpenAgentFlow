package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.ops.OpsMonitorDtos;
import com.openagentflow.service.OpsMonitorService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运营监控与告警中心接口。
 */
@RestController
@RequestMapping("/ops-monitor")
public class OpsMonitorController {

    /** 运营监控与告警中心服务。 */
    private final OpsMonitorService opsMonitorService;

    public OpsMonitorController(OpsMonitorService opsMonitorService) {
        this.opsMonitorService = opsMonitorService;
    }

    /**
     * 查询运营监控总览。
     *
     * @return 运营监控总览
     */
    @GetMapping("/overview")
    public ApiResponse<OpsMonitorDtos.Overview> overview() {
        return ApiResponse.ok(opsMonitorService.overview());
    }

    /**
     * 查询健康矩阵。
     *
     * @return 健康组件列表
     */
    @GetMapping("/health")
    public ApiResponse<List<OpsMonitorDtos.HealthItem>> healthMatrix() {
        return ApiResponse.ok(opsMonitorService.healthMatrix());
    }

    /**
     * 手动执行巡检。
     *
     * @return 最新健康组件列表
     */
    @PostMapping("/inspect")
    public ApiResponse<List<OpsMonitorDtos.HealthItem>> runInspection() {
        return ApiResponse.ok(opsMonitorService.runInspection());
    }

    /**
     * 分页查询告警规则。
     *
     * @param enabled 是否启用
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 告警规则分页
     */
    @GetMapping("/rules")
    public ApiResponse<PageResult<OpsMonitorDtos.AlertRuleSummary>> listRules(@RequestParam(required = false) Boolean enabled,
                                                                              @RequestParam(required = false) String keyword,
                                                                              @RequestParam(defaultValue = "1") Integer pageNo,
                                                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(opsMonitorService.listRules(enabled, keyword, pageNo, pageSize));
    }

    /**
     * 创建告警规则。
     *
     * @param request 告警规则请求
     * @return 告警规则摘要
     */
    @PostMapping("/rules")
    public ApiResponse<OpsMonitorDtos.AlertRuleSummary> createRule(@RequestBody OpsMonitorDtos.AlertRuleRequest request) {
        return ApiResponse.ok(opsMonitorService.createRule(request));
    }

    /**
     * 更新告警规则。
     *
     * @param id 告警规则ID
     * @param request 告警规则请求
     * @return 告警规则摘要
     */
    @PutMapping("/rules/{id}")
    public ApiResponse<OpsMonitorDtos.AlertRuleSummary> updateRule(@PathVariable String id,
                                                                   @RequestBody OpsMonitorDtos.AlertRuleRequest request) {
        return ApiResponse.ok(opsMonitorService.updateRule(id, request));
    }

    /**
     * 删除告警规则。
     *
     * @param id 告警规则ID
     * @return 空响应
     */
    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable String id) {
        opsMonitorService.deleteRule(id);
        return ApiResponse.ok(null);
    }

    /**
     * 分页查询告警事件。
     *
     * @param status 告警状态
     * @param severity 告警级别
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 告警事件分页
     */
    @GetMapping("/events")
    public ApiResponse<PageResult<OpsMonitorDtos.AlertEventSummary>> listEvents(@RequestParam(required = false) String status,
                                                                                @RequestParam(required = false) String severity,
                                                                                @RequestParam(required = false) String keyword,
                                                                                @RequestParam(defaultValue = "1") Integer pageNo,
                                                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(opsMonitorService.listEvents(status, severity, keyword, pageNo, pageSize));
    }

    /**
     * 处理告警事件。
     *
     * @param id 告警事件ID
     * @param request 处理请求
     * @return 告警事件摘要
     */
    @PostMapping("/events/{id}/handle")
    public ApiResponse<OpsMonitorDtos.AlertEventSummary> handleEvent(@PathVariable String id,
                                                                     @RequestBody OpsMonitorDtos.AlertHandleRequest request) {
        return ApiResponse.ok(opsMonitorService.handleEvent(id, request));
    }

    /**
     * 查询巡检项列表。
     *
     * @return 巡检项列表
     */
    @GetMapping("/checks")
    public ApiResponse<List<OpsMonitorDtos.HealthCheckSummary>> listChecks() {
        return ApiResponse.ok(opsMonitorService.listChecks());
    }

    /**
     * 查询通知渠道。
     *
     * @return 通知渠道列表
     */
    @GetMapping("/channels")
    public ApiResponse<List<OpsMonitorDtos.NotifyChannelSummary>> listChannels() {
        return ApiResponse.ok(opsMonitorService.listChannels());
    }
}
