package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.vector.VectorStoreStatus;
import com.openagentflow.service.VectorStoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 向量存储接口。
 */
@RestController
@RequestMapping("/vector-store")
public class VectorStoreController {

    /** 向量存储服务。 */
    private final VectorStoreService vectorStoreService;

    public VectorStoreController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 查询 Milvus 连接状态。
     *
     * @return Milvus 状态响应
     */
    @GetMapping("/status")
    public ApiResponse<VectorStoreStatus> status() {
        // 该接口用于前端设置页和部署自检页展示向量数据库状态。
        return ApiResponse.ok(vectorStoreService.getStatus());
    }
}
