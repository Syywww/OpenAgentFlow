package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.domain.session.AgentMessageSummary;
import com.openagentflow.domain.session.AgentSessionCreateRequest;
import com.openagentflow.domain.session.AgentSessionSummary;
import com.openagentflow.domain.session.AgentSessionUpdateRequest;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.AgentMessageEntity;
import com.openagentflow.entity.AgentSessionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.mapper.AgentMessageMapper;
import com.openagentflow.mapper.AgentSessionMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 历史会话服务。
 *
 * <p>负责会话列表、消息列表、会话归属校验和聊天过程中的消息持久化。</p>
 */
@Service
public class AgentSessionService {

    /** Agent Mapper。 */
    private final AgentMapper agentMapper;

    /** Agent 会话 Mapper。 */
    private final AgentSessionMapper agentSessionMapper;

    /** Agent 消息 Mapper。 */
    private final AgentMessageMapper agentMessageMapper;

    /** Agent 资源访问控制服务。 */
    private final AgentAccessService agentAccessService;

    /** JDBC 工具，用于补充聚合统计。 */
    private final JdbcTemplate jdbcTemplate;

    public AgentSessionService(AgentMapper agentMapper,
                               AgentSessionMapper agentSessionMapper,
                               AgentMessageMapper agentMessageMapper,
                               AgentAccessService agentAccessService,
                               JdbcTemplate jdbcTemplate) {
        this.agentMapper = agentMapper;
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.agentAccessService = agentAccessService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询当前用户在指定 Agent 下的历史会话。
     *
     * @param agentId Agent ID
     * @return 会话摘要列表
     */
    public List<AgentSessionSummary> listSessions(String agentId) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanView(agent);
        String userId = currentUserIdOrThrow();
        return agentSessionMapper.selectList(new LambdaQueryWrapper<AgentSessionEntity>()
                        .eq(AgentSessionEntity::getAgentId, agentId)
                        .eq(AgentSessionEntity::getUserId, userId)
                        .ne(AgentSessionEntity::getStatus, "deleted")
                        .orderByDesc(AgentSessionEntity::getUpdatedAt)
                        .last("limit 100"))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 创建一个空会话。
     *
     * @param agentId Agent ID
     * @param request 创建请求
     * @return 会话摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentSessionSummary createSession(String agentId, AgentSessionCreateRequest request) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanView(agent);
        AgentSessionEntity session = createSessionEntity(agent, request == null ? "" : request.getSessionTitle(), currentUserIdOrThrow());
        agentSessionMapper.insert(session);
        return toSummary(session);
    }

    /**
     * 更新会话标题或状态。
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID
     * @param request 更新请求
     * @return 会话摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentSessionSummary updateSession(String agentId, String sessionId, AgentSessionUpdateRequest request) {
        AgentSessionEntity session = requireOwnedSession(agentId, sessionId);
        if (request != null && StringUtils.hasText(request.getSessionTitle())) {
            session.setSessionTitle(truncate(request.getSessionTitle().trim(), 120));
        }
        if (request != null && StringUtils.hasText(request.getStatus())) {
            session.setStatus(request.getStatus());
        }
        session.setUpdatedAt(LocalDateTime.now());
        agentSessionMapper.updateById(session);
        return toSummary(session);
    }

    /**
     * 删除会话，采用软删除保留审计线索。
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String agentId, String sessionId) {
        AgentSessionEntity session = requireOwnedSession(agentId, sessionId);
        session.setStatus("deleted");
        session.setUpdatedAt(LocalDateTime.now());
        agentSessionMapper.updateById(session);
    }

    /**
     * 查询会话消息列表。
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID
     * @return 消息摘要列表
     */
    public List<AgentMessageSummary> listMessages(String agentId, String sessionId) {
        requireOwnedSession(agentId, sessionId);
        return agentMessageMapper.selectList(new LambdaQueryWrapper<AgentMessageEntity>()
                        .eq(AgentMessageEntity::getSessionId, sessionId)
                        .orderByAsc(AgentMessageEntity::getCreatedAt))
                .stream()
                .map(this::toMessageSummary)
                .toList();
    }

