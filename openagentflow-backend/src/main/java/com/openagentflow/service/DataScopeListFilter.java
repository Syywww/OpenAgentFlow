package com.openagentflow.service;

import com.openagentflow.domain.iam.PermissionGovernanceDtos;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.PlatformAuthorityPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 列表查询 data_scope 数据范围过滤 SQL 生成器。
 *
 * <p>把「当前用户在当前工作空间可见哪些资源」的判定下沉为一次列表查询的 WHERE 片段，
 * 消除「先拉全量再内存逐条判定」的 N 次 SQL。可见性 = public 资源 {@code OR} 本人 owner/创建人
 * {@code OR} 资源 ACL 三主体授权（语义复刻 {@code ResourceAclService.currentUserHasAcl}）
 * {@code OR}（模块权限 {@code AND} data_scope 放行）。data_scope 按 {@code DataScopePolicy.merge}
 * 解析最终范围，与治理展示路径 {@code WorkspaceAuthorizationService.resolveDataScope} 同源。</p>
 *
 * <p>生成的 SQL 使用 {@code {n}} 占位符（MyBatis-Plus {@code apply} 支持），参数按序绑定，
 * 无字符串拼接，防注入。dept_tree/custom 在构建阶段一次性算出部门集合，列表 SQL 零递归。</p>
 */
@Component
public class DataScopeListFilter {

    /** 资源类型元数据：表名、是否有 visibility 列、查看动作模块权限码。 */
    private record ResourceMeta(String table, boolean hasVisibility, List<String> viewPermissions) {
    }

    /** 支持的资源类型 → 表/列映射（表驱动，勿硬编码列名）。 */
    private static final Map<String, ResourceMeta> RESOURCES = Map.of(
            "agent", new ResourceMeta("agent", true, List.of("agent:view", "agent:manage")),
            "knowledge_base", new ResourceMeta("knowledge_base", true, List.of("knowledge:view", "knowledge:manage")),
            "tool", new ResourceMeta("tool_definition", false, List.of("tool:view", "tool:manage")),
            "workflow", new ResourceMeta("workflow_definition", true, List.of("workflow:view", "workflow:manage")));

    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public DataScopeListFilter(WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    /** 列表过滤片段：sql 含 {@code {n}} 占位符，args 按序绑定；sql 为 null 表示无需过滤。 */
    public record ListFilter(String sql, List<Object> args) {
        public boolean requiresFilter() {
            return sql != null && !sql.isBlank();
        }
    }

    /**
     * 生成当前用户的列表可见性过滤片段。
     *
     * @param workspaceId 工作空间 ID（可为空）
     * @param userId 当前用户 ID
     * @param resourceType 资源类型：agent/knowledge_base/tool/workflow
     * @return 过滤片段；平台管理员返回 null（全量可见，无需过滤）
     */
    public ListFilter buildListVisibilityFilter(String workspaceId, String userId, String resourceType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && PlatformAuthorityPolicy.isPlatformManager(authoritiesOf(authentication))) {
            return null;
        }
        return resolveAndBuild(workspaceId, userId, resourceType);
    }

    /** 跳过平台管理员短路的内部构建入口（供单测固定入参）。 */
    public ListFilter resolveAndBuild(String workspaceId, String userId, String resourceType) {
        ResourceMeta meta = RESOURCES.get(resourceType);
        if (meta == null) {
            throw new BusinessException("DATA_SCOPE_RESOURCE_UNSUPPORTED", "不支持的数据范围过滤资源类型");
        }
        if (!StringUtils.hasText(userId)) {
            // 未登录防御：列表端点已被 API 权限管理器 401 拦截，此处仅放行 public 资源。
            return meta.hasVisibility()
                    ? new ListFilter(meta.table() + ".visibility = 'public'", List.of())
                    : new ListFilter("1=0", List.of());
        }
        PermissionGovernanceDtos.DataScopeResult scope =
                workspaceAuthorizationService.resolveDataScopeInternal(workspaceId, userId);

        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("(");
        String table = meta.table();
        int slot = 0;

        // public 可见资源。
        if (meta.hasVisibility()) {
            sql.append(table).append(".visibility = 'public' OR ");
        }
        // 本人 owner 或创建人。
        sql.append(table).append(".owner_user_id = {").append(slot).append("} OR ")
                .append(table).append(".created_by = {").append(slot + 1).append("} OR ");
        args.add(userId);
        args.add(userId);
        slot += 2;
        // 资源 ACL 三主体授权（user 直接 / role 经成员角色 / department 经用户主部门），含过期与停用排除。
        sql.append("EXISTS (")
                .append("SELECT 1 FROM iam_resource_acl acl WHERE acl.workspace_id = {").append(slot).append("}")
                .append(" AND acl.resource_type = {").append(slot + 1).append("} AND acl.resource_id = ").append(table).append(".id")
                .append(" AND acl.permission_level IN ('owner','write','run','read')")
                .append(" AND acl.status = 'enabled' AND (acl.expires_at IS NULL OR acl.expires_at > CURRENT_TIMESTAMP(3))")
                .append(" AND ((acl.subject_type = 'user' AND acl.subject_id = {").append(slot + 2).append("})")
                .append(" OR (acl.subject_type = 'role' AND acl.subject_id IN (")
                .append("SELECT role_id FROM iam_workspace_member_role WHERE workspace_id = {").append(slot + 3)
                .append("} AND user_id = {").append(slot + 4).append("}))")
                .append(" OR (acl.subject_type = 'department' AND acl.subject_id = (")
                .append("SELECT department_id FROM iam_user WHERE id = {").append(slot + 5).append("})))")
                .append(") OR ");
        args.add(workspaceId);
        args.add(resourceType);
        args.add(userId);
        args.add(workspaceId);
        args.add(userId);
        args.add(userId);
        slot += 6;
        // 模块权限 + data_scope：数据范围放行必须以具备资源模块权限为前提（复刻 hasWorkspaceResourcePermission）。
        ModulePermissionPart permission = modulePermissionSql(meta.viewPermissions(), args, slot, workspaceId, userId);
        sql.append('(').append(permission.sql())
                .append(" AND ").append(dataScopeExpr(scope, table, args, permission.nextSlot(), userId))
                .append(')');
        sql.append(')');
        return new ListFilter(sql.toString(), args);
    }

