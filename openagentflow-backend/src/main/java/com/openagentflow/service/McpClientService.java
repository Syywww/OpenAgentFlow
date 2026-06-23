package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.domain.tool.ToolExecutionResult;
import com.openagentflow.entity.McpServerEntity;
import com.openagentflow.entity.ToolDefinitionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.McpServerMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * MCP 客户端服务。
 *
 * <p>当前真实打通 HTTP JSON-RPC 方式；stdio 与标准 SSE 会保留配置能力，后续可继续扩展进程启动和事件流会话。</p>
 */
@Service
public class McpClientService {

    /** MCP 初始化协议版本，便于服务端识别客户端能力。 */
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";

    /** HTTP 客户端，统一处理 MCP JSON-RPC 请求。 */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** MCP Server Mapper，用于 MCP 工具调用时反查连接配置。 */
    private final McpServerMapper mcpServerMapper;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    public McpClientService(McpServerMapper mcpServerMapper, ObjectMapper objectMapper) {
        this.mcpServerMapper = mcpServerMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 测试 MCP Server 连接并读取能力数量。
     *
     * @param server MCP Server 实体
     * @return 连接测试结果
     */
    public McpConnectionResult testConnection(McpServerEntity server) {
        Instant startedAt = Instant.now();
        McpConnectionResult result = new McpConnectionResult();
        try {
            McpDiscoveryPayload payload = discover(server);
            result.setSuccess(true);
            result.setToolsCount(payload.getTools().size());
            result.setPromptsCount(payload.getPrompts().size());
            result.setResourcesCount(payload.getResources().size());
            result.setResponsePayload(toJson(Map.of(
                    "toolsCount", payload.getTools().size(),
                    "promptsCount", payload.getPrompts().size(),
                    "resourcesCount", payload.getResources().size()
            )));
        } catch (Exception exception) {
            result.setSuccess(false);
            result.setToolsCount(0);
            result.setPromptsCount(0);
            result.setResourcesCount(0);
            result.setErrorMessage(exception.getMessage());
            result.setResponsePayload(toJson(Map.of("error", exception.getMessage())));
        }
        result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        return result;
    }

    /**
     * 发现 MCP Server 暴露的 tools、prompts、resources。
     *
     * @param server MCP Server 实体
     * @return 发现载荷
     */
    public McpDiscoveryPayload discover(McpServerEntity server) {
        assertHttpTransport(server);
        // 初始化请求可以提前暴露认证、地址或协议不兼容问题。
        JsonNode initializeResult = postJsonRpc(server, "initialize", initializeParams(), true);
        List<McpCapabilitySpec> tools = readCapabilities(optionalJsonRpc(server, "tools/list", objectMapper.createObjectNode()), "tools", "tool");
        List<McpCapabilitySpec> prompts = readCapabilities(optionalJsonRpc(server, "prompts/list", objectMapper.createObjectNode()), "prompts", "prompt");
        List<McpCapabilitySpec> resources = readCapabilities(optionalJsonRpc(server, "resources/list", objectMapper.createObjectNode()), "resources", "resource");

        McpDiscoveryPayload payload = new McpDiscoveryPayload();
        payload.setTools(tools);
        payload.setPrompts(prompts);
        payload.setResources(resources);
        payload.setRawPayload(toJson(Map.of(
                "initialize", initializeResult,
                "toolsCount", tools.size(),
                "promptsCount", prompts.size(),
                "resourcesCount", resources.size()
        )));
        return payload;
    }

    /**
     * 调用 MCP 工具。
     *
     * @param tool 工具中心中的 MCP 工具定义
     * @param inputParams 模型或调试台传入的参数
     * @return 工具执行结果
     */
    public ToolExecutionResult callTool(ToolDefinitionEntity tool, Map<String, Object> inputParams) {
        Instant startedAt = Instant.now();
        ToolExecutionResult result = new ToolExecutionResult();
        try {
            McpServerEntity server = requireServer(tool);
            String toolName = StringUtils.hasText(tool.getMcpToolName()) ? tool.getMcpToolName() : tool.getToolCode();
            ObjectNode params = objectMapper.createObjectNode();
            params.put("name", toolName);
            params.set("arguments", objectMapper.valueToTree(inputParams == null ? Map.of() : inputParams));
            JsonNode response = postJsonRpc(server, "tools/call", params, true);
            result.setSuccess(true);
            result.setStatusCode(200);
            result.setResponseBody(toJson(response));
        } catch (Exception exception) {
            result.setSuccess(false);
            result.setStatusCode(0);
            result.setErrorMessage(exception.getMessage());
            result.setResponseBody(toJson(Map.of("error", exception.getMessage())));
        }
        result.setConfirmationRequired(false);
        result.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        return result;
    }

    /**
     * 发送 JSON-RPC 请求。
     *
     * @param server MCP Server 实体
     * @param method JSON-RPC 方法名
     * @param params JSON-RPC 参数
     * @param failOnError 是否在 JSON-RPC error 时抛出异常
     * @return result 节点
     */
    private JsonNode postJsonRpc(McpServerEntity server, String method, JsonNode params, boolean failOnError) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("id", UUID.randomUUID().toString());
            request.put("method", method);
            request.set("params", params == null ? objectMapper.createObjectNode() : params);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(server.getEndpointUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(toJson(request), StandardCharsets.UTF_8));
            appendAuthHeaders(builder, server);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("MCP_HTTP_ERROR", "MCP HTTP 状态码异常：" + response.statusCode() + "，响应：" + response.body());
            }
            JsonNode body = objectMapper.readTree(response.body());
            if (body.hasNonNull("error") && failOnError) {
                throw new BusinessException("MCP_RPC_ERROR", "MCP JSON-RPC 错误：" + body.get("error").toString());
            }
            return body.has("result") ? body.get("result") : objectMapper.createObjectNode();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("MCP_REQUEST_FAILED", "MCP 请求失败：" + exception.getMessage());
        }
    }

    /**
     * 发送可选 JSON-RPC 请求，服务端未实现 prompts/resources 时返回空对象。
     *
     * @param server MCP Server 实体
     * @param method JSON-RPC 方法名
     * @param params 参数
     * @return result 节点
     */
    private JsonNode optionalJsonRpc(McpServerEntity server, String method, JsonNode params) {
        try {
            return postJsonRpc(server, method, params, false);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    /**
     * 读取 MCP 能力列表。
     *
     * @param result JSON-RPC result
     * @param arrayName 数组字段名
     * @param capabilityType 能力类型
     * @return 能力规范列表
     */
    private List<McpCapabilitySpec> readCapabilities(JsonNode result, String arrayName, String capabilityType) {
        List<McpCapabilitySpec> specs = new ArrayList<>();
        JsonNode array = result == null ? null : result.get(arrayName);
        if (array == null || !array.isArray()) {
            return specs;
        }
        for (JsonNode item : array) {
            String name = text(item, "name", text(item, "uri", ""));
            if (!StringUtils.hasText(name)) {
                continue;
            }
            McpCapabilitySpec spec = new McpCapabilitySpec();
            spec.setType(capabilityType);
            spec.setName(name);
            spec.setDescription(text(item, "description", ""));
            spec.setSchemaJson(toJson(firstNonNull(item.get("inputSchema"), item.get("input_schema"), item.get("schema"), objectMapper.createObjectNode())));
            spec.setMetadata(toJson(item));
            spec.setRiskLevel(inferRisk(name, spec.getDescription()));
            specs.add(spec);
        }
        return specs;
    }

    /**
     * 构造 MCP initialize 请求参数。
     *
     * @return initialize 参数节点
     */
    private ObjectNode initializeParams() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", MCP_PROTOCOL_VERSION);
        params.set("capabilities", objectMapper.createObjectNode());
        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "OpenAgentFlow-Java");
        clientInfo.put("version", "0.1.0");
        params.set("clientInfo", clientInfo);
        return params;
    }

    /**
     * 校验当前服务是否可走 HTTP JSON-RPC。
     *
     * @param server MCP Server 实体
     */
    private void assertHttpTransport(McpServerEntity server) {
        if (server == null || server.getDeletedAt() != null) {
            throw new BusinessException("MCP_SERVER_NOT_FOUND", "MCP Server 不存在");
        }
        String transport = safeText(server.getTransportType()).toLowerCase(Locale.ROOT);
        if (!List.of("http", "sse").contains(transport)) {
            throw new BusinessException("MCP_TRANSPORT_UNSUPPORTED", "当前已真实打通 HTTP JSON-RPC；stdio 传输仍保留配置，暂未启动进程调用");
        }
        if (!StringUtils.hasText(server.getEndpointUrl())) {
            throw new BusinessException("MCP_ENDPOINT_EMPTY", "HTTP/SSE MCP Server 需要配置端点 URL");
        }
    }

    /**
     * 根据工具反查可用 MCP Server。
     *
     * @param tool MCP 工具定义
     * @return MCP Server 实体
     */
    private McpServerEntity requireServer(ToolDefinitionEntity tool) {
        if (tool == null || !StringUtils.hasText(tool.getMcpServerId())) {
            throw new BusinessException("MCP_TOOL_INVALID", "MCP 工具未绑定 Server");
        }
        McpServerEntity server = mcpServerMapper.selectById(tool.getMcpServerId());
        assertHttpTransport(server);
        if (!"running".equalsIgnoreCase(safeText(server.getStatus()))) {
            throw new BusinessException("MCP_SERVER_STOPPED", "MCP Server 未处于运行中，请先连接测试或重新发现");
        }
        return server;
    }

    /**
     * 写入 MCP 请求认证头。
     *
     * @param builder HTTP 请求构造器
     * @param server MCP Server 实体
     */
    private void appendAuthHeaders(HttpRequest.Builder builder, McpServerEntity server) {
        builder.header("Content-Type", "application/json");
        builder.header("Accept", "application/json");
        Map<String, Object> auth = parseMap(server.getAuthConfig());
        String authType = safeText(server.getAuthType()).toLowerCase(Locale.ROOT);
        if ("bearer".equals(authType)) {
            Object token = auth.getOrDefault("token", auth.get("bearerToken"));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
        } else if ("api_key".equals(authType)) {
            String headerName = String.valueOf(auth.getOrDefault("headerName", "X-API-Key"));
            Object value = auth.getOrDefault("apiKey", auth.get("apiKeyValue"));
            if (value != null) {
                builder.header(headerName, String.valueOf(value));
            }
        } else if ("basic".equals(authType)) {
            String user = String.valueOf(auth.getOrDefault("username", ""));
            String password = String.valueOf(auth.getOrDefault("password", ""));
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * 判断 MCP 能力风险等级。
     *
     * @param name 能力名称
     * @param description 能力描述
     * @return 风险等级
     */
    private String inferRisk(String name, String description) {
        String text = (safeText(name) + " " + safeText(description)).toLowerCase(Locale.ROOT);
        if (text.matches(".*(delete|remove|write|move|exec|command|shell|process|filesystem|file|network|http|fetch|删除|写入|命令|执行|网络|文件).*")) {
            return "high";
        }
        if (text.matches(".*(update|create|send|post|webhook|修改|创建|发送).*")) {
            return "medium";
        }
        return "low";
    }

    /**
     * 读取 JSON 文本字段。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @param fallback 默认值
     * @return 字段文本
     */
    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asText(fallback);
    }

    /**
     * 返回第一个非空 JSON 节点。
     *
     * @param nodes 候选节点
     * @return 非空节点
     */
    private JsonNode firstNonNull(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return objectMapper.createObjectNode();
    }

    /**
     * 解析 JSON Map。
     *
     * @param json JSON 字符串
     * @return Map
     */
    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 转换 JSON 字符串。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 安全文本。
     *
     * @param text 原始文本
     * @return 非空文本
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * MCP 连接测试结果内部对象。
     */
    public static class McpConnectionResult {

        /** 是否连接成功。 */
        private Boolean success;

        /** 耗时毫秒。 */
        private Integer latencyMs;

        /** 工具数量。 */
        private Integer toolsCount;

        /** Prompt 数量。 */
        private Integer promptsCount;

        /** Resource 数量。 */
        private Integer resourcesCount;

        /** 响应载荷。 */
        private String responsePayload;

        /** 错误信息。 */
        private String errorMessage;

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
    }

    /**
     * MCP 能力发现载荷。
     */
    public static class McpDiscoveryPayload {

        /** 工具能力。 */
        private List<McpCapabilitySpec> tools = List.of();

        /** Prompt 能力。 */
        private List<McpCapabilitySpec> prompts = List.of();

        /** Resource 能力。 */
        private List<McpCapabilitySpec> resources = List.of();

        /** 原始响应摘要。 */
        private String rawPayload;

        public List<McpCapabilitySpec> getTools() {
            return tools;
        }

        public void setTools(List<McpCapabilitySpec> tools) {
            this.tools = tools;
        }

        public List<McpCapabilitySpec> getPrompts() {
            return prompts;
        }

        public void setPrompts(List<McpCapabilitySpec> prompts) {
            this.prompts = prompts;
        }

        public List<McpCapabilitySpec> getResources() {
            return resources;
        }

        public void setResources(List<McpCapabilitySpec> resources) {
            this.resources = resources;
        }

        public String getRawPayload() {
            return rawPayload;
        }

        public void setRawPayload(String rawPayload) {
            this.rawPayload = rawPayload;
        }
    }

    /**
     * MCP 单个能力规范。
     */
    public static class McpCapabilitySpec {

        /** 能力类型。 */
        private String type;

        /** 能力名称。 */
        private String name;

        /** 能力描述。 */
        private String description;

        /** Schema JSON。 */
        private String schemaJson;

        /** 原始元数据 JSON。 */
        private String metadata;

        /** 风险等级。 */
        private String riskLevel;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }
    }
}
