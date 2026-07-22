package com.openagentflow.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.entity.McpServerEntity;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** MCP HTTP 认证请求头辅助器。 */
public class McpHttpHeaderSupport {

    /** JSON 工具，用于解析认证配置。 */
    private final ObjectMapper objectMapper;

    public McpHttpHeaderSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 写入通用协议头和服务端认证头。 */
    public void apply(HttpRequest.Builder builder, McpServerEntity server, String accept) {
        builder.header("Content-Type", "application/json");
        builder.header("Accept", accept);
        Map<String, Object> auth = parseMap(server.getAuthConfig());
        String authType = server.getAuthType() == null ? "" : server.getAuthType().trim().toLowerCase(Locale.ROOT);
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
            String username = String.valueOf(auth.getOrDefault("username", ""));
            String password = String.valueOf(auth.getOrDefault("password", ""));
            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }
    }

    /** 容错解析认证配置，非法内容按空配置处理。 */
    private Map<String, Object> parseMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }
}
