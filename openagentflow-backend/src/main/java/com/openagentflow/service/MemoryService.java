package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.memory.MemoryDtos;
import com.openagentflow.domain.task.AsyncTaskDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentMemoryEntity;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.AgentMemoryMapper;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * Memory 记忆中心服务。
 *
 * <p>负责短期记忆、长期记忆、任务记忆和向量记忆的保存、召回、治理清理和聊天链路自动沉淀。</p>
 */
@Service
public class MemoryService implements DistributedTaskHandler {

    /** 默认向量集合 ID，对应初始化 SQL 中的 Agent Memory 集合。 */
    private static final String DEFAULT_MEMORY_VECTOR_COLLECTION_ID = "70000000-0000-0000-0000-000000000102";

    /** Agent 记忆 Mapper。 */
    private final AgentMemoryMapper agentMemoryMapper;

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** Agent 资源访问控制服务。 */
    private final AgentAccessService agentAccessService;

    /** Embedding 服务，用于生成语义记忆向量。 */
    private final EmbeddingService embeddingService;

    /** JDBC 工具，用于分页聚合和批量治理。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** 平台配置。 */
    private final OpenAgentFlowProperties properties;

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    /** Memory结构化事实提取器。 */
    private final MemoryExtractionService memoryExtractionService;

    /** Milvus向量读写服务。 */
    private final MilvusKnowledgeVectorService milvusVectorService;

    /** Redis短期记忆存储。 */
    private final StringRedisTemplate redisTemplate;

