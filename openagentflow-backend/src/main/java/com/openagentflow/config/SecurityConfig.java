package com.openagentflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.ApiResponse;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.security.JwtAuthenticationFilter;
import com.openagentflow.security.WorkspaceIsolationFilter;
import com.openagentflow.security.AuditOperationFilter;
import com.openagentflow.security.ApiPermissionAuthorizationManager;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** OpenAgentFlow 自定义配置。 */
    private final OpenAgentFlowProperties properties;

    public SecurityConfig(OpenAgentFlowProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 Spring Security 过滤链。
     *
     * @param http HTTP 安全配置对象
     * @param jwtAuthenticationFilter JWT 认证过滤器
     * @param objectMapper JSON 序列化工具
     * @return 安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   WorkspaceIsolationFilter workspaceIsolationFilter,
                                                   AuditOperationFilter auditOperationFilter,
                                                   ApiPermissionAuthorizationManager apiPermissionAuthorizationManager,
                                                   ObjectMapper objectMapper) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.disable())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // SSE 异步请求完成后如果进入容器错误分发，允许 ERROR dispatcher 直接通过。
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        // 登录、Swagger 和健康检查允许匿名访问。
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/captcha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // SSE 异步完成后容器可能内部转发到 /error，放行可避免响应已提交后再次触发 403。
                        .requestMatchers("/error").permitAll()
                        // 业务接口统一进入路由权限管理器，避免只登录即可访问未标注接口。
                        .anyRequest().access(apiPermissionAuthorizationManager)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (response.isCommitted()) {
                                return;
                            }
                            response.setStatus(401);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.fail("UNAUTHORIZED", "请先登录")
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (response.isCommitted()) {
                                return;
                            }
                            response.setStatus(403);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.fail("FORBIDDEN", "没有访问权限")
                            ));
                        })
                )
                .headers(headers -> headers
                        // 生产部署默认禁止被 iframe 嵌入，降低点击劫持风险。
                        .frameOptions(frame -> frame.sameOrigin())
                        // 禁止浏览器进行 MIME 嗅探，避免脚本内容被错误执行。
                        .contentTypeOptions(contentType -> {
                        })
                        // API 响应不暴露跨域策略之外的来源，前端静态资源安全头由 Nginx 处理。
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )
                // JWT 过滤器放在用户名密码过滤器之前，确保业务接口先尝试解析 token。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 工作空间过滤器必须在JWT认证之后执行，才能校验当前用户成员关系。
                .addFilterAfter(workspaceIsolationFilter, JwtAuthenticationFilter.class)
                // 接口访问日志放在认证和工作空间校验之后，可输出可信用户与空间信息。
                .addFilterAfter(auditOperationFilter, WorkspaceIsolationFilter.class)
                // 明确禁用默认 logout 端点，退出登录由 AuthController 处理 Redis token 删除。
                .logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/security-disabled-logout")));
        return http.build();
    }

    /** 禁止Servlet容器重复注册工作空间过滤器，仅由Spring Security过滤链管理顺序。 */
    @Bean
    public FilterRegistrationBean<WorkspaceIsolationFilter> workspaceIsolationFilterRegistration(WorkspaceIsolationFilter filter) {
        FilterRegistrationBean<WorkspaceIsolationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** 禁止Servlet容器重复注册审计过滤器，仅由Spring Security过滤链管理顺序。 */
    @Bean
    public FilterRegistrationBean<AuditOperationFilter> auditOperationFilterRegistration(AuditOperationFilter filter) {
        FilterRegistrationBean<AuditOperationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 密码编码器。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器。
     *
     * @param userDetailsService 用户加载服务
     * @param passwordEncoder 密码编码器
     * @return 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // 使用 ProviderManager 组装 DAO 认证提供者，登录接口通过它完成用户名密码认证。
        return new ProviderManager(provider);
    }

    /**
     * CORS 跨域配置。
     *
     * @return CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许来源由配置控制，生产环境必须通过 OAF_CORS_ALLOWED_ORIGINS 收敛到正式域名。
        configuration.setAllowedOriginPatterns(allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 读取 CORS 允许来源。
     *
     * @return 允许来源列表
     */
    private List<String> allowedOrigins() {
        String origins = properties.getSecurity().getAllowedOrigins();
        if (origins == null || origins.isBlank()) {
            return List.of("http://localhost:*", "http://127.0.0.1:*");
        }
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
