package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.knowledge.AgentKnowledgeBindingRequest;
import com.openagentflow.domain.knowledge.AgentKnowledgeBindingSummary;
import com.openagentflow.domain.knowledge.KnowledgeBaseDetail;
import com.openagentflow.domain.knowledge.KnowledgeBaseRequest;
import com.openagentflow.domain.knowledge.KnowledgeBaseSummary;
import com.openagentflow.domain.knowledge.KnowledgeChunkSummary;
import com.openagentflow.domain.knowledge.KnowledgeDocumentSummary;
import com.openagentflow.domain.knowledge.KnowledgeRetrievalRequest;
import com.openagentflow.domain.knowledge.KnowledgeRetrievalResult;
import com.openagentflow.domain.knowledge.KnowledgeUploadResult;
import com.openagentflow.domain.knowledge.KnowledgeVectorRebuildResult;
import com.openagentflow.service.KnowledgeBaseService;
import com.openagentflow.service.KnowledgeDocumentProcessingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理接口。
 */
@RestController
@RequestMapping
public class KnowledgeBaseController {

    /** 知识库应用服务。 */
    private final KnowledgeBaseService knowledgeBaseService;

    /** 知识库文档上传和处理服务。 */
    private final KnowledgeDocumentProcessingService knowledgeDocumentProcessingService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   KnowledgeDocumentProcessingService knowledgeDocumentProcessingService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeDocumentProcessingService = knowledgeDocumentProcessingService;
    }

    /**
     * 查询知识库列表。
     *
     * @return 知识库摘要列表
     */
    @GetMapping("/knowledge-bases")
    public ApiResponse<List<KnowledgeBaseSummary>> listKnowledgeBases() {
        return ApiResponse.ok(knowledgeBaseService.listKnowledgeBases());
    }

    /**
     * 创建知识库。
     *
     * @param request 保存请求
     * @return 知识库详情
     */
    @PostMapping("/knowledge-bases")
    public ApiResponse<KnowledgeBaseDetail> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResponse.ok(knowledgeBaseService.createKnowledgeBase(request));
    }

    /**
     * 查询知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库详情
     */
    @GetMapping("/knowledge-bases/{id}")
    public ApiResponse<KnowledgeBaseDetail> getKnowledgeBase(@PathVariable String id) {
        return ApiResponse.ok(knowledgeBaseService.getKnowledgeBase(id));
    }

    /**
     * 更新知识库。
     *
     * @param id 知识库 ID
     * @param request 保存请求
     * @return 知识库详情
     */
    @PutMapping("/knowledge-bases/{id}")
    public ApiResponse<KnowledgeBaseDetail> updateKnowledgeBase(@PathVariable String id,
                                                                @Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResponse.ok(knowledgeBaseService.updateKnowledgeBase(id, request));
    }

    /**
     * 删除知识库。
     *
     * @param id 知识库 ID
     * @return 空响应
     */
    @DeleteMapping("/knowledge-bases/{id}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable String id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.ok(null);
    }

    /**
     * 上传知识文档并执行解析、切片、向量化。
     *
     * @param id 知识库 ID
     * @param file 上传文件
     * @return 上传处理结果
     */
    @PostMapping("/knowledge-bases/{id}/documents")
    public ApiResponse<KnowledgeUploadResult> uploadDocument(@PathVariable String id,
                                                             @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(knowledgeDocumentProcessingService.acceptUpload(id, file));
    }

    /**
     * 查询知识库文档列表。
     *
     * @param id 知识库 ID
     * @return 文档摘要列表
     */
    @GetMapping("/knowledge-bases/{id}/documents")
    public ApiResponse<List<KnowledgeDocumentSummary>> listDocuments(@PathVariable String id) {
        return ApiResponse.ok(knowledgeBaseService.listDocuments(id));
    }

    /**
     * 查询单个知识库文档的处理状态和日志。
     *
     * @param id 知识库 ID
     * @param documentId 文档 ID
     * @return 文档处理状态
     */
    @GetMapping("/knowledge-bases/{id}/documents/{documentId}")
    public ApiResponse<KnowledgeDocumentSummary> getDocumentStatus(@PathVariable String id,
                                                                   @PathVariable String documentId) {
        return ApiResponse.ok(knowledgeDocumentProcessingService.getDocumentStatus(id, documentId));
    }

    /**
     * 查询知识库分片列表。
     *
     * @param id 知识库 ID
     * @return 分片摘要列表
     */
    @GetMapping("/knowledge-bases/{id}/chunks")
    public ApiResponse<List<KnowledgeChunkSummary>> listChunks(@PathVariable String id) {
        return ApiResponse.ok(knowledgeBaseService.listChunks(id, 100));
    }

    /**
     * 执行知识库检索测试。
     *
     * @param id 知识库 ID
     * @param request 检索请求
     * @return 检索结果
     */
    @PostMapping("/knowledge-bases/{id}/retrieval-test")
    public ApiResponse<KnowledgeRetrievalResult> retrievalTest(@PathVariable String id,
                                                               @Valid @RequestBody KnowledgeRetrievalRequest request) {
        return ApiResponse.ok(knowledgeBaseService.retrievalTest(id, request));
    }

    /**
     * 提交知识库向量重建任务。
     *
     * @param id 知识库 ID
     * @return 异步任务受理结果
     */
    @PostMapping("/knowledge-bases/{id}/vectors/rebuild")
    public ApiResponse<KnowledgeVectorRebuildResult> rebuildKnowledgeVectors(@PathVariable String id) {
        return ApiResponse.ok(knowledgeBaseService.rebuildKnowledgeVectors(id));
    }

    /**
     * 查询 Agent 已绑定知识库。
     *
     * @param agentId Agent ID
     * @return 绑定摘要列表
     */
    @GetMapping("/agents/{agentId}/knowledge-bases")
    public ApiResponse<List<AgentKnowledgeBindingSummary>> listAgentBindings(@PathVariable String agentId) {
        return ApiResponse.ok(knowledgeBaseService.listAgentBindings(agentId));
    }

    /**
     * 保存 Agent 知识库绑定。
     *
     * @param agentId Agent ID
     * @param request 绑定请求
     * @return 绑定摘要列表
     */
    @PutMapping("/agents/{agentId}/knowledge-bases")
    public ApiResponse<List<AgentKnowledgeBindingSummary>> saveAgentBindings(@PathVariable String agentId,
                                                                             @RequestBody AgentKnowledgeBindingRequest request) {
        return ApiResponse.ok(knowledgeBaseService.saveAgentBindings(agentId, request));
    }
}
