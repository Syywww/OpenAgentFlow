package com.openagentflow.support;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MySQL 容器集成测试基类。
 *
 * <p>统一提供干净的 MySQL 8 数据库，并通过 Flyway 初始化完整业务结构和种子数据。</p>
 */
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class MySqlContainerIntegrationTestSupport {

    /** 与生产数据库主版本一致的测试容器。 */
    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("openagentflow")
            // 首个迁移脚本包含 CREATE DATABASE，测试容器使用 root 执行完整初始化。
            .withUsername("root")
            .withPassword("openagentflow");

    /**
     * 将容器连接和 Flyway 开关注入 Spring 测试上下文。
     *
     * @param registry 动态配置注册器
     */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
    }
}
