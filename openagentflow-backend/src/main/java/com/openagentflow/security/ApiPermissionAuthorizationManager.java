package com.openagentflow.security;

import com.openagentflow.service.WorkspaceAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 统一 API 权限授权管理器。
 *
 * <p>数据库权限路径优先，核心模块策略兜底；严格模式下未声明的业务路由默认拒绝。</p>
 */
@Component
public class ApiPermissionAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    /** 权限路由缓存有效期。 */
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    /** 仅要求登录、不要求业务权限的认证辅助路径。 */
    private static final List<String> AUTHENTICATED_ONLY_PATHS = List.of("/auth/me", "/auth/logout");

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 工作空间授权服务。 */
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    /** 未匹配权限元数据时是否拒绝。 */
    private final boolean strictMode;

    /** Ant风格路径匹配器。 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 权限路由缓存。 */
    private volatile List<PermissionRoute> cachedRoutes = List.of();

    /** 最近缓存刷新时间。 */
    private volatile Instant cacheLoadedAt = Instant.EPOCH;

    public ApiPermissionAuthorizationManager(JdbcTemplate jdbcTemplate,
                                             WorkspaceAuthorizationService workspaceAuthorizationService,
                                             @Value("${openagentflow.security.permission-strict-mode:true}") boolean strictMode) {
        this.jdbcTemplate = jdbcTemplate;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.strictMode = strictMode;
    }

    /**
     * 对当前请求执行统一权限裁决。
     */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                       RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
        if (PlatformAuthorityPolicy.isPlatformManager(authorities)) {
            return new AuthorizationDecision(true);
        }

        HttpServletRequest request = context.getRequest();
        String path = requestPath(request);
        if (AUTHENTICATED_ONLY_PATHS.contains(path)) {
            return new AuthorizationDecision(true);
        }

        Set<String> required = resolveDatabaseAuthorities(request.getMethod(), path);
        if (required.isEmpty()) {
            required = ApiAuthorizationPolicy.requiredAuthorities(request.getMethod(), path);
        }
        if (required.isEmpty()) {
            return new AuthorizationDecision(!strictMode);
        }
        boolean granted = required.stream().anyMatch(authorities::contains)
                || workspaceAuthorizationService.currentUserHasAnyPermission(required);
        return new AuthorizationDecision(granted);
    }

    /** 根据数据库 API 方法和路径解析权限编码。 */
    private Set<String> resolveDatabaseAuthorities(String method, String path) {
        Set<String> result = new LinkedHashSet<>();
        for (PermissionRoute route : permissionRoutes()) {
            boolean methodMatches = !StringUtils.hasText(route.method())
                    || "ALL".equalsIgnoreCase(route.method()) || method.equalsIgnoreCase(route.method());
            if (methodMatches && pathMatcher.match(route.pathPattern(), path)) {
                result.add(route.permissionCode());
            }
        }
        return result;
    }

    /** 加载并短时缓存权限路由，避免每个请求都查询权限表。 */
    private List<PermissionRoute> permissionRoutes() {
        Instant now = Instant.now();
        if (cacheLoadedAt.plus(CACHE_TTL).isAfter(now)) {
            return cachedRoutes;
        }
        synchronized (this) {
            if (cacheLoadedAt.plus(CACHE_TTL).isAfter(now)) {
                return cachedRoutes;
            }
            List<PermissionRoute> loaded = jdbcTemplate.query("""
                    SELECT permission_code,api_method,api_path
                    FROM iam_permission
                    WHERE status='enabled' AND api_path IS NOT NULL AND api_path<>''
                    """, (rs, rowNum) -> new PermissionRoute(
                    rs.getString("permission_code"), rs.getString("api_method"), normalizePattern(rs.getString("api_path"))));
            cachedRoutes = List.copyOf(loaded);
            cacheLoadedAt = now;
            return cachedRoutes;
        }
    }

    /** 获取去掉 context-path 的业务路径。 */
    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = StringUtils.hasText(contextPath) && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length()) : uri;
        return normalizePath(path);
    }

    /** 标准化数据库路径，兼容历史数据中的 /api 前缀。 */
    private String normalizePattern(String pattern) {
        String normalized = normalizePath(pattern);
        return normalized.equals("/api") ? "/" : normalized.startsWith("/api/") ? normalized.substring(4) : normalized;
    }

    /** 标准化请求路径。 */
    private String normalizePath(String path) {
        String normalized = path == null ? "/" : path.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** 数据库权限路由。 */
    private record PermissionRoute(String permissionCode, String method, String pathPattern) {
    }
}
