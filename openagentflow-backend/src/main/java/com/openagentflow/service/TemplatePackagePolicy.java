package com.openagentflow.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 解决方案模板包领域规则。
 *
 * <p>集中处理敏感配置清洗、语义化版本和三方升级判定，确保发布、安装和升级使用同一套规则。</p>
 */
public final class TemplatePackagePolicy {

    /** 必须从模板包中递归移除的敏感字段名称。 */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "apikey", "api_key", "password", "secret", "secretkey", "secret_key",
            "token", "accesstoken", "access_token", "authorization", "credential", "credentials",
            "privatekey", "private_key", "clientsecret", "client_secret"
    );

    private TemplatePackagePolicy() {
    }

    /**
     * 递归清洗模板资源快照中的敏感字段。
     *
     * @param snapshot 原始资源快照
     * @return 不包含密钥明文的独立快照
     */
    public static Map<String, Object> sanitizeSnapshot(Map<String, Object> snapshot) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (snapshot == null) {
            return result;
        }
        snapshot.forEach((key, value) -> {
            if (!isSensitiveKey(key)) {
                result.put(key, sanitizeValue(value));
            }
        });
        return result;
    }

    /** 比较两个三段式语义化版本。 */
    public static int compareVersions(String left, String right) {
        int[] leftParts = parseVersion(left);
        int[] rightParts = parseVersion(right);
        for (int index = 0; index < 3; index++) {
            int compared = Integer.compare(leftParts[index], rightParts[index]);
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    /** 判断目标版本是否为提高主版本号的破坏性升级。 */
    public static boolean isBreakingUpgrade(String currentVersion, String targetVersion) {
        return parseVersion(targetVersion)[0] > parseVersion(currentVersion)[0];
    }

    /**
     * 根据旧模板、本地副本和新模板的内容哈希决定三方合并动作。
     *
     * @return use_new、keep_local、same_change 或 conflict
     */
    public static String mergeDecision(String oldHash, String localHash, String newHash) {
        if (equals(localHash, oldHash)) {
            return "use_new";
        }
        if (equals(newHash, oldHash)) {
            return "keep_local";
        }
        if (equals(localHash, newHash)) {
            return "same_change";
        }
        return "conflict";
    }

    /** 递归清洗Map、List和数组值。 */
    @SuppressWarnings("unchecked")
    private static Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                String field = String.valueOf(key);
                if (!isSensitiveKey(field)) {
                    converted.put(field, sanitizeValue(item));
                }
            });
            return converted;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> converted = new ArrayList<>();
            iterable.forEach(item -> converted.add(sanitizeValue(item)));
            return converted;
        }
        if (value instanceof Object[] array) {
            List<Object> converted = new ArrayList<>();
            for (Object item : array) {
                converted.add(sanitizeValue(item));
            }
            return converted;
        }
        return value;
    }

    /** 判断字段名是否属于敏感配置。 */
    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.replace("-", "_").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized)
                || normalized.endsWith("_password")
                || normalized.endsWith("_secret")
                || normalized.endsWith("_token")
                || normalized.endsWith("_api_key");
    }

    /** 解析严格的三段式语义化版本。 */
    private static int[] parseVersion(String version) {
        if (version == null || !version.matches("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")) {
            throw new IllegalArgumentException("模板版本必须使用主版本.次版本.修订版本格式");
        }
        String[] parts = version.split("\\.");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
    }

    /** 空值安全比较。 */
    private static boolean equals(String left, String right) {
        return String.valueOf(left).equals(String.valueOf(right));
    }
}
