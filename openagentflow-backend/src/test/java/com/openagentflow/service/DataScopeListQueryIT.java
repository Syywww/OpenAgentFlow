package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.domain.agent.AgentSummary;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.mapper.AgentMapper;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.AuthUserDetailsService;
import com.openagentflow.security.WorkspaceContextHolder;
import com.openagentflow.support.MySqlContainerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 列表查询 data_scope SQL 下沉集成测试。
 *
 * <p>验证 {@code AgentService.listAgents} 等四个列表方法在真实 MySQL 上的过滤行为：
 * data_scope 各范围（self/public/dept/dept_tree/custom/all）可见性、ACL 三主体授权与过期失效、
 * 平台管理员全量、跨工作空间隔离。最强回归护栏 {@code sqlFilterMatchesMemoryCanView} 断言
 * SQL 过滤后的列表结果与内存 {@code canView} 逐条判定完全一致。</p>
 */
@SpringBootTest
class DataScopeListQueryIT extends MySqlContainerIntegrationTestSupport {

    private static final String WS = "90000000-0000-0000-0000-000000000101";
    private static final String WS_OTHER = "cccccccc-0000-0000-0000-000000000101";
    private static final String ADMIN = "00000000-0000-0000-0000-000000000100";
    private static final String DEVELOPER = "00000000-0000-0000-0000-000000000101";
    private static final String USER = "00000000-0000-0000-0000-000000000102";
    private static final String DEPT_ROOT = "00000000-0000-0000-0000-000000000001";
    private static final String DEPT_RD = "00000000-0000-0000-0000-000000000002";
    private static final String DEPT_SUB = "99999999-0000-0000-0000-000000000003";

    @Autowired private AgentService agentService;
    @Autowired private KnowledgeBaseService knowledgeBaseService;
    @Autowired private ToolService toolService;
    @Autowired private WorkflowService workflowService;
    @Autowired private AgentAccessService agentAccessService;
    @Autowired private AgentMapper agentMapper;
    @Autowired private AuthUserDetailsService userDetailsService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 共享数据库：清空资源与角色/授权，避免历史数据干扰；仅恢复本类会修改的用户部门（不动 DEVELOPER 种子主部门）。
        jdbcTemplate.update("DELETE FROM agent");
        jdbcTemplate.update("DELETE FROM knowledge_base");
        jdbcTemplate.update("DELETE FROM tool_definition");
        jdbcTemplate.update("DELETE FROM workflow_definition");
        jdbcTemplate.update("DELETE FROM iam_resource_acl");
        jdbcTemplate.update("DELETE FROM iam_workspace_role");
        jdbcTemplate.update("DELETE FROM iam_department WHERE parent_id IS NOT NULL");
        jdbcTemplate.update("UPDATE iam_user SET department_id = NULL WHERE id IN (?, ?)", USER, ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        WorkspaceContextHolder.clear();
    }