    public MemoryService(AgentMemoryMapper agentMemoryMapper,
                         AgentMapper agentMapper,
                         AgentAccessService agentAccessService,
                         EmbeddingService embeddingService,
                         JdbcTemplate jdbcTemplate,
                         ObjectMapper objectMapper,
                         OpenAgentFlowProperties properties,
                         AsyncTaskService asyncTaskService,
                         MemoryExtractionService memoryExtractionService,
                         MilvusKnowledgeVectorService milvusVectorService,
                         StringRedisTemplate redisTemplate) {
        this.agentMemoryMapper = agentMemoryMapper;
        this.agentMapper = agentMapper;
        this.agentAccessService = agentAccessService;
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.asyncTaskService = asyncTaskService;
        this.memoryExtractionService = memoryExtractionService;
        this.milvusVectorService = milvusVectorService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询记忆中心概览。
     *
     * @return 记忆概览
     */
    public MemoryDtos.Overview overview() {
        String userScope = userScopeWhere();
        Object[] args = userScopeArgs();
        MemoryDtos.Overview overview = new MemoryDtos.Overview();
        overview.setTotalCount(count("SELECT COUNT(1) FROM agent_memory m WHERE m.status <> 'deleted' " + userScope, args));
        overview.setShortTermCount(count("SELECT COUNT(1) FROM agent_memory m WHERE m.status <> 'deleted' AND m.memory_type = 'short_term' " + userScope, args));
        overview.setLongTermCount(count("SELECT COUNT(1) FROM agent_memory m WHERE m.status <> 'deleted' AND m.memory_type = 'long_term' " + userScope, args));
        overview.setTaskCount(count("SELECT COUNT(1) FROM agent_memory m WHERE m.status <> 'deleted' AND m.memory_type = 'task' " + userScope, args));
        overview.setVectorCount(count("SELECT COUNT(1) FROM agent_memory m WHERE m.status <> 'deleted' AND m.memory_type = 'vector' " + userScope, args));
        overview.setExpiredCount(count("SELECT COUNT(1) FROM agent_memory m WHERE m.status = 'active' AND m.expired_at IS NOT NULL AND m.expired_at < NOW(3) " + userScope, args));
        overview.setPendingSyncCount(count("SELECT COUNT(1) FROM agent_memory m WHERE m.status <> 'deleted' AND m.sync_status = 'pending' " + userScope, args));
        return overview;
    }

    /**
     * 分页查询记忆列表。
     *
     * @param memoryType 记忆类型
     * @param status 记忆状态
     * @param agentId Agent ID
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 记忆分页
     */
    public PageResult<MemoryDtos.Summary> listMemories(String memoryType,
                                                       String status,
                                                       String agentId,
                                                       String keyword,
                                                       Integer pageNo,
                                                       Integer pageSize) {
        int currentPage = normalizePageNo(pageNo);
        int currentSize = normalizePageSize(pageSize);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE m.status <> 'deleted' ");
        appendUserScope(where, args);
        if (StringUtils.hasText(memoryType) && !"all".equalsIgnoreCase(memoryType)) {
            where.append(" AND m.memory_type = ? ");
            args.add(memoryType);
        }
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            where.append(" AND m.status = ? ");
            args.add(status);
        }
        if (StringUtils.hasText(agentId) && !"all".equalsIgnoreCase(agentId)) {
            where.append(" AND m.agent_id = ? ");
            args.add(agentId);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (m.memory_text LIKE ? OR m.memory_key LIKE ?) ");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_memory m " + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add((currentPage - 1) * currentSize);
        listArgs.add(currentSize);
        List<MemoryDtos.Summary> rows = jdbcTemplate.query("""
                        SELECT m.*, a.agent_name
                        FROM agent_memory m
                        LEFT JOIN agent a ON a.id = m.agent_id
                        %s
                        ORDER BY m.updated_at DESC
                        LIMIT ?, ?
                        """.formatted(where),
                (rs, rowNum) -> {
                    MemoryDtos.Summary item = new MemoryDtos.Summary();
                    item.setId(rs.getString("id"));
                    item.setAgentId(rs.getString("agent_id"));
                    item.setAgentName(rs.getString("agent_name"));
                    item.setUserId(rs.getString("user_id"));
                    item.setSessionId(rs.getString("session_id"));
                    item.setMemoryType(rs.getString("memory_type"));
                    item.setMemoryKey(rs.getString("memory_key"));
                    item.setMemoryText(rs.getString("memory_text"));
                    item.setMemoryValue(rs.getString("memory_value"));
                    item.setSyncStatus(rs.getString("sync_status"));
                    item.setImportanceScore(rs.getBigDecimal("importance_score"));
                    item.setExpiredAt(toLocalDateTime(rs.getTimestamp("expired_at")));
                    item.setStatus(rs.getString("status"));
                    item.setPrivacyScope(rs.getString("privacy_scope"));
                    item.setSourceRunId(rs.getString("source_run_id"));
                    item.setTagsJson(rs.getString("tags_json"));
                    item.setHitCount(rs.getInt("hit_count"));
                    item.setLastAccessedAt(toLocalDateTime(rs.getTimestamp("last_accessed_at")));
                    item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
                    return item;
                },
                listArgs.toArray());
        return new PageResult<>(rows, total == null ? 0 : total, currentPage, currentSize);
    }

    /**
     * 创建记忆。
     *
     * @param request 创建请求
     * @return 记忆摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public MemoryDtos.Summary createMemory(MemoryDtos.SaveRequest request) {
        AgentMemoryEntity entity = new AgentMemoryEntity();
        entity.setId(newId());
        applyRequest(entity, request, false);
        entity.setUserId(currentUserIdOrThrow());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        enrichEmbedding(entity);
        agentMemoryMapper.insert(entity);
        return findSummary(entity.getId());
    }

    /**
     * 更新记忆。
     *
     * @param id 记忆 ID
     * @param request 更新请求
     * @return 记忆摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public MemoryDtos.Summary updateMemory(String id, MemoryDtos.SaveRequest request) {
        AgentMemoryEntity entity = requireMemory(id);
        assertCanManageMemory(entity);
        String oldText = entity.getMemoryText();
        applyRequest(entity, request, true);
        entity.setUpdatedAt(LocalDateTime.now());
        if (!safeText(oldText).equals(safeText(entity.getMemoryText()))) {
            enrichEmbedding(entity);
        }
        agentMemoryMapper.updateById(entity);
        return findSummary(entity.getId());
    }

    /**
     * 删除记忆，采用软删除保留治理线索。
     *
     * @param id 记忆 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemory(String id) {
        AgentMemoryEntity entity = requireMemory(id);
        assertCanManageMemory(entity);
        if ("synced".equals(entity.getSyncStatus()) && entity.getEmbeddingDimension() != null) {
            try {
                milvusVectorService.deleteMemory(entity.getMilvusCollectionName(), entity.getEmbeddingDimension(), entity.getVectorPrimaryKey());
            } catch (Exception exception) {
                throw new BusinessException("MEMORY_VECTOR_DELETE_FAILED", "向量删除失败，请稍后重试：" + exception.getMessage());
            }
        }
        entity.setStatus("deleted");
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        agentMemoryMapper.updateById(entity);
    }

    /**
     * 手动召回记忆。
     *
     * @param request 召回请求
     * @return 召回结果
     */
    @Transactional(rollbackFor = Exception.class)
    public List<MemoryDtos.RecallItem> recall(MemoryDtos.RecallRequest request) {
        AgentEntity agent = StringUtils.hasText(request.getAgentId()) ? requireAgent(request.getAgentId()) : null;
        if (agent != null) {
            agentAccessService.assertCanView(agent);
        }
        return recallForChat(agent, request.getSessionId(), request.getQuery(), normalizeLimit(request.getLimit()));
    }

    /**
     * 聊天链路召回记忆。
     *
     * @param agent Agent 实体
     * @param sessionId 会话 ID
     * @param query 查询文本
     * @param limit 返回条数
     * @return 召回结果
     */
    @Transactional(rollbackFor = Exception.class)
    public List<MemoryDtos.RecallItem> recallForChat(AgentEntity agent, String sessionId, String query, int limit) {
        if (agent == null || !StringUtils.hasText(query) || !memoryEnabled(agent)) {
            return List.of();
        }
        String strategy = safeText(agent.getMemoryStrategy()).toLowerCase(Locale.ROOT);
        String currentUserId = currentUserIdOrThrow();
        String workspaceId = requireWorkspace(agent);
        limit = Math.min(limit, integer(memoryPolicy(workspaceId, agent.getId()).get("recall_limit"), 8));
        List<AgentMemoryEntity> candidates = agentMemoryMapper.selectList(new LambdaQueryWrapper<AgentMemoryEntity>()
                .eq(AgentMemoryEntity::getWorkspaceId, workspaceId)
                .eq(AgentMemoryEntity::getAgentId, agent.getId())
                .and(wrapper -> wrapper.eq(AgentMemoryEntity::getUserId, currentUserId)
                        .or()
                        .in(AgentMemoryEntity::getPrivacyScope, List.of("agent", "workspace")))
                .eq(AgentMemoryEntity::getStatus, "active")
                .and(wrapper -> wrapper.isNull(AgentMemoryEntity::getValidFrom).or().le(AgentMemoryEntity::getValidFrom, LocalDateTime.now()))
                .and(wrapper -> wrapper.isNull(AgentMemoryEntity::getValidTo).or().gt(AgentMemoryEntity::getValidTo, LocalDateTime.now()))
                .and(wrapper -> wrapper.isNull(AgentMemoryEntity::getExpiredAt).or().gt(AgentMemoryEntity::getExpiredAt, LocalDateTime.now()))
                .orderByDesc(AgentMemoryEntity::getUpdatedAt)
                .last("limit 100"));
        List<Double> queryVector = buildQueryVector(query, candidates);
        Map<String, Double> milvusScores = searchMilvus(agent, workspaceId, currentUserId, queryVector, limit * 4);
        if (!milvusScores.isEmpty()) {
            List<AgentMemoryEntity> vectorCandidates = agentMemoryMapper.selectBatchIds(milvusScores.keySet());
            Map<String, AgentMemoryEntity> merged = new LinkedHashMap<>();
            candidates.forEach(item -> merged.put(item.getId(), item));
            vectorCandidates.stream().filter(item -> workspaceId.equals(item.getWorkspaceId()))
                    .forEach(item -> merged.put(item.getId(), item));
            candidates = new ArrayList<>(merged.values());
        }
        List<MemoryDtos.RecallItem> recalls = candidates.stream()
                .filter(memory -> memoryTypeAllowed(strategy, memory, sessionId))
                .map(memory -> toRecallItem(memory, query, queryVector, milvusScores.get(memory.getId())))
                .filter(item -> item.getScore() >= recallThreshold(workspaceId, agent.getId()))
                .sorted(Comparator.comparingDouble(MemoryDtos.RecallItem::getScore).reversed())
                .limit(Math.max(1, limit))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        recalls.addAll(redisShortTermMemories(workspaceId, agent.getId(), currentUserId, sessionId, query));
        recalls = recalls.stream().sorted(Comparator.comparingDouble(MemoryDtos.RecallItem::getScore).reversed())
                .limit(Math.max(1, limit)).toList();
        markHits(recalls);
        updateRecallMetric(workspaceId, agent.getId(), recalls.size());
        return recalls;
    }

    /**
     * 将一次成功对话沉淀为记忆。
     *
     * @param agent Agent 实体
     * @param sessionId 会话 ID
     * @param userInput 用户输入
     * @param assistantOutput 助手输出
     * @param runId 运行 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void captureConversationMemory(AgentEntity agent,
                                          String sessionId,
                                          String userInput,
                                          String assistantOutput,
                                          String runId) {
        if (agent == null || !memoryEnabled(agent) || !StringUtils.hasText(userInput) || !StringUtils.hasText(assistantOutput)) {
            return;
        }
        String workspaceId = requireWorkspace(agent);
        String userId = currentUserIdOrThrow();
        Map<String, Object> policy = memoryPolicy(workspaceId, agent.getId());
        cacheShortTermConversation(workspaceId, agent.getId(), userId, sessionId, userInput, assistantOutput,
                integer(policy.get("short_term_ttl_days"), 7));
        if (Boolean.FALSE.equals(policy.get("extraction_enabled")) || Integer.valueOf(0).equals(policy.get("extraction_enabled"))) return;
        asyncTaskService.createTask("提取Agent长期记忆", "MEMORY_CAPTURE", "memory_pipeline", runId,
                "runtime_run", runId, workspaceId, Map.of(
                        "agentId", agent.getId(), "sessionId", safeText(sessionId), "runId", safeText(runId),
                        "userId", userId, "userInput", truncate(userInput, 6000)));
    }

    /**
     * 清理记忆。
     *
     * @return 清理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public MemoryDtos.CleanupResult cleanup() {
        assertCanManageCenter();
        return cleanupForWorkspace(requiredCurrentWorkspace());
    }

    /** 按工作空间执行过期归档和低价值清理。 */
    private MemoryDtos.CleanupResult cleanupForWorkspace(String workspaceId) {
        int archivedExpired = jdbcTemplate.update("""
                UPDATE agent_memory
                SET status = 'archived', updated_at = NOW(3)
                WHERE workspace_id = ? AND status = 'active'
                  AND expired_at IS NOT NULL
                  AND expired_at < NOW(3)
                """, workspaceId);
        int deletedLowValue = jdbcTemplate.update("""
                UPDATE agent_memory
                SET status = 'deleted', updated_at = NOW(3)
                WHERE workspace_id = ? AND status = 'archived'
                  AND hit_count = 0
                  AND updated_at < DATE_SUB(NOW(3), INTERVAL 30 DAY)
                """, workspaceId);
        MemoryDtos.CleanupResult result = new MemoryDtos.CleanupResult();
        result.setArchivedExpiredCount(archivedExpired);
        result.setDeletedLowValueCount(deletedLowValue);
        result.setMessages(List.of("已归档过期记忆 " + archivedExpired + " 条", "已删除低价值归档记忆 " + deletedLowValue + " 条"));
        return result;
    }

    /**
     * 提交 Memory 治理清理任务到 Kafka。
     *
     * @return 异步任务详情
     */
    public AsyncTaskDtos.Detail submitCleanupTask() {
        assertCanManageCenter();
        AsyncTaskEntity task = asyncTaskService.createTask(
                "清理过期和低价值 Memory",
                "MEMORY_CLEANUP",
                "memory_governance",
                null,
                "agent_memory",
                null,
                requiredCurrentWorkspace(),
                Map.of("scope", "expired_and_archived_low_value"));
        return asyncTaskService.getTask(task.getId());
    }

    /** 查询Memory生产运营指标。 */
    public Map<String, Object> productionOverview() {
        String workspaceId = requiredCurrentWorkspace();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active", count("SELECT COUNT(1) FROM agent_memory WHERE workspace_id=? AND status='active'", new Object[]{workspaceId}));
        result.put("syncFailed", count("SELECT COUNT(1) FROM agent_memory WHERE workspace_id=? AND status='active' AND sync_status='failed'", new Object[]{workspaceId}));
        result.put("openIssues", count("SELECT COUNT(1) FROM memory_governance_issue WHERE workspace_id=? AND status='open'", new Object[]{workspaceId}));
        result.put("conflicts", count("SELECT COUNT(1) FROM memory_governance_issue WHERE workspace_id=? AND issue_type='conflict' AND status='open'", new Object[]{workspaceId}));
        List<Map<String, Object>> metrics = jdbcTemplate.queryForList("""
                SELECT COALESCE(SUM(extraction_total),0) extractionTotal,
                       COALESCE(SUM(extraction_accepted),0) extractionAccepted,
                       COALESCE(SUM(recall_total),0) recallTotal,
                       COALESCE(SUM(recall_hit_total),0) recallHits,
                       COALESCE(SUM(feedback_positive),0) positiveFeedback,
                       COALESCE(SUM(feedback_negative),0) negativeFeedback
                FROM memory_access_metric WHERE workspace_id=? AND metric_date>=DATE_SUB(CURRENT_DATE,INTERVAL 30 DAY)
                """, workspaceId);
        result.put("last30Days", metrics.isEmpty() ? Map.of() : metrics.getFirst());
        return result;
    }

    /** 查询当前空间的Memory策略。 */
    public List<Map<String, Object>> listPolicies() {
        return jdbcTemplate.queryForList("SELECT * FROM memory_policy WHERE workspace_id=? ORDER BY agent_id IS NULL DESC,updated_at DESC", requiredCurrentWorkspace());
    }

    /** 保存空间或Agent级Memory策略。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> savePolicy(Map<String, Object> request) {
        assertCanManageCenter();
        String workspaceId = requiredCurrentWorkspace();
        String id = String.valueOf(request.getOrDefault("id", newId()));
        String agentId = emptyToNull(String.valueOf(request.getOrDefault("agentId", "")));
        jdbcTemplate.update("""
                INSERT INTO memory_policy(id,workspace_id,agent_id,policy_name,extraction_enabled,min_importance,min_confidence,
                  recall_threshold,recall_limit,prompt_token_budget,short_term_ttl_days,long_term_ttl_days,max_memories_per_user,
                  pii_mode,conflict_mode,status,created_by)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE policy_name=VALUES(policy_name),extraction_enabled=VALUES(extraction_enabled),
                  min_importance=VALUES(min_importance),min_confidence=VALUES(min_confidence),recall_threshold=VALUES(recall_threshold),
                  recall_limit=VALUES(recall_limit),prompt_token_budget=VALUES(prompt_token_budget),short_term_ttl_days=VALUES(short_term_ttl_days),
                  long_term_ttl_days=VALUES(long_term_ttl_days),max_memories_per_user=VALUES(max_memories_per_user),
                  pii_mode=VALUES(pii_mode),conflict_mode=VALUES(conflict_mode),status=VALUES(status)
                """, id, workspaceId, agentId, String.valueOf(request.getOrDefault("policyName", "Memory策略")),
                Boolean.FALSE.equals(request.get("extractionEnabled")) ? 0 : 1,
                number(request.get("minImportance"), 0.55D), number(request.get("minConfidence"), 0.65D),
                number(request.get("recallThreshold"), 0.35D), integer(request.get("recallLimit"), 8),
                integer(request.get("promptTokenBudget"), 1200), integer(request.get("shortTermTtlDays"), 7),
                request.get("longTermTtlDays"), integer(request.get("maxMemoriesPerUser"), 10000),
                String.valueOf(request.getOrDefault("piiMode", "redact")), String.valueOf(request.getOrDefault("conflictMode", "supersede")),
                String.valueOf(request.getOrDefault("status", "enabled")), currentUserIdOrThrow());
        return jdbcTemplate.queryForMap("SELECT * FROM memory_policy WHERE id=?", id);
    }

    /** 分页查询Memory治理问题。 */
    public PageResult<Map<String, Object>> listGovernanceIssues(String status, String type, Integer pageNo, Integer pageSize) {
        String workspaceId = requiredCurrentWorkspace();
        int page = normalizePageNo(pageNo), size = normalizePageSize(pageSize);
        StringBuilder where = new StringBuilder(" WHERE i.workspace_id=? ");
        List<Object> args = new ArrayList<>(List.of(workspaceId));
        if (StringUtils.hasText(status) && !"all".equals(status)) { where.append(" AND i.status=? "); args.add(status); }
        if (StringUtils.hasText(type) && !"all".equals(type)) { where.append(" AND i.issue_type=? "); args.add(type); }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM memory_governance_issue i" + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args); listArgs.add((page - 1) * size); listArgs.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT i.*,m.memory_text,m.fact_key FROM memory_governance_issue i LEFT JOIN agent_memory m ON m.id=i.memory_id" + where + " ORDER BY FIELD(i.severity,'critical','high','medium','low'),i.created_at DESC LIMIT ?,?", listArgs.toArray());
        return new PageResult<>(rows, total == null ? 0 : total, page, size);
    }

