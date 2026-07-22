package com.openagentflow.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限治理核心策略测试。
 */
class PermissionGovernancePolicyTests {

    /** 普通模块管理权限不能被提升为平台管理员。 */
    @Test
    void shouldOnlyTreatPlatformRolesAsPlatformManager() {
        assertThat(PlatformAuthorityPolicy.isPlatformManager(Set.of("ROLE_super_admin"))).isTrue();
        assertThat(PlatformAuthorityPolicy.isPlatformManager(Set.of("ROLE_admin"))).isTrue();
        assertThat(PlatformAuthorityPolicy.isPlatformManager(Set.of("agent:manage"))).isFalse();
        assertThat(PlatformAuthorityPolicy.isPlatformManager(Set.of("workspace:manage"))).isFalse();
    }

    /** 所有包含空间数据的业务模块都必须携带工作空间上下文。 */
    @Test
    void shouldRequireWorkspaceForTenantResourcePaths() {
        assertThat(WorkspacePathPolicy.requiresWorkspace("GET", "/agents")).isTrue();
        assertThat(WorkspacePathPolicy.requiresWorkspace("GET", "/mcp-servers")).isTrue();
        assertThat(WorkspacePathPolicy.requiresWorkspace("POST", "/model-gateway/policies")).isTrue();
        assertThat(WorkspacePathPolicy.requiresWorkspace("GET", "/templates")).isTrue();
        assertThat(WorkspacePathPolicy.requiresWorkspace("GET", "/runs/1001")).isTrue();
        assertThat(WorkspacePathPolicy.requiresWorkspace("GET", "/iam-admin/governance/workspace-roles")).isTrue();
        assertThat(WorkspacePathPolicy.requiresWorkspace("POST", "/iam-admin/resource-acls")).isTrue();
        assertThat(WorkspacePathPolicy.requiresWorkspace("GET", "/iam-admin/users")).isFalse();
        assertThat(WorkspacePathPolicy.requiresWorkspace("POST", "/auth/logout")).isFalse();
    }

    /** 路由策略必须区分查看、管理和运行权限。 */
    @Test
    void shouldResolveAuthoritiesByModuleAndOperation() {
        assertThat(ApiAuthorizationPolicy.requiredAuthorities("GET", "/agents"))
                .contains("agent:view", "agent:manage");
        assertThat(ApiAuthorizationPolicy.requiredAuthorities("POST", "/agents"))
                .contains("agent:create", "agent:manage");
        assertThat(ApiAuthorizationPolicy.requiredAuthorities("POST", "/agents/1001/run"))
                .contains("agent:run", "agent:manage");
        assertThat(ApiAuthorizationPolicy.requiredAuthorities("GET", "/iam-admin/users"))
                .containsExactly("iam:manage");
        assertThat(ApiAuthorizationPolicy.requiredAuthorities("GET", "/unknown-module"))
                .isEmpty();
    }

    /** 到期或停用的资源授权不能继续放行。 */
    @Test
    void shouldRejectExpiredOrDisabledResourceAcl() {
        Instant now = Instant.parse("2026-07-22T08:00:00Z");
        assertThat(ResourceAclPolicy.isActive("enabled", null, now)).isTrue();
        assertThat(ResourceAclPolicy.isActive("enabled", now.plusSeconds(60), now)).isTrue();
        assertThat(ResourceAclPolicy.isActive("enabled", now.minusSeconds(1), now)).isFalse();
        assertThat(ResourceAclPolicy.isActive("disabled", null, now)).isFalse();
    }

    /** 多个角色的数据范围必须合并为权限更大的范围。 */
    @Test
    void shouldMergeDataScopeByPrivilegeOrder() {
        assertThat(DataScopePolicy.merge(Set.of("self", "dept"))).isEqualTo("dept");
        assertThat(DataScopePolicy.merge(Set.of("custom", "dept_tree"))).isEqualTo("dept_tree");
        assertThat(DataScopePolicy.merge(Set.of("self", "all"))).isEqualTo("all");
        assertThat(DataScopePolicy.merge(Set.of())).isEqualTo("self");
    }

    /** 资源查看权限不能被误用为编辑权限。 */
    @Test
    void shouldSeparateResourceReadAndManagePermissions() {
        assertThat(ResourceModulePermissionPolicy.requiredPermissions("tool", false))
                .containsExactlyInAnyOrder("tool:view", "tool:manage");
        assertThat(ResourceModulePermissionPolicy.requiredPermissions("tool", true))
                .containsExactly("tool:manage");
        assertThat(ResourceModulePermissionPolicy.requiredPermissions("agent", true))
                .containsExactlyInAnyOrder("agent:update", "agent:manage");
    }
}
