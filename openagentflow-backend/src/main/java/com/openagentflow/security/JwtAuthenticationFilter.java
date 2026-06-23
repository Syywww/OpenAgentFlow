package com.openagentflow.security;

import com.openagentflow.config.OpenAgentFlowProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>该过滤器位于 Spring Security 过滤链中，负责把请求头里的 Bearer Token 转换为认证上下文。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** OpenAgentFlow 自定义配置。 */
    private final OpenAgentFlowProperties properties;

    /** JWT 服务。 */
    private final JwtTokenService jwtTokenService;

    /** Redis token 状态服务。 */
    private final RedisTokenService redisTokenService;

    /** 用户加载服务。 */
    private final AuthUserDetailsService authUserDetailsService;

    public JwtAuthenticationFilter(OpenAgentFlowProperties properties,
                                   JwtTokenService jwtTokenService,
                                   RedisTokenService redisTokenService,
                                   AuthUserDetailsService authUserDetailsService) {
        this.properties = properties;
        this.jwtTokenService = jwtTokenService;
        this.redisTokenService = redisTokenService;
        this.authUserDetailsService = authUserDetailsService;
    }

    /**
     * 执行 JWT 认证。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param filterChain 后续过滤链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!Boolean.TRUE.equals(properties.getSecurity().getAuthEnabled())) {
            // 开发环境关闭鉴权时直接放行，保留配置开关方便排查问题。
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtTokenPayload payload = jwtTokenService.parseAndValidate(token);
                if (redisTokenService.isTokenValid(payload.getTokenId(), payload.getUserId())) {
                    UserDetails userDetails = authUserDetailsService.loadUserByUsername(payload.getUsername());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // Redis 校验通过后写入 SecurityContext，后续 Controller 可获取当前用户。
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // token 无效时保持未认证状态，最终由 Spring Security 入口统一返回 401。
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头解析 Bearer Token。
     *
     * @param request 当前请求
     * @return JWT 字符串
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