    /** 处置Memory治理问题。 */
    public Map<String, Object> resolveGovernanceIssue(String id, Map<String, Object> request) {
        assertCanManageCenter();
        jdbcTemplate.update("UPDATE memory_governance_issue SET status=?,resolution=?,resolved_by=?,resolved_at=NOW(3) WHERE id=? AND workspace_id=?",
                String.valueOf(request.getOrDefault("status", "resolved")), String.valueOf(request.getOrDefault("resolution", "已处置")),
                currentUserIdOrThrow(), id, requiredCurrentWorkspace());
        return jdbcTemplate.queryForMap("SELECT * FROM memory_governance_issue WHERE id=?", id);
    }

    /** 保存召回质量反馈并调整记忆效用分。 */
    @Transactional(rollbackFor = Exception.class)
    public void feedback(String memoryId, Map<String, Object> request) {
        AgentMemoryEntity memory = requireMemory(memoryId);
        String type = String.valueOf(request.getOrDefault("feedbackType", "helpful"));
        boolean positive = "helpful".equals(type);
        jdbcTemplate.update("INSERT INTO memory_feedback(id,workspace_id,memory_id,run_id,user_id,feedback_type,score,comment_text) VALUES(UUID(),?,?,?,?,?,?,?)",
                memory.getWorkspaceId(), memoryId, request.get("runId"), currentUserIdOrThrow(), type,
                request.get("score"), truncate(String.valueOf(request.getOrDefault("comment", "")), 1000));
        jdbcTemplate.update("UPDATE agent_memory SET utility_score=LEAST(1,GREATEST(0,utility_score+?)) WHERE id=?", positive ? 0.05D : -0.1D, memoryId);
        jdbcTemplate.update("""
                INSERT INTO memory_access_metric(id,workspace_id,agent_id,metric_date,feedback_positive,feedback_negative)
                VALUES(UUID(),?,?,CURRENT_DATE,?,?) ON DUPLICATE KEY UPDATE
                  feedback_positive=feedback_positive+VALUES(feedback_positive),feedback_negative=feedback_negative+VALUES(feedback_negative)
                """, memory.getWorkspaceId(), memory.getAgentId(), positive ? 1 : 0, positive ? 0 : 1);
        if (List.of("incorrect", "outdated", "sensitive").contains(type)) {
            createGovernanceIssue(memory.getWorkspaceId(), memoryId, type, "sensitive".equals(type) ? "high" : "medium", request);
        }
    }

    /** 提交向量补偿重建任务。 */
    public AsyncTaskDtos.Detail submitVectorRebuild() {
        assertCanManageCenter();
        AsyncTaskEntity task = asyncTaskService.createTask("重建Memory向量", "MEMORY_VECTOR_REBUILD", "memory_governance", null,
                "agent_memory", null, requiredCurrentWorkspace(), Map.of("scope", "pending_and_failed"));
        return asyncTaskService.getTask(task.getId());
    }

