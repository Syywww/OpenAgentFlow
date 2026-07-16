package com.openagentflow.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** 日志、Trace和治理载荷统一敏感数据脱敏器。 */
@Component
public class SensitiveDataSanitizer {

    /** 模型密钥、云密钥和常见Bearer令牌。 */
    private static final Pattern SECRET = Pattern.compile(
            "(?i)(?:sk|ark|ak)-[a-z0-9_-]{12,}|bearer\\s+[a-z0-9._~+/-]{16,}");

    /** 中国大陆手机号。 */
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    /** 中国大陆身份证号。 */
    private static final Pattern ID_CARD = Pattern.compile(
            "(?<!\\d)[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx](?!\\d)");

    /** 常见密码、令牌和密钥JSON字段。 */
    private static final Pattern CREDENTIAL_FIELD = Pattern.compile(
            "(?i)(\\\"?(?:password|apiKey|api_key|accessToken|access_token|secret|authorization)\\\"?\\s*[:=]\\s*\\\"?)[^\\\",;\\s}]+");

    /** 对字符串执行不可逆展示脱敏。 */
    public String sanitize(String value) {
        if (value == null || value.isBlank()) return value;
        String result = SECRET.matcher(value).replaceAll("***");
        result = MOBILE.matcher(result).replaceAll("***手机号***");
        result = ID_CARD.matcher(result).replaceAll("***身份证***");
        return CREDENTIAL_FIELD.matcher(result).replaceAll("$1***");
    }

    /** 递归脱敏Map、List和字符串载荷，其他标量保持原值。 */
    public Object sanitizeObject(Object value) {
        if (value instanceof String text) return sanitize(text);
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, item) -> result.put(String.valueOf(key), sanitizeObject(item)));
            return result;
        }
        if (value instanceof List<?> source) {
            List<Object> result = new ArrayList<>();
            source.forEach(item -> result.add(sanitizeObject(item)));
            return result;
        }
        return value;
    }
}
