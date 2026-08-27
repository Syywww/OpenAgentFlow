package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.iam.PermissionGovernanceDtos;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.DataScopePolicy;
import com.openagentflow.security.PlatformAuthorityPolicy;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 工作空间角色、权限和数据范围服务。
 */
@Service
public class WorkspaceAuthorizationService {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    /** 空间治理服务。 */
    private final WorkspaceGovernanceService workspaceGovernanceService;

    public WorkspaceAuthorizationService(JdbcTemplate jdbcTemplate,
                                         ObjectMapper objectMapper,
                                         WorkspaceGovernanceService workspaceGovernanceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.workspaceGovernanceService = workspaceGovernanceService;
    }

    /**
     * 判断当前用户是否拥有任意一个系统或当前空间权限。
     *
     * @param permissionCodes 权限编码集合
     * @return 是否拥有权限
     */
    public boolean currentUserHasAnyPermission(Collection<String> permissionCodes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
        if (PlatformAuthorityPolicy.isPlatformManager(authorities)
                || permissionCodes.stream().anyMatch(authorities::contains)) {
            return true;
        }
        if (!(authentication.getPrincipal() instanceof AuthUserDetails userDetails)) {
            return false;
        }
        return hasAnyPermission(WorkspaceContextHolder.current(), userDetails.getUserId(), permissionCodes);
    }