    /** 提交Memory治理扫描任务。 */
    public AsyncTaskDtos.Detail submitGovernanceScan() {
        assertCanManageCenter();
        AsyncTaskEntity task = asyncTaskService.createTask("扫描Memory治理问题", "MEMORY_GOVERNANCE_SCAN", "memory_governance", null,
                "agent_memory", null, requiredCurrentWorkspace(), Map.of("scope", "workspace"));
        return asyncTaskService.getTask(task.getId());
    }

    /** 按主体执行一键遗忘，并同步清理向量。 */
    @Transactional(rollbackFor = Exception.class)
    public int forgetSubject(String subjectId) {
        String workspaceId = requiredCurrentWorkspace();
        String currentUser = currentUserIdOrThrow();
        if (!isMemoryManager() && !currentUser.equals(subjectId)) throw new BusinessException("MEMORY_FORGET_FORBIDDEN", "只能遗忘自己的Memory");
        List<AgentMemoryEntity> memories = agentMemoryMapper.selectList(new LambdaQueryWrapper<AgentMemoryEntity>()
                .eq(AgentMemoryEntity::getWorkspaceId, workspaceId)
                .and(wrapper -> wrapper.eq(AgentMemoryEntity::getSubjectId, subjectId).or().eq(AgentMemoryEntity::getUserId, subjectId))
                .ne(AgentMemoryEntity::getStatus, "deleted"));
        for (AgentMemoryEntity memory : memories) deleteMemory(memory.getId());
        return memories.size();
    }

    /**
     * 返回 Memory 清理任务类型。
     */
    @Override
    public String taskType() {
        return "MEMORY_CLEANUP";
    }

    @Override
    public Set<String> taskTypes() {
        return Set.of("MEMORY_CLEANUP", "MEMORY_CAPTURE", "MEMORY_VECTOR_REBUILD", "MEMORY_GOVERNANCE_SCAN");
    }