    /** self 范围：仅本人拥有或创建的资源可见，他人资源不可见。 */
    @Test
    void shouldShowOnlyOwnResourcesUnderSelfScope() {
        bindSelfScope();
        String owned = insertAgent(WS, DEVELOPER, DEVELOPER, "private");
        String created = insertAgent(WS, USER, DEVELOPER, "private");
        String foreign = insertAgent(WS, USER, USER, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(owned, created).doesNotContain(foreign);
    }

    /** self 范围下 public 资源对所有已登录用户可见，私有他人资源不可见。 */
    @Test
    void shouldShowPublicResourcesUnderSelfScope() {
        bindSelfScope();
        String pub = insertAgent(WS, USER, USER, "public");
        String priv = insertAgent(WS, USER, USER, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(pub).doesNotContain(priv);
    }

    /** ACL user 主体授权让非拥有者可见（端到端授权链路）。 */
    @Test
    void shouldGrantVisibilityViaUserAcl() {
        bindSelfScope();
        String granted = insertAgent(WS, USER, USER, "private");
        String denied = insertAgent(WS, USER, USER, "private");
        insertAcl(WS, "agent", granted, "user", DEVELOPER, "read", null);

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(granted).doesNotContain(denied);
    }

    /** ACL role 主体授权：关联该空间角色的成员可见。 */
    @Test
    void shouldGrantVisibilityViaRoleAcl() {
        bindSelfScope();
        String roleId = insertRole(WS, "it-acl-role", "self");
        bindRole(WS, DEVELOPER, roleId);
        String granted = insertAgent(WS, USER, USER, "private");
        insertAcl(WS, "agent", granted, "role", roleId, "read", null);

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(granted);
    }

    /** ACL department 主体授权：命中用户主部门即可见。 */
    @Test
    void shouldGrantVisibilityViaDepartmentAcl() {
        bindSelfScope();
        String granted = insertAgent(WS, USER, USER, "private");
        insertAcl(WS, "agent", granted, "department", DEPT_RD, "read", null);

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(granted);
    }

    /** 过期授权不生效。 */
    @Test
    void shouldRejectExpiredAcl() {
        bindSelfScope();
        String expired = insertAgent(WS, USER, USER, "private");
        insertAcl(WS, "agent", expired, "user", DEVELOPER, "read", LocalDateTime.now().minusSeconds(1));

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).doesNotContain(expired);
    }

    /** dept 范围：同部门用户资源可见，其他部门不可见（需模块权限点，走数据范围分支）。 */
    @Test
    void shouldShowSameDepartmentResourcesUnderDeptScope() {
        jdbcTemplate.update("UPDATE iam_user SET department_id = ? WHERE id = ?", DEPT_RD, USER);
        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        bindRole(WS, DEVELOPER, insertRoleWithPermission(WS, "it-dept", "dept", "agent:view"));
        String sameDept = insertAgent(WS, USER, USER, "private");
        String otherDept = insertAgent(WS, ADMIN, ADMIN, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(sameDept).doesNotContain(otherDept);
    }

    /** dept_tree 范围：主部门及全部子孙部门的资源可见。 */
    @Test
    void shouldIncludeDescendantDepartmentsUnderDeptTreeScope() {
        jdbcTemplate.update("INSERT INTO iam_department (id, parent_id, dept_code, dept_name) VALUES (?, ?, 'it-sub-rd', 'IT子研发中心')",
                DEPT_SUB, DEPT_RD);
        jdbcTemplate.update("UPDATE iam_user SET department_id = ? WHERE id = ?", DEPT_RD, USER);
        jdbcTemplate.update("UPDATE iam_user SET department_id = ? WHERE id = ?", DEPT_SUB, ADMIN);
        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        bindRole(WS, DEVELOPER, insertRoleWithPermission(WS, "it-dept-tree", "dept_tree", "agent:view"));
        String inDept = insertAgent(WS, USER, USER, "private");
        String inSubDept = insertAgent(WS, ADMIN, ADMIN, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(inDept, inSubDept);
    }

    /** custom 范围：仅配置的自定义部门资源可见。 */
    @Test
    void shouldShowConfiguredDepartmentsUnderCustomScope() {
        jdbcTemplate.update("UPDATE iam_user SET department_id = ? WHERE id = ?", DEPT_RD, USER);
        jdbcTemplate.update("UPDATE iam_user SET department_id = ? WHERE id = ?", DEPT_ROOT, ADMIN);
        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        String roleId = insertRoleWithPermission(WS, "it-custom", "custom", "agent:view");
        bindDepartments(roleId, DEPT_RD);
        bindRole(WS, DEVELOPER, roleId);
        String inScope = insertAgent(WS, USER, USER, "private");
        String outScope = insertAgent(WS, ADMIN, ADMIN, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(inScope).doesNotContain(outScope);
    }

    /** all 范围：满足模块权限时空间内全部资源可见。 */
    @Test
    void shouldShowAllResourcesUnderAllScope() {
        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        bindRole(WS, DEVELOPER, insertRoleWithPermission(WS, "it-all", "all", "agent:view"));
        String a = insertAgent(WS, USER, USER, "private");
        String b = insertAgent(WS, ADMIN, ADMIN, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(a, b);
    }

    /** 平台管理员列表全量可见（过滤短路）。 */
    @Test
    void shouldSeeAllWorkspaceResourcesAsPlatformManager() {
        authenticateAs(ADMIN);
        WorkspaceContextHolder.bind(WS);
        String a = insertAgent(WS, USER, USER, "private");
        String b = insertAgent(WS, DEVELOPER, DEVELOPER, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(a, b);
    }

    /** 跨工作空间资源不泄漏：绑定当前空间看不到其他空间的同名 owner 资源。 */
    @Test
    void shouldIsolateResourcesAcrossWorkspaces() {
        bindSelfScope();
        String local = insertAgent(WS, DEVELOPER, DEVELOPER, "private");
        String remote = insertAgent(WS_OTHER, DEVELOPER, DEVELOPER, "private");

        List<String> ids = agentService.listAgents().stream().map(AgentSummary::getId).toList();

        assertThat(ids).contains(local).doesNotContain(remote);
    }

    /** 四种资源类型统一下沉：self 范围下各自仅返回本人资源。 */
    @Test
    void shouldFilterAllResourceTypesBySelfScope() {
        bindSelfScope();
        String agentMine = insertAgent(WS, DEVELOPER, DEVELOPER, "private");
        String agentForeign = insertAgent(WS, USER, USER, "private");
        String kbMine = insertKb(WS, DEVELOPER, DEVELOPER, "private");
        String kbForeign = insertKb(WS, USER, USER, "private");
        String toolMine = insertTool(WS, DEVELOPER, DEVELOPER);
        String toolForeign = insertTool(WS, USER, USER);
        String workflowMine = insertWorkflow(WS, DEVELOPER, DEVELOPER, "private");
        String workflowForeign = insertWorkflow(WS, USER, USER, "private");

        assertThat(agentService.listAgents()).extracting(AgentSummary::getId)
                .contains(agentMine).doesNotContain(agentForeign);
        assertThat(knowledgeBaseService.listKnowledgeBases()).extracting(com.openagentflow.domain.knowledge.KnowledgeBaseSummary::getId)
                .contains(kbMine).doesNotContain(kbForeign);
        assertThat(toolService.listTools()).extracting(com.openagentflow.domain.tool.ToolDefinitionSummary::getId)
                .contains(toolMine).doesNotContain(toolForeign);
        assertThat(workflowService.listWorkflows()).extracting(WorkflowDtos.Summary::getId)
                .contains(workflowMine).doesNotContain(workflowForeign);
    }

    /** 回归护栏：SQL 过滤结果与内存 canView 逐条判定一致（self 范围，含 public/ACL 混合数据）。 */
    @Test
    void sqlFilterMatchesMemoryCanView() {
        bindSelfScope();
        insertAgent(WS, DEVELOPER, DEVELOPER, "private");
        insertAgent(WS, USER, USER, "private");
        insertAgent(WS, USER, USER, "public");
        String aclAgent = insertAgent(WS, USER, USER, "private");
        insertAcl(WS, "agent", aclAgent, "user", DEVELOPER, "read", null);

        assertSqlEqualsMemoryCanView();
    }

    /** 回归护栏（dept 范围）：数据范围分支的 SQL 与内存判定一致。 */
    @Test
    void sqlFilterMatchesMemoryCanViewUnderDeptScope() {
        jdbcTemplate.update("UPDATE iam_user SET department_id = ? WHERE id = ?", DEPT_RD, USER);
        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        bindRole(WS, DEVELOPER, insertRoleWithPermission(WS, "it-guard-dept", "dept", "agent:view"));
        insertAgent(WS, USER, USER, "private");
        insertAgent(WS, ADMIN, ADMIN, "private");

        assertSqlEqualsMemoryCanView();
    }

    /** 核心护栏：当前空间全部 agent 经 SQL 过滤的列表，必须与逐条 canView 判定结果完全一致。 */
    private void assertSqlEqualsMemoryCanView() {
        List<String> sqlIds = agentService.listAgents().stream().map(AgentSummary::getId).sorted().toList();
        List<String> memoryIds = agentMapper.selectList(
                        new LambdaQueryWrapper<AgentEntity>().isNull(AgentEntity::getDeletedAt))
                .stream().filter(agentAccessService::canView).map(AgentEntity::getId).sorted().toList();
        assertThat(sqlIds).as("SQL 列表过滤结果应与内存 canView 判定一致").containsExactlyElementsOf(memoryIds);
    }

    /** 绑定 self 角色并以开发者视角查询默认工作空间。 */
    private void bindSelfScope() {
        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        bindRole(WS, DEVELOPER, insertRole(WS, "it-self", "self"));
    }

    private String insertAgent(String workspaceId, String owner, String createdBy, String visibility) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO agent (id, workspace_id, agent_code, agent_name, category, agent_type, model_params,
                                   owner_user_id, created_by, visibility, status)
                VALUES (?, ?, ?, ?, '测试', 'chat', '{}', ?, ?, ?, 'draft')
                """, id, workspaceId, "it_agent_" + id.substring(0, 8), "Agent_" + id.substring(0, 8), owner, createdBy, visibility);
        return id;
    }

    private String insertKb(String workspaceId, String owner, String createdBy, String visibility) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO knowledge_base (id, workspace_id, kb_code, kb_name, owner_user_id, created_by, visibility, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'active')
                """, id, workspaceId, "it_kb_" + id.substring(0, 8), "KB_" + id.substring(0, 8), owner, createdBy, visibility);
        return id;
    }

    private String insertTool(String workspaceId, String owner, String createdBy) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO tool_definition (id, workspace_id, tool_code, tool_name, tool_type, auth_config, headers,
                                             request_schema, response_schema, owner_user_id, created_by, status)
                VALUES (?, ?, ?, ?, 'http', '{}', '{}', '{}', '{}', ?, ?, 'active')
                """, id, workspaceId, "it_tool_" + id.substring(0, 8), "Tool_" + id.substring(0, 8), owner, createdBy);
        return id;
    }

    private String insertWorkflow(String workspaceId, String owner, String createdBy, String visibility) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO workflow_definition (id, workspace_id, workflow_code, workflow_name, graph_json,
                                                 variable_schema, owner_user_id, created_by, visibility, status)
                VALUES (?, ?, ?, ?, '{}', '{}', ?, ?, ?, 'draft')
                """, id, workspaceId, "it_wf_" + id.substring(0, 8), "Workflow_" + id.substring(0, 8), owner, createdBy, visibility);
        return id;
    }

    private String insertRole(String workspaceId, String roleCode, String dataScope) {
        String roleId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO iam_workspace_role
                  (id, workspace_id, role_code, role_name, description, data_scope, built_in, status, created_by)
                VALUES (?, ?, ?, ?, NULL, ?, 0, 'enabled', ?)
                """, roleId, workspaceId, roleCode, roleCode, dataScope, ADMIN);
        return roleId;
    }

    private String insertRoleWithPermission(String workspaceId, String roleCode, String dataScope, String... permissionCodes) {
        String roleId = insertRole(workspaceId, roleCode, dataScope);
        for (String code : permissionCodes) {
            grantPermission(roleId, code);
        }
        return roleId;
    }

    private void grantPermission(String roleId, String permissionCode) {
        String permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam_permission WHERE permission_code = ?", String.class, permissionCode);
        jdbcTemplate.update("INSERT INTO iam_workspace_role_permission (role_id, permission_id, created_by) VALUES (?, ?, ?)",
                roleId, permissionId, ADMIN);
    }

    private void bindRole(String workspaceId, String userId, String roleId) {
        jdbcTemplate.update("""
                INSERT INTO iam_workspace_member_role (workspace_id, user_id, role_id, created_by)
                VALUES (?, ?, ?, ?)
                """, workspaceId, userId, roleId, ADMIN);
    }

    private void bindDepartments(String roleId, String... departmentIds) {
        for (String departmentId : departmentIds) {
            jdbcTemplate.update("""
                    INSERT INTO iam_workspace_role_department (role_id, department_id, created_by)
                    VALUES (?, ?, ?)
                    """, roleId, departmentId, ADMIN);
        }
    }

    private void insertAcl(String workspaceId, String resourceType, String resourceId,
                           String subjectType, String subjectId, String permissionLevel, LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO iam_resource_acl
                  (id, workspace_id, resource_type, resource_id, subject_type, subject_id,
                   permission_level, status, expires_at, grant_reason, granted_by, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'enabled', ?, NULL, ?, ?)
                """, UUID.randomUUID().toString(), workspaceId, resourceType, resourceId,
                subjectType, subjectId, permissionLevel, expiresAt, ADMIN, ADMIN);
    }

    private void authenticateAs(String userId) {
        AuthUserDetails details = userDetailsService.loadUserById(userId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
    }
}
