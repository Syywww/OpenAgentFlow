package com.openagentflow.service;

import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.AuthUserDetailsService;
import com.openagentflow.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/** 权限治理查询链路测试。 */
@SpringBootTest(properties = {
        "openagentflow.async-task.consumer-enabled=false",
        "openagentflow.observability.otlp-enabled=false",
        "openagentflow.milvus.enabled=false"
})
class PermissionGovernanceServiceTests {

    /** 工作空间授权服务。 */
    @Autowired
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    /** 资源授权服务。 */
    @Autowired
    private ResourceAclService resourceAclService;

    /** 用户加载服务。 */
    @Autowired
    private AuthUserDetailsService userDetailsService;

    /** 数据库工具。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 测试工作空间ID。 */
    private String workspaceId;

    /** 建立管理员和工作空间上下文。 */
    @BeforeEach
    void setUpContext() {
        String userId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam_user WHERE username='admin' LIMIT 1", String.class);
        workspaceId = jdbcTemplate.queryForObject(
                "SELECT id FROM oaf_workspace WHERE status='enabled' ORDER BY default_flag DESC LIMIT 1", String.class);
        AuthUserDetails details = userDetailsService.loadUserById(userId);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
        WorkspaceContextHolder.bind(workspaceId);
    }

    /** 清理线程上下文。 */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        WorkspaceContextHolder.clear();
    }

    /** 管理员查询权限治理页面的全部数据源时不应出现运行时异常。 */
    @Test
    void shouldLoadAllPermissionGovernanceDataSources() {
        assertThat(workspaceAuthorizationService.listRoles(workspaceId)).isNotEmpty();
        assertThat(resourceAclService.list(workspaceId)).isNotEmpty();
        assertThat(workspaceAuthorizationService.listAudits(workspaceId)).isNotNull();
    }
}