    /**
     * 在 Kafka Worker 中执行 Memory 治理清理。
     */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        if ("MEMORY_CAPTURE".equals(task.getTaskType())) return executeCaptureTask(task);
        if ("MEMORY_VECTOR_REBUILD".equals(task.getTaskType())) return executeVectorRebuild(task);
        if ("MEMORY_GOVERNANCE_SCAN".equals(task.getTaskType())) return executeGovernanceScan(task);
        asyncTaskService.updateProgress(task.getId(), "memory_cleanup", "正在清理过期和低价值 Memory", 40, null);
        MemoryDtos.CleanupResult result = cleanupForWorkspace(task.getWorkspaceId());
        return Map.of(
                "archivedExpiredCount", result.getArchivedExpiredCount(),
                "deletedLowValueCount", result.getDeletedLowValueCount(),
                "messages", result.getMessages());
    }

    /** 执行对话事实提取、去重、冲突替代和向量同步。 */
    private Map<String, Object> executeCaptureTask(AsyncTaskEntity task) {
        Map<String, Object> payload = taskPayload(task);
        AgentEntity agent = requireAgent(String.valueOf(payload.get("agentId")));
        String workspaceId = requireWorkspace(agent);
        String userId = String.valueOf(payload.get("userId"));
        String runId = String.valueOf(payload.getOrDefault("runId", ""));
        String sessionId = String.valueOf(payload.getOrDefault("sessionId", ""));
        String userInput = String.valueOf(payload.getOrDefault("userInput", ""));
        asyncTaskService.updateProgress(task.getId(), "extract", "正在提取结构化用户事实", 25, null);
        Map<String, Object> policy = memoryPolicy(workspaceId, agent.getId());
        List<MemoryExtractionService.Candidate> candidates = memoryExtractionService.extract(agent, workspaceId, runId, userInput,
                String.valueOf(policy.getOrDefault("pii_mode", "redact")));
        double minImportance = number(policy.get("min_importance"), 0.55D);
        double minConfidence = number(policy.get("min_confidence"), 0.65D);
        int quota = integer(policy.get("max_memories_per_user"), 10000);
        long activeCount = count("SELECT COUNT(1) FROM agent_memory WHERE workspace_id=? AND user_id=? AND status='active'", new Object[]{workspaceId, userId});
        int accepted = 0;
        int duplicate = 0;
        int rejected = 0;
        for (MemoryExtractionService.Candidate candidate : candidates) {
            if (candidate.importance() < minImportance || candidate.confidence() < minConfidence || activeCount + accepted >= quota) {
                rejected++;
                continue;
            }
            String hash = contentHash(candidate.text());
            Long exists = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_memory WHERE workspace_id=? AND user_id=? AND content_hash=? AND status='active'", Long.class, workspaceId, userId, hash);
            if (exists != null && exists > 0) { duplicate++; continue; }
            AgentMemoryEntity entity = extractedMemory(agent, workspaceId, userId, sessionId, runId, candidate, hash);
            AgentMemoryEntity previous = agentMemoryMapper.selectOne(new LambdaQueryWrapper<AgentMemoryEntity>()
                    .eq(AgentMemoryEntity::getWorkspaceId, workspaceId).eq(AgentMemoryEntity::getAgentId, agent.getId())
                    .eq(AgentMemoryEntity::getUserId, userId).eq(AgentMemoryEntity::getFactKey, candidate.factKey())
                    .eq(AgentMemoryEntity::getStatus, "active").orderByDesc(AgentMemoryEntity::getVersionNo).last("limit 1"));
            if (previous != null) entity.setVersionNo((previous.getVersionNo() == null ? 1 : previous.getVersionNo()) + 1);
            int longTermTtl = integer(policy.get("long_term_ttl_days"), 0);
            if (longTermTtl > 0) entity.setExpiredAt(LocalDateTime.now().plusDays(longTermTtl));
            agentMemoryMapper.insert(entity);
            enrichEmbedding(entity);
            agentMemoryMapper.updateById(entity);
            String conflictMode = String.valueOf(policy.getOrDefault("conflict_mode", "supersede"));
            if (previous != null && !hash.equals(previous.getContentHash()) && "supersede".equals(conflictMode)) {
                previous.setStatus("archived");
                previous.setValidTo(LocalDateTime.now());
                previous.setSupersededBy(entity.getId());
                agentMemoryMapper.updateById(previous);
                createGovernanceIssue(workspaceId, entity.getId(), "conflict", "medium", Map.of("supersededMemoryId", previous.getId()));
            } else if (previous != null && !hash.equals(previous.getContentHash()) && "review".equals(conflictMode)) {
                createGovernanceIssue(workspaceId, entity.getId(), "conflict", "high", Map.of("conflictingMemoryId", previous.getId()));
            }
            accepted++;
        }
        updateExtractionMetric(workspaceId, agent.getId(), candidates.size(), accepted);
        return Map.of("extracted", candidates.size(), "accepted", accepted, "duplicates", duplicate, "rejected", rejected);
    }

    /** 批量补偿待同步或失败的Memory向量。 */
    private Map<String, Object> executeVectorRebuild(AsyncTaskEntity task) {
        String workspaceId = task.getWorkspaceId();
        List<AgentMemoryEntity> memories = agentMemoryMapper.selectList(new LambdaQueryWrapper<AgentMemoryEntity>()
                .eq(StringUtils.hasText(workspaceId), AgentMemoryEntity::getWorkspaceId, workspaceId)
                .in(AgentMemoryEntity::getSyncStatus, List.of("pending", "failed"))
                .eq(AgentMemoryEntity::getStatus, "active").last("limit 500"));
        int success = 0;
        for (AgentMemoryEntity memory : memories) {
            enrichEmbedding(memory);
            agentMemoryMapper.updateById(memory);
            if ("synced".equals(memory.getSyncStatus())) success++;
        }
        return Map.of("processed", memories.size(), "synced", success, "failed", memories.size() - success);
    }

    /** 扫描重复、过期、低价值和同步失败问题。 */
    private Map<String, Object> executeGovernanceScan(AsyncTaskEntity task) {
        String workspaceId = task.getWorkspaceId();
        jdbcTemplate.update("""
                INSERT INTO memory_governance_issue(id,workspace_id,memory_id,issue_type,severity,issue_detail,status)
                SELECT UUID(),m.workspace_id,m.id,'sync_failed','high',JSON_OBJECT('syncError',m.sync_error),'open'
                FROM agent_memory m WHERE m.workspace_id=? AND m.status='active' AND m.sync_status='failed'
                AND NOT EXISTS(SELECT 1 FROM memory_governance_issue i WHERE i.memory_id=m.id AND i.issue_type='sync_failed' AND i.status='open')
                """, workspaceId);
        jdbcTemplate.update("""
                INSERT INTO memory_governance_issue(id,workspace_id,memory_id,issue_type,severity,issue_detail,status)
                SELECT UUID(),m.workspace_id,m.id,'low_value','low',JSON_OBJECT('hitCount',m.hit_count),'open'
                FROM agent_memory m WHERE m.workspace_id=? AND m.status='active' AND m.hit_count=0 AND m.created_at<DATE_SUB(NOW(),INTERVAL 90 DAY)
                AND NOT EXISTS(SELECT 1 FROM memory_governance_issue i WHERE i.memory_id=m.id AND i.issue_type='low_value' AND i.status='open')
                """, workspaceId);
        Long issues = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM memory_governance_issue WHERE workspace_id=? AND status='open'", Long.class, workspaceId);
        return Map.of("openIssues", issues == null ? 0 : issues);
    }

    /** 构造提取后的长期记忆实体。 */
    private AgentMemoryEntity extractedMemory(AgentEntity agent, String workspaceId, String userId, String sessionId,
                                               String runId, MemoryExtractionService.Candidate candidate, String hash) {
        AgentMemoryEntity entity = new AgentMemoryEntity();
        entity.setId(newId()); entity.setWorkspaceId(workspaceId); entity.setOrganizationId(resolveOrganizationId(workspaceId));
        entity.setAgentId(agent.getId()); entity.setUserId(userId); entity.setSessionId(emptyToNull(sessionId));
        entity.setSubjectId(userId); entity.setMemoryType("long_term"); entity.setMemoryKey("fact:" + candidate.factKey());
        entity.setFactKey(candidate.factKey()); entity.setMemoryText(candidate.text()); entity.setContentHash(hash);
        entity.setMemoryValue(toJson(Map.of("source", "llm_extraction", "category", candidate.category(), "runId", runId)));
        entity.setImportanceScore(BigDecimal.valueOf(candidate.importance())); entity.setConfidenceScore(BigDecimal.valueOf(candidate.confidence()));
        entity.setSourceReliability(BigDecimal.valueOf(candidate.sourceReliability())); entity.setUtilityScore(BigDecimal.valueOf(0.5D));
        entity.setStatus("active"); entity.setPrivacyScope("private"); entity.setSourceRunId(emptyToNull(runId));
        entity.setTagsJson(toJson(List.of(candidate.category(), "结构化提取"))); entity.setHitCount(0);
        entity.setSyncStatus("pending"); entity.setSyncRetryCount(0); entity.setMilvusCollectionName(properties.getMilvus().getDefaultMemoryCollection());
        entity.setVectorCollectionId(DEFAULT_MEMORY_VECTOR_COLLECTION_ID); entity.setVectorPrimaryKey("memory_" + entity.getId());
        entity.setVersionNo(1); entity.setValidFrom(LocalDateTime.now()); entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now()); entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    /** 在Redis中保留有限长度的短期会话上下文。 */
    private void cacheShortTermConversation(String workspaceId, String agentId, String userId, String sessionId,
                                            String userInput, String assistantOutput, int ttlDays) {
        if (!StringUtils.hasText(sessionId)) return;
        String key = redisMemoryKey(workspaceId, agentId, userId, sessionId);
        String value = truncate("用户：" + userInput + "\n助手：" + assistantOutput, 2000);
        redisTemplate.opsForList().rightPush(key, value);
        redisTemplate.opsForList().trim(key, -20, -1);
        redisTemplate.expire(key, Duration.ofDays(Math.max(1, ttlDays)));
    }

    /** 读取Redis短期记忆并进行轻量关键词评分。 */
    private List<MemoryDtos.RecallItem> redisShortTermMemories(String workspaceId, String agentId, String userId,
                                                               String sessionId, String query) {
        if (!StringUtils.hasText(sessionId)) return List.of();
        List<String> values = redisTemplate.opsForList().range(redisMemoryKey(workspaceId, agentId, userId, sessionId), -10, -1);
        if (values == null) return List.of();
        List<MemoryDtos.RecallItem> result = new ArrayList<>();
        for (int index = values.size() - 1; index >= 0; index--) {
            String text = values.get(index);
            double score = keywordScore(query, text) * 0.7D + 0.2D;
            if (score < 0.25D) continue;
            MemoryDtos.RecallItem item = new MemoryDtos.RecallItem();
            item.setId("redis:" + index); item.setAgentId(agentId); item.setMemoryType("short_term");
            item.setMemoryText(text); item.setImportanceScore(BigDecimal.valueOf(0.5D)); item.setScore(score);
            result.add(item);
        }
        return result;
    }

    private String redisMemoryKey(String workspaceId, String agentId, String userId, String sessionId) {
        return "oaf:memory:short:" + workspaceId + ":" + agentId + ":" + userId + ":" + sessionId;
    }

    /**
     * 构建 Prompt 注入文本。
     *
     * @param recalls 召回结果
     * @return Prompt 文本
     */
    public String buildMemoryPrompt(List<MemoryDtos.RecallItem> recalls) {
        if (recalls == null || recalls.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("以下是当前 Agent 召回的用户相关记忆。请在回答时参考，但不要泄露内部记忆编号；如果记忆与问题无关，请忽略。\n");
        String workspaceId = WorkspaceContextHolder.current();
        String agentId = recalls.getFirst().getAgentId();
        int maxChars = Math.max(400, integer(memoryPolicy(workspaceId, agentId).get("prompt_token_budget"), 1200) * 4);
        for (int index = 0; index < recalls.size(); index++) {
            MemoryDtos.RecallItem item = recalls.get(index);
            builder.append("\n[记忆").append(index + 1).append("] ")
                    .append(memoryTypeLabel(item.getMemoryType()))
                    .append("，得分 ")
                    .append(String.format(Locale.ROOT, "%.4f", item.getScore()))
                    .append("\n")
                    .append(item.getMemoryText());
            if (builder.length() >= maxChars) break;
        }
        return truncate(builder.toString(), maxChars);
    }

    /**
     * 保存请求字段到实体。
     *
     * @param entity 记忆实体
     * @param request 保存请求
     * @param partial 是否为更新操作
     */
    private void applyRequest(AgentMemoryEntity entity, MemoryDtos.SaveRequest request, boolean partial) {
        if (request == null) {
            throw new BusinessException("MEMORY_REQUEST_EMPTY", "记忆请求不能为空");
        }
        if (!partial || StringUtils.hasText(request.getAgentId())) {
            entity.setAgentId(request.getAgentId());
            if (StringUtils.hasText(request.getAgentId())) {
                agentAccessService.assertCanView(requireAgent(request.getAgentId()));
            }
        }
        if (!partial || StringUtils.hasText(request.getSessionId())) {
            entity.setSessionId(emptyToNull(request.getSessionId()));
        }
        if (!partial || StringUtils.hasText(request.getMemoryType())) {
            entity.setMemoryType(normalizeMemoryType(request.getMemoryType()));
        }
        if (!partial || request.getMemoryKey() != null) {
            entity.setMemoryKey(truncate(request.getMemoryKey(), 160));
        }
        if (!partial || StringUtils.hasText(request.getMemoryText())) {
            if (!StringUtils.hasText(request.getMemoryText())) {
                throw new BusinessException("MEMORY_TEXT_EMPTY", "记忆文本不能为空");
            }
            entity.setMemoryText(request.getMemoryText().trim());
        }
        if (!partial || request.getMemoryValue() != null) {
            entity.setMemoryValue(validJsonOrDefault(request.getMemoryValue(), "{}"));
        }
        if (!partial || request.getImportanceScore() != null) {
            entity.setImportanceScore(clampImportance(request.getImportanceScore()));
        }
        if (!partial || request.getExpiredAt() != null) {
            entity.setExpiredAt(request.getExpiredAt());
        }
        if (!partial || StringUtils.hasText(request.getStatus())) {
            entity.setStatus(normalizeStatus(request.getStatus()));
        }
        if (!partial || StringUtils.hasText(request.getPrivacyScope())) {
            entity.setPrivacyScope(normalizePrivacyScope(request.getPrivacyScope()));
        }
        if (!partial || request.getTagsJson() != null) {
            entity.setTagsJson(validJsonOrDefault(request.getTagsJson(), "[]"));
        }
        if (!partial) {
            AgentEntity ownerAgent = StringUtils.hasText(entity.getAgentId()) ? requireAgent(entity.getAgentId()) : null;
            entity.setWorkspaceId(ownerAgent == null ? WorkspaceContextHolder.current() : ownerAgent.getWorkspaceId());
            entity.setOrganizationId(resolveOrganizationId(entity.getWorkspaceId()));
            entity.setCreatedBy(currentUserIdOrThrow());
            entity.setSyncStatus("pending");
            entity.setSyncRetryCount(0);
            entity.setMilvusCollectionName(properties.getMilvus().getDefaultMemoryCollection());
            entity.setVectorCollectionId(DEFAULT_MEMORY_VECTOR_COLLECTION_ID);
            entity.setVectorPrimaryKey("memory_" + entity.getId());
            entity.setHitCount(0);
            entity.setConfidenceScore(BigDecimal.valueOf(0.8D));
            entity.setSourceReliability(BigDecimal.valueOf(0.8D));
            entity.setUtilityScore(BigDecimal.valueOf(0.5D));
            entity.setVersionNo(1);
            entity.setValidFrom(LocalDateTime.now());
        }
        if (StringUtils.hasText(entity.getMemoryText())) entity.setContentHash(contentHash(entity.getMemoryText()));
    }

    /**
     * 生成并填充记忆向量。
     *
     * @param entity 记忆实体
     */
    private void enrichEmbedding(AgentMemoryEntity entity) {
        if (!List.of("long_term", "vector").contains(safeText(entity.getMemoryType()))) {
            entity.setSyncStatus("skipped");
            return;
        }
        try {
            ModelConfigEntity model = embeddingService.resolveEmbeddingModel(null);
            EmbeddingBatchResult result = embeddingService.embedWithTrace(model, List.of(entity.getMemoryText()));
            List<List<Double>> vectors = result.getVectors();
            if (vectors != null && !vectors.isEmpty()) {
                entity.setEmbeddingJson(toJson(vectors.getFirst()));
                entity.setEmbeddingModelId(model.getId());
                entity.setEmbeddingDimension(vectors.getFirst().size());
                entity.setEmbeddingVersion(model.getModelCode());
                if (Boolean.TRUE.equals(result.getFallbackUsed())) {
                    entity.setSyncStatus("failed");
                    entity.setSyncError("Embedding使用了降级向量，未写入生产Milvus");
                } else {
                    milvusVectorService.upsertMemories(entity.getMilvusCollectionName(), List.of(entity), vectors);
                    entity.setSyncStatus("synced");
                    entity.setSyncError(null);
                    entity.setLastSyncedAt(LocalDateTime.now());
                }
            }
        } catch (Exception exception) {
            // 记忆保存不能因为向量模型欠费、未配置或网络失败而中断，后续可在治理页面补偿重建。
            entity.setSyncStatus("failed");
            entity.setSyncRetryCount((entity.getSyncRetryCount() == null ? 0 : entity.getSyncRetryCount()) + 1);
            entity.setSyncError(truncate(exception.getMessage(), 1000));
            entity.setMemoryValue(mergeJson(entity.getMemoryValue(), Map.of("embeddingError", safeText(exception.getMessage()))));
        }
    }

    /**
     * 将实体转换成召回项。
     *
     * @param memory 记忆实体
     * @param query 查询文本
     * @param queryVector 查询向量
     * @return 召回项
     */
    private MemoryDtos.RecallItem toRecallItem(AgentMemoryEntity memory, String query, List<Double> queryVector, Double milvusScore) {
        MemoryDtos.RecallItem item = new MemoryDtos.RecallItem();
        item.setId(memory.getId());
        item.setAgentId(memory.getAgentId());
        item.setAgentName(agentName(memory.getAgentId()));
        item.setMemoryType(memory.getMemoryType());
        item.setMemoryText(memory.getMemoryText());
        item.setImportanceScore(memory.getImportanceScore());
        item.setScore(calculateScore(memory, query, queryVector, milvusScore));
        return item;
    }

    /**
     * 计算召回得分。
     *
     * @param memory 记忆实体
     * @param query 查询文本
     * @param queryVector 查询向量
     * @return 得分
     */
    private double calculateScore(AgentMemoryEntity memory, String query, List<Double> queryVector, Double milvusScore) {
        double vectorScore = milvusScore == null ? cosine(queryVector, parseVector(memory.getEmbeddingJson())) : milvusScore;
        double keywordScore = keywordScore(query, memory.getMemoryText());
        double importance = memory.getImportanceScore() == null ? 0.5D : memory.getImportanceScore().doubleValue();
        double confidence = memory.getConfidenceScore() == null ? 0.7D : memory.getConfidenceScore().doubleValue();
        double recency = recencyScore(memory.getUpdatedAt());
        double hitBoost = Math.min(0.1D, (memory.getHitCount() == null ? 0 : memory.getHitCount()) * 0.01D);
        return Math.max(vectorScore, keywordScore) * 0.55D + importance * 0.15D + confidence * 0.15D + recency * 0.1D + hitBoost * 0.05D;
    }

    /**
     * 构建查询向量。
     *
     * @param query 查询文本
     * @param candidates 候选记忆
     * @return 查询向量
     */
    private List<Double> buildQueryVector(String query, List<AgentMemoryEntity> candidates) {
        try {
            ModelConfigEntity model = embeddingService.resolveEmbeddingModel(null);
            EmbeddingBatchResult result = embeddingService.embedWithTrace(model, List.of(query));
            return result.getVectors() == null || result.getVectors().isEmpty() ? List.of() : result.getVectors().getFirst();
        } catch (Exception exception) {
            return List.of();
        }
    }

    /** 调用Milvus ANN并转换为记忆ID到相似度映射。 */
    private Map<String, Double> searchMilvus(AgentEntity agent, String workspaceId, String userId,
                                             List<Double> queryVector, int topK) {
        if (queryVector == null || queryVector.isEmpty()) return Map.of();
        try {
            Map<String, Double> scores = new HashMap<>();
            for (MilvusKnowledgeVectorService.MemoryHit hit : milvusVectorService.searchMemories(
                    properties.getMilvus().getDefaultMemoryCollection(), workspaceId, agent.getId(), userId, queryVector, topK)) {
                if (StringUtils.hasText(hit.memoryId())) scores.put(hit.memoryId(), hit.score());
            }
            return scores;
        } catch (Exception ignored) {
            // Milvus短暂不可用时保留关键词候选，生产环境由治理问题和告警推动补偿。
            return Map.of();
        }
    }

    /** 读取Agent级策略，缺失时回退工作空间默认策略。 */
    private Map<String, Object> memoryPolicy(String workspaceId, String agentId) {
        if (!StringUtils.hasText(workspaceId)) return Map.of();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM memory_policy WHERE workspace_id=? AND status='enabled'
                  AND (agent_id=? OR agent_id IS NULL)
                ORDER BY CASE WHEN agent_id=? THEN 0 ELSE 1 END LIMIT 1
                """, workspaceId, agentId, agentId);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    private double recallThreshold(String workspaceId, String agentId) {
        return number(memoryPolicy(workspaceId, agentId).get("recall_threshold"), 0.35D);
    }

    /** 写入每日召回聚合指标。 */
    private void updateRecallMetric(String workspaceId, String agentId, int hitCount) {
        jdbcTemplate.update("""
                INSERT INTO memory_access_metric(id,workspace_id,agent_id,metric_date,recall_total,recall_hit_total)
                VALUES(UUID(),?,?,CURRENT_DATE,1,?)
                ON DUPLICATE KEY UPDATE recall_total=recall_total+1,recall_hit_total=recall_hit_total+VALUES(recall_hit_total)
                """, workspaceId, agentId, hitCount);
    }

    /** 写入每日提取聚合指标。 */
    private void updateExtractionMetric(String workspaceId, String agentId, int total, int accepted) {
        jdbcTemplate.update("""
                INSERT INTO memory_access_metric(id,workspace_id,agent_id,metric_date,extraction_total,extraction_accepted)
                VALUES(UUID(),?,?,CURRENT_DATE,?,?)
                ON DUPLICATE KEY UPDATE extraction_total=extraction_total+VALUES(extraction_total),
                  extraction_accepted=extraction_accepted+VALUES(extraction_accepted)
                """, workspaceId, agentId, total, accepted);
    }

    /** 创建去重后的治理问题。 */
    private void createGovernanceIssue(String workspaceId, String memoryId, String type, String severity, Map<String, Object> detail) {
        jdbcTemplate.update("""
                INSERT INTO memory_governance_issue(id,workspace_id,memory_id,issue_type,severity,issue_detail,status)
                VALUES(UUID(),?,?,?,?,CAST(? AS JSON),'open')
                """, workspaceId, memoryId, type, severity, toJson(detail));
    }

    /** 解析异步任务JSON载荷。 */
    private Map<String, Object> taskPayload(AsyncTaskEntity task) {
        try {
            return objectMapper.readValue(task.getRequestPayload(), new TypeReference<Map<String, Object>>() { });
        } catch (Exception exception) {
            throw new BusinessException("MEMORY_TASK_PAYLOAD_INVALID", "Memory任务参数无效");
        }
    }

    /** 获取Agent工作空间，禁止生产记忆脱离租户边界。 */
    private String requireWorkspace(AgentEntity agent) {
        String workspaceId = agent == null ? WorkspaceContextHolder.current() : agent.getWorkspaceId();
        if (!StringUtils.hasText(workspaceId)) throw new BusinessException("MEMORY_WORKSPACE_REQUIRED", "Memory必须归属工作空间");
        return workspaceId;
    }

    private String requiredCurrentWorkspace() {
        String workspaceId = WorkspaceContextHolder.current();
        if (!StringUtils.hasText(workspaceId)) throw new BusinessException("MEMORY_WORKSPACE_REQUIRED", "请先选择工作空间");
        return workspaceId;
    }

    private String resolveOrganizationId(String workspaceId) {
        if (!StringUtils.hasText(workspaceId)) return null;
        List<String> rows = jdbcTemplate.query("SELECT organization_id FROM oaf_workspace WHERE id=?",
                (rs, rowNum) -> rs.getString(1), workspaceId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** 内容规范化后生成SHA-256幂等哈希。 */
    private String contentHash(String text) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(safeText(text).trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            return DigestUtils.md5DigestAsHex(safeText(text).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** 按更新时间计算指数式时间衰减近似值。 */
    private double recencyScore(LocalDateTime updatedAt) {
        if (updatedAt == null) return 0.3D;
        long days = Math.max(0L, java.time.temporal.ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now()));
        return Math.exp(-days / 90D);
    }

    private double number(Object value, double fallback) { return value instanceof Number number ? number.doubleValue() : fallback; }
    private int integer(Object value, int fallback) { return value instanceof Number number ? number.intValue() : fallback; }

    /**
     * 标记记忆命中。
     *
     * @param recalls 召回项
     */
    private void markHits(List<MemoryDtos.RecallItem> recalls) {
        List<String> ids = recalls.stream().map(MemoryDtos.RecallItem::getId)
                .filter(id -> StringUtils.hasText(id) && !id.startsWith("redis:"))
                .distinct().toList();
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        jdbcTemplate.update("UPDATE agent_memory SET hit_count=hit_count+1,last_accessed_at=NOW(3) WHERE id IN (" + placeholders + ")", ids.toArray());
    }

    /**
     * 判断记忆类型是否符合 Agent 策略。
     *
     * @param strategy Agent 记忆策略
     * @param memory 记忆实体
     * @param sessionId 当前会话 ID
     * @return 是否允许召回
     */
    private boolean memoryTypeAllowed(String strategy, AgentMemoryEntity memory, String sessionId) {
        if ("short_term".equals(strategy)) {
            return "short_term".equals(memory.getMemoryType()) && safeText(sessionId).equals(safeText(memory.getSessionId()));
        }
        if ("long_term".equals(strategy)) {
            return List.of("short_term", "long_term", "task", "vector").contains(memory.getMemoryType())
                    && (!"short_term".equals(memory.getMemoryType()) || safeText(sessionId).equals(safeText(memory.getSessionId())));
        }
        return List.of("long_term", "task", "vector").contains(memory.getMemoryType());
    }

    /**
     * 判断 Agent 是否启用记忆。
     *
     * @param agent Agent 实体
     * @return 是否启用
     */
    private boolean memoryEnabled(AgentEntity agent) {
        return agent != null && StringUtils.hasText(agent.getMemoryStrategy()) && !"none".equalsIgnoreCase(agent.getMemoryStrategy());
    }

    /**
     * 查询单条摘要。
     *
     * @param id 记忆 ID
     * @return 记忆摘要
     */
    private MemoryDtos.Summary findSummary(String id) {
        PageResult<MemoryDtos.Summary> page = listMemories("all", "all", "all", id, 1, 1);
        return page.getRecords().isEmpty() ? toSummary(requireMemory(id)) : page.getRecords().getFirst();
    }

    /**
     * 实体转摘要。
     *
     * @param entity 记忆实体
     * @return 记忆摘要
     */
    private MemoryDtos.Summary toSummary(AgentMemoryEntity entity) {
        MemoryDtos.Summary summary = new MemoryDtos.Summary();
        summary.setId(entity.getId());
        summary.setAgentId(entity.getAgentId());
        summary.setAgentName(agentName(entity.getAgentId()));
        summary.setUserId(entity.getUserId());
        summary.setSessionId(entity.getSessionId());
        summary.setMemoryType(entity.getMemoryType());
        summary.setMemoryKey(entity.getMemoryKey());
        summary.setMemoryText(entity.getMemoryText());
        summary.setMemoryValue(entity.getMemoryValue());
        summary.setSyncStatus(entity.getSyncStatus());
        summary.setImportanceScore(entity.getImportanceScore());
        summary.setExpiredAt(entity.getExpiredAt());
        summary.setStatus(entity.getStatus());
        summary.setPrivacyScope(entity.getPrivacyScope());
        summary.setSourceRunId(entity.getSourceRunId());
        summary.setTagsJson(entity.getTagsJson());
        summary.setHitCount(entity.getHitCount());
        summary.setLastAccessedAt(entity.getLastAccessedAt());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    /**
     * 查询 Agent 名称。
     *
     * @param agentId Agent ID
     * @return Agent 名称
     */
    private String agentName(String agentId) {
        if (!StringUtils.hasText(agentId)) {
            return "用户通用记忆";
        }
        AgentEntity agent = agentMapper.selectById(agentId);
        return agent == null ? "未知 Agent" : agent.getAgentName();
    }

    /**
     * 获取记忆实体。
     *
     * @param id 记忆 ID
     * @return 记忆实体
     */
    private AgentMemoryEntity requireMemory(String id) {
        AgentMemoryEntity entity = agentMemoryMapper.selectById(id);
        if (entity == null || "deleted".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("MEMORY_NOT_FOUND", "记忆不存在");
        }
        assertCanViewMemory(entity);
        return entity;
    }

    /**
     * 获取 Agent 实体。
     *
     * @param agentId Agent ID
     * @return Agent 实体
     */
    private AgentEntity requireAgent(String agentId) {
        AgentEntity agent = agentMapper.selectById(agentId);
        if (agent == null || agent.getDeletedAt() != null) {
            throw new BusinessException("AGENT_NOT_FOUND", "Agent 不存在");
        }
        return agent;
    }

    /**
     * 校验记忆查看权限。
     *
     * @param entity 记忆实体
     */
    private void assertCanViewMemory(AgentMemoryEntity entity) {
        assertWorkspaceScope(entity);
        if (isMemoryManager() || currentUserIdOrThrow().equals(entity.getUserId())) {
            return;
        }
        throw new BusinessException("MEMORY_FORBIDDEN", "没有访问该记忆的权限");
    }

    /**
     * 校验记忆管理权限。
     *
     * @param entity 记忆实体
     */
    private void assertCanManageMemory(AgentMemoryEntity entity) {
        assertWorkspaceScope(entity);
        if (isMemoryManager() || currentUserIdOrThrow().equals(entity.getUserId())) {
            return;
        }
        throw new BusinessException("MEMORY_FORBIDDEN", "没有管理该记忆的权限");
    }

    /**
     * 校验中心级管理权限。
     */
    private void assertCanManageCenter() {
        if (!isMemoryManager()) {
            throw new BusinessException("MEMORY_FORBIDDEN", "没有管理记忆中心的权限");
        }
    }

    /**
     * 追加用户范围过滤。
     *
     * @param where SQL 条件
     * @param args 参数列表
     */
    private void appendUserScope(StringBuilder where, List<Object> args) {
        where.append(" AND m.workspace_id = ? ");
        args.add(requiredCurrentWorkspace());
        if (isMemoryManager()) {
            return;
        }
        where.append(" AND m.user_id = ? ");
        args.add(currentUserIdOrThrow());
    }

    /**
     * 构建用户范围 SQL。
     *
     * @return SQL 片段
     */
    private String userScopeWhere() {
        return " AND m.workspace_id = ? " + (isMemoryManager() ? "" : " AND m.user_id = ? ");
    }

    /**
     * 构建用户范围参数。
     *
     * @return 参数数组
     */
    private Object[] userScopeArgs() {
        List<Object> args = new ArrayList<>();
        args.add(requiredCurrentWorkspace());
        if (!isMemoryManager()) args.add(currentUserIdOrThrow());
        return args.toArray();
    }

    /** 校验当前请求不能越过工作空间边界。 */
    private void assertWorkspaceScope(AgentMemoryEntity entity) {
        String currentWorkspace = requiredCurrentWorkspace();
        if (!currentWorkspace.equals(entity.getWorkspaceId())) {
            throw new BusinessException("MEMORY_WORKSPACE_FORBIDDEN", "不能访问其他工作空间的Memory");
        }
    }

    /**
     * 判断当前用户是否可管理全部记忆。
     *
     * @return 是否可管理
     */
    private boolean isMemoryManager() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("memory:manage");
    }

    /**
     * 判断当前用户是否拥有权限。
     *
     * @param authority 权限标识
     * @return 是否拥有
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = agentAccessService.currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
    }

    /**
     * 统计数量。
     *
     * @param sql SQL
     * @param args 参数
     * @return 数量
     */
    private long count(String sql, Object[] args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    /**
     * 计算关键词得分。
     *
     * @param query 查询文本
     * @param text 记忆文本
     * @return 得分
     */
    private double keywordScore(String query, String text) {
        Set<String> words = tokenize(query);
        if (words.isEmpty() || !StringUtils.hasText(text)) {
            return 0D;
        }
        String target = text.toLowerCase(Locale.ROOT);
        long hits = words.stream().filter(target::contains).count();
        return Math.min(1D, hits / (double) Math.max(1, words.size()));
    }

    /**
     * 分词，兼容中文短句和英文关键字。
     *
     * @param text 文本
     * @return 词集合
     */
    private Set<String> tokenize(String text) {
        Set<String> words = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) {
            return words;
        }
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}\\s]+", " ");
        for (String word : normalized.split(" ")) {
            if (word.length() >= 2) {
                words.add(word);
            }
        }
        if (words.isEmpty() && text.length() >= 2) {
            for (int index = 0; index < text.length() - 1; index++) {
                words.add(text.substring(index, index + 2));
            }
        }
        return words;
    }

    /**
     * 计算余弦相似度。
     *
     * @param left 左向量
     * @param right 右向量
     * @return 相似度
     */
    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || left.size() != right.size()) {
            return 0D;
        }
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < left.size(); index++) {
            dot += left.get(index) * right.get(index);
            leftNorm += left.get(index) * left.get(index);
            rightNorm += right.get(index) * right.get(index);
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    /**
     * 解析向量 JSON。
     *
     * @param json 向量 JSON
     * @return 向量
     */
    private List<Double> parseVector(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    /**
     * 合并 JSON。
     *
     * @param rawJson 原始 JSON
     * @param values 新值
     * @return 合并后的 JSON
     */
    private String mergeJson(String rawJson, Map<String, Object> values) {
        Map<String, Object> merged = new LinkedHashMap<>();
        try {
            if (StringUtils.hasText(rawJson)) {
                merged.putAll(objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {
                }));
            }
        } catch (Exception ignored) {
            // 原始 JSON 异常时直接用新值覆盖。
        }
        merged.putAll(values);
        return toJson(merged);
    }

    /**
     * 校验 JSON，不合法时返回默认值。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return JSON 字符串
     */
    private String validJsonOrDefault(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            objectMapper.readTree(value);
            return value;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    /**
     * 转换 JSON。
     *
     * @param value 对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 限制重要度范围。
     *
     * @param value 原始重要度
     * @return 合法重要度
     */
    private BigDecimal clampImportance(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.5D);
        }
        double number = Math.max(0D, Math.min(1D, value.doubleValue()));
        return BigDecimal.valueOf(number);
    }

    /**
     * 默认重要度。
     *
     * @param memoryType 记忆类型
     * @return 重要度
     */
    private BigDecimal defaultImportance(String memoryType) {
        if ("long_term".equals(memoryType) || "vector".equals(memoryType)) {
            return BigDecimal.valueOf(0.8D);
        }
        if ("task".equals(memoryType)) {
            return BigDecimal.valueOf(0.7D);
        }
        return BigDecimal.valueOf(0.5D);
    }

    /**
     * 规范化记忆类型。
     *
     * @param value 原始类型
     * @return 合法类型
     */
    private String normalizeMemoryType(String value) {
        String type = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "long_term";
        if (!List.of("short_term", "long_term", "task", "vector").contains(type)) {
            throw new BusinessException("MEMORY_TYPE_INVALID", "记忆类型不支持");
        }
        return type;
    }

    /**
     * 规范化状态。
     *
     * @param value 原始状态
     * @return 合法状态
     */
    private String normalizeStatus(String value) {
        String status = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "active";
        return List.of("active", "archived", "deleted").contains(status) ? status : "active";
    }

    /**
     * 规范化可见范围。
     *
     * @param value 原始范围
     * @return 合法范围
     */
    private String normalizePrivacyScope(String value) {
        String scope = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "private";
        return List.of("private", "agent", "workspace").contains(scope) ? scope : "private";
    }

    /**
     * 规范化页码。
     *
     * @param pageNo 页码
     * @return 页码
     */
    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    /**
     * 规范化每页大小。
     *
     * @param pageSize 每页大小
     * @return 每页大小
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    /**
     * 规范化召回条数。
     *
     * @param limit 召回条数
     * @return 召回条数
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    /**
     * 文本截断。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + "…";
    }

    /**
     * 空字符串转 null。
     *
     * @param value 原始值
     * @return 转换值
     */
    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * 文本空值兜底。
     *
     * @param text 原始文本
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 记忆类型中文名。
     *
     * @param type 记忆类型
     * @return 中文名
     */
    private String memoryTypeLabel(String type) {
        return switch (safeText(type)) {
            case "short_term" -> "短期会话记忆";
            case "long_term" -> "长期记忆";
            case "task" -> "任务记忆";
            case "vector" -> "向量记忆";
            default -> "记忆";
        };
    }

    /**
     * 时间类型转换。
     *
     * @param timestamp JDBC 时间
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 生成 UUID。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }
}
