package com.openagentflow.security;

import java.util.List;
import java.util.Locale;

/**
 * 工作空间资源类型与模块权限的映射策略。
 *
 * <p>查看动作允许命中查看或管理权限，管理动作只允许命中写权限，防止只读角色越权修改资源。</p>
 */
public final class ResourceModulePermissionPolicy {

    private ResourceModulePermissionPolicy() {
    }

    /**
     * 获取指定资源动作允许使用的权限编码。
     *
     * @param resourceType 资源类型
     * @param manage 是否为管理动作
     * @return 任意命中即可放行的权限编码
     */
    public static List<String> requiredPermissions(String resourceType, boolean manage) {
        String normalized = resourceType == null ? "" : resourceType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "agent" -> manage
                    ? List.of("agent:update", "agent:manage")
                    : List.of("agent:view", "agent:manage");
            case "knowledge_base", "knowledge" -> manage
                    ? List.of("knowledge:manage")
                    : List.of("knowledge:view", "knowledge:manage");
            case "tool" -> manage ? List.of("tool:manage") : List.of("tool:view", "tool:manage");
            case "workflow" -> manage
                    ? List.of("workflow:manage")
                    : List.of("workflow:view", "workflow:manage");
            case "mcp_server", "mcp" -> manage
                    ? List.of("mcp:manage")
                    : List.of("mcp:view", "mcp:manage");
            case "prompt" -> manage
                    ? List.of("prompt:manage")
                    : List.of("prompt:view", "prompt:manage");
            case "evaluation", "eval" -> manage
                    ? List.of("evaluation:manage", "eval:manage")
                    : List.of("evaluation:view", "evaluation:manage", "eval:manage");
            case "template" -> manage
                    ? List.of("template:manage", "template:publish")
                    : List.of("template:view", "template:manage", "template:publish");
            default -> manage
                    ? List.of("workspace:manage")
                    : List.of("workspace:view", "workspace:manage");
        };
    }
}
