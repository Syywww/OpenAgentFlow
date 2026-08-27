package com.openagentflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.ToolDefinitionForModel;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 讯飞星火适配器协议构造单元测试：不依赖真实密钥与网络，固定入参校验鉴权 URL 与请求帧契约。 */
class SparkChatClientTests {

    private final SparkChatClient client = new SparkChatClient(new ObjectMapper());

    @Test
    void shouldBuildAuthUrlWithHmacSignatureQueryParams() throws Exception {
        String url = client.buildAuthUrl(
                "wss://spark-api.xf-yun.com/v4.0/chat", "api-key-123", "api-secret-456");

        assertThat(url).startsWith("wss://spark-api.xf-yun.com/v4.0/chat?");

        URI uri = URI.create(url);
        Map<String, String> params = queryParams(uri.getRawQuery());
        assertThat(params.keySet()).containsExactlyInAnyOrder("authorization", "date", "host");
        assertThat(params.get("host")).isEqualTo("spark-api.xf-yun.com");

        // date 必须符合 RFC1123 GMT，能被官方签名规则解析（GMT 解析为 UTC 偏移）。
        ZonedDateTime date = ZonedDateTime.parse(params.get("date"), DateTimeFormatter.RFC_1123_DATE_TIME);
        assertThat(date.getOffset()).isEqualTo(java.time.ZoneOffset.UTC);

        // authorization 为 Base64(api_key="...", algorithm="hmac-sha256", headers="host date request-line", signature="...")。
        String authorization = new String(Base64.getDecoder().decode(params.get("authorization")), StandardCharsets.UTF_8);
        assertThat(authorization).startsWith("api_key=\"api-key-123\", algorithm=\"hmac-sha256\", "
                + "headers=\"host date request-line\", signature=\"");
        assertThat(authorization).endsWith("\"");

        // 端到端重算签名：用响应里的 date 重建 host/date/request-line 三段，HMAC-SHA256 后必须与签名一致。
        String signatureInput = "host: spark-api.xf-yun.com\n"
                + "date: " + params.get("date") + "\n"
                + "GET /v4.0/chat HTTP/1.1";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                "api-secret-456".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expectedSignature = Base64.getEncoder().encodeToString(
                mac.doFinal(signatureInput.getBytes(StandardCharsets.UTF_8)));
        String actualSignature = authorization.substring(
                authorization.indexOf("signature=\"") + "signature=\"".length(),
                authorization.length() - 1);
        assertThat(actualSignature).isEqualTo(expectedSignature);
    }

