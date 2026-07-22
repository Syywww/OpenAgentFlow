package com.openagentflow.service;

import com.openagentflow.domain.iam.PermissionGovernanceDtos;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.PlatformAuthorityPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 通用资源访问控制服务。
 *
 * <p>统一支持用户、工作空间角色和部门主体，并自动排除停用或已过期授权。</p>
 */
@Service
public class ResourceAclService {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 空间授权服务。 */
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public ResourceAclService(JdbcTemplate jdbcTemplate,
                              WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    /** 查询工作空间资源授权。 */
    public List<PermissionGovernanceDtos.ResourceAclSummary> list(String workspaceId) {
        workspaceAuthorizationService.assertCanManageAcl(workspaceId);
        return jdbcTemplate.query("""
                SELECT id,workspace_id,resource_type,resource_id,subject_type,subject_id,permission_level,
                       status,expires_at,grant_reason,granted_by,created_at
                FROM iam_resource_acl WHERE workspace_id=? ORDER BY created_at DESC LIMIT 500
                """, (rs, rowNum) -> new PermissionGovernanceDtos.ResourceAclSummary(
                rs.getString("id"), rs.getString("workspace_id"), rs.getString("resource_type"),
                rs.getString("resource_id"), rs.getString("subject_type"), rs.getString("subject_id"),
                rs.getString("permission_level"), rs.getString("status"),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("grant_reason"), rs.getString("granted_by"), rs.getTimestamp("created_at").toLocalDateTime()),
                workspaceId);
    }

    /** 创建或替换资源授权。 */
    @Transactional(rollbackFor = Exception.class)
    public PermissionGovernanceDtos.ResourceAclSummary grant(PermissionGovernanceDtos.ResourceAclRequest request) {
        workspaceAuthorizationService.assertCanManageAcl(request.workspaceId());
        validateRequest(request);
        String id = UUID.randomUUID().toString();
        String operator = currentUserId();
        jdbcTemplate.update("""
                INSERT INTO iam_resource_acl
                  (id,workspace_id,resource_type,resource_id,subject_type,subject_id,permission_level,status,
                   expires_at,grant_reason,granted_by,created_by)
                VALUES (?,?,?,?,?,?,?,'enabled',?,?,?,?)
                ON DUPLICATE KEY UPDATE workspace_id=VALUES(workspace_id),permission_level=VALUES(permission_level),
                  status='enabled',expires_at=VALUES(expires_at),grant_reason=VALUES(grant_reason),granted_by=VALUES(granted_by)
                """, id, request.workspaceId(), request.resourceType(), request.resourceId(), request.subjectType(),
                request.subjectId(), request.permissionLevel(), request.expiresAt(), request.reason(), operator, operator);
        workspaceAuthorizationService.writeAudit(request.workspaceId(), "grant", request.resourceType(), request.resourceId(),
                request.subjectType(), request.subjectId(), request.reason(), Map.of(),
                Map.of("permissionLevel", request.permissionLevel()));
        return list(request.workspaceId()).stream()
                .filter(item -> item.resourceType().equals(request.resourceType())
                        && item.resourceId().equals(request.resourceId())
                        && item.subjectType().equals(request.subjectType())
                        && item.subjectId().equals(request.subjectId()))
                .findFirst().orElseThrow();
    }

    /** 撤销资源授权。 */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String workspaceId, String aclId, String reason) {
        workspaceAuthorizationService.assertCanManageAcl(workspaceId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM iam_resource_acl WHERE id=? AND workspace_id=?", aclId, workspaceId);
        if (rows.isEmpty()) {
            throw new BusinessException("RESOURCE_ACL_NOT_FOUND", "资源授权不存在");
        }
        Map<String, Object> row = rows.getFirst();
        jdbcTemplate.update("UPDATE iam_resource_acl SET status='revoked',expires_at=CURRENT_TIMESTAMP(3) WHERE id=?", aclId);
        workspaceAuthorizationService.writeAudit(workspaceId, "revoke", String.valueOf(row.get("resource_type")),
                String.valueOf(row.get("resource_id")), String.valueOf(row.get("subject_type")),
                String.valueOf(row.get("subject_id")), reason, Map.of("permissionLevel", row.get("permission_level")), Map.of());
    }

    /**
     * 判断当前用户是否命中指定资源授权级别。
     */
    public boolean currentUserHasAcl(String workspaceId, String resourceType, String resourceId,
                                     Collection<String> permissionLevels) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserDetails userDetails)) {
            return false;
        }
        Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        if (PlatformAuthorityPolicy.isPlatformManager(authorities)) {
            return true;
        }
        if (!StringUtils.hasText(workspaceId) || permissionLevels == null || permissionLevels.isEmpty()) {
            return false;
        }
        String levelPlaceholders = String.join(",", permissionLevels.stream().map(item -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(workspaceId);
        args.add(resourceType);
        args.add(resourceId);
        args.addAll(permissionLevels);
        args.add(userDetails.getUserId());
        args.add(workspaceId);
        args.add(userDetails.getUserId());
        args.add(userDetails.getUserId());
        String aclSql = """
                SELECT COUNT(1) FROM iam_resource_acl acl
                WHERE acl.workspace_id=? AND acl.resource_type=? AND acl.resource_id=?
                  AND acl.permission_level IN (%s)
                  AND acl.status='enabled' AND (acl.expires_at IS NULL OR acl.expires_at>CURRENT_TIMESTAMP(3))
                  AND (
                    (acl.subject_type='user' AND acl.subject_id=?)
                    OR (acl.subject_type='role' AND acl.subject_id IN (
                      SELECT role_id FROM iam_workspace_member_role WHERE workspace_id=? AND user_id=?
                    ))
                    OR (acl.subject_type='department' AND acl.subject_id=(
                      SELECT department_id FROM iam_user WHERE id=?
                    ))
                  )
                """.formatted(levelPlaceholders);
        Long count = jdbcTemplate.queryForObject(aclSql, Long.class, args.toArray());
        return count != null && count > 0;
    }

    /** 校验授权参数枚举。 */
    private void validateRequest(PermissionGovernanceDtos.ResourceAclRequest request) {
        if (!List.of("user", "role", "department").contains(request.subjectType())) {
            throw new BusinessException("RESOURCE_ACL_SUBJECT_INVALID", "资源授权主体类型不受支持");
        }
        if (!List.of("read", "run", "write", "owner").contains(request.permissionLevel())) {
            throw new BusinessException("RESOURCE_ACL_LEVEL_INVALID", "资源授权级别不受支持");
        }
    }

    /** 获取当前用户ID。 */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUserDetails details
                ? details.getUserId() : null;
    }
}
