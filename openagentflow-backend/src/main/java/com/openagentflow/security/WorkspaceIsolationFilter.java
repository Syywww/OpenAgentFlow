package com.openagentflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 工作空间硬隔离过滤器。
 *
 * <p>客户端通过X-Workspace-Id选择空间，过滤器在业务代码执行前校验成员关系并绑定可信上下文。</p>
 */
@Component
public class WorkspaceIsolationFilter extends OncePerRequestFilter {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    /** 是否对租户资源接口强制要求工作空间上下文。 */
    private final boolean requireWorkspaceContext;

    public WorkspaceIsolationFilter(JdbcTemplate jdbcTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("${openagentflow.tenancy.require-workspace-context:true}") boolean requireWorkspaceContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.requireWorkspaceContext = requireWorkspaceContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String workspaceId = request.getHeader("X-Workspace-Id");
        if (!StringUtils.hasText(workspaceId)) {
            if (requireWorkspaceContext && WorkspacePathPolicy.requiresWorkspace(request.getMethod(), requestPath(request))) {
                writeForbidden(response, "WORKSPACE_REQUIRED", "请选择工作空间后再访问租户资源");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserDetails userDetails)) {
            writeForbidden(response, "WORKSPACE_AUTH_REQUIRED", "工作空间访问需要有效登录身份");
            return;
        }
        if (!isSystemManager(authentication) && !isWorkspaceMember(workspaceId, userDetails.getUserId())) {
            writeForbidden(response, "WORKSPACE_FORBIDDEN", "无权访问所选工作空间");
            return;
        }
        try {
            WorkspaceContextHolder.bind(workspaceId);
            filterChain.doFilter(request, response);
        } finally {
            WorkspaceContextHolder.clear();
        }
    }

    /** 获取去掉 context-path 的业务路径。 */
    private String requestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** 输出统一的工作空间拒绝响应。 */
    private void writeForbidden(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(code, message)));
    }

    private boolean isWorkspaceMember(String workspaceId, String userId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM oaf_workspace_member
                WHERE workspace_id=? AND user_id=? AND status IN ('active','enabled')
                """, Long.class, workspaceId, userId);
        return count != null && count > 0;
    }

    private boolean isSystemManager(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        return PlatformAuthorityPolicy.isPlatformManager(authorities);
    }
}
