package com.openagentflow.service;

import com.openagentflow.mcp.McpCommandPolicy;
import com.openagentflow.mcp.McpSseEventParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** MCP 原生传输基础策略测试。 */
class McpNativeTransportPolicyTests {

    /** SSE 解析器应保留事件类型、事件 ID 和多行数据。 */
    @Test
    void shouldParseSseEvent() {
        McpSseEventParser.Event event = McpSseEventParser.parse(List.of(
                "id: evt-1",
                "event: message",
                "data: {\"jsonrpc\":\"2.0\",",
                "data: \"id\":\"1\"}"
        ));

        assertThat(event.id()).isEqualTo("evt-1");
        assertThat(event.type()).isEqualTo("message");
        assertThat(event.data()).isEqualTo("{\"jsonrpc\":\"2.0\",\n\"id\":\"1\"}");
    }

    /** stdio 只能启动白名单中的可执行命令。 */
    @Test
    void shouldRejectCommandOutsideAllowList() {
        McpCommandPolicy policy = new McpCommandPolicy(List.of("node", "npx", "java"));
        assertThat(policy.requireAllowed("C:\\Program Files\\nodejs\\node.exe")).endsWith("node.exe");
        assertThatThrownBy(() -> policy.requireAllowed("powershell.exe"))
                .hasMessageContaining("白名单");
    }
}

