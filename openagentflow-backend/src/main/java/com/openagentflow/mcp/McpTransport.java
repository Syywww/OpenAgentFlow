package com.openagentflow.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.entity.McpServerEntity;

import java.time.Duration;

/** MCP JSON-RPC 原生传输接口。 */
public interface McpTransport {

    /** 判断当前实现是否支持指定传输类型。 */
    boolean supports(String transportType);

    /** 发送需要响应的 JSON-RPC 请求并返回完整响应对象。 */
    JsonNode request(McpServerEntity server, ObjectNode payload, Duration timeout);

    /** 发送不需要响应的 JSON-RPC 通知。 */
    void notify(McpServerEntity server, ObjectNode payload, Duration timeout);

    /** 关闭指定服务端对应的本地会话资源。 */
    default void close(String serverId) {
    }
}
