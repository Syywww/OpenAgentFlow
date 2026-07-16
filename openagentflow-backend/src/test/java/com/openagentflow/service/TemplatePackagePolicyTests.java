package com.openagentflow.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 解决方案模板包安全清洗、版本和三方升级规则测试。 */
class TemplatePackagePolicyTests {

    /** 模板快照必须递归清除密钥和认证信息，同时保留普通配置。 */
    @Test
    void shouldRecursivelyRemoveSensitiveConfiguration() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("endpointUrl", "https://example.com/api");
        snapshot.put("apiKey", "secret-key");
        snapshot.put("authConfig", Map.of("token", "secret-token", "type", "bearer"));
        snapshot.put("items", List.of(Map.of("password", "123456", "name", "database")));

        Map<String, Object> sanitized = TemplatePackagePolicy.sanitizeSnapshot(snapshot);

        assertThat(sanitized).containsEntry("endpointUrl", "https://example.com/api");
        assertThat(sanitized).doesNotContainKey("apiKey");
        assertThat((Map<String, Object>) sanitized.get("authConfig")).containsEntry("type", "bearer").doesNotContainKey("token");
        assertThat((Map<String, Object>) ((List<?>) sanitized.get("items")).getFirst()).containsEntry("name", "database").doesNotContainKey("password");
    }

    /** 语义化版本按主、次、修订位比较，并能识别破坏性主版本升级。 */
    @Test
    void shouldCompareSemanticVersions() {
        assertThat(TemplatePackagePolicy.compareVersions("1.9.9", "2.0.0")).isNegative();
        assertThat(TemplatePackagePolicy.compareVersions("2.1.0", "2.0.9")).isPositive();
        assertThat(TemplatePackagePolicy.isBreakingUpgrade("1.8.0", "2.0.0")).isTrue();
        assertThat(TemplatePackagePolicy.isBreakingUpgrade("1.8.0", "1.9.0")).isFalse();
    }

    /** 本地未修改时采用新版，本地和新版都改动且不同则标记冲突。 */
    @Test
    void shouldResolveThreeWayMergeDecision() {
        assertThat(TemplatePackagePolicy.mergeDecision("old", "old", "new")).isEqualTo("use_new");
        assertThat(TemplatePackagePolicy.mergeDecision("old", "local", "old")).isEqualTo("keep_local");
        assertThat(TemplatePackagePolicy.mergeDecision("old", "local", "local")).isEqualTo("same_change");
        assertThat(TemplatePackagePolicy.mergeDecision("old", "local", "new")).isEqualTo("conflict");
    }
}
