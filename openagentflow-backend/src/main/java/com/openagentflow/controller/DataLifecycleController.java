package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.DataLifecycleService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 数据生命周期治理接口。 */
@RestController
@RequestMapping("/governance/lifecycle")
public class DataLifecycleController {

    /** 生命周期服务。 */
    private final DataLifecycleService lifecycleService;

    public DataLifecycleController(DataLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    /** 提交文档跨存储彻底清理作业。 */
    @PostMapping("/documents/{documentId}/purge")
    public ApiResponse<Map<String, Object>> purgeDocument(@PathVariable String documentId) {
        return ApiResponse.ok(lifecycleService.submitDocumentPurge(documentId));
    }
}
