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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资源 ACL 授权判定集成测试。
 *
 * <p>现有单测只覆盖纯策略函数（到期判断、模块权限、数据范围合并），本测试验证
 * {@code ResourceAclService.currentUserHasAcl} 的**真实判定 SQL**：user/role/department
 * 三种主体、过期/撤销失效、跨工作空间隔离、平台管理员短路。</p>
 */
@SpringBootTest
class ResourceAclServiceIT extends MySqlContainerIntegrationTestSupport {

    private static final String WS = "90000000-0000-0000-0000-000000000101";
    private static final String WS_OTHER = "bbbbbbbb-0000-0000-0000-000000000002";
    private static final String ADMIN = "00000000-0000-0000-0000-000000000100";
    private static final String DEVELOPER = "00000000-0000-0000-0000-000000000101";
    private static final String USER = "00000000-0000-0000-0000-000000000102";
    private static final String DEPT = "00000000-0000-0000-0000-000000000002";

    @Autowired private ResourceAclService resourceAclService;
    @Autowired private AuthUserDetailsService userDetailsService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 共享数据库：清理授权与测试角色（member_role 等依赖角色级联删除），避免断言受历史数据干扰。
        jdbcTemplate.update("DELETE FROM iam_resource_acl");
        jdbcTemplate.update("DELETE FROM iam_workspace_role");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        WorkspaceContextHolder.clear();
    }

    /** 管理员为开发者授予资源读权限，开发者命中、普通用户不命中（端到端 grant 链路）。 */
    @Test
    void shouldGrantUserReadAclAndResolveForGrantee() {
        authenticateAs(ADMIN);
        WorkspaceContextHolder.bind(WS);
        resourceAclService.grant(request(WS, "agent", "agent-001", "user", DEVELOPER, "read", null, "测试授权"));

        authenticateAs(DEVELOPER);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-001", List.of("read"))).isTrue();
        authenticateAs(USER);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-001", List.of("read"))).isFalse();
    }

    /** 授权绑定所属工作空间：同一用户切到另一空间后授权不生效。 */
    @Test
    void shouldScopeAclByWorkspace() {
        insertAcl(WS, "agent", "agent-002", "user", DEVELOPER, "read", null);
        authenticateAs(DEVELOPER);

        WorkspaceContextHolder.bind(WS_OTHER);
        assertThat(resourceAclService.currentUserHasAcl(WS_OTHER, "agent", "agent-002", List.of("read"))).isFalse();

        WorkspaceContextHolder.bind(WS);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-002", List.of("read"))).isTrue();
    }

    /** 过期授权失效、未过期授权生效。 */
    @Test
    void shouldRespectExpiresAt() {
        insertAcl(WS, "agent", "agent-expired", "user", DEVELOPER, "read", LocalDateTime.now().minusSeconds(1));
        insertAcl(WS, "agent", "agent-valid", "user", DEVELOPER, "run", LocalDateTime.now().plusSeconds(3600));
        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);

        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-expired", List.of("read"))).isFalse();
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-valid", List.of("run"))).isTrue();
    }

    /** 撤销后授权失效（端到端 revoke 链路）。 */
    @Test
    void shouldRejectRevokedAcl() {
        authenticateAs(ADMIN);
        WorkspaceContextHolder.bind(WS);
        PermissionGovernanceDtos.ResourceAclSummary granted = resourceAclService.grant(
                request(WS, "agent", "agent-003", "user", DEVELOPER, "write", null, "待撤销"));

        authenticateAs(DEVELOPER);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-003", List.of("write"))).isTrue();

        authenticateAs(ADMIN);
        resourceAclService.revoke(WS, granted.id(), "权限回收测试");

        authenticateAs(DEVELOPER);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-003", List.of("write"))).isFalse();
    }

    /** 角色主体授权：关联该空间角色的成员命中，未关联用户不命中。 */
    @Test
    void shouldMatchRoleSubjectAcl() {
        String roleId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO iam_workspace_role
                  (id, workspace_id, role_code, role_name, description, data_scope, built_in, status, created_by)
                VALUES (?, ?, 'it-role', '测试角色', NULL, 'self', 0, 'enabled', ?)
                """, roleId, WS, ADMIN);
        jdbcTemplate.update("""
                INSERT INTO iam_workspace_member_role (workspace_id, user_id, role_id, created_by)
                VALUES (?, ?, ?, ?)
                """, WS, DEVELOPER, roleId, ADMIN);
        insertAcl(WS, "agent", "agent-role", "role", roleId, "read", null);

        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-role", List.of("read"))).isTrue();

        authenticateAs(USER);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-role", List.of("read"))).isFalse();
    }

    /** 部门主体授权：命中用户所属部门；不同部门不命中。 */
    @Test
    void shouldMatchDepartmentSubjectAcl() {
        insertAcl(WS, "agent", "agent-dept", "department", DEPT, "read", null);
        insertAcl(WS, "agent", "agent-other-dept", "department", "11111111-0000-0000-0000-000000000099", "read", null);

        authenticateAs(DEVELOPER);
        WorkspaceContextHolder.bind(WS);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-dept", List.of("read"))).isTrue();
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "agent-other-dept", List.of("read"))).isFalse();
    }

    /** 平台管理员（super_admin/admin 角色）无需授权即对任意资源有判定放行。 */
    @Test
    void shouldGrantPlatformManagerUnconditionalAccess() {
        authenticateAs(ADMIN);
        WorkspaceContextHolder.bind(WS);
        assertThat(resourceAclService.currentUserHasAcl(WS, "agent", "any-resource", List.of("read"))).isTrue();
    }

    private PermissionGovernanceDtos.ResourceAclRequest request(String workspaceId, String resourceType,
                                                                String resourceId, String subjectType,
                                                                String subjectId, String permissionLevel,
                                                                LocalDateTime expiresAt, String reason) {
        return new PermissionGovernanceDtos.ResourceAclRequest(
                workspaceId, resourceType, resourceId, subjectType, subjectId, permissionLevel, expiresAt, reason);
    }

    /** 直接写入一条资源授权记录（绕过管理端权限校验，聚焦判定逻辑）。 */
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