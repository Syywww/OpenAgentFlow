package com.openagentflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作空间隔离过滤器（WorkspaceIsolationFilter）组件级测试。
 *
 * <p>覆盖 HTTP 入口的租户拦截行为：缺失 X-Workspace-Id、非成员访问、成员绑定与清理、系统管理员放行。
 * 不启动容器，用 Mockito 模拟成员关系查询，聚焦过滤器自身逻辑。</p>
 */
class WorkspaceIsolationFilterTests {

    private static final String WORKSPACE_A = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String USER_ID = "00000000-0000-0000-0000-000000000100";

    private final JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    private final ObjectMapper objectMapper = createObjectMapper();

    /** ApiResponse 含 LocalDateTime 时间戳，需注册 JSR-310 模块，否则 403 响应序列化失败。 */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        WorkspaceContextHolder.clear();
    }

    private WorkspaceIsolationFilter filter(boolean requireWorkspaceContext) {
        return new WorkspaceIsolationFilter(jdbcTemplate, objectMapper, requireWorkspaceContext);
    }

    private MockHttpServletRequest request(String path, String workspaceHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setContextPath("/api");
        request.setRequestURI(path);
        if (workspaceHeader != null) {
            request.addHeader("X-Workspace-Id", workspaceHeader);
        }
        return request;
    }

    private void authenticate(AuthUserDetails details) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
    }

    private AuthUserDetails ordinaryMember() {
        AuthUserDetails details = Mockito.mock(AuthUserDetails.class);
        when(details.getUserId()).thenReturn(USER_ID);
        // getAuthorities 返回 extends 通配符，thenReturn 无法匹配捕获，改用 doReturn。
        doReturn(memberAuthorities()).when(details).getAuthorities();
        return details;
    }

    private AuthUserDetails systemManager() {
        AuthUserDetails details = Mockito.mock(AuthUserDetails.class);
        when(details.getUserId()).thenReturn(USER_ID);
        doReturn(managerAuthorities()).when(details).getAuthorities();
        return details;
    }

    private FilterChain chain(AtomicBoolean executed) {
        return (request, response) -> executed.set(true);
    }

    /** 以 Collection 类型变量传入，匹配 UserDetails#getAuthorities 的 extends 通配符捕获。 */
    private Collection<GrantedAuthority> memberAuthorities() {
        return List.<GrantedAuthority>of();
    }

    private Collection<GrantedAuthority> managerAuthorities() {
        return List.<GrantedAuthority>of(() -> "ROLE_admin");
    }

    /** 缺失 X-Workspace-Id 且路径属于租户资源 → 403 WORKSPACE_REQUIRED，不进入业务链路。 */
    @Test
    void shouldRejectMissingWorkspaceHeaderOnGovernedPath() throws Exception {
        WorkspaceIsolationFilter filter = filter(true);
        MockHttpServletRequest request = request("/api/agents", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainExecuted = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainExecuted));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("WORKSPACE_REQUIRED");
        assertThat(chainExecuted).isFalse();
    }

    /** 缺失头但路径不是租户资源 → 放行（健康检查等公开端点）。 */
    @Test
    void shouldPassThroughWhenMissingHeaderOnNonGovernedPath() throws Exception {
        WorkspaceIsolationFilter filter = filter(true);
        MockHttpServletRequest request = request("/api/health", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainExecuted = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainExecuted));

        assertThat(chainExecuted).isTrue();
    }

    /** 携带头但不是该空间成员 → 403 WORKSPACE_FORBIDDEN。 */
    @Test
    void shouldRejectNonMemberWorkspaceAccess() throws Exception {
        authenticate(ordinaryMember());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);

        WorkspaceIsolationFilter filter = filter(true);
        MockHttpServletRequest request = request("/api/agents", WORKSPACE_A);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainExecuted = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainExecuted));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("WORKSPACE_FORBIDDEN");
        assertThat(chainExecuted).isFalse();
    }

    /** 成员访问：业务链路内绑定工作空间上下文，过滤器返回后清理，防线程池复用串租户。 */
    @Test
    void shouldBindWorkspaceContextForMember() throws Exception {
        authenticate(ordinaryMember());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);

        WorkspaceIsolationFilter filter = filter(true);
        MockHttpServletRequest request = request("/api/agents", WORKSPACE_A);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean boundInChain = new AtomicBoolean(false);

        FilterChain chain = (req, res) -> boundInChain.set(WorkspaceContextHolder.current().equals(WORKSPACE_A));
        filter.doFilter(request, response, chain);

        assertThat(boundInChain).isTrue();
        assertThat(WorkspaceContextHolder.current()).isNull();
    }

    /** 系统管理员无需成员校验即可进入任意空间（SQL 层仍按所选空间过滤）。 */
    @Test
    void shouldAllowSystemManagerWithoutMembershipCheck() throws Exception {
        authenticate(systemManager());
        // 故意不 stub 成员查询：若实现仍查成员表，mock 返回 null 会走向 403，从而暴露错误。

        WorkspaceIsolationFilter filter = filter(true);
        MockHttpServletRequest request = request("/api/agents", WORKSPACE_A);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainExecuted = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(chainExecuted));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainExecuted).isTrue();
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class), any(Object[].class));
    }
}