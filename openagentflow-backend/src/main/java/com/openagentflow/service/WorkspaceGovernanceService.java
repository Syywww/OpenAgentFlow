package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.domain.workspace.WorkspaceDtos;
import com.openagentflow.entity.IamUserEntity;
import com.openagentflow.entity.OafOrganizationEntity;
import com.openagentflow.entity.OafOrganizationMemberEntity;
import com.openagentflow.entity.OafWorkspaceEntity;
import com.openagentflow.entity.OafWorkspaceMemberEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.IamUserMapper;
import com.openagentflow.mapper.OafOrganizationMapper;
import com.openagentflow.mapper.OafOrganizationMemberMapper;
import com.openagentflow.mapper.OafWorkspaceMapper;
import com.openagentflow.mapper.OafWorkspaceMemberMapper;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.PlatformAuthorityPolicy;
import com.openagentflow.security.ResourceModulePermissionPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 组织与工作空间治理服务。
 *
 * <p>该服务是 P11 的核心入口，负责组织、空间、成员、资源归属和空间级访问控制。</p>
 */
@Service
public class WorkspaceGovernanceService {

    /** 默认组织 ID，用于承接历史数据和本地演示数据。 */
    private static final String DEFAULT_ORG_ID = "90000000-0000-0000-0000-000000000001";

    /** 默认工作空间 ID，用于承接历史资源。 */
    private static final String DEFAULT_WORKSPACE_ID = "90000000-0000-0000-0000-000000000101";

    /** 组织 Mapper。 */
    private final OafOrganizationMapper organizationMapper;

    /** 组织成员 Mapper。 */
    private final OafOrganizationMemberMapper organizationMemberMapper;

    /** 工作空间 Mapper。 */
    private final OafWorkspaceMapper workspaceMapper;

    /** 工作空间成员 Mapper。 */
    private final OafWorkspaceMemberMapper workspaceMemberMapper;

    /** 用户 Mapper，用于展示成员名称。 */
    private final IamUserMapper iamUserMapper;

    /** JDBC 工具，用于统计和通用归属写入。 */
    private final JdbcTemplate jdbcTemplate;