    /**
     * 确保聊天请求拥有一个合法会话；没有传会话 ID 时自动创建新会话。
     *
     * @param agent Agent 实体
     * @param sessionId 请求中的会话 ID
     * @param firstInput 首条用户输入
     * @return 可用会话
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentSessionEntity ensureSession(AgentEntity agent, String sessionId, String firstInput) {
        if (agent == null) {
            return null;
        }
        if (StringUtils.hasText(sessionId)) {
            return requireOwnedSession(agent.getId(), sessionId);
        }
        AgentSessionEntity session = createSessionEntity(agent, firstInput, currentUserIdOrThrow());
        agentSessionMapper.insert(session);
        return session;
    }

    /**
     * 追加用户消息。
     *
     * @param sessionId 会话 ID
     * @param content 消息内容
     * @param runId 运行 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendUserMessage(String sessionId, String content, String runId) {
        appendMessage(sessionId, "user", content, 0, Map.of("runId", safeText(runId)));
    }

    /**
     * 追加助手消息。
     *
     * @param sessionId 会话 ID
     * @param content 消息内容
     * @param tokenCount Token 数量
     * @param metadata 消息元数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendAssistantMessage(String sessionId,
                                       String content,
                                       Integer tokenCount,
                                       Map<String, Object> metadata) {
        appendMessage(sessionId, "assistant", content, tokenCount, metadata);
    }

    /**
     * 追加一条消息并刷新会话更新时间。
     *
     * @param sessionId 会话 ID
     * @param role 消息角色
     * @param content 消息内容
     * @param tokenCount Token 数量
     * @param metadata 消息元数据
     */
    private void appendMessage(String sessionId,
                               String role,
                               String content,
                               Integer tokenCount,
                               Map<String, Object> metadata) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(content)) {
            return;
        }
        AgentSessionEntity session = agentSessionMapper.selectById(sessionId);
        if (session == null || "deleted".equalsIgnoreCase(session.getStatus())) {
            return;
        }
        AgentMessageEntity message = new AgentMessageEntity();
        message.setId(newId());
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setContentType("markdown");
        message.setTokenCount(tokenCount == null ? 0 : tokenCount);
        message.setMetadata(toJson(metadata == null ? Map.of() : metadata));
        message.setCreatedAt(LocalDateTime.now());
        agentMessageMapper.insert(message);

        // 每次追加消息都刷新会话更新时间，历史列表可按最近对话排序。
        session.setUpdatedAt(LocalDateTime.now());
        agentSessionMapper.updateById(session);
    }

    /**
     * 创建会话实体。
     *
     * @param agent Agent 实体
     * @param titleSource 标题来源
     * @param userId 当前用户 ID
     * @return 会话实体
     */
    private AgentSessionEntity createSessionEntity(AgentEntity agent, String titleSource, String userId) {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(newId());
        session.setAgentId(agent.getId());
        session.setUserId(userId);
        session.setSessionTitle(buildTitle(titleSource, agent.getAgentName()));
        session.setStatus("active");
        session.setMetadata("{}");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return session;
    }

    /**
     * 校验会话归属当前用户和当前 Agent。
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID
     * @return 会话实体
     */
    private AgentSessionEntity requireOwnedSession(String agentId, String sessionId) {
        AgentEntity agent = requireAgent(agentId);
        agentAccessService.assertCanView(agent);
        String userId = currentUserIdOrThrow();
        AgentSessionEntity session = agentSessionMapper.selectById(sessionId);
        if (session == null
                || !agentId.equals(session.getAgentId())
                || !userId.equals(session.getUserId())
                || "deleted".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException("AGENT_SESSION_NOT_FOUND", "会话不存在或无权访问");
        }
        return session;
    }

    /**
     * 查询 Agent 实体。
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
     * 转换会话摘要。
     *
     * @param session 会话实体
     * @return 会话摘要
     */
    private AgentSessionSummary toSummary(AgentSessionEntity session) {
        AgentSessionSummary summary = new AgentSessionSummary();
        summary.setId(session.getId());
        summary.setAgentId(session.getAgentId());
        summary.setUserId(session.getUserId());
        summary.setSessionTitle(session.getSessionTitle());
        summary.setStatus(session.getStatus());
        summary.setCreatedAt(session.getCreatedAt());
        summary.setUpdatedAt(session.getUpdatedAt());
        summary.setMessageCount(countMessages(session.getId()));
        summary.setLastMessage(findLastMessage(session.getId()));
        return summary;
    }

    /**
     * 转换消息摘要。
     *
     * @param message 消息实体
     * @return 消息摘要
     */
    private AgentMessageSummary toMessageSummary(AgentMessageEntity message) {
        AgentMessageSummary summary = new AgentMessageSummary();
        summary.setId(message.getId());
        summary.setSessionId(message.getSessionId());
        summary.setRole(message.getRole());
        summary.setContent(message.getContent());
        summary.setContentType(message.getContentType());
        summary.setToolCallId(message.getToolCallId());
        summary.setTokenCount(message.getTokenCount());
        summary.setMetadata(message.getMetadata());
        summary.setCreatedAt(message.getCreatedAt());
        return summary;
    }

    /**
     * 统计会话消息数量。
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    private Integer countMessages(String sessionId) {
        Number count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM agent_message WHERE session_id = ?",
                Number.class,
                sessionId);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询最近一条消息预览。
     *
     * @param sessionId 会话 ID
     * @return 最近消息预览
     */
    private String findLastMessage(String sessionId) {
        List<String> rows = jdbcTemplate.query(
                "SELECT content FROM agent_message WHERE session_id = ? ORDER BY created_at DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("content"),
                sessionId);
        return rows.isEmpty() ? "" : truncate(rows.get(0), 90);
    }

    /**
     * 构建会话标题。
     *
     * @param input 用户输入或标题
     * @param agentName Agent 名称
     * @return 会话标题
     */
    private String buildTitle(String input, String agentName) {
        if (StringUtils.hasText(input)) {
            return truncate(input.trim().replaceAll("\\s+", " "), 42);
        }
        return truncate(safeText(agentName) + " 的新会话", 42);
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
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 截断文本。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + "…";
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
     * 简单 JSON 序列化，当前只处理扁平 Map，避免为消息落库再引入额外依赖。
     *
     * @param value 元数据
     * @return JSON 字符串
     */
    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            if (index++ > 0) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            Object item = entry.getValue();
            if (item instanceof Number || item instanceof Boolean) {
                builder.append(item);
            } else {
                builder.append('"').append(escape(String.valueOf(item))).append('"');
            }
        }
        builder.append('}');
        return builder.toString();
    }

    /**
     * 转义 JSON 字符串。
     *
     * @param text 原始文本
     * @return 转义文本
     */
    private String escape(String text) {
        return safeText(text).replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
