package com.openagentflow.domain.mcp;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MCP 模块接口 DTO 集合。
 *
 * <p>统一放在一个文件中，避免 MCP 管理接口早期阶段生成过多零散对象。</p>
 */
public final class McpDtos {

    private McpDtos() {
    }

    /**
     * MCP Server 创建或更新请求。
     */
    public static class ServerRequest {

        /** 服务编码，作为平台内唯一标识。 */
        private String serverCode;

        /** 服务名称，用于前端展示。 */
        @NotBlank(message = "MCP Server 名称不能为空")
        private String serverName;

        /** 服务描述，帮助管理员识别用途。 */
        private String description;

        /** 传输类型：http、streamable_http、sse 或 stdio。 */
        @NotBlank(message = "MCP 传输类型不能为空")
        private String transportType;

        /** stdio 模式启动命令，必须位于服务端安全白名单。 */
        private String command;

        /** stdio 模式参数 JSON 数组。 */
        private String args;

        /** HTTP JSON-RPC 端点 URL。 */
        private String endpointUrl;

        /** 认证方式：none、bearer、api_key、basic。 */
        private String authType;

        /** 认证配置 JSON，不在工具中心重复明文展开。 */
        private String authConfig;

        /** stdio 子进程环境变量 JSON。 */
        private String envVars;

        /** 允许访问路径 JSON 数组，供文件系统类 MCP 安全限制使用。 */
        private String allowedPaths;

        /** 风险策略 JSON，记录禁用或确认策略。 */
        private String riskPolicy;

        /** Server 状态：stopped、running、error、deleted。 */
        private String status;

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTransportType() {
            return transportType;
        }

        public void setTransportType(String transportType) {
            this.transportType = transportType;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }

        public String getEndpointUrl() {
            return endpointUrl;
        }

        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public String getAuthConfig() {
            return authConfig;
        }

        public void setAuthConfig(String authConfig) {
            this.authConfig = authConfig;
        }

        public String getEnvVars() {
            return envVars;
        }

        public void setEnvVars(String envVars) {
            this.envVars = envVars;
        }

        public String getAllowedPaths() {
            return allowedPaths;
        }

        public void setAllowedPaths(String allowedPaths) {
            this.allowedPaths = allowedPaths;
        }

        public String getRiskPolicy() {
            return riskPolicy;
        }

