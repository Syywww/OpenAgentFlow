package com.openagentflow.service;

import com.openagentflow.domain.iam.PermissionGovernanceDtos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 列表 data_scope 过滤 SQL 生成器单元测试。
 *
 * <p>以固定入参驱动 {@link DataScopeListFilter#resolveAndBuild}（绕过安全上下文），验证：
 * 各数据范围片段的 SQL 形态、tool 无 public 分支、ACL 三主体、占位符与参数一致性（防注入）、
 * 平台管理员短路与未登录回退。</p>
 */
@ExtendWith(MockitoExtension.class)
class DataScopeListFilterTest {

    private static final String WS = "90000000-0000-0000-0000-000000000101";
    private static final String USER = "00000000-0000-0000-0000-000000000101";
    private static final String DEPT_RD = "00000000-0000-0000-0000-000000000002";
    private static final String DEPT_SUB = "99999999-0000-0000-0000-000000000003";

    @Mock private WorkspaceAuthorizationService workspaceAuthorizationService;
    @InjectMocks private DataScopeListFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 平台管理员（ROLE_admin）短路：不构建过滤 SQL，全量可见。 */
    @Test
    void platformManagerReturnsNullFilter() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));

        assertThat(filter.buildListVisibilityFilter(WS, USER, "agent")).isNull();
    }

    /** 普通用户不走短路，返回可用的过滤片段。 */
    @Test
    void nonManagerBuildsFilter() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("self", List.of(), true));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "user", null, List.of(new SimpleGrantedAuthority("ROLE_developer"))));

        DataScopeListFilter.ListFilter result = filter.buildListVisibilityFilter(WS, USER, "agent");

        assertThat(result).isNotNull();
        assertThat(result.requiresFilter()).isTrue();
    }

    /** 未登录防御：有可见性列的资源仅放行 public；tool 无 public 列则直接不可见。 */
    @Test
    void nullUserFallsBackToPublicOnly() {
        DataScopeListFilter.ListFilter agent = filter.resolveAndBuild(WS, null, "agent");
        assertThat(agent.sql()).isEqualTo("agent.visibility = 'public'");
        assertThat(agent.args()).isEmpty();

        DataScopeListFilter.ListFilter tool = filter.resolveAndBuild(WS, null, "tool");
        assertThat(tool.sql()).isEqualTo("1=0");
        assertThat(tool.args()).isEmpty();
    }

    /** self 范围：owner/createdBy/public/ACL 四分支保留，数据范围分支为 1=0。 */
    @Test
    void selfScopeBuildsFullShapeSql() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("self", List.of(), true));

        DataScopeListFilter.ListFilter result = filter.resolveAndBuild(WS, USER, "agent");

        assertThat(result.sql())
                .contains("agent.visibility = 'public'")
                .contains("agent.owner_user_id = {0}")
                .contains("agent.created_by = {1}")
                .contains("iam_resource_acl")
                .contains("1=0");
        assertThat(result.args()).containsExactly(USER, USER, WS, "agent", USER, WS, USER, USER, WS, USER, WS, USER);
        assertPlaceholdersMatchArgs(result);
    }

    /** all 范围：数据范围分支恒真（仍受外层模块权限门控）。 */
    @Test
    void allScopeUsesOneEqualsOne() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("all", List.of(), false));

        assertThat(filter.resolveAndBuild(WS, USER, "agent").sql()).contains("1=1");
    }

    /** dept 范围：资源归属部门与当前用户主部门相等比较。 */
    @Test
    void deptScopeComparesDepartments() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("dept", List.of(DEPT_RD), false));

        DataScopeListFilter.ListFilter result = filter.resolveAndBuild(WS, USER, "agent");

        assertThat(result.sql())
                .contains("SELECT department_id FROM iam_user WHERE id = COALESCE(agent.owner_user_id, agent.created_by)")
                .contains("= (SELECT department_id FROM iam_user WHERE id = {");
        assertPlaceholdersMatchArgs(result);
    }

    /** dept_tree / custom 范围：资源归属部门落在预计算部门集合内。 */
    @Test
    void deptTreeAndCustomUseInSet() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("dept_tree", List.of(DEPT_RD, DEPT_SUB), false));

        DataScopeListFilter.ListFilter tree = filter.resolveAndBuild(WS, USER, "agent");
        assertThat(tree.sql()).contains("IN ({");
        assertPlaceholdersMatchArgs(tree);

        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("custom", List.of(DEPT_RD), false));

        DataScopeListFilter.ListFilter custom = filter.resolveAndBuild(WS, USER, "agent");
        assertThat(custom.sql()).contains("IN ({");
        assertPlaceholdersMatchArgs(custom);
    }

    /** 集合为空的 dept_tree/custom：不得生成 IN () 语法错误，直接不可见。 */
    @Test
    void emptyDepartmentSetYieldsOneEqualsZero() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("dept_tree", List.of(), false));

        assertThat(filter.resolveAndBuild(WS, USER, "agent").sql()).contains("1=0");
    }

    /** tool 资源无 visibility 列：SQL 不含 public 分支。 */
    @Test
    void toolOmitsPublicBranch() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("self", List.of(), true));

        assertThat(filter.resolveAndBuild(WS, USER, "tool").sql()).doesNotContain("visibility");
    }

    /** ACL 三主体（user 直接 / role 经成员角色 / department 经用户主部门）都在 EXISTS 中。 */
    @Test
    void aclExistsCoversThreeSubjects() {
        when(workspaceAuthorizationService.resolveDataScopeInternal(WS, USER))
                .thenReturn(new PermissionGovernanceDtos.DataScopeResult("self", List.of(), true));

        String sql = filter.resolveAndBuild(WS, USER, "agent").sql();

        assertThat(sql)
                .contains("acl.subject_type = 'user'")
                .contains("acl.subject_type = 'role'")
                .contains("acl.subject_type = 'department'")
                .contains("acl.expires_at IS NULL OR acl.expires_at > CURRENT_TIMESTAMP(3)");
    }

    /** 不支持的资源类型直接拒绝，避免拼出错误的表名。 */
    @Test
    void unsupportedResourceTypeThrows() {
        assertThatThrownBy(() -> filter.resolveAndBuild(WS, USER, "prompt_template"))
                .isInstanceOf(com.openagentflow.exception.BusinessException.class);
    }

    /** 防注入护栏：SQL 中出现的 {n} 占位符必须与参数数组一一对应。 */
    private void assertPlaceholdersMatchArgs(DataScopeListFilter.ListFilter result) {
        Matcher matcher = Pattern.compile("\\{(\\d+)}").matcher(result.sql());
        int maxSlot = -1;
        while (matcher.find()) {
            maxSlot = Math.max(maxSlot, Integer.parseInt(matcher.group(1)));
        }
        assertThat(maxSlot + 1).as("占位符数量与参数数量一致").isEqualTo(result.args().size());
    }
}
