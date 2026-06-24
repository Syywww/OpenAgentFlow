package com.openagentflow.security;

import com.openagentflow.entity.AuditOperationLogEntity;
import com.openagentflow.mapper.AuditOperationLogMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

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

    /** 审计操作日志 Mapper。 */
    private final AuditOperationLogMapper auditOperationLogMapper;

    public AuditOperationFilter(AuditOperationLogMapper auditOperationLogMapper) {
        this.auditOperationLogMapper = auditOperationLogMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant startedAt = Instant.now();
        Exception failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            writeAuditLog(request, response, startedAt, failure);
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
                               Exception failure) {
        try {
            AuditOperationLogEntity log = new AuditOperationLogEntity();
            log.setId(UUID.randomUUID().toString());
            log.setTraceId(request.getHeader("X-Request-Id"));
            fillUser(log);
            log.setOperationType(resolveOperationType(request.getMethod(), request.getRequestURI()));
            log.setResourceType(resolveResourceType(request.getRequestURI()));
            log.setRequestMethod(request.getMethod());
            log.setRequestPath(request.getRequestURI());
            log.setRequestParams(limit(request.getQueryString(), 2000));
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
