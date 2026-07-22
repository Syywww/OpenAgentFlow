package com.openagentflow.mcp;

import com.openagentflow.exception.BusinessException;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** MCP stdio 可执行命令安全策略。 */
public class McpCommandPolicy {

    /** 允许启动的命令基本名。 */
    private final List<String> allowedCommands;

    public McpCommandPolicy(List<String> allowedCommands) {
        this.allowedCommands = allowedCommands == null ? List.of() : allowedCommands.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .toList();
    }

    /** 校验命令白名单并返回原命令。 */
    public String requireAllowed(String command) {
        String normalized = normalize(Path.of(command == null ? "" : command).getFileName().toString());
        boolean allowed = allowedCommands.stream().anyMatch(item -> item.equals(normalized));
        if (!allowed) {
            throw new BusinessException("MCP_STDIO_COMMAND_FORBIDDEN", "MCP stdio 命令不在安全白名单中：" + normalized);
        }
        return command;
    }

    private String normalize(String command) {
        String value = command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
        return value.endsWith(".exe") ? value.substring(0, value.length() - 4) : value;
    }
}

