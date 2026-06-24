package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.knowledge.KnowledgeGovernanceDtos;
import com.openagentflow.service.KnowledgeGovernanceService;
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
import java.util.Map;

/**
 * 知识库治理增强接口。
 */
@RestController
@RequestMapping("/knowledge-governance")
public class KnowledgeGovernanceController {

    /** 知识库治理服务。 */
    private final KnowledgeGovernanceService knowledgeGovernanceService;

    public KnowledgeGovernanceController(KnowledgeGovernanceService knowledgeGovernanceService) {
        this.knowledgeGovernanceService = knowledgeGovernanceService;
    }

    /**
     * 查询知识库治理概览。
     *
     * @return 治理概览指标
     */
    @GetMapping("/overview")
    public ApiResponse<KnowledgeGovernanceDtos.Overview> overview() {
        return ApiResponse.ok(knowledgeGovernanceService.overview());
    }

    /**
     * 查询知识库质量列表。
     *
     * @return 知识库质量列表
     */
    @GetMapping("/quality")
    public ApiResponse<List<KnowledgeGovernanceDtos.QualityRow>> qualityRows() {
        return ApiResponse.ok(knowledgeGovernanceService.listQualityRows());
    }

    /**
     * 扫描并生成知识库治理问题。
     *
     * @return 扫描结果
     */
    @PostMapping("/scan")
    public ApiResponse<Map<String, Object>> scanIssues() {
        return ApiResponse.ok(knowledgeGovernanceService.scanIssues());
    }

    /**
     * 查询知识库治理问题。
     *
     * @param status 处理状态
     * @param severity 严重级别
     * @param issueType 问题类型
     * @param kbId 知识库ID
     * @param limit 返回条数
     * @return 问题列表
     */
    @GetMapping("/issues")
    public ApiResponse<List<KnowledgeGovernanceDtos.IssueSummary>> listIssues(@RequestParam(required = false) String status,
                                                                              @RequestParam(required = false) String severity,
                                                                              @RequestParam(required = false) String issueType,
                                                                              @RequestParam(required = false) String kbId,
                                                                              @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(knowledgeGovernanceService.listIssues(status, severity, issueType, kbId, limit));
    }

    /**
     * 处理知识库治理问题。
     *
     * @param id 问题ID
     * @param request 处理请求
     * @return 处理后的问题
     */
    @PostMapping("/issues/{id}/handle")
    public ApiResponse<KnowledgeGovernanceDtos.IssueSummary> handleIssue(@PathVariable String id,
                                                                         @RequestBody KnowledgeGovernanceDtos.IssueHandleRequest request) {
        return ApiResponse.ok(knowledgeGovernanceService.handleIssue(id, request));
    }

    /**
     * 查询知识库治理策略。
     *
     * @return 策略列表
     */
    @GetMapping("/policies")
    public ApiResponse<List<KnowledgeGovernanceDtos.PolicySummary>> listPolicies() {
        return ApiResponse.ok(knowledgeGovernanceService.listPolicies());
    }

    /**
     * 创建知识库治理策略。
     *
     * @param request 策略请求
     * @return 创建后的策略
     */
    @PostMapping("/policies")
    public ApiResponse<KnowledgeGovernanceDtos.PolicySummary> createPolicy(@RequestBody KnowledgeGovernanceDtos.PolicyRequest request) {
        return ApiResponse.ok(knowledgeGovernanceService.createPolicy(request));
    }

    /**
     * 更新知识库治理策略。
     *
     * @param id 策略ID
     * @param request 策略请求
     * @return 更新后的策略
     */
    @PutMapping("/policies/{id}")
    public ApiResponse<KnowledgeGovernanceDtos.PolicySummary> updatePolicy(@PathVariable String id,
                                                                          @RequestBody KnowledgeGovernanceDtos.PolicyRequest request) {
        return ApiResponse.ok(knowledgeGovernanceService.updatePolicy(id, request));
    }

    /**
     * 删除知识库治理策略。
     *
     * @param id 策略ID
     * @return 空响应
     */
    @DeleteMapping("/policies/{id}")
    public ApiResponse<Void> deletePolicy(@PathVariable String id) {
        knowledgeGovernanceService.deletePolicy(id);
        return ApiResponse.ok(null);
    }
}
