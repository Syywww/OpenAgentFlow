package com.openagentflow.domain.tool;

import java.time.LocalDateTime;

/**
 * MCP 服务摘要对象。
 */
public class McpServerSummary {

    /** MCP 服务主键ID。 */
    private String id;

    /** MCP 服务编码。 */
    private String serverCode;

    /** MCP 服务名称。 */
    private String serverName;

    /** 传输类型，例如 stdio、sse、http。 */
    private String transportType;

    /** 运行状态。 */
    private String status;

    /** 最后心跳时间。 */
    private LocalDateTime lastHeartbeatAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServerCode() {
        return serverCode;
    }

    public void setServerCode(String serverCode) {
        this.serverCode = serverCode;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }
}