    /**
     * 判断用户在指定空间是否拥有任意权限。
     */
    public boolean hasAnyPermission(String workspaceId, String userId, Collection<String> permissionCodes) {
        if (!StringUtils.hasText(workspaceId) || !StringUtils.hasText(userId)
                || permissionCodes == null || permissionCodes.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", permissionCodes.stream().map(item -> "?").toList());
        Object[] parameters = new Object[2 + permissionCodes.size()];
        parameters[0] = workspaceId;
        parameters[1] = userId;
        int index = 2;
        for (String code : permissionCodes) {
            parameters[index++] = code;
        }
        String permissionSql = """
                SELECT COUNT(1)
                FROM iam_workspace_member_role mr
                JOIN iam_workspace_role role ON role.id=mr.role_id AND role.workspace_id=mr.workspace_id
                JOIN iam_workspace_role_permission rp ON rp.role_id=role.id
                JOIN iam_permission p ON p.id=rp.permission_id
                JOIN oaf_workspace_member wm ON wm.workspace_id=mr.workspace_id AND wm.user_id=mr.user_id
                WHERE mr.workspace_id=? AND mr.user_id=?
                  AND role.status='enabled' AND p.status='enabled'
                  AND wm.status IN ('active','enabled')
                  AND p.permission_code IN (%s)
                """.formatted(placeholders);
        Long count = jdbcTemplate.queryForObject(permissionSql, Long.class, parameters);
        if (count != null && count > 0) {
            return true;
        }
        // 兼容尚未完成多角色回填的旧空间，所有者和空间管理员拥有当前空间业务权限。
        Long legacyManager = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM oaf_workspace_member
                WHERE workspace_id=? AND user_id=? AND member_role IN ('owner','admin')
                  AND status IN ('active','enabled')
                """, Long.class, workspaceId, userId);
        return legacyManager != null && legacyManager > 0;
    }

    /** 校验当前用户是否拥有空间权限治理管理能力。 */
    public void assertCanManageGovernance(String workspaceId) {
        assertCurrentUserPermission(workspaceId, List.of("iam:governance:manage"),
                "WORKSPACE_GOVERNANCE_FORBIDDEN", "没有管理该工作空间权限配置的权限");
    }

    /** 校验当前用户是否拥有空间资源授权管理能力。 */
    public void assertCanManageAcl(String workspaceId) {
        assertCurrentUserPermission(workspaceId, List.of("iam:acl:manage"),
                "WORKSPACE_ACL_FORBIDDEN", "没有管理该工作空间资源授权的权限");
    }

    /** 查询权限治理概览。 */
    public PermissionGovernanceDtos.GovernanceOverview overview(String workspaceId) {
        assertCanViewGovernance(workspaceId);
        return new PermissionGovernanceDtos.GovernanceOverview(
                count("SELECT COUNT(1) FROM iam_workspace_role WHERE workspace_id=?", workspaceId),
                count("SELECT COUNT(1) FROM iam_workspace_member_role WHERE workspace_id=?", workspaceId),
                count("SELECT COUNT(1) FROM iam_resource_acl WHERE workspace_id=? AND status='enabled' AND (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP(3))", workspaceId),
                count("SELECT COUNT(1) FROM iam_authorization_audit WHERE workspace_id=?", workspaceId));
    }

    /** 查询空间角色。 */
    public List<PermissionGovernanceDtos.WorkspaceRoleSummary> listRoles(String workspaceId) {
        assertCanViewGovernance(workspaceId);
        return jdbcTemplate.query("""
                SELECT id,workspace_id,role_code,role_name,description,data_scope,built_in,status
                FROM iam_workspace_role WHERE workspace_id=? ORDER BY built_in DESC,role_name
                """, (rs, rowNum) -> new PermissionGovernanceDtos.WorkspaceRoleSummary(
                rs.getString("id"), rs.getString("workspace_id"), rs.getString("role_code"),
                rs.getString("role_name"), rs.getString("description"), rs.getString("data_scope"),
                rs.getBoolean("built_in"), rs.getString("status"),
                jdbcTemplate.queryForList("SELECT permission_id FROM iam_workspace_role_permission WHERE role_id=?", String.class, rs.getString("id")),
                jdbcTemplate.queryForList("""
                        SELECT p.permission_code FROM iam_workspace_role_permission rp
                        JOIN iam_permission p ON p.id=rp.permission_id WHERE rp.role_id=? ORDER BY p.permission_code
                        """, String.class, rs.getString("id")),
                jdbcTemplate.queryForList("SELECT department_id FROM iam_workspace_role_department WHERE role_id=?", String.class, rs.getString("id")),
                count("SELECT COUNT(1) FROM iam_workspace_member_role WHERE role_id=?", rs.getString("id"))),
                workspaceId);
    }

    /** 创建或更新空间角色。 */
    @Transactional(rollbackFor = Exception.class)
    public PermissionGovernanceDtos.WorkspaceRoleSummary saveRole(String roleId,
                                                                   PermissionGovernanceDtos.WorkspaceRoleRequest request) {
        assertCanManageGovernance(request.workspaceId());
        validateDataScope(request.dataScope());
        String id = StringUtils.hasText(roleId) ? roleId : UUID.randomUUID().toString();
        if (StringUtils.hasText(roleId)) {
            Long exists = count("SELECT COUNT(1) FROM iam_workspace_role WHERE id=? AND workspace_id=?", roleId, request.workspaceId());
            if (exists == 0) {
                throw new BusinessException("WORKSPACE_ROLE_NOT_FOUND", "工作空间角色不存在");
            }
            jdbcTemplate.update("""
                    UPDATE iam_workspace_role SET role_name=?,description=?,data_scope=?,status=? WHERE id=? AND workspace_id=?
                    """, request.roleName().trim(), request.description(), request.dataScope(), request.status(), id, request.workspaceId());
        } else {
            jdbcTemplate.update("""
                    INSERT INTO iam_workspace_role
                      (id,workspace_id,role_code,role_name,description,data_scope,built_in,status,created_by)
                    VALUES (?,?,?,?,?,?,0,?,?)
                    """, id, request.workspaceId(), request.roleCode().trim(), request.roleName().trim(),
                    request.description(), request.dataScope(), request.status(), currentUserId());
        }
        replaceRoleRelations(id, request.permissionIds(), request.departmentIds());
        writeAudit(request.workspaceId(), StringUtils.hasText(roleId) ? "update_role" : "create_role",
                "workspace_role", id, null, null, request.description(), Map.of(), Map.of("roleCode", request.roleCode()));
        return listRoles(request.workspaceId()).stream().filter(item -> id.equals(item.id())).findFirst().orElseThrow();
    }

    /** 删除非内置空间角色。 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String workspaceId, String roleId) {
        assertCanManageGovernance(workspaceId);
        Long builtIn = count("SELECT COUNT(1) FROM iam_workspace_role WHERE id=? AND workspace_id=? AND built_in=1", roleId, workspaceId);
        if (builtIn > 0) {
            throw new BusinessException("WORKSPACE_ROLE_BUILT_IN", "内置空间角色不能删除");
        }
        jdbcTemplate.update("DELETE FROM iam_workspace_role WHERE id=? AND workspace_id=?", roleId, workspaceId);
        writeAudit(workspaceId, "delete_role", "workspace_role", roleId, null, null, null, Map.of(), Map.of());
    }

    /** 为空间成员重新分配角色。 */
    @Transactional(rollbackFor = Exception.class)
    public void assignMemberRoles(String workspaceId, String userId,
                                  PermissionGovernanceDtos.MemberRoleRequest request) {
        assertCanManageGovernance(workspaceId);
        Long member = count("SELECT COUNT(1) FROM oaf_workspace_member WHERE workspace_id=? AND user_id=? AND status IN ('active','enabled')", workspaceId, userId);
        if (member == 0) {
            throw new BusinessException("WORKSPACE_MEMBER_NOT_FOUND", "工作空间成员不存在");
        }
        List<String> roleIds = request.roleIds() == null ? List.of() : request.roleIds().stream().distinct().toList();
        for (String roleId : roleIds) {
            if (count("SELECT COUNT(1) FROM iam_workspace_role WHERE id=? AND workspace_id=? AND status='enabled'", roleId, workspaceId) == 0) {
                throw new BusinessException("WORKSPACE_ROLE_INVALID", "包含不属于当前空间的角色");
            }
        }
        List<String> before = jdbcTemplate.queryForList("SELECT role_id FROM iam_workspace_member_role WHERE workspace_id=? AND user_id=?", String.class, workspaceId, userId);
        jdbcTemplate.update("DELETE FROM iam_workspace_member_role WHERE workspace_id=? AND user_id=?", workspaceId, userId);
        roleIds.forEach(roleId -> jdbcTemplate.update("""
                INSERT INTO iam_workspace_member_role(workspace_id,user_id,role_id,created_by) VALUES (?,?,?,?)
                """, workspaceId, userId, roleId, currentUserId()));
        writeAudit(workspaceId, "assign_role", "workspace_member", userId, "user", userId,
                request.reason(), Map.of("roleIds", before), Map.of("roleIds", roleIds));
    }

    /** 查询空间成员当前绑定的角色ID。 */
    public List<String> listMemberRoleIds(String workspaceId, String userId) {
        assertCanViewGovernance(workspaceId);
        return jdbcTemplate.queryForList("""
                SELECT role_id FROM iam_workspace_member_role
                WHERE workspace_id=? AND user_id=? ORDER BY created_at
                """, String.class, workspaceId, userId);
    }

    /** 解析用户在当前空间的数据范围。 */
    public PermissionGovernanceDtos.DataScopeResult resolveDataScope(String workspaceId, String userId) {
        assertCanViewGovernance(workspaceId);
        return resolveDataScopeInternal(workspaceId, userId);
    }

    /** 解析用户在当前空间的数据范围（不校验治理查看权限，供列表过滤等内部消费复用）。 */
    PermissionGovernanceDtos.DataScopeResult resolveDataScopeInternal(String workspaceId, String userId) {
        List<String> scopes = jdbcTemplate.queryForList("""
                SELECT DISTINCT role.data_scope FROM iam_workspace_member_role mr
                JOIN iam_workspace_role role ON role.id=mr.role_id
                WHERE mr.workspace_id=? AND mr.user_id=? AND role.status='enabled'
                """, String.class, workspaceId, userId);
        String merged = DataScopePolicy.merge(scopes);
        Set<String> departments = new LinkedHashSet<>();
        if ("custom".equals(merged)) {
            departments.addAll(jdbcTemplate.queryForList("""
                    SELECT DISTINCT rd.department_id FROM iam_workspace_member_role mr
                    JOIN iam_workspace_role_department rd ON rd.role_id=mr.role_id
                    WHERE mr.workspace_id=? AND mr.user_id=?
                    """, String.class, workspaceId, userId));
        } else if (List.of("dept", "dept_tree").contains(merged)) {
            departments.addAll(jdbcTemplate.queryForList("SELECT department_id FROM iam_user WHERE id=? AND department_id IS NOT NULL", String.class, userId));
            if ("dept_tree".equals(merged) && !departments.isEmpty()) {
                departments.addAll(findDescendantDepartments(departments));
            }
        }
        return new PermissionGovernanceDtos.DataScopeResult(merged, List.copyOf(departments), "self".equals(merged));
    }

    /** 查询授权审计。 */
    public List<PermissionGovernanceDtos.AuthorizationAuditSummary> listAudits(String workspaceId) {
        assertCanViewGovernance(workspaceId);
        return jdbcTemplate.query("""
                SELECT * FROM iam_authorization_audit WHERE workspace_id=? ORDER BY created_at DESC LIMIT 200
                """, (rs, rowNum) -> new PermissionGovernanceDtos.AuthorizationAuditSummary(
                rs.getString("id"), rs.getString("workspace_id"), rs.getString("operator_user_id"),
                rs.getString("action_type"), rs.getString("target_type"), rs.getString("target_id"),
                rs.getString("subject_type"), rs.getString("subject_id"), rs.getString("reason"),
                readMap(rs.getString("before_data")), readMap(rs.getString("after_data")),
                rs.getTimestamp("created_at").toLocalDateTime()),
                workspaceId);
    }

    /** 写入授权审计。 */
    public void writeAudit(String workspaceId, String actionType, String targetType, String targetId,
                           String subjectType, String subjectId, String reason,
                           Map<String, Object> beforeData, Map<String, Object> afterData) {
        jdbcTemplate.update("""
                INSERT INTO iam_authorization_audit
                  (id,workspace_id,operator_user_id,action_type,target_type,target_id,subject_type,subject_id,before_data,after_data,reason)
                VALUES (?,?,?,?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON),?)
                """, UUID.randomUUID().toString(), workspaceId, currentUserId(), actionType, targetType, targetId,
                subjectType, subjectId, toJson(beforeData), toJson(afterData), reason);
    }

    /** 判断当前用户能否查看空间。 */
    private boolean canViewWorkspace(String workspaceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserDetails userDetails)) {
            return false;
        }
        Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        return PlatformAuthorityPolicy.isPlatformManager(authorities)
                || count("SELECT COUNT(1) FROM oaf_workspace_member WHERE workspace_id=? AND user_id=? AND status IN ('active','enabled')", workspaceId, userDetails.getUserId()) > 0;
    }

    /** 校验当前用户是否拥有空间权限治理查看能力。 */
    private void assertCanViewGovernance(String workspaceId) {
        assertCurrentUserPermission(workspaceId, List.of("iam:governance:view", "iam:governance:manage"),
                "WORKSPACE_GOVERNANCE_FORBIDDEN", "没有查看该工作空间权限治理数据的权限");
    }

    /** 按指定权限集合执行空间成员和权限双重校验。 */
    private void assertCurrentUserPermission(String workspaceId, Collection<String> permissions,
                                             String errorCode, String errorMessage) {
        String userId = currentUserId();
        if (!canViewWorkspace(workspaceId)
                || (!workspaceGovernanceService.isSystemManager()
                && !hasAnyPermission(workspaceId, userId, permissions))) {
            throw new BusinessException(errorCode, errorMessage);
        }
    }

    /** 替换角色权限和自定义部门范围。 */
    private void replaceRoleRelations(String roleId, List<String> permissionIds, List<String> departmentIds) {
        jdbcTemplate.update("DELETE FROM iam_workspace_role_permission WHERE role_id=?", roleId);
        if (permissionIds != null) {
            permissionIds.stream().filter(StringUtils::hasText).distinct().forEach(permissionId ->
                    jdbcTemplate.update("INSERT INTO iam_workspace_role_permission(role_id,permission_id,created_by) VALUES (?,?,?)",
                            roleId, permissionId, currentUserId()));
        }
        jdbcTemplate.update("DELETE FROM iam_workspace_role_department WHERE role_id=?", roleId);
        if (departmentIds != null) {
            departmentIds.stream().filter(StringUtils::hasText).distinct().forEach(departmentId ->
                    jdbcTemplate.update("INSERT INTO iam_workspace_role_department(role_id,department_id,created_by) VALUES (?,?,?)",
                            roleId, departmentId, currentUserId()));
        }
    }

    /** 递归查询下级部门。 */
    private Set<String> findDescendantDepartments(Set<String> roots) {
        Set<String> result = new LinkedHashSet<>();
        Set<String> frontier = new LinkedHashSet<>(roots);
        while (!frontier.isEmpty()) {
            String placeholders = String.join(",", frontier.stream().map(item -> "?").toList());
            List<String> children = jdbcTemplate.queryForList("SELECT id FROM iam_department WHERE parent_id IN (" + placeholders + ") AND status='enabled'",
                    String.class, frontier.toArray());
            frontier = new LinkedHashSet<>(children);
            frontier.removeAll(result);
            result.addAll(frontier);
        }
        return result;
    }

    /** 校验数据范围枚举。 */
    private void validateDataScope(String value) {
        if (!List.of("all", "dept", "dept_tree", "self", "custom").contains(value)) {
            throw new BusinessException("DATA_SCOPE_INVALID", "数据范围不受支持");
        }
    }

    /** 获取当前用户ID。 */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUserDetails details
                ? details.getUserId() : null;
    }

    /** 查询数量。 */
    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    /** JSON序列化。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new BusinessException("AUTHORIZATION_JSON_INVALID", "授权审计数据无法序列化");
        }
    }

    /** JSON反序列化。 */
    private Map<String, Object> readMap(String value) {
        try {
            return !StringUtils.hasText(value) ? Map.of() : objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
