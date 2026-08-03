package com.openagentflow;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 生产基础设施容器冒烟测试。
 *
 * <p>本机没有Docker时自动跳过，CI具备Docker时检查MySQL和Kafka真实容器连通性。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class InfrastructureContainerSmokeTests {

    /** MySQL测试容器。 */
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("openagentflow")
            .withUsername("openagentflow")
            .withPassword("openagentflow");

    /** Kafka测试容器。 */
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.1"));

    /** 检查数据库和消息Broker已真实启动。 */
    @Test
    void infrastructureShouldBeReachable() throws Exception {
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT 1")) {
            result.next();
            assertEquals(1, result.getInt(1));
        }
        assertFalse(KAFKA.getBootstrapServers().isBlank());
    }
}
