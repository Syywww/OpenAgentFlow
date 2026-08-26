package com.openagentflow.service;

import com.openagentflow.entity.AsyncTaskOutboxEntity;
import com.openagentflow.mapper.AsyncTaskOutboxMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：应用 JVM 与 MySQL 时区不一致时，Transactional Outbox 投递仍正常。
 *
 * <p>本容器固定 UTC，与 JVM 本地时区（非 UTC 机器）形成刻意错位，复现生产时区漂移。</p>
 * <p>修复前 {@code available_at <= NOW(3)} 用 MySQL 时间比较 JVM 本地时间写入的值，
 * 时区错位时领取门永远不成立，Outbox 投递静默停摆；修复后领取门绑定 JVM 时间，错位不再影响。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaOutboxTimezoneDivergenceIT {

    /** 固定 UTC 的容器：与 JVM 默认时区刻意不同，模拟应用与数据库部署在不同时区。 */
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("openagentflow")
            .withUsername("root")
            .withPassword("openagentflow")
            .withEnv("TZ", "UTC");

    /** 与基类相同的数据源注入方式，容器时区刻意不同。 */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
    }

    @Autowired private AsyncTaskService asyncTaskService;
    @Autowired private AsyncTaskOutboxService outboxService;
    @Autowired private AsyncTaskOutboxMapper outboxMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** 测试类共享同一 MySQL 容器与数据库，且 claimBatch 领取全部 pending Outbox，用例前清空任务表。 */
    @BeforeEach
    void isolateTaskTables() {
        jdbcTemplate.update("DELETE FROM async_task_outbox");
        jdbcTemplate.update("DELETE FROM async_task");
    }

    @AfterEach
    void clearThreadLease() {
        AsyncTaskExecutionContext.clear();
    }

    /** 数据库时区（UTC）与 JVM 本地时区错位时，新建任务的 Outbox 仍能被原子领取并投递。 */
    @Test
    void shouldClaimOutboxDespiteDatabaseTimezoneMismatch() {
        // 本机 JVM 若已是 UTC，则不存在错位，该用例无意义（跳过），仅对非 UTC 时区生效。
        ZoneId jvmZone = ZoneId.systemDefault();
        Assumptions.assumeTrue(!ZoneOffset.UTC.equals(jvmZone.getRules().getOffset(Instant.now())),
                "JVM 时区为 UTC，无时区错位，跳过本回归用例");

        String taskId = asyncTaskService.createTask(
                        "时区漂移回归测试任务",
                        "DOCUMENT_PROCESS",
                        "test",
                        null,
                        "demo",
                        null,
                        null,
                        Map.of("sample", "value"))
                .getId();

        List<AsyncTaskOutboxEntity> claimed = outboxService.claimBatch("publisher-timezone", 10);

        // 修复前：available_at（JVM 本地时间，早于 MySQL UTC 时间）<= NOW(3) 永远不成立，领不到消息；
        // 修复后：领取门绑定 JVM 时间比较，与写入时钟同源，错位不影响领取。
        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getTaskId()).isEqualTo(taskId);
        assertThat(claimed.get(0).getStatus()).isEqualTo("sending");

        outboxService.markSent(claimed.get(0).getId());
        AsyncTaskOutboxEntity sent = outboxMapper.selectById(claimed.get(0).getId());
        assertThat(sent.getStatus()).isEqualTo("sent");
    }
}