    public WorkspaceGovernanceService(OafOrganizationMapper organizationMapper,
                                      OafOrganizationMemberMapper organizationMemberMapper,
                                      OafWorkspaceMapper workspaceMapper,
                                      OafWorkspaceMemberMapper workspaceMemberMapper,
                                      IamUserMapper iamUserMapper,
                                      JdbcTemplate jdbcTemplate) {
        this.organizationMapper = organizationMapper;
        this.organizationMemberMapper = organizationMemberMapper;
        this.workspaceMapper = workspaceMapper;
        this.workspaceMemberMapper = workspaceMemberMapper;
        this.iamUserMapper = iamUserMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询当前用户可见组织。
     *
     * @return 组织摘要列表
     */
    public List<WorkspaceDtos.OrganizationSummary> listOrganizations() {
        return organizationMapper.selectList(new LambdaQueryWrapper<OafOrganizationEntity>()
                        .isNull(OafOrganizationEntity::getDeletedAt)
                        .orderByAsc(OafOrganizationEntity::getCreatedAt))
                .stream()
                .filter(this::canViewOrganization)
                .map(this::toOrganizationSummary)
                .toList();
    }

    /**
     * 创建组织，并把创建者加入为 owner。
     *
     * @param request 组织请求
     * @return 组织摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDtos.OrganizationSummary createOrganization(WorkspaceDtos.OrganizationRequest request) {
        String userId = currentUserIdOrThrow();
        OafOrganizationEntity entity = new OafOrganizationEntity();
        entity.setId(newId());
        entity.setOrgCode(uniqueOrgCode(StringUtils.hasText(request.getOrgCode()) ? request.getOrgCode() : slugify(request.getOrgName())));
        entity.setOrgName(request.getOrgName().trim());
        entity.setDescription(request.getDescription());
        entity.setOwnerUserId(userId);
        entity.setStatus("enabled");
        entity.setCreatedBy(userId);
        organizationMapper.insert(entity);
        addOrganizationMember(entity.getId(), userId, "owner");
        return toOrganizationSummary(entity);
    }

    /**
     * 查询当前用户可见工作空间。
     *
     * @return 工作空间摘要列表
     */
    public List<WorkspaceDtos.WorkspaceSummary> listWorkspaces() {
        return workspaceMapper.selectList(new LambdaQueryWrapper<OafWorkspaceEntity>()
                        .isNull(OafWorkspaceEntity::getDeletedAt)
                        .orderByDesc(OafWorkspaceEntity::getDefaultFlag)
                        .orderByAsc(OafWorkspaceEntity::getCreatedAt))
                .stream()
                .filter(this::canViewWorkspace)
                .map(this::toWorkspaceSummary)
                .toList();
    }

    /**
     * 查询工作空间详情。
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间详情
     */
    public WorkspaceDtos.WorkspaceDetail getWorkspace(String workspaceId) {
        OafWorkspaceEntity workspace = requireWorkspace(workspaceId);
        if (!canViewWorkspace(workspace)) {
            throw new BusinessException("WORKSPACE_FORBIDDEN", "没有访问该工作空间的权限");
        }
        WorkspaceDtos.WorkspaceDetail detail = new WorkspaceDtos.WorkspaceDetail();
        copyWorkspaceSummary(toWorkspaceSummary(workspace), detail);
        detail.setMembers(listWorkspaceMembers(workspaceId));
        return detail;
    }

    /**
     * 创建工作空间，并把创建者加入为 owner。
     *
     * @param request 工作空间请求
     * @return 工作空间详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDtos.WorkspaceDetail createWorkspace(WorkspaceDtos.WorkspaceRequest request) {
        String userId = currentUserIdOrThrow();
        String organizationId = StringUtils.hasText(request.getOrganizationId()) ? request.getOrganizationId() : DEFAULT_ORG_ID;
        OafOrganizationEntity organization = requireOrganization(organizationId);
        if (!canManageOrganization(organization)) {
            throw new BusinessException("WORKSPACE_FORBIDDEN", "没有在该组织下创建工作空间的权限");
        }
        OafWorkspaceEntity entity = new OafWorkspaceEntity();
        entity.setId(newId());
        entity.setOrganizationId(organizationId);
        entity.setWorkspaceCode(uniqueWorkspaceCode(StringUtils.hasText(request.getWorkspaceCode()) ? request.getWorkspaceCode() : slugify(request.getWorkspaceName())));
        entity.setWorkspaceName(request.getWorkspaceName().trim());
        entity.setDescription(request.getDescription());
        entity.setWorkspaceType(StringUtils.hasText(request.getWorkspaceType()) ? request.getWorkspaceType() : "team");
        entity.setOwnerUserId(userId);
        entity.setDefaultFlag(Boolean.TRUE.equals(request.getDefaultFlag()));
        entity.setStatus("enabled");
        entity.setCreatedBy(userId);
        workspaceMapper.insert(entity);
        addWorkspaceMember(entity.getId(), userId, "owner");
        initializeWorkspaceRoles(entity.getId(), userId);
        return getWorkspace(entity.getId());
    }

    /**
     * 更新工作空间基础信息。
     *
     * @param workspaceId 工作空间 ID
     * @param request 工作空间请求
     * @return 工作空间详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDtos.WorkspaceDetail updateWorkspace(String workspaceId, WorkspaceDtos.WorkspaceRequest request) {
        OafWorkspaceEntity entity = requireWorkspace(workspaceId);
        assertCanManageWorkspace(entity.getId());
        entity.setWorkspaceName(request.getWorkspaceName().trim());
        entity.setDescription(request.getDescription());
        entity.setWorkspaceType(StringUtils.hasText(request.getWorkspaceType()) ? request.getWorkspaceType() : entity.getWorkspaceType());
        entity.setDefaultFlag(Boolean.TRUE.equals(request.getDefaultFlag()));
        workspaceMapper.updateById(entity);
        return getWorkspace(entity.getId());
    }

    /**
     * 新增或更新工作空间成员。
     *
     * @param workspaceId 工作空间 ID
     * @param request 成员请求
     * @return 工作空间详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDtos.WorkspaceDetail saveWorkspaceMember(String workspaceId, WorkspaceDtos.MemberRequest request) {
        assertCanManageWorkspace(workspaceId);
        addWorkspaceMember(workspaceId, request.getUserId(), normalizeRole(request.getMemberRole()));
        return getWorkspace(workspaceId);
    }

    /**
     * 移除工作空间成员。
     *
     * @param workspaceId 工作空间 ID
     * @param userId 用户 ID
     * @return 工作空间详情
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceDtos.WorkspaceDetail removeWorkspaceMember(String workspaceId, String userId) {
        assertCanManageWorkspace(workspaceId);
        workspaceMemberMapper.delete(new LambdaQueryWrapper<OafWorkspaceMemberEntity>()
                .eq(OafWorkspaceMemberEntity::getWorkspaceId, workspaceId)
                .eq(OafWorkspaceMemberEntity::getUserId, userId));
        return getWorkspace(workspaceId);
    }

    /**
     * 为资源写入工作空间归属。
     *
     * @param workspaceId 工作空间 ID，为空时使用当前用户默认空间
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param ownerUserId 资源所有者
     * @return 最终工作空间 ID
     */
    public String attachResource(String workspaceId, String resourceType, String resourceId, String ownerUserId) {
        String finalWorkspaceId = StringUtils.hasText(workspaceId) ? workspaceId : defaultWorkspaceIdForCurrentUser();
        if (!canManageWorkspace(finalWorkspaceId)) {
            throw new BusinessException("WORKSPACE_FORBIDDEN", "没有在该工作空间写入资源的权限");
        }
        // 使用唯一键保证资源只能属于一个工作空间，重复保存时更新空间和所有者。
        jdbcTemplate.update("""
                INSERT INTO oaf_workspace_resource
                  (id, workspace_id, resource_type, resource_id, owner_user_id, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE workspace_id = VALUES(workspace_id), owner_user_id = VALUES(owner_user_id)
                """, newId(), finalWorkspaceId, resourceType, resourceId, ownerUserId, currentUserId());
        return finalWorkspaceId;
    }

    /**
     * 判断当前用户是否可以查看某个资源。
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param workspaceId 工作空间 ID
     * @param ownerUserId 所有者用户 ID
     * @param createdBy 创建人 ID
     * @param visibility 可见范围
     * @return 是否可查看
     */
    public boolean canViewResource(String resourceType,
                                   String resourceId,
                                   String workspaceId,
                                   String ownerUserId,
                                   String createdBy,
                                   String visibility) {
        if ("public".equalsIgnoreCase(visibility) || isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        if (userId.equals(ownerUserId) || userId.equals(createdBy)) {
            return true;
        }
        return StringUtils.hasText(workspaceId)
                && hasWorkspaceResourcePermission(workspaceId, userId, resourceType, false)
                && dataScopeAllows(workspaceId, userId, ownerUserId, createdBy);
    }

    /**
     * 判断当前用户是否可以管理某个资源。
     *
     * @param workspaceId 工作空间 ID
     * @param ownerUserId 所有者用户 ID
     * @param createdBy 创建人 ID
     * @return 是否可管理
     */
    public boolean canManageResource(String resourceType,
                                     String workspaceId,
                                     String ownerUserId,
                                     String createdBy) {
        if (isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        if (userId.equals(ownerUserId) || userId.equals(createdBy)) {
            return true;
        }
        return StringUtils.hasText(workspaceId)
                && hasWorkspaceResourcePermission(workspaceId, userId, resourceType, true)
                && dataScopeAllows(workspaceId, userId, ownerUserId, createdBy);
    }

    /**
     * 判断空间角色是否拥有资源模块权限。
     *
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @param manage 是否要求管理权限
     * @return 是否拥有权限
     */
    private boolean hasWorkspaceResourcePermission(String workspaceId, String userId,
                                                   String resourceType, boolean manage) {
        List<String> permissionCodes = ResourceModulePermissionPolicy.requiredPermissions(resourceType, manage);
        String placeholders = String.join(",", permissionCodes.stream().map(item -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(workspaceId);
        args.add(userId);
        args.addAll(permissionCodes);
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM iam_workspace_member_role mr
                JOIN iam_workspace_role role ON role.id=mr.role_id AND role.status='enabled'
                JOIN iam_workspace_role_permission rp ON rp.role_id=role.id
                JOIN iam_permission p ON p.id=rp.permission_id AND p.status='enabled'
                WHERE mr.workspace_id=? AND mr.user_id=? AND p.permission_code IN (%s)
                """.formatted(placeholders), Long.class, args.toArray());
        if (count != null && count > 0) {
            return true;
        }
        // 兼容迁移前已存在的空间所有者和管理员。
        return count("SELECT COUNT(1) FROM oaf_workspace_member WHERE workspace_id=? AND user_id=? AND member_role IN ('owner','admin') AND status IN ('active','enabled')",
                workspaceId, userId) > 0;
    }

    /**
     * 根据空间角色数据范围判断资源所有者是否可见。
     */
    private boolean dataScopeAllows(String workspaceId, String userId, String ownerUserId, String createdBy) {
        String targetUserId = StringUtils.hasText(ownerUserId) ? ownerUserId : createdBy;
        if (userId.equals(targetUserId)) {
            return true;
        }
        List<String> scopes = jdbcTemplate.queryForList("""
                SELECT DISTINCT role.data_scope FROM iam_workspace_member_role mr
                JOIN iam_workspace_role role ON role.id=mr.role_id
                WHERE mr.workspace_id=? AND mr.user_id=? AND role.status='enabled'
                """, String.class, workspaceId, userId);
        if (scopes.contains("all")) {
            return true;
        }
        if (!StringUtils.hasText(targetUserId)) {
            return false;
        }
        String currentDepartment = firstText("SELECT department_id FROM iam_user WHERE id=?", userId);
        String targetDepartment = firstText("SELECT department_id FROM iam_user WHERE id=?", targetUserId);
        if (!StringUtils.hasText(targetDepartment)) {
            return false;
        }
        if (scopes.contains("dept") && targetDepartment.equals(currentDepartment)) {
            return true;
        }
        if (scopes.contains("dept_tree") && StringUtils.hasText(currentDepartment)) {
            Long descendant = jdbcTemplate.queryForObject("""
                    WITH RECURSIVE department_tree AS (
                      SELECT id FROM iam_department WHERE id=?
                      UNION ALL
                      SELECT child.id FROM iam_department child JOIN department_tree parent ON child.parent_id=parent.id
                    ) SELECT COUNT(1) FROM department_tree WHERE id=?
                    """, Long.class, currentDepartment, targetDepartment);
            if (descendant != null && descendant > 0) {
                return true;
            }
        }
        if (scopes.contains("custom")) {
            return count("""
                    SELECT COUNT(1) FROM iam_workspace_member_role mr
                    JOIN iam_workspace_role_department rd ON rd.role_id=mr.role_id
                    WHERE mr.workspace_id=? AND mr.user_id=? AND rd.department_id=?
                    """, workspaceId, userId, targetDepartment) > 0;
        }
        return false;
    }

    /** 查询单个文本字段，未命中时返回空字符串。 */
    private String firstText(String sql, Object... args) {
        List<String> values = jdbcTemplate.queryForList(sql, String.class, args);
        return values.isEmpty() || values.getFirst() == null ? "" : values.getFirst();
    }

    /**
     * 获取当前用户默认工作空间。
     *
     * @return 工作空间 ID
     */
    public String defaultWorkspaceIdForCurrentUser() {
        String userId = currentUserIdOrThrow();
        if (isSystemManager()) {
            return DEFAULT_WORKSPACE_ID;
        }
        List<OafWorkspaceMemberEntity> memberships = workspaceMemberMapper.selectList(new LambdaQueryWrapper<OafWorkspaceMemberEntity>()
                .eq(OafWorkspaceMemberEntity::getUserId, userId)
                .eq(OafWorkspaceMemberEntity::getStatus, "enabled")
                .last("limit 1"));
        if (!memberships.isEmpty()) {
            return memberships.get(0).getWorkspaceId();
        }
        return DEFAULT_WORKSPACE_ID;
    }

    /**
     * 判断当前用户是否系统级管理员。
     *
     * @return 是否系统管理员
     */
    public boolean isSystemManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return PlatformAuthorityPolicy.isPlatformManager(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID，未登录返回 null
     */
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 判断当前用户是否可以管理工作空间。
     *
     * @param workspaceId 工作空间 ID
     * @return 是否可管理
     */
    public boolean canManageWorkspace(String workspaceId) {
        if (isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(workspaceId)) {
            return false;
        }
        String role = currentUserWorkspaceRole(workspaceId);
        return List.of("owner", "admin").contains(role);
    }

    /**
     * 校验当前用户是否可以管理工作空间。
     *
     * @param workspaceId 工作空间 ID
     */
    public void assertCanManageWorkspace(String workspaceId) {
        if (!canManageWorkspace(workspaceId)) {
            throw new BusinessException("WORKSPACE_FORBIDDEN", "没有管理该工作空间的权限");
        }
    }

    /**
     * 查询空间成员列表。
     *
     * @param workspaceId 工作空间 ID
     * @return 成员摘要列表
     */
    private List<WorkspaceDtos.MemberSummary> listWorkspaceMembers(String workspaceId) {
        return workspaceMemberMapper.selectList(new LambdaQueryWrapper<OafWorkspaceMemberEntity>()
                        .eq(OafWorkspaceMemberEntity::getWorkspaceId, workspaceId)
                        .eq(OafWorkspaceMemberEntity::getStatus, "enabled"))
                .stream()
                .map(this::toMemberSummary)
                .toList();
    }

    /**
     * 判断组织是否可见。
     *
     * @param organization 组织实体
     * @return 是否可见
     */
    private boolean canViewOrganization(OafOrganizationEntity organization) {
        return isSystemManager()
                || currentUserId().equals(organization.getOwnerUserId())
                || count("SELECT COUNT(1) FROM oaf_organization_member WHERE organization_id = ? AND user_id = ? AND status = 'enabled'",
                organization.getId(), currentUserId()) > 0;
    }

    /**
     * 判断组织是否可管理。
     *
     * @param organization 组织实体
     * @return 是否可管理
     */
    private boolean canManageOrganization(OafOrganizationEntity organization) {
        if (isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        if (userId.equals(organization.getOwnerUserId())) {
            return true;
        }
        return count("SELECT COUNT(1) FROM oaf_organization_member WHERE organization_id = ? AND user_id = ? AND member_role IN ('owner','admin') AND status = 'enabled'",
                organization.getId(), userId) > 0;
    }

    /**
     * 判断工作空间是否可见。
     *
     * @param workspace 工作空间实体
     * @return 是否可见
     */
    private boolean canViewWorkspace(OafWorkspaceEntity workspace) {
        if (isSystemManager()) {
            return true;
        }
        String userId = currentUserId();
        return StringUtils.hasText(userId)
                && (userId.equals(workspace.getOwnerUserId()) || isWorkspaceMember(workspace.getId(), userId));
    }

    /**
     * 判断用户是否工作空间成员。
     *
     * @param workspaceId 工作空间 ID
     * @param userId 用户 ID
     * @return 是否成员
     */
    private boolean isWorkspaceMember(String workspaceId, String userId) {
        return count("SELECT COUNT(1) FROM oaf_workspace_member WHERE workspace_id = ? AND user_id = ? AND status = 'enabled'",
                workspaceId, userId) > 0;
    }

    /**
     * 查询当前用户在空间内的角色。
     *
     * @param workspaceId 工作空间 ID
     * @return 成员角色
     */
    private String currentUserWorkspaceRole(String workspaceId) {
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            return "";
        }
        List<String> roles = jdbcTemplate.queryForList(
                "SELECT member_role FROM oaf_workspace_member WHERE workspace_id = ? AND user_id = ? AND status = 'enabled' LIMIT 1",
                String.class,
                workspaceId,
                userId);
        return roles.isEmpty() ? "" : roles.get(0);
    }

    /**
     * 添加组织成员。
     *
     * @param organizationId 组织 ID
     * @param userId 用户 ID
     * @param role 成员角色
     */
    private void addOrganizationMember(String organizationId, String userId, String role) {
        jdbcTemplate.update("""
                INSERT INTO oaf_organization_member
                  (id, organization_id, user_id, member_role, status, created_by)
                VALUES (?, ?, ?, ?, 'enabled', ?)
                ON DUPLICATE KEY UPDATE member_role = VALUES(member_role), status = 'enabled'
                """, newId(), organizationId, userId, normalizeRole(role), currentUserId());
    }

    /**
     * 添加工作空间成员。
     *
     * @param workspaceId 工作空间 ID
     * @param userId 用户 ID
     * @param role 成员角色
     */
    private void addWorkspaceMember(String workspaceId, String userId, String role) {
        jdbcTemplate.update("""
                INSERT INTO oaf_workspace_member
                  (id, workspace_id, user_id, member_role, status, created_by)
                VALUES (?, ?, ?, ?, 'enabled', ?)
                ON DUPLICATE KEY UPDATE member_role = VALUES(member_role), status = 'enabled'
                """, newId(), workspaceId, userId, normalizeRole(role), currentUserId());
        String workspaceRoleCode = List.of("owner", "admin").contains(normalizeRole(role)) ? normalizeRole(role) : "viewer";
        jdbcTemplate.update("""
                INSERT IGNORE INTO iam_workspace_member_role(workspace_id,user_id,role_id,created_by)
                SELECT ?,?,id,? FROM iam_workspace_role WHERE workspace_id=? AND role_code=? AND status='enabled'
                """, workspaceId, userId, currentUserId(), workspaceId, workspaceRoleCode);
    }

    /**
     * 为新工作空间初始化内置角色、权限和所有者角色关系。
     *
     * @param workspaceId 工作空间ID
     * @param ownerUserId 所有者用户ID
     */
    private void initializeWorkspaceRoles(String workspaceId, String ownerUserId) {
        List<List<String>> roles = List.of(
                List.of("owner", "空间所有者", "拥有当前工作空间全部业务权限", "all"),
                List.of("admin", "空间管理员", "管理当前工作空间成员和业务资源", "all"),
                List.of("developer", "开发者", "创建、调试和运行Agent、RAG、工具与工作流", "dept_tree"),
                List.of("auditor", "审计员", "查看运行、用量、风险和治理数据", "all"),
                List.of("viewer", "只读成员", "只查看被授权的当前工作空间资源", "self")
        );
        for (List<String> role : roles) {
            jdbcTemplate.update("""
                    INSERT IGNORE INTO iam_workspace_role
                      (id,workspace_id,role_code,role_name,description,data_scope,built_in,status,created_by)
                    VALUES (?,?,?,?,?,?,1,'enabled',?)
                    """, newId(), workspaceId, role.get(0), role.get(1), role.get(2), role.get(3), ownerUserId);
        }
        jdbcTemplate.update("""
                INSERT IGNORE INTO iam_workspace_role_permission(role_id,permission_id,created_by)
                SELECT role.id,p.id,? FROM iam_workspace_role role JOIN iam_permission p ON p.status='enabled'
                WHERE role.workspace_id=? AND role.role_code IN ('owner','admin')
                  AND p.permission_code REGEXP '^(agent|agent-team|debug|knowledge|tool|mcp|workflow|trace|runtime|usage|ops:monitor|notification|delivery:acceptance|model-gateway|workspace|async-task|governance|prompt|memory|evaluation|eval|template):'
                """, ownerUserId, workspaceId);
        jdbcTemplate.update("""
                INSERT IGNORE INTO iam_workspace_member_role(workspace_id,user_id,role_id,created_by)
                SELECT ?,?,id,? FROM iam_workspace_role WHERE workspace_id=? AND role_code='owner'
                """, workspaceId, ownerUserId, ownerUserId, workspaceId);
    }

    /**
     * 转组织摘要。
     *
     * @param entity 组织实体
     * @return 组织摘要
     */
    private WorkspaceDtos.OrganizationSummary toOrganizationSummary(OafOrganizationEntity entity) {
        WorkspaceDtos.OrganizationSummary summary = new WorkspaceDtos.OrganizationSummary();
        summary.setId(entity.getId());
        summary.setOrgCode(entity.getOrgCode());
        summary.setOrgName(entity.getOrgName());
        summary.setDescription(entity.getDescription());
        summary.setStatus(entity.getStatus());
        summary.setMemberCount(count("SELECT COUNT(1) FROM oaf_organization_member WHERE organization_id = ? AND status = 'enabled'", entity.getId()));
        summary.setWorkspaceCount(count("SELECT COUNT(1) FROM oaf_workspace WHERE organization_id = ? AND deleted_at IS NULL", entity.getId()));
        summary.setCanManage(canManageOrganization(entity));
        summary.setCreatedAt(entity.getCreatedAt());
        return summary;
    }

    /**
     * 转工作空间摘要。
     *
     * @param entity 工作空间实体
     * @return 工作空间摘要
     */
    private WorkspaceDtos.WorkspaceSummary toWorkspaceSummary(OafWorkspaceEntity entity) {
        WorkspaceDtos.WorkspaceSummary summary = new WorkspaceDtos.WorkspaceSummary();
        summary.setId(entity.getId());
        summary.setOrganizationId(entity.getOrganizationId());
        summary.setOrganizationName(findOrganizationName(entity.getOrganizationId()));
        summary.setWorkspaceCode(entity.getWorkspaceCode());
        summary.setWorkspaceName(entity.getWorkspaceName());
        summary.setDescription(entity.getDescription());
        summary.setWorkspaceType(entity.getWorkspaceType());
        summary.setMemberCount(count("SELECT COUNT(1) FROM oaf_workspace_member WHERE workspace_id = ? AND status = 'enabled'", entity.getId()));
        summary.setAgentCount(count("SELECT COUNT(1) FROM agent WHERE workspace_id = ? AND deleted_at IS NULL", entity.getId()));
        summary.setKnowledgeBaseCount(count("SELECT COUNT(1) FROM knowledge_base WHERE workspace_id = ? AND deleted_at IS NULL", entity.getId()));
        summary.setToolCount(count("SELECT COUNT(1) FROM tool_definition WHERE workspace_id = ? AND deleted_at IS NULL", entity.getId()));
        summary.setWorkflowCount(count("SELECT COUNT(1) FROM workflow_definition WHERE workspace_id = ? AND deleted_at IS NULL", entity.getId()));
        summary.setDefaultFlag(Boolean.TRUE.equals(entity.getDefaultFlag()));
        summary.setCurrentUserRole(isSystemManager() ? "admin" : currentUserWorkspaceRole(entity.getId()));
        summary.setCanManage(canManageWorkspace(entity.getId()));
        summary.setCreatedAt(entity.getCreatedAt());
        return summary;
    }

    /**
     * 拷贝工作空间摘要字段。
     *
     * @param source 来源摘要
     * @param target 目标摘要
     */
    private void copyWorkspaceSummary(WorkspaceDtos.WorkspaceSummary source, WorkspaceDtos.WorkspaceSummary target) {
        target.setId(source.getId());
        target.setOrganizationId(source.getOrganizationId());
        target.setOrganizationName(source.getOrganizationName());
        target.setWorkspaceCode(source.getWorkspaceCode());
        target.setWorkspaceName(source.getWorkspaceName());
        target.setDescription(source.getDescription());
        target.setWorkspaceType(source.getWorkspaceType());
        target.setMemberCount(source.getMemberCount());
        target.setAgentCount(source.getAgentCount());
        target.setKnowledgeBaseCount(source.getKnowledgeBaseCount());
        target.setToolCount(source.getToolCount());
        target.setWorkflowCount(source.getWorkflowCount());
        target.setDefaultFlag(source.getDefaultFlag());
        target.setCurrentUserRole(source.getCurrentUserRole());
        target.setCanManage(source.getCanManage());
        target.setCreatedAt(source.getCreatedAt());
    }

    /**
     * 转成员摘要。
     *
     * @param entity 成员实体
     * @return 成员摘要
     */
    private WorkspaceDtos.MemberSummary toMemberSummary(OafWorkspaceMemberEntity entity) {
        IamUserEntity user = iamUserMapper.selectById(entity.getUserId());
        WorkspaceDtos.MemberSummary summary = new WorkspaceDtos.MemberSummary();
        summary.setId(entity.getId());
        summary.setUserId(entity.getUserId());
        summary.setUsername(user == null ? "" : user.getUsername());
        summary.setDisplayName(user == null ? "" : user.getDisplayName());
        summary.setMemberRole(entity.getMemberRole());
        summary.setStatus(entity.getStatus());
        summary.setJoinedAt(entity.getJoinedAt());
        return summary;
    }

    /**
     * 查询组织。
     *
     * @param organizationId 组织 ID
     * @return 组织实体
     */
    private OafOrganizationEntity requireOrganization(String organizationId) {
        OafOrganizationEntity entity = organizationMapper.selectById(organizationId);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("ORG_NOT_FOUND", "组织不存在");
        }
        return entity;
    }

    /**
     * 查询工作空间。
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间实体
     */
    private OafWorkspaceEntity requireWorkspace(String workspaceId) {
        OafWorkspaceEntity entity = workspaceMapper.selectById(workspaceId);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new BusinessException("WORKSPACE_NOT_FOUND", "工作空间不存在");
        }
        return entity;
    }

    /**
     * 查询组织名称。
     *
     * @param organizationId 组织 ID
     * @return 组织名称
     */
    private String findOrganizationName(String organizationId) {
        OafOrganizationEntity organization = organizationMapper.selectById(organizationId);
        return organization == null ? "" : organization.getOrgName();
    }

    /**
     * 统计数量。
     *
     * @param sql SQL
     * @param args 参数
     * @return 数量
     */
    private Integer count(String sql, Object... args) {
        Number number = jdbcTemplate.queryForObject(sql, Number.class, args);
        return number == null ? 0 : number.intValue();
    }

    /**
     * 生成组织唯一编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueOrgCode(String baseCode) {
        String candidate = baseCode;
        int suffix = 1;
        while (organizationMapper.selectCount(new LambdaQueryWrapper<OafOrganizationEntity>().eq(OafOrganizationEntity::getOrgCode, candidate)) > 0) {
            candidate = baseCode + "-" + suffix++;
        }
        return candidate;
    }

    /**
     * 生成工作空间唯一编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueWorkspaceCode(String baseCode) {
        String candidate = baseCode;
        int suffix = 1;
        while (workspaceMapper.selectCount(new LambdaQueryWrapper<OafWorkspaceEntity>().eq(OafWorkspaceEntity::getWorkspaceCode, candidate)) > 0) {
            candidate = baseCode + "-" + suffix++;
        }
        return candidate;
    }

    /**
     * 标准化成员角色。
     *
     * @param role 原始角色
     * @return 合法角色
     */
    private String normalizeRole(String role) {
        String normalized = StringUtils.hasText(role) ? role.toLowerCase(Locale.ROOT) : "member";
        return List.of("owner", "admin", "member", "viewer").contains(normalized) ? normalized : "member";
    }

    /**
     * 将名称转为编码。
     *
     * @param text 原始名称
     * @return 编码
     */
    private String slugify(String text) {
        String cleaned = text == null ? "workspace" : text.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(cleaned) ? cleaned : "workspace";
    }

    /**
     * 获取当前用户 ID，未登录抛出异常。
     *
     * @return 当前用户 ID
     */
    private String currentUserIdOrThrow() {
        String userId = currentUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        return userId;
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
