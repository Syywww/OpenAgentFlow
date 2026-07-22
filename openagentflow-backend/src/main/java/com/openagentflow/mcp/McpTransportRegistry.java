package com.openagentflow.mcp;

import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** MCP 原生传输注册中心。 */
@Component
public class McpTransportRegistry {

    /** Spring 注入的全部原生传输实现。 */
    private final List<McpTransport> transports;

    public McpTransportRegistry(List<McpTransport> transports) {
        this.transports = transports == null ? List.of() : List.copyOf(transports);
    }

    /** 根据服务配置选择原生传输实现。 */
    public McpTransport require(McpServerEntity server) {
        if (server == null || server.getDeletedAt() != null) {
            throw new BusinessException("MCP_SERVER_NOT_FOUND", "MCP Server 不存在");
        }
        String type = server.getTransportType() == null ? "" : server.getTransportType().trim().toLowerCase(Locale.ROOT);
        return transports.stream()
                .filter(transport -> transport.supports(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException("MCP_TRANSPORT_UNSUPPORTED", "不支持的 MCP 传输类型：" + type));
    }
}
