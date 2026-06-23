package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.DashboardOverview;
import com.openagentflow.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页概览接口。
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    /** 首页概览服务。 */
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询首页概览数据。
     *
     * @return 首页概览响应
     */
    @GetMapping("/overview")
    public ApiResponse<DashboardOverview> overview() {
        // Controller 只负责协议适配，具体统计逻辑交给 Service。
        return ApiResponse.ok(dashboardService.getOverview());
    }
}
