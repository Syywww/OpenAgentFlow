package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.knowledge.KnowledgeGovernanceDtos;
import com.openagentflow.entity.KnowledgeGovernanceIssueEntity;
import com.openagentflow.entity.KnowledgeGovernancePolicyEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.KnowledgeGovernanceIssueMapper;
import com.openagentflow.mapper.KnowledgeGovernancePolicyMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库治理增强服务。
 * <p>负责从知识库、文档、分片、向量和智能体绑定等真实数据中识别治理问题。</p>
 */
@Service
public class KnowledgeGovernanceService {

    /** JSON工具，用于保存和解析问题证据。 */
    private final ObjectMapper objectMapper;

    /** JDBC工具，用于执行治理统计和聚合查询。 */
    private final JdbcTemplate jdbcTemplate;

    /** 治理策略Mapper。 */
    private final KnowledgeGovernancePolicyMapper policyMapper;

    /** 治理问题Mapper。 */
    private final KnowledgeGovernanceIssueMapper issueMapper;

    public KnowledgeGovernanceService(ObjectMapper objectMapper,
                                      JdbcTemplate jdbcTemplate,
                                      KnowledgeGovernancePolicyMapper policyMapper,
                                      KnowledgeGovernanceIssueMapper issueMapper) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.policyMapper = policyMapper;
        this.issueMapper = issueMapper;
    }

    /**
     * 查询知识库治理概览。
     *
     * @return 治理概览指标
     */
    public KnowledgeGovernanceDtos.Overview overview() {
        KnowledgeGovernanceDtos.Overview overview = new KnowledgeGovernanceDtos.Overview();
        KnowledgeGovernancePolicyEntity policy = defaultPolicy();
        overview.setKnowledgeBaseCount(count("SELECT COUNT(1) FROM knowledge_base WHERE deleted_at IS NULL"));
        overview.setDocumentCount(count("SELECT COUNT(1) FROM knowledge_document"));
        overview.setParsedDocumentCount(count("SELECT COUNT(1) FROM knowledge_document WHERE parse_status = 'parsed'"));
        overview.setFailedDocumentCount(count("SELECT COUNT(1) FROM knowledge_document WHERE parse_status = 'failed'"));
        overview.setProcessingDocumentCount(count("SELECT COUNT(1) FROM knowledge_document WHERE parse_status IN ('pending','processing')"));
        overview.setChunkCount(count("SELECT COUNT(1) FROM knowledge_chunk"));
        overview.setEmbeddingCount(count("SELECT COUNT(1) FROM knowledge_embedding"));
        overview.setMilvusFallbackCount(count("""
                SELECT COUNT(1)
                FROM knowledge_embedding
                WHERE sync_status IS NULL OR sync_status <> 'synced'
                """));
        overview.setOpenIssueCount(count("SELECT COUNT(1) FROM knowledge_governance_issue WHERE status = 'open'"));
        overview.setHighRiskIssueCount(count("""
                SELECT COUNT(1)
                FROM knowledge_governance_issue
                WHERE status = 'open' AND severity IN ('high','critical')
                """));
        overview.setStaleDocumentCount(count("""
                SELECT COUNT(1)
                FROM knowledge_document
                WHERE uploaded_at < DATE_SUB(NOW(3), INTERVAL ? DAY)
                """, policy.getStaleDays()));
        overview.setUnboundKnowledgeBaseCount(count("""
                SELECT COUNT(1)
                FROM knowledge_base kb
                LEFT JOIN agent_knowledge_binding akb ON akb.knowledge_base_id = kb.id AND akb.enabled = 1
                WHERE kb.deleted_at IS NULL AND akb.agent_id IS NULL
                """));
        return overview;
    }

    /**
     * 查询知识库质量列表。
     *
     * @return 每个知识库的质量指标
     */
    public List<KnowledgeGovernanceDtos.QualityRow> listQualityRows() {
        return jdbcTemplate.query("""
                SELECT
                  kb.id AS kb_id,
                  kb.kb_name AS kb_name,
                  COUNT(DISTINCT d.id) AS document_count,
                  COUNT(DISTINCT c.id) AS chunk_count,
                  COUNT(DISTINCT e.id) AS embedding_count,
                  SUM(CASE WHEN d.parse_status = 'failed' THEN 1 ELSE 0 END) AS failed_document_count,
                  COUNT(DISTINCT CASE WHEN e.sync_status IS NULL OR e.sync_status <> 'synced' THEN e.id END) AS fallback_embedding_count,
                  COUNT(DISTINCT akb.agent_id) AS agent_binding_count,
                  MAX(d.uploaded_at) AS last_uploaded_at
                FROM knowledge_base kb
                LEFT JOIN knowledge_document d ON d.kb_id = kb.id
                LEFT JOIN knowledge_chunk c ON c.kb_id = kb.id
                LEFT JOIN knowledge_embedding e ON e.kb_id = kb.id
                LEFT JOIN agent_knowledge_binding akb ON akb.knowledge_base_id = kb.id AND akb.enabled = 1
                WHERE kb.deleted_at IS NULL
                GROUP BY kb.id, kb.kb_name
                ORDER BY last_uploaded_at DESC, kb.created_at DESC
                """, (rs, rowNum) -> {
            KnowledgeGovernanceDtos.QualityRow row = new KnowledgeGovernanceDtos.QualityRow();
            row.setKbId(rs.getString("kb_id"));
            row.setKbName(rs.getString("kb_name"));
            row.setDocumentCount(rs.getLong("document_count"));
            row.setChunkCount(rs.getLong("chunk_count"));
            row.setEmbeddingCount(rs.getLong("embedding_count"));
            row.setFailedDocumentCount(rs.getLong("failed_document_count"));
            row.setFallbackEmbeddingCount(rs.getLong("fallback_embedding_count"));
            row.setAgentBindingCount(rs.getLong("agent_binding_count"));
            row.setLastUploadedAt(rs.getTimestamp("last_uploaded_at") == null ? null : rs.getTimestamp("last_uploaded_at").toLocalDateTime());
            row.setQualityScore(calculateQualityScore(row));
            row.setRiskLevel(riskLevel(row.getQualityScore()));
            return row;
        });
    }

    /**
     * 查询治理策略列表。
     *
     * @return 策略摘要列表
     */
    public List<KnowledgeGovernanceDtos.PolicySummary> listPolicies() {
        return policyMapper.selectList(new LambdaQueryWrapper<KnowledgeGovernancePolicyEntity>()
                        .orderByDesc(KnowledgeGovernancePolicyEntity::getCreatedAt))
                .stream()
                .map(this::toPolicySummary)
                .toList();
    }

    /**
     * 创建治理策略。
     *
     * @param request 策略请求
     * @return 创建后的策略
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeGovernanceDtos.PolicySummary createPolicy(KnowledgeGovernanceDtos.PolicyRequest request) {
        KnowledgeGovernancePolicyEntity entity = new KnowledgeGovernancePolicyEntity();
        entity.setId(newId());
        applyPolicyRequest(entity, request);
        entity.setCreatedBy(currentUserId());
        policyMapper.insert(entity);
        return toPolicySummary(entity);
    }

    /**
     * 更新治理策略。
     *
     * @param id 策略ID
     * @param request 策略请求
     * @return 更新后的策略
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeGovernanceDtos.PolicySummary updatePolicy(String id, KnowledgeGovernanceDtos.PolicyRequest request) {
        KnowledgeGovernancePolicyEntity entity = requirePolicy(id);
        applyPolicyRequest(entity, request);
        policyMapper.updateById(entity);
        return toPolicySummary(requirePolicy(id));
    }

    /**
     * 删除治理策略。
     *
     * @param id 策略ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(String id) {
        if (policyMapper.deleteById(id) == 0) {
            throw new BusinessException("KNOWLEDGE_GOVERNANCE_POLICY_NOT_FOUND", "知识库治理策略不存在");
        }
    }

    /**
     * 扫描知识库并生成治理问题。
     *
     * @return 本次扫描新生成的问题数量
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanIssues() {
        int created = 0;
        created += scanFailedDocuments();
        created += scanStuckDocuments();
        created += scanStaleDocuments();
        created += scanMissingEmbeddings();
        created += scanMilvusFallback();
        created += scanLowQualityChunks();
        created += scanUnboundKnowledgeBases();
        created += scanEmptyKnowledgeBases();
        return Map.of("createdIssueCount", created, "openIssueCount", overview().getOpenIssueCount());
    }

    /**
     * 查询治理问题列表。
     *
     * @param status 处理状态
     * @param severity 严重级别
     * @param issueType 问题类型
     * @param kbId 知识库ID
     * @param limit 返回条数
     * @return 治理问题列表
     */
    public List<KnowledgeGovernanceDtos.IssueSummary> listIssues(String status,
                                                                 String severity,
                                                                 String issueType,
                                                                 String kbId,
                                                                 Integer limit) {
        LambdaQueryWrapper<KnowledgeGovernanceIssueEntity> wrapper = new LambdaQueryWrapper<KnowledgeGovernanceIssueEntity>()
                .eq(StringUtils.hasText(status), KnowledgeGovernanceIssueEntity::getStatus, status)
                .eq(StringUtils.hasText(severity), KnowledgeGovernanceIssueEntity::getSeverity, severity)
                .eq(StringUtils.hasText(issueType), KnowledgeGovernanceIssueEntity::getIssueType, issueType)
                .eq(StringUtils.hasText(kbId), KnowledgeGovernanceIssueEntity::getKbId, kbId)
                .orderByDesc(KnowledgeGovernanceIssueEntity::getCreatedAt)
                .last("limit " + Math.min(limit == null ? 100 : Math.max(limit, 1), 500));
        return issueMapper.selectList(wrapper).stream().map(this::toIssueSummary).toList();
    }

    /**
     * 处理治理问题。
     *
     * @param id 问题ID
     * @param request 处理请求
     * @return 处理后的问题
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeGovernanceDtos.IssueSummary handleIssue(String id, KnowledgeGovernanceDtos.IssueHandleRequest request) {
        KnowledgeGovernanceIssueEntity entity = issueMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("KNOWLEDGE_GOVERNANCE_ISSUE_NOT_FOUND", "知识库治理问题不存在");
        }
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus() : "resolved";
        if (!List.of("resolved", "ignored", "open").contains(status)) {
            throw new BusinessException("KNOWLEDGE_GOVERNANCE_STATUS_INVALID", "治理问题状态只支持 open、resolved、ignored");
        }
        entity.setStatus(status);
        entity.setHandleNote(request.getHandleNote());
        entity.setHandlerUserId(currentUserId());
        entity.setHandledAt("open".equals(status) ? null : LocalDateTime.now());
        issueMapper.updateById(entity);
        return toIssueSummary(issueMapper.selectById(id));
    }

    /**
     * 扫描解析失败文档。
     *
     * @return 新增问题数量
     */
    private int scanFailedDocuments() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.id AS document_id, d.kb_id, d.doc_name, d.parse_error
                FROM knowledge_document d
                WHERE d.parse_status = 'failed'
                """);
        int created = 0;
        for (Map<String, Object> row : rows) {
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    string(row.get("document_id")),
                    null,
                    "FAILED_DOCUMENT",
                    "high",
                    "文档解析失败",
                    "文档解析失败会导致知识库无法完整检索，需要重新上传或修复解析器。",
                    row);
        }
        return created;
    }

    /**
     * 扫描长时间停留在处理中的文档。
     *
     * @return 新增问题数量
     */
    private int scanStuckDocuments() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.id AS document_id, d.kb_id, d.doc_name, d.parse_status, d.updated_at
                FROM knowledge_document d
                WHERE d.parse_status IN ('pending','processing')
                  AND d.updated_at < DATE_SUB(NOW(3), INTERVAL 30 MINUTE)
                """);
        int created = 0;
        for (Map<String, Object> row : rows) {
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    string(row.get("document_id")),
                    null,
                    "PROCESSING_STUCK",
                    "medium",
                    "文档处理长时间未完成",
                    "文档超过30分钟仍处于处理中，建议查看异步任务、模型调用和向量写入日志。",
                    row);
        }
        return created;
    }

    /**
     * 扫描长期未更新的文档。
     *
     * @return 新增问题数量
     */
    private int scanStaleDocuments() {
        KnowledgeGovernancePolicyEntity policy = defaultPolicy();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.id AS document_id, d.kb_id, d.doc_name, d.uploaded_at
                FROM knowledge_document d
                WHERE d.uploaded_at < DATE_SUB(NOW(3), INTERVAL ? DAY)
                """, policy.getStaleDays());
        int created = 0;
        for (Map<String, Object> row : rows) {
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    string(row.get("document_id")),
                    null,
                    "STALE_DOCUMENT",
                    "low",
                    "文档长期未更新",
                    "文档已超过治理策略设置的更新时间阈值，建议确认内容是否仍然有效。",
                    row);
        }
        return created;
    }

    /**
     * 扫描分片和向量数量不一致的文档。
     *
     * @return 新增问题数量
     */
    private int scanMissingEmbeddings() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.id AS document_id, d.kb_id, d.doc_name,
                       COUNT(DISTINCT c.id) AS chunk_count,
                       COUNT(DISTINCT e.id) AS embedding_count
                FROM knowledge_document d
                LEFT JOIN knowledge_chunk c ON c.document_id = d.id
                LEFT JOIN knowledge_embedding e ON e.chunk_id = c.id
                WHERE d.parse_status = 'parsed'
                GROUP BY d.id, d.kb_id, d.doc_name
                HAVING chunk_count > embedding_count
                """);
        int created = 0;
        for (Map<String, Object> row : rows) {
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    string(row.get("document_id")),
                    null,
                    "MISSING_EMBEDDING",
                    "high",
                    "分片缺少向量",
                    "已解析文档存在分片未生成向量，RAG召回可能遗漏内容。",
                    row);
        }
        return created;
    }

    /**
     * 扫描未同步到Milvus或降级存储的向量。
     *
     * @return 新增问题数量
     */
    private int scanMilvusFallback() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT kb.id AS kb_id, kb.kb_name, COUNT(e.id) AS fallback_count
                FROM knowledge_base kb
                JOIN knowledge_embedding e ON e.kb_id = kb.id
                WHERE kb.deleted_at IS NULL
                  AND (e.sync_status IS NULL OR e.sync_status <> 'synced')
                GROUP BY kb.id, kb.kb_name
                """);
        int created = 0;
        for (Map<String, Object> row : rows) {
            KnowledgeGovernancePolicyEntity policy = policyForKb(string(row.get("kb_id")));
            if (Boolean.FALSE.equals(policy.getRequireMilvusSync())) {
                continue;
            }
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    null,
                    null,
                    "MILVUS_FALLBACK",
                    "medium",
                    "向量未完成Milvus同步",
                    "知识库存在向量未同步到Milvus或降级写入MySQL，检索性能和召回稳定性会受影响。",
                    row);
        }
        return created;
    }

    /**
     * 扫描切片过短或过长的问题。
     *
     * @return 新增问题数量
     */
    private int scanLowQualityChunks() {
        KnowledgeGovernancePolicyEntity policy = defaultPolicy();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT c.id AS chunk_id, c.kb_id, c.document_id, c.chunk_no, c.token_count
                FROM knowledge_chunk c
                WHERE c.token_count < ? OR c.token_count > ?
                LIMIT 500
                """, policy.getMinChunkTokens(), policy.getMaxChunkTokens());
        int created = 0;
        for (Map<String, Object> row : rows) {
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    string(row.get("document_id")),
                    string(row.get("chunk_id")),
                    "LOW_CHUNK_QUALITY",
                    "low",
                    "切片Token数量异常",
                    "切片过短或过长会影响召回粒度和上下文质量，建议调整知识库切分策略后重建索引。",
                    row);
        }
        return created;
    }

    /**
     * 扫描未绑定智能体的知识库。
     *
     * @return 新增问题数量
     */
    private int scanUnboundKnowledgeBases() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT kb.id AS kb_id, kb.kb_name
                FROM knowledge_base kb
                LEFT JOIN agent_knowledge_binding akb ON akb.knowledge_base_id = kb.id AND akb.enabled = 1
                WHERE kb.deleted_at IS NULL AND akb.agent_id IS NULL
                """);
        int created = 0;
        for (Map<String, Object> row : rows) {
            KnowledgeGovernancePolicyEntity policy = policyForKb(string(row.get("kb_id")));
            if (Boolean.FALSE.equals(policy.getRequireAgentBinding())) {
                continue;
            }
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    null,
                    null,
                    "UNBOUND_KNOWLEDGE_BASE",
                    "medium",
                    "知识库未绑定智能体",
                    "知识库尚未绑定任何启用中的智能体，上传的文档不会进入实际问答链路。",
                    row);
        }
        return created;
    }

    /**
     * 扫描没有文档的空知识库。
     *
     * @return 新增问题数量
     */
    private int scanEmptyKnowledgeBases() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT kb.id AS kb_id, kb.kb_name
                FROM knowledge_base kb
                LEFT JOIN knowledge_document d ON d.kb_id = kb.id
                WHERE kb.deleted_at IS NULL
                GROUP BY kb.id, kb.kb_name
                HAVING COUNT(d.id) = 0
                """);
        int created = 0;
        for (Map<String, Object> row : rows) {
            created += createIssueIfAbsent(
                    string(row.get("kb_id")),
                    null,
                    null,
                    "EMPTY_KNOWLEDGE_BASE",
                    "low",
                    "知识库暂无文档",
                    "知识库未上传任何文档，无法为Agent提供企业知识召回。",
                    row);
        }
        return created;
    }

    /**
     * 不重复创建打开状态的问题。
     *
     * @return 本次是否创建，创建返回1，已存在返回0
     */
    private int createIssueIfAbsent(String kbId,
                                    String documentId,
                                    String chunkId,
                                    String issueType,
                                    String severity,
                                    String title,
                                    String detail,
                                    Map<String, Object> evidence) {
        KnowledgeGovernancePolicyEntity policy = policyForKb(kbId);
        if (Boolean.FALSE.equals(policy.getAutoIssueEnabled())) {
            return 0;
        }
        Long exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM knowledge_governance_issue
                WHERE kb_id = ?
                  AND issue_type = ?
                  AND status = 'open'
                  AND document_id <=> ?
                  AND chunk_id <=> ?
                """, Long.class, kbId, issueType, documentId, chunkId);
        if (exists != null && exists > 0) {
            return 0;
        }
        KnowledgeGovernanceIssueEntity entity = new KnowledgeGovernanceIssueEntity();
        entity.setId(newId());
        entity.setKbId(kbId);
        entity.setDocumentId(documentId);
        entity.setChunkId(chunkId);
        entity.setIssueType(issueType);
        entity.setSeverity(severity);
        entity.setIssueTitle(title);
        entity.setIssueDetail(detail);
        entity.setEvidenceJson(toJson(evidence));
        entity.setStatus("open");
        issueMapper.insert(entity);
        return 1;
    }

    /**
     * 把策略请求写入实体，并补齐默认值。
     */
    private void applyPolicyRequest(KnowledgeGovernancePolicyEntity entity, KnowledgeGovernanceDtos.PolicyRequest request) {
        if (!StringUtils.hasText(request.getPolicyName())) {
            throw new BusinessException("KNOWLEDGE_GOVERNANCE_POLICY_NAME_REQUIRED", "知识库治理策略名称不能为空");
        }
        entity.setPolicyCode(StringUtils.hasText(request.getPolicyCode()) ? request.getPolicyCode().trim() : "knowledge-governance-" + System.currentTimeMillis());
        entity.setPolicyName(request.getPolicyName().trim());
        entity.setKbId(request.getKbId());
        entity.setStaleDays(defaultInt(request.getStaleDays(), 90));
        entity.setMinChunkTokens(defaultInt(request.getMinChunkTokens(), 20));
        entity.setMaxChunkTokens(defaultInt(request.getMaxChunkTokens(), 1200));
        entity.setMaxFailedDocuments(defaultInt(request.getMaxFailedDocuments(), 0));
        entity.setRequireAgentBinding(request.getRequireAgentBinding() == null || request.getRequireAgentBinding());
        entity.setRequireMilvusSync(request.getRequireMilvusSync() == null || request.getRequireMilvusSync());
        entity.setAutoIssueEnabled(request.getAutoIssueEnabled() == null || request.getAutoIssueEnabled());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
    }

    /**
     * 查询指定知识库策略，优先使用知识库专属策略。
     */
    private KnowledgeGovernancePolicyEntity policyForKb(String kbId) {
        if (StringUtils.hasText(kbId)) {
            KnowledgeGovernancePolicyEntity policy = policyMapper.selectOne(new LambdaQueryWrapper<KnowledgeGovernancePolicyEntity>()
                    .eq(KnowledgeGovernancePolicyEntity::getKbId, kbId)
                    .eq(KnowledgeGovernancePolicyEntity::getStatus, "enabled")
                    .last("limit 1"));
            if (policy != null) {
                return policy;
            }
        }
        return defaultPolicy();
    }

    /**
     * 查询默认策略；数据库未初始化时返回内存默认策略，避免页面不可用。
     */
    private KnowledgeGovernancePolicyEntity defaultPolicy() {
        KnowledgeGovernancePolicyEntity policy = policyMapper.selectOne(new LambdaQueryWrapper<KnowledgeGovernancePolicyEntity>()
                .isNull(KnowledgeGovernancePolicyEntity::getKbId)
                .eq(KnowledgeGovernancePolicyEntity::getStatus, "enabled")
                .orderByAsc(KnowledgeGovernancePolicyEntity::getCreatedAt)
                .last("limit 1"));
        if (policy != null) {
            return policy;
        }
        KnowledgeGovernancePolicyEntity fallback = new KnowledgeGovernancePolicyEntity();
        fallback.setStaleDays(90);
        fallback.setMinChunkTokens(20);
        fallback.setMaxChunkTokens(1200);
        fallback.setMaxFailedDocuments(0);
        fallback.setRequireAgentBinding(true);
        fallback.setRequireMilvusSync(true);
        fallback.setAutoIssueEnabled(true);
        fallback.setStatus("enabled");
        return fallback;
    }

    /**
     * 查询策略，不存在则抛出业务异常。
     */
    private KnowledgeGovernancePolicyEntity requirePolicy(String id) {
        KnowledgeGovernancePolicyEntity entity = policyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("KNOWLEDGE_GOVERNANCE_POLICY_NOT_FOUND", "知识库治理策略不存在");
        }
        return entity;
    }

    /**
     * 把策略实体转换为前端摘要。
     */
    private KnowledgeGovernanceDtos.PolicySummary toPolicySummary(KnowledgeGovernancePolicyEntity entity) {
        KnowledgeGovernanceDtos.PolicySummary summary = new KnowledgeGovernanceDtos.PolicySummary();
        summary.setId(entity.getId());
        summary.setPolicyCode(entity.getPolicyCode());
        summary.setPolicyName(entity.getPolicyName());
        summary.setKbId(entity.getKbId());
        summary.setStaleDays(entity.getStaleDays());
        summary.setMinChunkTokens(entity.getMinChunkTokens());
        summary.setMaxChunkTokens(entity.getMaxChunkTokens());
        summary.setMaxFailedDocuments(entity.getMaxFailedDocuments());
        summary.setRequireAgentBinding(entity.getRequireAgentBinding());
        summary.setRequireMilvusSync(entity.getRequireMilvusSync());
        summary.setAutoIssueEnabled(entity.getAutoIssueEnabled());
        summary.setStatus(entity.getStatus());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    /**
     * 把问题实体转换为前端摘要，并补充知识库和文档名称。
     */
    private KnowledgeGovernanceDtos.IssueSummary toIssueSummary(KnowledgeGovernanceIssueEntity entity) {
        KnowledgeGovernanceDtos.IssueSummary summary = new KnowledgeGovernanceDtos.IssueSummary();
        summary.setId(entity.getId());
        summary.setKbId(entity.getKbId());
        summary.setKbName(findName("SELECT kb_name FROM knowledge_base WHERE id = ?", entity.getKbId()));
        summary.setDocumentId(entity.getDocumentId());
        summary.setDocumentName(findName("SELECT doc_name FROM knowledge_document WHERE id = ?", entity.getDocumentId()));
        summary.setChunkId(entity.getChunkId());
        summary.setIssueType(entity.getIssueType());
        summary.setSeverity(entity.getSeverity());
        summary.setIssueTitle(entity.getIssueTitle());
        summary.setIssueDetail(entity.getIssueDetail());
        summary.setEvidence(fromJson(entity.getEvidenceJson()));
        summary.setStatus(entity.getStatus());
        summary.setHandlerUserId(entity.getHandlerUserId());
        summary.setHandledAt(entity.getHandledAt());
        summary.setHandleNote(entity.getHandleNote());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    /**
     * 计算知识库质量分，越高表示越适合交付。
     */
    private int calculateQualityScore(KnowledgeGovernanceDtos.QualityRow row) {
        int score = 100;
        if (row.getDocumentCount() == 0) {
            score -= 30;
        }
        if (row.getFailedDocumentCount() > 0) {
            score -= Math.min(30, row.getFailedDocumentCount().intValue() * 10);
        }
        if (row.getFallbackEmbeddingCount() > 0) {
            score -= Math.min(25, row.getFallbackEmbeddingCount().intValue() * 3);
        }
        if (row.getChunkCount() > row.getEmbeddingCount()) {
            score -= Math.min(25, (int) (row.getChunkCount() - row.getEmbeddingCount()));
        }
        if (row.getAgentBindingCount() == 0) {
            score -= 15;
        }
        return Math.max(score, 0);
    }

    /**
     * 根据质量分转换风险级别。
     */
    private String riskLevel(Integer score) {
        if (score == null || score < 60) {
            return "high";
        }
        if (score < 80) {
            return "medium";
        }
        return "low";
    }

    /**
     * 查询数量。
     */
    private Long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    /**
     * 查询名称，参数为空或未命中时返回空字符串。
     */
    private String findName(String sql, String id) {
        if (!StringUtils.hasText(id)) {
            return "";
        }
        List<String> names = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), id);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 生成新ID。
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 获取当前登录用户ID。
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 读取整数默认值。
     */
    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 安全转换字符串。
     */
    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 把对象转成JSON字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    /**
     * 把JSON字符串解析为Map。
     */
    private Map<String, Object> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }
}
