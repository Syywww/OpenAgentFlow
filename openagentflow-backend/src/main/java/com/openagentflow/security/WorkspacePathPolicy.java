package com.openagentflow.security;

import java.util.List;
import java.util.Locale;

/**
 * 工作空间资源路径分类策略。
 */
public final class WorkspacePathPolicy {

    /** 包含租户数据、必须绑定工作空间上下文的路径前缀。 */
    private static final List<String> WORKSPACE_PATH_PREFIXES = List.of(
            "/agents", "/agent-teams", "/chat", "/knowledge-bases", "/knowledge-governance",
            "/tools", "/mcp-servers", "/workflows", "/tasks", "/memories", "/evaluations",
            "/prompt-templates", "/templates", "/runs", "/usage", "/notifications", "/workflow-api",
            "/model-gateway", "/ops-monitor", "/delivery-acceptance", "/release-gates",
            "/iam-admin/governance", "/iam-admin/resource-acls"
    );

    private WorkspacePathPolicy() {
    }

    /**
     * 判断请求是否必须携带工作空间上下文。
     *
     * @param method HTTP 方法
     * @param path 请求路径
     * @return 是否必须携带空间上下文
     */
    public static boolean requiresWorkspace(String method, String path) {
        if ("OPTIONS".equalsIgnoreCase(method) || path == null) {
            return false;
        }
        String normalized = normalize(path);
        return WORKSPACE_PATH_PREFIXES.stream()
                .anyMatch(prefix -> normalized.equals(prefix) || normalized.startsWith(prefix + "/"));
    }

    /** 去除尾部斜杠并统一为小写，保证路径匹配稳定。 */
    private static String normalize(String path) {
        String value = path.trim().toLowerCase(Locale.ROOT);
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
