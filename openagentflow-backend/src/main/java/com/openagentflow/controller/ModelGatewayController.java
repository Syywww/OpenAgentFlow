package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.model.ModelGatewayDtos;
import com.openagentflow.service.ModelGatewayService;
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
 * 模型网关与模型治理接口。
 */
@RestController
@RequestMapping("/model-gateway")
public class ModelGatewayController {

    /** 模型网关服务。 */
    private final ModelGatewayService modelGatewayService;

    public ModelGatewayController(ModelGatewayService modelGatewayService) {
        this.modelGatewayService = modelGatewayService;
    }

    /**
     * 查询模型网关概览。
     *
     * @return 概览数据
     */
    @GetMapping("/overview")
    public ApiResponse<ModelGatewayDtos.Overview> getOverview() {
        return ApiResponse.ok(modelGatewayService.getOverview());
    }

    /**
     * 查询模型路由策略列表。
     *
     * @return 策略列表
     */
    @GetMapping("/policies")
    public ApiResponse<List<ModelGatewayDtos.PolicySummary>> listPolicies() {
        return ApiResponse.ok(modelGatewayService.listPolicies());
    }

    /**
     * 创建模型路由策略。
     *
     * @param request 保存请求
     * @return 策略摘要
     */
    @PostMapping("/policies")
    public ApiResponse<ModelGatewayDtos.PolicySummary> createPolicy(@RequestBody ModelGatewayDtos.PolicyRequest request) {
        return ApiResponse.ok(modelGatewayService.createPolicy(request));
    }

    /**
     * 更新模型路由策略。
     *
     * @param id 策略ID
     * @param request 保存请求
     * @return 策略摘要
     */
    @PutMapping("/policies/{id}")
    public ApiResponse<ModelGatewayDtos.PolicySummary> updatePolicy(@PathVariable String id,
                                                                    @RequestBody ModelGatewayDtos.PolicyRequest request) {
        return ApiResponse.ok(modelGatewayService.updatePolicy(id, request));
    }

    /**
     * 删除模型路由策略。
     *
     * @param id 策略ID
     * @return 空响应
     */
    @DeleteMapping("/policies/{id}")
    public ApiResponse<Void> deletePolicy(@PathVariable String id) {
        modelGatewayService.deletePolicy(id);
        return ApiResponse.ok(null);
    }

    /**
     * 查询模型健康状态。
     *
     * @return 模型健康列表
     */
    @GetMapping("/health")
    public ApiResponse<List<ModelGatewayDtos.ModelHealthSummary>> listModelHealth() {
        return ApiResponse.ok(modelGatewayService.listModelHealth());
    }

    /**
     * 查询最近模型网关调用。
     *
     * @param limit 返回数量
     * @return 调用列表
     */
    @GetMapping("/calls")
    public ApiResponse<List<ModelGatewayDtos.GatewayCallSummary>> listRecentCalls(@RequestParam(defaultValue = "30") Integer limit) {
        return ApiResponse.ok(modelGatewayService.listRecentCalls(limit));
    }
}
