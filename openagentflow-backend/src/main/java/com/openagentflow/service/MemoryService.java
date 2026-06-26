package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.memory.MemoryDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentMemoryEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.AgentMemoryMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Memory 记忆中心服务。
 *
 * <p>负责短期记忆、长期记忆、任务记忆和向量记忆的保存、召回、治理清理和聊天链路自动沉淀。</p>
 */
@Service
public class MemoryService {

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

    public MemoryService(AgentMemoryMapper agentMemoryMapper,
                         AgentMapper agentMapper,
                         AgentAccessService agentAccessService,
                         EmbeddingService embeddingService,
                         JdbcTemplate jdbcTemplate,
                         ObjectMapper objectMapper,
                         OpenAgentFlowProperties properties) {
        this.agentMemoryMapper = agentMemoryMapper;
        this.agentMapper = agentMapper;
        this.agentAccessService = agentAccessService;
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
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
        entity.setStatus("deleted");
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
        List<AgentMemoryEntity> candidates = agentMemoryMapper.selectList(new LambdaQueryWrapper<AgentMemoryEntity>()
                .eq(AgentMemoryEntity::getAgentId, agent.getId())
                .and(wrapper -> wrapper.eq(AgentMemoryEntity::getUserId, currentUserId)
                        .or()
                        .in(AgentMemoryEntity::getPrivacyScope, List.of("agent", "workspace")))
                .eq(AgentMemoryEntity::getStatus, "active")
                .and(wrapper -> wrapper.isNull(AgentMemoryEntity::getExpiredAt).or().gt(AgentMemoryEntity::getExpiredAt, LocalDateTime.now()))
                .orderByDesc(AgentMemoryEntity::getUpdatedAt)
                .last("limit 200"));
        List<Double> queryVector = buildQueryVector(query, candidates);
        List<MemoryDtos.RecallItem> recalls = candidates.stream()
                .filter(memory -> memoryTypeAllowed(strategy, memory, sessionId))
                .map(memory -> toRecallItem(memory, query, queryVector))
                .filter(item -> item.getScore() > 0.05D)
                .sorted(Comparator.comparingDouble(MemoryDtos.RecallItem::getScore).reversed())
                .limit(Math.max(1, limit))
                .toList();
        markHits(recalls);
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
        String memoryType = "long_term".equalsIgnoreCase(agent.getMemoryStrategy()) ? "long_term" : "short_term";
        if ("vector".equalsIgnoreCase(agent.getMemoryStrategy())) {
            memoryType = "vector";
        }
        AgentMemoryEntity entity = new AgentMemoryEntity();
        entity.setId(newId());
        entity.setAgentId(agent.getId());
        entity.setUserId(currentUserIdOrThrow());
        entity.setSessionId(sessionId);
        entity.setMemoryType(memoryType);
        entity.setMemoryKey("chat:" + safeText(runId));
        entity.setMemoryText(truncate("用户：" + userInput + "\n助手：" + assistantOutput, 1600));
        entity.setMemoryValue(toJson(Map.of(
                "source", "agent_chat",
                "runId", safeText(runId),
                "agentId", agent.getId(),
                "sessionId", safeText(sessionId)
        )));
        entity.setImportanceScore(defaultImportance(memoryType));
        entity.setExpiredAt("short_term".equals(memoryType) ? LocalDateTime.now().plusDays(7) : null);
        entity.setStatus("active");
        entity.setPrivacyScope("private");
        entity.setSourceRunId(runId);
        entity.setTagsJson("[\"自动沉淀\"]");
        entity.setHitCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        enrichEmbedding(entity);
        agentMemoryMapper.insert(entity);
    }