        public void setRiskPolicy(String riskPolicy) {
            this.riskPolicy = riskPolicy;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * MCP Server 摘要。
     */
    public static class ServerSummary {

        /** MCP Server 主键 ID。 */
        private String id;

        /** 服务编码。 */
        private String serverCode;

        /** 服务名称。 */
        private String serverName;

        /** 服务描述。 */
        private String description;

        /** 传输类型。 */
        private String transportType;

        /** 命令文本。 */
        private String command;

        /** HTTP JSON-RPC 端点。 */
        private String endpointUrl;

        /** 认证类型。 */
        private String authType;

        /** 状态。 */
        private String status;

        /** 最近心跳或连接成功时间。 */
        private LocalDateTime lastHeartbeatAt;

        /** 已发现工具数量。 */
        private Integer toolsCount;

        /** 已发现 Prompt 数量。 */
        private Integer promptsCount;

        /** 已发现 Resource 数量。 */
        private Integer resourcesCount;

        /** 当前用户是否可管理。 */
        private Boolean canManage;

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTransportType() {
            return transportType;
        }

        public void setTransportType(String transportType) {
            this.transportType = transportType;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getEndpointUrl() {
            return endpointUrl;
        }

        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
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

        public Integer getToolsCount() {
            return toolsCount;
        }

        public void setToolsCount(Integer toolsCount) {
            this.toolsCount = toolsCount;
        }

        public Integer getPromptsCount() {
            return promptsCount;
        }

        public void setPromptsCount(Integer promptsCount) {
            this.promptsCount = promptsCount;
        }

        public Integer getResourcesCount() {
            return resourcesCount;
        }

        public void setResourcesCount(Integer resourcesCount) {
            this.resourcesCount = resourcesCount;
        }

        public Boolean getCanManage() {
            return canManage;
        }

        public void setCanManage(Boolean canManage) {
            this.canManage = canManage;
        }
    }

    /**
     * MCP Server 详情。
     */
    public static class ServerDetail extends ServerSummary {

        /** stdio 参数 JSON 数组。 */
        private String args;

        /** 认证配置 JSON。 */
        private String authConfig;

        /** 环境变量 JSON。 */
        private String envVars;

        /** 允许路径 JSON。 */
        private String allowedPaths;

        /** 风险策略 JSON。 */
        private String riskPolicy;

        /** 已发现能力列表。 */
        private List<CapabilitySummary> capabilities;

        /** 最近一次连接测试。 */
        private ConnectionTestResult lastTest;

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }

        public String getAuthConfig() {
            return authConfig;
        }

        public void setAuthConfig(String authConfig) {
            this.authConfig = authConfig;
        }

        public String getEnvVars() {
            return envVars;
        }

        public void setEnvVars(String envVars) {
            this.envVars = envVars;
        }

        public String getAllowedPaths() {
            return allowedPaths;
        }

        public void setAllowedPaths(String allowedPaths) {
            this.allowedPaths = allowedPaths;
        }

        public String getRiskPolicy() {
            return riskPolicy;
        }

        public void setRiskPolicy(String riskPolicy) {
            this.riskPolicy = riskPolicy;
        }

        public List<CapabilitySummary> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(List<CapabilitySummary> capabilities) {
            this.capabilities = capabilities;
        }

        public ConnectionTestResult getLastTest() {
            return lastTest;
        }

        public void setLastTest(ConnectionTestResult lastTest) {
            this.lastTest = lastTest;
        }
    }

    /**
     * MCP 能力摘要。
     */
    public static class CapabilitySummary {

        /** 能力主键 ID。 */
        private String id;

        /** 归属 Server ID。 */
        private String serverId;

        /** 能力类型：tool、prompt、resource。 */
        private String capabilityType;

        /** 能力名称。 */
        private String capabilityName;

        /** 能力描述。 */
        private String description;

        /** 输入或访问 Schema JSON。 */
        private String schemaJson;

        /** MCP 原始元数据 JSON。 */
        private String metadata;

        /** 是否启用。 */
        private Boolean enabled;

        /** 风险等级。 */
        private String riskLevel;

        /** 风险中文标签。 */
        private String riskLabel;

        /** 发现时间。 */
        private LocalDateTime discoveredAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getServerId() {
            return serverId;
        }

        public void setServerId(String serverId) {
            this.serverId = serverId;
        }

        public String getCapabilityType() {
            return capabilityType;
        }

        public void setCapabilityType(String capabilityType) {
            this.capabilityType = capabilityType;
        }

        public String getCapabilityName() {
            return capabilityName;
        }

        public void setCapabilityName(String capabilityName) {
            this.capabilityName = capabilityName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSchemaJson() {
            return schemaJson;
        }

        public void setSchemaJson(String schemaJson) {
            this.schemaJson = schemaJson;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getRiskLabel() {
            return riskLabel;
        }

        public void setRiskLabel(String riskLabel) {
            this.riskLabel = riskLabel;
        }

        public LocalDateTime getDiscoveredAt() {
            return discoveredAt;
        }

        public void setDiscoveredAt(LocalDateTime discoveredAt) {
            this.discoveredAt = discoveredAt;
        }
    }

    /**
     * MCP 连接测试结果。
     */
    public static class ConnectionTestResult {

        /** 是否连接成功。 */
        private Boolean success;

        /** 连接耗时毫秒。 */
        private Integer latencyMs;

        /** 工具数量。 */
        private Integer toolsCount;

        /** Prompt 数量。 */
        private Integer promptsCount;

        /** Resource 数量。 */
        private Integer resourcesCount;

        /** 脱敏后的响应载荷 JSON。 */
        private String responsePayload;

        /** 错误信息。 */
        private String errorMessage;

        /** 测试时间。 */
        private LocalDateTime createdAt;

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Integer getToolsCount() {
            return toolsCount;
        }

        public void setToolsCount(Integer toolsCount) {
            this.toolsCount = toolsCount;
        }

        public Integer getPromptsCount() {
            return promptsCount;
        }

        public void setPromptsCount(Integer promptsCount) {
            this.promptsCount = promptsCount;
        }

        public Integer getResourcesCount() {
            return resourcesCount;
        }

        public void setResourcesCount(Integer resourcesCount) {
            this.resourcesCount = resourcesCount;
        }

        public String getResponsePayload() {
            return responsePayload;
        }

        public void setResponsePayload(String responsePayload) {
            this.responsePayload = responsePayload;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * MCP 发现结果。
     */
    public static class DiscoveryResult {

        /** 发现任务 ID。 */
        private String taskId;

        /** 任务状态。 */
        private String status;

        /** 工具数量。 */
        private Integer toolsCount;

        /** Prompt 数量。 */
        private Integer promptsCount;

        /** Resource 数量。 */
        private Integer resourcesCount;

        /** 同步后的能力列表。 */
        private List<CapabilitySummary> capabilities;

        /** 错误信息。 */
        private String errorMessage;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getToolsCount() {
            return toolsCount;
        }

        public void setToolsCount(Integer toolsCount) {
            this.toolsCount = toolsCount;
        }

        public Integer getPromptsCount() {
            return promptsCount;
        }

        public void setPromptsCount(Integer promptsCount) {
            this.promptsCount = promptsCount;
        }

        public Integer getResourcesCount() {
            return resourcesCount;
        }

        public void setResourcesCount(Integer resourcesCount) {
            this.resourcesCount = resourcesCount;
        }

        public List<CapabilitySummary> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(List<CapabilitySummary> capabilities) {
            this.capabilities = capabilities;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
