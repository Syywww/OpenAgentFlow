package com.openagentflow.security;

import com.openagentflow.entity.AuditOperationLogEntity;
import com.openagentflow.mapper.AuditOperationLogMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 操作审计过滤器。
 * <p>记录所有业务接口的访问结果，供审计与风险治理中心检索。</p>
 */
@Component
@Order(200)
public class AuditOperationFilter extends OncePerRequestFilter {

    /** HTTP访问日志分类。 */
    private static final Logger accessLog = LoggerFactory.getLogger("com.openagentflow.http.access");

    /** 审计操作日志 Mapper。 */
    private final AuditOperationLogMapper auditOperationLogMapper;

    /** 慢请求阈值。 */
    private final long slowRequestMs;

    public AuditOperationFilter(AuditOperationLogMapper auditOperationLogMapper,
                                @Value("${openagentflow.logging.slow-request-ms:3000}") long slowRequestMs) {
        this.auditOperationLogMapper = auditOperationLogMapper;
        this.slowRequestMs = Math.max(1L, slowRequestMs);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant startedAt = Instant.now();
        String requestId = resolveRequestId(request);
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        Exception failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                writeAccessLog(request, response, startedAt, failure, requestId);
                writeAuditLog(request, response, startedAt, failure, requestId);
            } finally {
                MDC.remove("requestId");
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.endsWith("/auth/captcha")
                || path.endsWith("/error");
    }

    /**
     * 写入操作审计日志。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param startedAt 请求开始时间
     * @param failure 捕获到的异常
     */
    private void writeAuditLog(HttpServletRequest request,
                               HttpServletResponse response,
                               Instant startedAt,
                               Exception failure,
                               String requestId) {
        try {
            AuditOperationLogEntity log = new AuditOperationLogEntity();
            log.setId(UUID.randomUUID().toString());
            log.setTraceId(requestId);
            fillUser(log);
            log.setOperationType(resolveOperationType(request.getMethod(), request.getRequestURI()));
            log.setResourceType(resolveResourceType(request.getRequestURI()));
            log.setRequestMethod(request.getMethod());
            log.setRequestPath(request.getRequestURI());
            log.setRequestParams(limit(sanitizeQuery(request.getQueryString()), 2000));
            log.setResponseStatus(response.getStatus());
            log.setSuccess(failure == null && response.getStatus() < 400);
            log.setFailureReason(failure == null ? null : limit(failure.getMessage(), 1000));
            log.setClientIp(resolveClientIp(request));
            log.setUserAgent(limit(request.getHeader("User-Agent"), 1000));
            log.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
            auditOperationLogMapper.insert(log);
        } catch (Exception ignored) {
            // 审计失败不能影响主请求，避免因为治理链路异常阻断业务接口。
        }
    }

    /**
     * 向IDEA控制台打印前端接口调用结果。
     */
    private void writeAccessLog(HttpServletRequest request,
                                HttpServletResponse response,
                                Instant startedAt,
                                Exception failure,
                                String requestId) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        int status = failure == null ? response.getStatus() : Math.max(500, response.getStatus());
        String username = currentUsername();
        String workspaceId = request.getHeader("X-Workspace-Id");
        String message = "HTTP {} {} route={} status={} durationMs={} user={} workspace={} clientIp={} requestId={} query={}";
        Object[] arguments = {
                request.getMethod(), request.getRequestURI(), resolveMatchedRoute(request), status, latencyMs,
                safeValue(username), safeValue(workspaceId), resolveClientIp(request), requestId,
                safeValue(sanitizeQuery(request.getQueryString()))
        };
        if (failure != null || status >= 500) {
            accessLog.error(message + " error={}", append(arguments, failure == null ? "HTTP " + status : failure.getMessage()));
        } else if (status >= 400 || latencyMs >= slowRequestMs) {
            accessLog.warn(message, arguments);
        } else {
            accessLog.info(message, arguments);
        }
    }

    /** 获取当前登录用户名。 */
    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    /**
     * 获取Spring MVC实际匹配的接口模板路由。
     *
     * <p>例如实际请求为 {@code /agents/123} 时返回 {@code /agents/{id}}；
     * 请求在认证阶段被拦截或没有匹配到Controller时返回短横线。</p>
     *
     * @param request HTTP请求
     * @return 接口模板路由
     */
    private String resolveMatchedRoute(HttpServletRequest request) {
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return route == null ? "-" : safeValue(route.toString());
    }

    /** 生成或复用前端传入的请求ID。 */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return StringUtils.hasText(requestId) ? limit(requestId.replaceAll("[^a-zA-Z0-9_-]", ""), 80)
                : UUID.randomUUID().toString().replace("-", "");
    }

    /** 对查询参数中的敏感值进行脱敏。 */
    private String sanitizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.replaceAll("(?i)(password|token|api[_-]?key|secret|authorization)=([^&]*)", "$1=***");
    }

    /** 把异常文本追加到SLF4J参数数组。 */
    private Object[] append(Object[] source, Object value) {
        Object[] target = java.util.Arrays.copyOf(source, source.length + 1);
        target[source.length] = value;
        return target;
    }

    /** 将空值转换为短横线，方便检索日志。 */
    private String safeValue(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    /**
     * 填充当前登录用户信息。
     *
     * @param log 审计日志实体
     */
    private void fillUser(AuditOperationLogEntity log) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            log.setUserId(userDetails.getUser().getId());
            log.setUsername(userDetails.getUsername());
        } else if (authentication != null && StringUtils.hasText(authentication.getName())) {
            log.setUsername(authentication.getName());
        }
    }

    /**
     * 根据 HTTP 方法推导操作类型。
     *
     * @param method HTTP 方法
     * @param path 请求路径
     * @return 操作类型
     */
    private String resolveOperationType(String method, String path) {
        if (path != null && path.contains("/auth/login")) {
            return "login";
        }
        return switch (method == null ? "GET" : method.toUpperCase()) {
            case "POST" -> "create_or_action";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> "query";
        };
    }

    /**
     * 根据路径推导资源类型。
     *
     * @param path 请求路径
     * @return 资源类型
     */
    private String resolveResourceType(String path) {
        if (!StringUtils.hasText(path)) {
            return "unknown";
        }
        String normalized = path.replaceFirst("^/api/?", "");
        int slash = normalized.indexOf('/');
        return slash < 0 ? normalized : normalized.substring(0, slash);
    }

    /**
     * 获取客户端 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 限制文本长度，避免审计日志过大。
     *
     * @param text 原始文本
     * @param max 最大长度
     * @return 截断后的文本
     */
    private String limit(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
