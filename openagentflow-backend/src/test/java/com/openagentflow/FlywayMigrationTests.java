package com.openagentflow;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Flyway完整迁移链集成测试。 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTests {

    /** 提供与生产版本一致的MySQL 8测试容器。 */
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("openagentflow")
            .withPassword("123456");

    /** 空数据库必须能够一次执行到P66对应的V042结构。 */
    @Test
    void freshDatabaseShouldMigrateToVersion42() throws Exception {
        Flyway flyway = Flyway.configure()
                // 完整迁移包含建库语句，使用容器root账号执行DDL。
                .dataSource(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();

        flyway.migrate();

        assertEquals("42", flyway.info().current().getVersion().getVersion());
        try (Connection connection = MYSQL.createConnection("")) {
            assertTrue(columnExists(connection, "knowledge_document", "current_pipeline_root_id"));
            assertTrue(columnExists(connection, "document_pipeline_node", "generation_no"));
            assertTrue(tableExists(connection, "document_pipeline_reconcile_issue"));
        }
    }

    /** 查询列是否已由迁移创建。 */
    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns("openagentflow", null, table, column)) {
            return resultSet.next();
        }
    }

    /** 查询表是否已由迁移创建。 */
    private boolean tableExists(Connection connection, String table) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getTables("openagentflow", null, table, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }
}