    /**
     * 清理记忆。
     *
     * @return 清理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public MemoryDtos.CleanupResult cleanup() {
        assertCanManageCenter();
        int archivedExpired = jdbcTemplate.update("""
                UPDATE agent_memory
                SET status = 'archived', updated_at = NOW(3)
                WHERE status = 'active'
                  AND expired_at IS NOT NULL
                  AND expired_at < NOW(3)
                """);
        int deletedLowValue = jdbcTemplate.update("""
                UPDATE agent_memory
                SET status = 'deleted', updated_at = NOW(3)
                WHERE status = 'archived'
                  AND hit_count = 0
                  AND updated_at < DATE_SUB(NOW(3), INTERVAL 30 DAY)
                """);
        MemoryDtos.CleanupResult result = new MemoryDtos.CleanupResult();
        result.setArchivedExpiredCount(archivedExpired);
        result.setDeletedLowValueCount(deletedLowValue);
        result.setMessages(List.of("已归档过期记忆 " + archivedExpired + " 条", "已删除低价值归档记忆 " + deletedLowValue + " 条"));
        return result;
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
        for (int index = 0; index < recalls.size(); index++) {
            MemoryDtos.RecallItem item = recalls.get(index);
            builder.append("\n[记忆").append(index + 1).append("] ")
                    .append(memoryTypeLabel(item.getMemoryType()))
                    .append("，得分 ")
                    .append(String.format(Locale.ROOT, "%.4f", item.getScore()))
                    .append("\n")
                    .append(item.getMemoryText());
        }
        return builder.toString();
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
            entity.setSyncStatus("pending");
            entity.setMilvusCollectionName(properties.getMilvus().getDefaultMemoryCollection());
            entity.setVectorCollectionId(DEFAULT_MEMORY_VECTOR_COLLECTION_ID);
            entity.setVectorPrimaryKey("memory_" + entity.getId());
            entity.setHitCount(0);
        }
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
                entity.setSyncStatus(Boolean.TRUE.equals(result.getFallbackUsed()) ? "failed" : "synced");
                entity.setLastSyncedAt(LocalDateTime.now());
            }
        } catch (Exception exception) {
            // 记忆保存不能因为向量模型欠费、未配置或网络失败而中断，后续可在治理页面补偿重建。
            entity.setSyncStatus("failed");
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
    private MemoryDtos.RecallItem toRecallItem(AgentMemoryEntity memory, String query, List<Double> queryVector) {
        MemoryDtos.RecallItem item = new MemoryDtos.RecallItem();
        item.setId(memory.getId());
        item.setAgentId(memory.getAgentId());
        item.setAgentName(agentName(memory.getAgentId()));
        item.setMemoryType(memory.getMemoryType());
        item.setMemoryText(memory.getMemoryText());
        item.setImportanceScore(memory.getImportanceScore());
        item.setScore(calculateScore(memory, query, queryVector));
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
    private double calculateScore(AgentMemoryEntity memory, String query, List<Double> queryVector) {
        double vectorScore = cosine(queryVector, parseVector(memory.getEmbeddingJson()));
        double keywordScore = keywordScore(query, memory.getMemoryText());
        double importance = memory.getImportanceScore() == null ? 0.5D : memory.getImportanceScore().doubleValue();
        double hitBoost = Math.min(0.1D, (memory.getHitCount() == null ? 0 : memory.getHitCount()) * 0.01D);
        return Math.max(vectorScore, keywordScore) * 0.75D + importance * 0.2D + hitBoost;
    }

    /**
     * 构建查询向量。
     *
     * @param query 查询文本
     * @param candidates 候选记忆
     * @return 查询向量
     */
    private List<Double> buildQueryVector(String query, List<AgentMemoryEntity> candidates) {
        boolean hasVectorCandidate = candidates.stream().anyMatch(item -> StringUtils.hasText(item.getEmbeddingJson()));
        if (!hasVectorCandidate) {
            return List.of();
        }
        try {
            ModelConfigEntity model = embeddingService.resolveEmbeddingModel(null);
            EmbeddingBatchResult result = embeddingService.embedWithTrace(model, List.of(query));
            return result.getVectors() == null || result.getVectors().isEmpty() ? List.of() : result.getVectors().getFirst();
        } catch (Exception exception) {
            return List.of();
        }
    }

    /**
     * 标记记忆命中。
     *
     * @param recalls 召回项
     */
    private void markHits(List<MemoryDtos.RecallItem> recalls) {
        for (MemoryDtos.RecallItem item : recalls) {
            jdbcTemplate.update("UPDATE agent_memory SET hit_count = hit_count + 1, last_accessed_at = NOW(3) WHERE id = ?", item.getId());
        }
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
        return isMemoryManager() ? "" : " AND m.user_id = ? ";
    }

    /**
     * 构建用户范围参数。
     *
     * @return 参数数组
     */
    private Object[] userScopeArgs() {
        return isMemoryManager() ? new Object[]{} : new Object[]{currentUserIdOrThrow()};
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
