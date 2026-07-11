package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.service.KnowledgeIndexGovernanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 知识库索引版本治理接口。 */
@RestController
@RequestMapping("/knowledge-bases/{kbId}/index-versions")
public class KnowledgeIndexGovernanceController {

    /** 索引治理服务。 */
    private final KnowledgeIndexGovernanceService service;

    public KnowledgeIndexGovernanceController(KnowledgeIndexGovernanceService service) { this.service = service; }

    /** 创建蓝绿索引新版本。 */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@PathVariable String kbId) { return ApiResponse.ok(service.createVersion(kbId)); }

    /** 查询索引版本。 */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable String kbId) { return ApiResponse.ok(service.list(kbId)); }

    /** 激活已就绪索引版本。 */
    @PostMapping("/{versionId}/activate")
    public ApiResponse<Map<String, Object>> activate(@PathVariable String kbId, @PathVariable String versionId) {
        return ApiResponse.ok(service.activate(versionId));
    }

    /** 标记索引构建已完成。 */
    @PostMapping("/{versionId}/ready")
    public ApiResponse<Map<String, Object>> markReady(@PathVariable String kbId,
                                                      @PathVariable String versionId,
                                                      @RequestParam Integer dimension,
                                                      @RequestParam Long chunkCount) {
        return ApiResponse.ok(service.markReady(versionId, dimension, chunkCount));
    }
}
