package com.openagentflow.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * API 模块与权限编码映射策略。
 *
 * <p>数据库路由权限优先于此策略；本策略为所有核心模块提供默认拒绝所需的稳定兜底映射。</p>
 */
public final class ApiAuthorizationPolicy {

    /** 核心模块权限规则，顺序从更具体的路径到更通用的路径。 */
    private static final List<RouteRule> RULES = List.of(
            new RouteRule("/iam-admin", Set.of("iam:manage"), Set.of("iam:manage")),
            new RouteRule("/agent-teams", Set.of("agent-team:view", "agent-team:manage"), Set.of("agent-team:manage")),
            new RouteRule("/agents", Set.of("agent:view", "agent:manage"), Set.of("agent:create", "agent:update", "agent:manage")),
            new RouteRule("/chat", Set.of("debug:use", "agent:run"), Set.of("debug:use", "agent:run")),
            new RouteRule("/knowledge-governance", Set.of("knowledge:governance:view", "knowledge:governance:manage"), Set.of("knowledge:governance:manage")),
            new RouteRule("/knowledge-bases", Set.of("knowledge:view", "knowledge:manage", "knowledge:retrieve"), Set.of("knowledge:manage")),
            new RouteRule("/tools", Set.of("tool:view", "tool:manage"), Set.of("tool:manage")),
            new RouteRule("/mcp-servers", Set.of("mcp:view", "mcp:manage"), Set.of("mcp:manage")),
            new RouteRule("/workflows", Set.of("workflow:view", "workflow:manage"), Set.of("workflow:manage")),
            new RouteRule("/workflow-api", Set.of("workflow:api:invoke", "workflow:manage"), Set.of("workflow:api:invoke", "workflow:manage")),
            new RouteRule("/runs", Set.of("trace:view", "trace:manage", "runtime:manage"), Set.of("trace:manage", "runtime:manage")),
            new RouteRule("/usage", Set.of("usage:view", "usage:export", "usage:quota:manage"), Set.of("usage:quota:manage")),
            new RouteRule("/ops-monitor", Set.of("ops:monitor:view", "ops:monitor:manage"), Set.of("ops:monitor:manage")),
            new RouteRule("/notifications", Set.of("notification:view", "notification:manage"), Set.of("notification:manage")),
            new RouteRule("/delivery-acceptance", Set.of("delivery:acceptance:view", "delivery:acceptance:manage"), Set.of("delivery:acceptance:manage")),
            new RouteRule("/model-gateway", Set.of("model-gateway:view", "model-gateway:manage", "model:manage"), Set.of("model-gateway:manage", "model:manage")),
            new RouteRule("/model-providers", Set.of("model:manage"), Set.of("model:manage")),
            new RouteRule("/workspaces", Set.of("workspace:view", "workspace:manage"), Set.of("workspace:manage")),
            new RouteRule("/organizations", Set.of("workspace:view", "workspace:manage"), Set.of("workspace:manage")),
            new RouteRule("/tasks", Set.of("async-task:view", "async-task:manage"), Set.of("async-task:manage")),
            new RouteRule("/prompt-templates", Set.of("prompt:view", "prompt:manage"), Set.of("prompt:manage")),
            new RouteRule("/memories", Set.of("memory:view", "memory:recall", "memory:manage"), Set.of("memory:manage")),
            new RouteRule("/evaluations", Set.of("evaluation:view", "evaluation:manage", "eval:manage"), Set.of("evaluation:manage", "eval:manage")),
            new RouteRule("/templates", Set.of("template:view", "template:manage"), Set.of("template:publish", "template:operate", "template:manage")),
            new RouteRule("/governance", Set.of("governance:view", "governance:manage"), Set.of("governance:manage")),
            new RouteRule("/dashboard", Set.of("dashboard:view"), Set.of("dashboard:view")),
            new RouteRule("/vector-store", Set.of("knowledge:view", "knowledge:manage"), Set.of("knowledge:manage")),
            new RouteRule("/tenant-isolation", Set.of("ROLE_super_admin"), Set.of("ROLE_super_admin")),
            new RouteRule("/sre", Set.of("ops:view", "ops:monitor:view"), Set.of("ops:manage", "ops:monitor:manage")),
            new RouteRule("/production-readiness", Set.of("ops:manage"), Set.of("ops:manage")),
            new RouteRule("/release-gates", Set.of("risk:manage", "evaluation:manage"), Set.of("risk:manage", "evaluation:manage")),
            new RouteRule("/compliance", Set.of("governance:view", "governance:manage"), Set.of("governance:manage")),
            new RouteRule("/ops", Set.of("ops:monitor:view", "ops:monitor:manage"), Set.of("ops:monitor:manage"))
    );

    private ApiAuthorizationPolicy() {
    }

    /**
     * 根据请求返回任意命中即可放行的权限编码。
     *
     * @param method HTTP 方法
     * @param path 请求路径
     * @return 允许权限集合，空集合表示没有内置规则
     */
    public static Set<String> requiredAuthorities(String method, String path) {
        if (path == null || "OPTIONS".equalsIgnoreCase(method)) {
            return Set.of();
        }
        String normalized = normalize(path);
        RouteRule rule = RULES.stream()
                .filter(item -> normalized.equals(item.pathPrefix()) || normalized.startsWith(item.pathPrefix() + "/"))
                .findFirst()
                .orElse(null);
        if (rule == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        if (isReadMethod(method)) {
            result.addAll(rule.readAuthorities());
        } else {
            result.addAll(rule.writeAuthorities());
        }
        // 对运行类接口优先开放专用运行权限，同时保留模块管理权限。
        if (normalized.matches(".*/(run|execute|debug|test)(/.*)?$")) {
            if (normalized.startsWith("/agents/") || normalized.startsWith("/chat")) {
                result.add("agent:run");
            } else if (normalized.startsWith("/agent-teams/")) {
                result.add("agent-team:run");
            } else if (normalized.startsWith("/workflows/")) {
                result.add("workflow:run");
            }
        }
        return Set.copyOf(result);
    }

    /** 判断是否为只读 HTTP 方法。 */
    private static boolean isReadMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    /** 标准化请求路径。 */
    private static String normalize(String path) {
        String value = path.trim().toLowerCase(Locale.ROOT);
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /** 模块路由的查看权限和写权限。 */
    private record RouteRule(String pathPrefix, Set<String> readAuthorities, Set<String> writeAuthorities) {
    }
}
