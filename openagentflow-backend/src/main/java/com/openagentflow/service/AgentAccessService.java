package com.openagentflow.service;

import com.openagentflow.entity.AgentEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Agent 资源级访问控制服务。
 *
 * <p>系统级角色负责全局管理，Agent 所有者和资源 ACL 负责单个 Agent 的查看、运行和编辑权限。</p>
 */
@Service
public class AgentAccessService {

    /** 资源 ACL 固定资源类型。 */
    private static final String RESOURCE_TYPE_AGENT = "agent";

    /** JDBC 工具，用于访问通用 ACL 表。 */
    private final JdbcTemplate jdbcTemplate;

    /** 工作空间治理服务，用于判断空间成员是否可以访问资源。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    public AgentAccessService(JdbcTemplate jdbcTemplate,
                              WorkspaceGovernanceService workspaceGovernanceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.workspaceGovernanceService = workspaceGovernanceService;
    }

    /**
     * 校验当前用户是否可以查看 Agent。
     *
     * @param agent Agent 实体
     */
    public void assertCanView(AgentEntity agent) {
        if (!canView(agent)) {
            throw new BusinessException("AGENT_FORBIDDEN", "没有访问该 Agent 的权限");
        }
    }

    /**
     * 校验当前用户是否可以管理 Agent。
     *
     * @param agent Agent 实体
     */
    public void assertCanManage(AgentEntity agent) {
        if (!canManage(agent)) {
            throw new BusinessException("AGENT_FORBIDDEN", "没有管理该 Agent 的权限");
        }
    }

    /**
     * 判断当前用户是否可以查看 Agent。
     *
     * @param agent Agent 实体
     * @return 是否可查看
     */
    public boolean canView(AgentEntity agent) {
        if (agent == null || agent.getDeletedAt() != null) {
            return false;
        }
        if ("public".equalsIgnoreCase(agent.getVisibility())) {
            return true;
        }
        if (isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        if (userId.equals(agent.getOwnerUserId()) || userId.equals(agent.getCreatedBy())) {
            return true;
        }
        return hasAcl(agent.getId(), userId, List.of("owner", "write", "run", "read"))
                || workspaceGovernanceService.canViewResource(
                RESOURCE_TYPE_AGENT,
                agent.getId(),
                agent.getWorkspaceId(),
                agent.getOwnerUserId(),
                agent.getCreatedBy(),
                agent.getVisibility());
    }

    /**
     * 判断当前用户是否可以管理 Agent。
     *
     * @param agent Agent 实体
     * @return 是否可管理
     */
    public boolean canManage(AgentEntity agent) {
        if (agent == null || agent.getDeletedAt() != null) {
            return false;
        }
        if (isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        if (userId.equals(agent.getOwnerUserId()) || userId.equals(agent.getCreatedBy())) {
            return true;
        }
        return hasAcl(agent.getId(), userId, List.of("owner", "write"))
                || workspaceGovernanceService.canManageResource(agent.getWorkspaceId(), agent.getOwnerUserId(), agent.getCreatedBy());
    }

    /**
     * 为新建或复制的 Agent 写入所有者 ACL。
     *
     * @param agentId Agent ID
     * @param ownerUserId 所有者用户 ID
     */
    public void grantOwner(String agentId, String ownerUserId) {
        if (!StringUtils.hasText(agentId) || !StringUtils.hasText(ownerUserId)) {
            return;
        }
        // 使用 INSERT IGNORE 保证重复保存或重试时不会破坏已有 ACL。
        jdbcTemplate.update(
                "INSERT IGNORE INTO iam_resource_acl "
                        + "(id, resource_type, resource_id, subject_type, subject_id, permission_level, created_by) "
                        + "VALUES (?, ?, ?, 'user', ?, 'owner', ?)",
                UUID.randomUUID().toString(), RESOURCE_TYPE_AGENT, agentId, ownerUserId, ownerUserId);
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 当前用户 ID，未登录时返回 null
     */
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 判断当前用户是否是系统级管理角色。
     *
     * @return 是否拥有全局 Agent 管理权限
     */
    private boolean isSystemManager() {
        return hasAuthority("ROLE_super_admin")
                || hasAuthority("ROLE_admin")
                || hasAuthority("agent:manage");
    }

    /**
     * 判断当前用户是否拥有指定权限标识。
     *
     * @param authority 权限标识
     * @return 是否拥有权限
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }

    /**
     * 查询资源 ACL 是否包含指定权限级别。
     *
     * @param agentId Agent ID
     * @param userId 用户 ID
     * @param levels 允许的权限级别
     * @return 是否命中 ACL
     */
    private boolean hasAcl(String agentId, String userId, List<String> levels) {
        if (!StringUtils.hasText(agentId) || !StringUtils.hasText(userId) || levels.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", levels.stream().map(level -> "?").toList());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) "
                        + "FROM iam_resource_acl "
                        + "WHERE resource_type = ? "
                        + "AND resource_id = ? "
                        + "AND subject_type = 'user' "
                        + "AND subject_id = ? "
                        + "AND permission_level IN (" + placeholders + ")",
                Integer.class,
                buildAclArgs(agentId, userId, levels));
        return count != null && count > 0;
    }

    /**
     * 组装 ACL 查询参数。
     *
     * @param agentId Agent ID
     * @param userId 用户 ID
     * @param levels 权限级别
     * @return JDBC 参数数组
     */
    private Object[] buildAclArgs(String agentId, String userId, List<String> levels) {
        Object[] args = new Object[3 + levels.size()];
        args[0] = RESOURCE_TYPE_AGENT;
        args[1] = agentId;
        args[2] = userId;
        for (int index = 0; index < levels.size(); index++) {
            args[3 + index] = levels.get(index);
        }
        return args;
    }
}
