package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;
import com.openagentflow.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 编译器。
 *
 * <p>负责强类型变量校验、默认值填充、分层装配、模板渲染、Token 估算和内容哈希。</p>
 */
@Component
public class PromptCompiler {

    /** Prompt 变量占位符。 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_.-]*)\\s*}}", Pattern.MULTILINE);

    /** 常见 Prompt 注入语句。 */
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+)?previous|system\\s+prompt|忽略.{0,8}(之前|以上|系统)|泄露.{0,6}(提示词|密钥))"
    );

    /** 常见密钥明文模式。 */
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(sk-[a-z0-9_-]{12,}|ark-[a-z0-9_-]{12,}|bearer\\s+[a-z0-9._-]{16,}|api[_-]?key\\s*[:=]\\s*[^\\s]{8,})"
    );

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    public PromptCompiler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 编译 Prompt。
     *
     * @param templateContent 模板正文
     * @param variableSchemaJson 变量定义 JSON
     * @param runtimeVariables 运行时变量
     * @param promptLayers 装配层
     * @param strict 是否严格校验
     * @return 编译结果
     */
    public PromptRuntimeDtos.CompileResult compile(String templateContent,
                                                   String variableSchemaJson,
                                                   Map<String, Object> runtimeVariables,
                                                   List<PromptRuntimeDtos.PromptLayer> promptLayers,
                                                   boolean strict) {
        List<PromptRuntimeDtos.VariableDefinition> definitions = parseDefinitions(variableSchemaJson);
        Map<String, Object> resolvedVariables = new LinkedHashMap<>();
        PromptRuntimeDtos.CompileResult result = new PromptRuntimeDtos.CompileResult();
        Map<String, Object> supplied = runtimeVariables == null ? Map.of() : runtimeVariables;

        // 先按 Schema 解析请求值和默认值，再处理模板中未声明的兼容变量。
        for (PromptRuntimeDtos.VariableDefinition definition : definitions) {
            resolveVariable(definition, supplied, resolvedVariables, result);
        }
        extractVariableNames(templateContent).stream()
                .filter(name -> !resolvedVariables.containsKey(name))
                .forEach(name -> {
                    if (supplied.containsKey(name)) {
                        resolvedVariables.put(name, supplied.get(name));
                        result.variableSources.put(name, "request");
                    }
                });

        if (strict && !result.missingVariables.isEmpty()) {
            throw new BusinessException("PROMPT_VARIABLE_REQUIRED", "Prompt 缺少必填变量：" + String.join("、", result.missingVariables));
        }

        String renderedTemplate = render(templateContent, resolvedVariables);
        List<PromptRuntimeDtos.PromptLayer> layers = new ArrayList<>();
        if (promptLayers != null) {
            promptLayers.stream()
                    .filter(layer -> layer != null && StringUtils.hasText(layer.content))
                    .sorted(Comparator.comparing(layer -> layer.orderNo == null ? 50 : layer.orderNo))
                    .forEach(layer -> layers.add(new PromptRuntimeDtos.PromptLayer(
                            layer.layerCode, layer.layerName, render(layer.content, resolvedVariables), layer.orderNo
                    )));
        }
        layers.add(new PromptRuntimeDtos.PromptLayer("template", "模板正文", renderedTemplate, 100));

        // 使用显式分层标题，方便 Runtime 解释器和 Trace 还原最终装配过程。
        result.layers = layers;
        result.renderedPrompt = layers.stream()
                .map(layer -> "[" + safeLayerName(layer) + "]\n" + layer.content)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse(renderedTemplate);
        if (layers.size() == 1) {
            result.renderedPrompt = renderedTemplate;
        }
        result.estimatedTokens = estimateTokens(result.renderedPrompt);
        result.contentHash = sha256(result.renderedPrompt);
        inspectContent(result.renderedPrompt, result.warnings);
        return result;
    }

    /** 解析变量定义，兼容旧版仅包含 name 的变量数组。 */
    private List<PromptRuntimeDtos.VariableDefinition> parseDefinitions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new BusinessException("PROMPT_VARIABLE_SCHEMA_INVALID", "Prompt 变量 Schema 不是合法 JSON 数组");
        }
    }

    /** 解析单个强类型变量。 */
    private void resolveVariable(PromptRuntimeDtos.VariableDefinition definition,
                                 Map<String, Object> supplied,
                                 Map<String, Object> resolved,
                                 PromptRuntimeDtos.CompileResult result) {
        if (definition == null || !StringUtils.hasText(definition.name)) {
            return;
        }
        String name = definition.name.trim();
        Object value;
        String source;
        if (supplied.containsKey(name) && supplied.get(name) != null) {
            value = supplied.get(name);
            source = Boolean.TRUE.equals(definition.sensitive) ? "request:sensitive" : "request";
        } else if (definition.defaultValue != null) {
            value = definition.defaultValue;
            source = "default";
        } else {
            if (Boolean.TRUE.equals(definition.required)) {
                result.missingVariables.add(name);
            }
            return;
        }
        validateType(name, value, definition.type);
        validateEnum(name, value, definition.enumValues);
        resolved.put(name, value);
        result.variableSources.put(name, source);
        if (Boolean.TRUE.equals(definition.sensitive)) {
            result.sensitiveVariableNames.add(name);
        }
    }

    /** 校验变量类型。 */
    private void validateType(String name, Object value, String configuredType) {
        String type = StringUtils.hasText(configuredType) ? configuredType.toLowerCase(Locale.ROOT) : "string";
        boolean valid = switch (type) {
            case "number" -> value instanceof Number || canParseNumber(value, false);
            case "integer" -> value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || canParseNumber(value, true);
            case "boolean" -> value instanceof Boolean || "true".equalsIgnoreCase(String.valueOf(value)) || "false".equalsIgnoreCase(String.valueOf(value));
            case "object" -> value instanceof Map<?, ?>;
            case "array" -> value instanceof List<?> || value.getClass().isArray();
            default -> value instanceof CharSequence || value instanceof Character || value instanceof Number || value instanceof Boolean;
        };
        if (!valid) {
            throw new BusinessException("PROMPT_VARIABLE_TYPE_INVALID", "Prompt 变量 " + name + " 类型不符合 " + type);
        }
    }

    /** 校验变量枚举。 */
    private void validateEnum(String name, Object value, List<Object> enumValues) {
        if (enumValues == null || enumValues.isEmpty()) {
            return;
        }
        boolean matched = enumValues.stream().anyMatch(item -> String.valueOf(item).equals(String.valueOf(value)));
        if (!matched) {
            throw new BusinessException("PROMPT_VARIABLE_ENUM_INVALID", "Prompt 变量 " + name + " 不在允许值范围内");
        }
    }

    /** 尝试解析数字。 */
    private boolean canParseNumber(Object value, boolean integer) {
        try {
            String text = String.valueOf(value);
            if (integer) {
                Long.parseLong(text);
            } else {
                Double.parseDouble(text);
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /** 渲染模板变量，缺失变量保留原占位符供预览页面提示。 */
    private String render(String content, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content == null ? "" : content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = variables.get(name);
            String replacement = value == null ? matcher.group() : stringify(value);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /** 将复杂变量转换为 JSON。 */
    private String stringify(Object value) {
        if (value instanceof Map<?, ?> || value instanceof List<?> || value.getClass().isArray()) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception ignored) {
                return String.valueOf(value);
            }
        }
        return String.valueOf(value);
    }

    /** 提取模板变量名称。 */
    private List<String> extractVariableNames(String content) {
        List<String> names = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content == null ? "" : content);
        while (matcher.find()) {
            if (!names.contains(matcher.group(1))) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }

    /** 检查 Prompt 注入和密钥明文风险。 */
    private void inspectContent(String content, List<String> warnings) {
        if (INJECTION_PATTERN.matcher(content == null ? "" : content).find()) {
            warnings.add("检测到可能的 Prompt 注入或系统指令泄露语句");
        }
        if (SECRET_PATTERN.matcher(content == null ? "" : content).find()) {
            warnings.add("检测到疑似 API Key 或令牌明文");
        }
    }

    /** 根据中英文混合文本粗略估算 Token。 */
    private int estimateTokens(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(content.codePointCount(0, content.length()) / 3.0D));
    }

    /** 计算 SHA-256 内容哈希。 */
    private String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算 Prompt 内容哈希", exception);
        }
    }

    /** 获取装配层显示名称。 */
    private String safeLayerName(PromptRuntimeDtos.PromptLayer layer) {
        if (StringUtils.hasText(layer.layerName)) {
            return layer.layerName;
        }
        return StringUtils.hasText(layer.layerCode) ? layer.layerCode : "Prompt";
    }
}
