package com.openagentflow.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** 内置解决方案模板资源包测试。 */
class BuiltinTemplatePackageTests {

    /** P0 迁移必须补齐七个模板版本以及完整解决方案资源类型。 */
    @Test
    void shouldContainInstallableBuiltinPackages() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/migration/V051__p0_complete_builtin_solution_packages.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("solution-customer-service", "solution-knowledge-assistant",
                    "solution-data-analyst", "solution-devops", "customer-support", "knowledge-qa", "sql-analyst");
            assertThat(sql).contains("'prompt'", "'tool'", "'knowledge'", "'document'", "'chunk'",
                    "'workflow'", "'agent'", "'team'", "'memory'");
        }
    }
}
