package com.openagentflow.service;

import com.openagentflow.domain.iam.PermissionGovernanceDtos;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据范围解析集成测试。
 *
 * <p>现有单测只覆盖纯策略合并函数 {@code DataScopePolicy.merge}，本测试验证
 * {@code WorkspaceAuthorizationService.resolveDataScope} 的**真实解析 SQL**：
 * 多角色数据范围合并、无角色默认 self、custom 部门聚合、dept/dept_tree 按用户主部门与递归、
 * 禁用角色过滤与跨工作空间隔离。</p>
 */
@SpringBootTest
class WorkspaceDataScopeIT extends MySqlContainerIntegrationTestSupport {

    private static final String WS = "90000000-0000-0000-0000-000000000101";
    private static final String WS_OTHER = "cccccccc-0000-0000-0000-000000000101";
    private static final String ADMIN = "00000000-0000-0000-0000-000000000100";
    private static final String DEVELOPER = "00000000-0000-0000-0000-000000000101";
    private static final String DEPT_ROOT = "00000000-0000-0000-0000-000000000001";
    private static final String DEPT_RD = "00000000-0000-0000-0000-000000000002";
    private static final String DEPT_SUB = "99999999-0000-0000-0000-000000000003";

    @Autowired private WorkspaceAuthorizationService authorizationService;
    @Autowired private AuthUserDetailsService userDetailsService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 共享数据库：清空空间角色（member_role、role_department 随之级联删除）与测试隔离数据，避免历史绑定干扰断言。
        jdbcTemplate.update("DELETE FROM iam_workspace_role");
        jdbcTemplate.update("DELETE FROM iam_department WHERE parent_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM oaf_workspace WHERE workspace_code='it-data-scope'");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        WorkspaceContextHolder.clear();
    }

    /** 多角色数据范围合并取最大（self<dept<all），且 all 不收集部门。 */
    @Test
    void shouldMergeDataScopesAcrossRoles() {
        authenticateAs(ADMIN);
        bindRole(WS, DEVELOPER, insertRole(WS, "it-self", "self"));
        bindRole(WS, DEVELOPER, insertRole(WS, "it-dept", "dept"));
        bindRole(WS, DEVELOPER, insertRole(WS, "it-all", "all"));

        PermissionGovernanceDtos.DataScopeResult result = authorizationService.resolveDataScope(WS, DEVELOPER);

        assertThat(result.scopeType()).isEqualTo("all");
        assertThat(result.ownerOnly()).isFalse();
        assertThat(result.departmentIds()).isEmpty();
    }

    /** 未绑定任何空间角色时回退本人数据范围。 */
    @Test
    void shouldDefaultToSelfWithoutRoles() {
        authenticateAs(ADMIN);

        PermissionGovernanceDtos.DataScopeResult result = authorizationService.resolveDataScope(WS, DEVELOPER);

        assertThat(result.scopeType()).isEqualTo("self");
        assertThat(result.ownerOnly()).isTrue();
    }

    /** custom 范围跨多个角色聚合部门并去重。 */
    @Test
    void shouldAggregateCustomDepartmentsFromRoles() {
        authenticateAs(ADMIN);
        String customA = insertRole(WS, "it-custom-a", "custom");
        String customB = insertRole(WS, "it-custom-b", "custom");
        bindDepartments(customA, DEPT_ROOT, DEPT_RD);
        bindDepartments(customB, DEPT_RD);
        bindRole(WS, DEVELOPER, customA);
        bindRole(WS, DEVELOPER, customB);

        PermissionGovernanceDtos.DataScopeResult result = authorizationService.resolveDataScope(WS, DEVELOPER);

        assertThat(result.scopeType()).isEqualTo("custom");
        assertThat(result.departmentIds()).containsExactlyInAnyOrder(DEPT_ROOT, DEPT_RD);
    }

    /** dept 范围取用户主部门（真实 SQL 查 iam_user.department_id）。 */
    @Test
    void shouldResolveDeptFromPrimaryDepartment() {
        authenticateAs(ADMIN);
        bindRole(WS, DEVELOPER, insertRole(WS, "it-dept", "dept"));

        PermissionGovernanceDtos.DataScopeResult result = authorizationService.resolveDataScope(WS, DEVELOPER);

        assertThat(result.scopeType()).isEqualTo("dept");
        assertThat(result.departmentIds()).containsExactly(DEPT_RD);
    }

    /** dept_tree 范围返回主部门及其全部子孙部门（递归查询）。 */
    @Test
    void shouldResolveDeptTreeWithDescendants() {
        jdbcTemplate.update("""
                INSERT INTO iam_department (id, parent_id, dept_code, dept_name)
                VALUES (?, ?, 'it-sub-rd', 'IT子研发中心')
                """, DEPT_SUB, DEPT_RD);
        authenticateAs(ADMIN);
        bindRole(WS, DEVELOPER, insertRole(WS, "it-dept-tree", "dept_tree"));

        PermissionGovernanceDtos.DataScopeResult result = authorizationService.resolveDataScope(WS, DEVELOPER);

        assertThat(result.scopeType()).isEqualTo("dept_tree");
        assertThat(result.departmentIds()).containsExactlyInAnyOrder(DEPT_RD, DEPT_SUB);
    }

    /** 角色按工作空间隔离且仅统计启用角色：另一空间的 all 角色与禁用角色均不跨空间生效。 */
    @Test
    void shouldIsolateScopeByWorkspaceAndIgnoreDisabledRoles() {
        jdbcTemplate.update("""
                INSERT INTO oaf_workspace (id, organization_id, workspace_code, workspace_name, workspace_type,
                                           owner_user_id, default_flag, status, created_by)
                VALUES (?, '90000000-0000-0000-0000-000000000001', 'it-data-scope', '数据范围测试空间', 'team', ?, 0, 'enabled', ?)
                """, WS_OTHER, ADMIN, ADMIN);
        authenticateAs(ADMIN);
        bindRole(WS, DEVELOPER, insertRole(WS, "it-all", "all"));
        bindRole(WS_OTHER, DEVELOPER, insertRole(WS_OTHER, "it-disabled", "all", "disabled"));

        PermissionGovernanceDtos.DataScopeResult result = authorizationService.resolveDataScope(WS_OTHER, DEVELOPER);

        assertThat(result.scopeType()).isEqualTo("self");
    }

    private String insertRole(String workspaceId, String roleCode, String dataScope) {
        return insertRole(workspaceId, roleCode, dataScope, "enabled");
    }

    private String insertRole(String workspaceId, String roleCode, String dataScope, String status) {
        String roleId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO iam_workspace_role
                  (id, workspace_id, role_code, role_name, description, data_scope, built_in, status, created_by)
                VALUES (?, ?, ?, ?, NULL, ?, 0, ?, ?)
                """, roleId, workspaceId, roleCode, roleCode, dataScope, status, ADMIN);
        return roleId;
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

    private void authenticateAs(String userId) {
        AuthUserDetails details = userDetailsService.loadUserById(userId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
    }
}