    /** 模块权限 EXISTS 及其消费后的参数槽位（调用方需从 nextSlot 续接 data_scope 占位符）。 */
    private record ModulePermissionPart(String sql, int nextSlot) {
    }

    /** 模块权限 EXISTS：具备资源模块查看/管理权限点，或是旧空间 owner/admin 成员。 */
    private ModulePermissionPart modulePermissionSql(List<String> viewPermissions, List<Object> args,
                                                     int slot, String workspaceId, String userId) {
        String permissions = viewPermissions.stream().map(code -> "'" + code + "'").collect(Collectors.joining(","));
        args.add(workspaceId);
        args.add(userId);
        args.add(workspaceId);
        args.add(userId);
        int nextSlot = slot + 4;
        return new ModulePermissionPart(
                "("
                        + "EXISTS ("
                        + "SELECT 1 FROM iam_workspace_member_role mr"
                        + " JOIN iam_workspace_role role ON role.id=mr.role_id AND role.status='enabled'"
                        + " JOIN iam_workspace_role_permission rp ON rp.role_id=role.id"
                        + " JOIN iam_permission p ON p.id=rp.permission_id AND p.status='enabled'"
                        + " WHERE mr.workspace_id={" + slot + "} AND mr.user_id={" + (slot + 1) + "}"
                        + " AND p.permission_code IN (" + permissions + "))"
                        + " OR EXISTS ("
                        + "SELECT 1 FROM oaf_workspace_member wm WHERE wm.workspace_id={" + (slot + 2) + "}"
                        + " AND wm.user_id={" + (slot + 3) + "} AND wm.member_role IN ('owner','admin')"
                        + " AND wm.status IN ('active','enabled'))"
                        + ")",
                nextSlot);
    }

    /** 按 merge 后的最终数据范围生成资源归属过滤表达式（{@code t} 为资源表名）。 */
    private String dataScopeExpr(PermissionGovernanceDtos.DataScopeResult scope, String table,
                                 List<Object> args, int slot, String userId) {
        // COALESCE 复刻 dataScopeAllows 的归属人选择：owner 优先，缺失时回退 created_by。
        String targetDept = "(SELECT department_id FROM iam_user WHERE id = COALESCE("
                + table + ".owner_user_id, " + table + ".created_by))";
        return switch (scope.scopeType()) {
            case "all" -> "1=1";
            case "dept" -> {
                args.add(userId);
                yield targetDept + " = (SELECT department_id FROM iam_user WHERE id = {" + slot + "})";
            }
            case "dept_tree", "custom" -> {
                // dept_tree 的 departmentIds 已含主部门及其子孙；custom 为角色配置的部门集合。
                if (scope.departmentIds().isEmpty()) {
                    yield "1=0";
                }
                StringBuilder in = new StringBuilder(targetDept + " IN (");
                int index = slot;
                for (String departmentId : scope.departmentIds()) {
                    if (index > slot) {
                        in.append(',');
                    }
                    args.add(departmentId);
                    in.append('{').append(index).append('}');
                    index++;
                }
                in.append(')');
                yield in.toString();
            }
            case "self" -> "1=0";
            default -> "1=0";
        };
    }

    /** 提取认证上下文持有的权限编码。 */
    private Set<String> authoritiesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
