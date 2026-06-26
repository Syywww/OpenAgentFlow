package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.delivery.DeliveryAcceptanceDtos;
import com.openagentflow.service.DeliveryAcceptanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 交付验收中心接口。
 */
@RestController
@RequestMapping("/delivery-acceptance")
public class DeliveryAcceptanceController {

    /** 交付验收中心服务。 */
    private final DeliveryAcceptanceService deliveryAcceptanceService;

    public DeliveryAcceptanceController(DeliveryAcceptanceService deliveryAcceptanceService) {
        this.deliveryAcceptanceService = deliveryAcceptanceService;
    }

    /**
     * 查询交付验收总览。
     *
     * @return 总览信息
     */
    @GetMapping("/overview")
    public ApiResponse<DeliveryAcceptanceDtos.Overview> overview() {
        return ApiResponse.ok(deliveryAcceptanceService.overview());
    }

    /**
     * 查询交付检查项。
     *
     * @return 检查项列表
     */
    @GetMapping("/checks")
    public ApiResponse<List<DeliveryAcceptanceDtos.CheckItem>> checks() {
        return ApiResponse.ok(deliveryAcceptanceService.checklist());
    }

    /**
     * 执行交付验收并生成报告。
     *
     * @return 报告详情
     */
    @PostMapping("/run")
    public ApiResponse<DeliveryAcceptanceDtos.ReportDetail> run() {
        return ApiResponse.ok(deliveryAcceptanceService.runAcceptance());
    }

    /**
     * 分页查询交付报告。
     *
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 报告分页
     */
    @GetMapping("/reports")
    public ApiResponse<PageResult<DeliveryAcceptanceDtos.ReportSummary>> reports(@RequestParam(defaultValue = "1") Integer pageNo,
                                                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(deliveryAcceptanceService.listReports(pageNo, pageSize));
    }

    /**
     * 查询交付报告详情。
     *
     * @param id 报告 ID
     * @return 报告详情
     */
    @GetMapping("/reports/{id}")
    public ApiResponse<DeliveryAcceptanceDtos.ReportDetail> reportDetail(@PathVariable String id) {
        return ApiResponse.ok(deliveryAcceptanceService.getReport(id));
    }
}
