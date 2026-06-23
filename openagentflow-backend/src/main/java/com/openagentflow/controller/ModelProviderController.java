package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.model.ModelConfigSummary;
import com.openagentflow.domain.model.ModelConnectivityResult;
import com.openagentflow.domain.model.ModelProviderRequest;
import com.openagentflow.domain.model.ModelProviderSummary;
import com.openagentflow.service.ModelProviderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型服务商接口。
 */
@RestController
@RequestMapping("/model-providers")
public class ModelProviderController {

    /** 模型服务商应用服务。 */
    private final ModelProviderService modelProviderService;

    public ModelProviderController(ModelProviderService modelProviderService) {
        this.modelProviderService = modelProviderService;
    }

    /**
     * 查询模型服务商列表。
     *
     * @return 模型服务商摘要列表
     */
    @GetMapping
    public ApiResponse<List<ModelProviderSummary>> listProviders() {
        // 模型密钥只返回脱敏值，避免敏感配置泄露到前端。
        return ApiResponse.ok(modelProviderService.listProviders());
    }

    /**
     * 查询聊天模型列表。
     *
     * @return 聊天模型摘要列表
     */
    @GetMapping("/chat-models")
    public ApiResponse<List<ModelConfigSummary>> listChatModels() {
        return ApiResponse.ok(modelProviderService.listChatModels());
    }

    /**
     * 查询模型服务商详情。
     *
     * @param id 模型服务商 ID
     * @return 模型服务商摘要
     */
    @GetMapping("/{id}")
    public ApiResponse<ModelProviderSummary> getProvider(@PathVariable String id) {
        return ApiResponse.ok(modelProviderService.getProvider(id));
    }

    /**
     * 创建模型服务商。
     *
     * @param request 保存请求
     * @return 模型服务商摘要
     */
    @PostMapping
    public ApiResponse<ModelProviderSummary> createProvider(@Valid @RequestBody ModelProviderRequest request) {
        return ApiResponse.ok(modelProviderService.createProvider(request));
    }

    /**
     * 更新模型服务商。
     *
     * @param id 模型服务商 ID
     * @param request 保存请求
     * @return 模型服务商摘要
     */
    @PutMapping("/{id}")
    public ApiResponse<ModelProviderSummary> updateProvider(@PathVariable String id,
                                                            @Valid @RequestBody ModelProviderRequest request) {
        return ApiResponse.ok(modelProviderService.updateProvider(id, request));
    }

    /**
     * 删除模型服务商。
     *
     * @param id 模型服务商 ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProvider(@PathVariable String id) {
        modelProviderService.deleteProvider(id);
        return ApiResponse.ok(null);
    }

    /**
     * 执行模型连通性测试。
     *
     * @param id 模型服务商 ID
     * @return 测试结果
     */
    @PostMapping("/{id}/test")
    public ApiResponse<ModelConnectivityResult> testProvider(@PathVariable String id) {
        return ApiResponse.ok(modelProviderService.testProvider(id));
    }
}