    @Test
    void shouldRejectBaseUrlWithoutHost() {
        assertThatThrownBy(() -> client.buildAuthUrl("not-a-url", "key", "secret"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("SPARK_BASE_URL_INVALID");
    }

    @Test
    void shouldBuildRequestFrameWithHeaderParameterPayload() throws Exception {
        ModelConfigEntity model = new ModelConfigEntity();
        model.setDefaultParams("{\"domain\":\"generalv3.5\"}");

        ChatRunContext context = new ChatRunContext();
        context.setModel(model);
        context.setMessages(List.of(
                new ChatMessage("system", "你是助手"),
                new ChatMessage("user", "你好"),
                new ChatMessage("assistant", "在的"),
                new ChatMessage("tool", "工具结果")));

        String frame = client.buildRequestJson(context, "app-id-1", 0.2, 512, null);
        JsonNode root = new ObjectMapper().readTree(frame);

        // header.app_id / header.uid
        assertThat(root.path("header").path("app_id").asText()).isEqualTo("app-id-1");
        assertThat(root.path("header").path("uid").asText()).isNotBlank();

        // parameter.chat 与传入参数对齐
        assertThat(root.path("parameter").path("chat").path("domain").asText()).isEqualTo("generalv3.5");
        assertThat(root.path("parameter").path("chat").path("temperature").asDouble()).isEqualTo(0.2);
        assertThat(root.path("parameter").path("chat").path("max_tokens").asInt()).isEqualTo(512);

        // payload.message.text 顺序与角色映射（tool 回退 user）
        JsonNode text = root.path("payload").path("message").path("text");
        assertThat(text).hasSize(4);
        assertThat(text.get(0).path("role").asText()).isEqualTo("system");
        assertThat(text.get(1).path("role").asText()).isEqualTo("user");
        assertThat(text.get(2).path("role").asText()).isEqualTo("assistant");
        assertThat(text.get(3).path("role").asText()).isEqualTo("user");
        assertThat(text.get(3).path("content").asText()).isEqualTo("工具结果");
    }

    @Test
    void shouldApplyDefaultsWhenTemperatureAndMaxTokensAbsent() throws Exception {
        ChatRunContext context = new ChatRunContext();
        context.setMessages(List.of(new ChatMessage("user", "hi")));

        String frame = client.buildRequestJson(context, "app-id-1", null, null, null);
        JsonNode chat = new ObjectMapper().readTree(frame).path("parameter").path("chat");
        assertThat(chat.path("temperature").asDouble()).isEqualTo(0.5);
        assertThat(chat.path("max_tokens").asInt()).isEqualTo(2048);
        assertThat(chat.path("domain").asText()).isEqualTo("4.0Ultra");
    }

    @Test
    void shouldParseCompoundCredentials() {
        assertThat(client.parseCredentials("app-1:key-2:secret-3"))
                .containsExactly("app-1", "key-2", "secret-3");
        // 按前两个冒号拆分，密钥本身可含冒号。
        assertThat(client.parseCredentials("app-1:key:with:colon:secret"))
                .containsExactly("app-1", "key", "with:colon:secret");
    }

    @Test
    void shouldRejectMissingOrMalformedCredentials() {
        assertThatThrownBy(() -> client.parseCredentials(""))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("SPARK_CREDENTIAL_MISSING");
        assertThatThrownBy(() -> client.parseCredentials("app:key"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("SPARK_CREDENTIAL_INVALID");
        assertThatThrownBy(() -> client.parseCredentials("app::"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("SPARK_CREDENTIAL_INVALID");
    }

    @Test
    void shouldMapRolesWithToolFallbackToUser() {
        assertThat(client.mapRole("system")).isEqualTo("system");
        assertThat(client.mapRole("user")).isEqualTo("user");
        assertThat(client.mapRole("assistant")).isEqualTo("assistant");
        assertThat(client.mapRole("tool")).isEqualTo("user");
        assertThat(client.mapRole("function")).isEqualTo("user");
        assertThat(client.mapRole(null)).isEqualTo("user");
    }

    @Test
    void shouldExposeIdleCancellationState() {
        assertThat(client.activeRunIds()).isEmpty();
        assertThat(client.cancel("run-not-active")).isFalse();
    }

    @Test
    void shouldResolveDomainFromDefaultParamsWithFallback() {
        ModelConfigEntity model = new ModelConfigEntity();

        model.setDefaultParams("{\"domain\":\"generalv3.5\"}");
        assertThat(client.resolveDomain(model)).isEqualTo("generalv3.5");

        model.setDefaultParams("{}");
        assertThat(client.resolveDomain(model)).isEqualTo("4.0Ultra");

        model.setDefaultParams("not-json");
        assertThat(client.resolveDomain(model)).isEqualTo("4.0Ultra");

        assertThat(client.resolveDomain(null)).isEqualTo("4.0Ultra");
    }

    @Test
    void shouldBuildRequestFrameWithFunctions() throws Exception {
        ToolDefinitionForModel tool = new ToolDefinitionForModel();
        tool.setName("get_weather");
        tool.setDescription("查询天气");
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("type", "object");
        params.putObject("properties").putObject("city").put("type", "string");
        params.putArray("required").add("city");
        tool.setParameters(params);

        ModelConfigEntity model = new ModelConfigEntity();
        model.setDefaultParams("{\"domain\":\"4.0Ultra\"}");
        ChatRunContext context = new ChatRunContext();
        context.setModel(model);
        context.setMessages(List.of(new ChatMessage("user", "合肥天气")));

        String frame = client.buildRequestJson(context, "app-id-1", 0.5, 512, List.of(tool));
        JsonNode root = new ObjectMapper().readTree(frame);

        // payload.functions.text 序列化 name/description/parameters（含 required）。
        JsonNode fn = root.path("payload").path("functions").path("text").path(0);
        assertThat(fn.path("name").asText()).isEqualTo("get_weather");
        assertThat(fn.path("description").asText()).isEqualTo("查询天气");
        assertThat(fn.path("parameters").path("type").asText()).isEqualTo("object");
        assertThat(fn.path("parameters").path("required")).hasSize(1);

        // message 与 functions 同在 payload 下（payload 节点复用，未互相覆盖）。
        assertThat(root.path("payload").path("message").path("text")).hasSize(1);
        assertThat(root.path("payload").path("functions").path("text")).hasSize(1);
    }

    @Test
    void shouldOmitFunctionsWhenNoTools() throws Exception {
        ChatRunContext context = new ChatRunContext();
        context.setMessages(List.of(new ChatMessage("user", "hi")));

        String frame = client.buildRequestJson(context, "app-id-1", null, null, null);
        JsonNode root = new ObjectMapper().readTree(frame);
        assertThat(root.path("payload").path("functions").isMissingNode()).isTrue();
    }

    /** 把查询串解析为有序键值（值保持 URL 编码原样）。 */
    private Map<String, String> queryParams(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String part : rawQuery.split("&")) {
            int idx = part.indexOf('=');
            params.put(URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8),
                    URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return params;
    }
}