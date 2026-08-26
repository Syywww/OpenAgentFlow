package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.task.AsyncTaskMessage;
import com.openagentflow.support.MySqlContainerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kafka Transactional Outbox 幂等性与 Fencing Token 集成测试。
 *
 * <p>验证分布式任务在「同一消息被重复投递 / Worker 失联接管」两种真实故障下仍保持正确性，
 * 核心不变量：</p>
 * <ol>
 *   <li><b>幂等抢占</b>：多个 Worker 竞争同一任务时，MySQL 条件更新原子领取只有一个成功，
 *       Kafka 重复消息不会导致任务被并发执行；</li>
 *   <li><b>Fencing Token 防双写</b>：Worker 心跳超时被接管后，旧执行代次（lock_version）
 *       的任何提交都被拒绝，新代次正常推进。</li>
 * </ol>
 *
 * <p>测试只依赖 MySQL 容器，不启动 Kafka 消费/发布（test Profile 已关闭）。</p>
 */
@SpringBootTest
class KafkaTaskOutboxIdempotencyIT extends MySqlContainerIntegrationTestSupport {

    @Autowired private AsyncTaskService asyncTaskService;
    @Autowired private AsyncTaskOutboxService outboxService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void clearThreadLease() {
        AsyncTaskExecutionContext.clear();
    }

    /**
     * 测试类共享同一 MySQL 容器与数据库，而 claimBatch 领取全部 pending Outbox。
     * 每个用例前清空任务相关表，保证断言精确且与执行顺序无关。
     */
    @BeforeEach
    void isolateTaskTables() {
        jdbcTemplate.update("DELETE FROM async_task_outbox");
        jdbcTemplate.update("DELETE FROM async_task");
    }

    /** 同一任务被两个 Worker 重复领取，条件更新原子抢占保证只有第一个成功。 */
    @Test
    void shouldClaimTaskExactlyOnceAcrossDuplicateWorkers() {
        String taskId = createTask();

        Long firstClaim = asyncTaskService.tryClaim(taskId, "worker-A");
        Long secondClaim = asyncTaskService.tryClaim(taskId, "worker-B");

        // 第一个领取成功且执行代次为 1；第二个因状态已变 running 且心跳新鲜而不命中。
        assertThat(firstClaim).isEqualTo(1L);
        assertThat(secondClaim).isNull();

        Map<String, Object> row = taskRow(taskId);
        assertThat(row.get("status")).isEqualTo("running");
        assertThat(row.get("locked_by")).isEqualTo("worker-A");
        assertThat(longValue(row.get("lock_version"))).isEqualTo(1L);
    }

    /** Worker 心跳超时被接管后，旧代次 Worker 的任何写入都被 Fencing Token 拒绝。 */
    @Test
    void shouldRejectStaleGenerationWriteAfterTakeover() {
        String taskId = createTask();

        // 1. worker-A 领取，执行代次 1，并绑定线程租约。
        Long generationA = asyncTaskService.tryClaim(taskId, "worker-A");
        assertThat(generationA).isEqualTo(1L);
        AsyncTaskExecutionContext.bind(taskId, "worker-A", generationA);

        // 2. 模拟 worker-A 失联（清空心跳），worker-B 按失联规则接管，执行代次升到 2。
        jdbcTemplate.update("UPDATE async_task SET heartbeat_at = NULL WHERE id = ?", taskId);
        Long generationB = asyncTaskService.tryClaim(taskId, "worker-B");
        assertThat(generationB).isEqualTo(2L);

        // 3. 旧代次（worker-A，代次 1）提交写入 → assertActiveLease 校验失败被拒绝。
        assertThatThrownBy(() -> asyncTaskService.updateProgress(
                taskId, "stale", "旧代次 Worker 提交", 50, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TASK_LEASE_LOST");

        // 4. 新代次（worker-B，代次 2）可正常推进，任务归属正确。
        AsyncTaskExecutionContext.bind(taskId, "worker-B", generationB);
        asyncTaskService.updateProgress(taskId, "running", "新代次推进", 60, Map.of());

        Map<String, Object> row = taskRow(taskId);
        assertThat(row.get("locked_by")).isEqualTo("worker-B");
        assertThat(longValue(row.get("lock_version"))).isEqualTo(2L);
        assertThat(row.get("current_stage")).isEqualTo("running");
    }

    /** 任务创建时主表与 Outbox 消息同事务落库，补偿调度不会对已有待发消息重复补投。 */
    @Test
    void shouldEnqueueSinglePendingOutboxOnTaskCreate() throws Exception {
        String taskId = createTask();

        Map<String, Object> outbox = jdbcTemplate.queryForMap(
                "SELECT * FROM async_task_outbox WHERE task_id = ?", taskId);
        assertThat(outbox.get("status")).isEqualTo("pending");
        assertThat(String.valueOf(outbox.get("schema_version"))).isEqualTo("1");
        assertThat(outbox.get("message_key")).isEqualTo(taskId);

        // payload 可反序列化为消息契约，任务标识与类型一致。
        AsyncTaskMessage message = objectMapper.readValue(
                String.valueOf(outbox.get("payload_json")), AsyncTaskMessage.class);
        assertThat(message.getSchemaVersion()).isEqualTo(1);
        assertThat(message.getTaskId()).isEqualTo(taskId);
        assertThat(message.getTaskType()).isEqualTo("DOCUMENT_PROCESS");

        // 补偿调度：存在待发送消息时不重复补投。
        assertThat(outboxService.enqueueRecovery(asyncTaskService.findById(taskId))).isFalse();
        assertThat(countPendingOutbox(taskId)).isEqualTo(1L);
    }

    /** 两个发布器竞争同一 Outbox 消息，条件更新只允许一个领取；Broker 确认后进入终态。 */
    @Test
    void shouldClaimOutboxBatchExactlyOnce() {
        String taskId = createTask();
        String outboxId = jdbcTemplate.queryForObject(
                "SELECT id FROM async_task_outbox WHERE task_id = ?", String.class, taskId);

        List<com.openagentflow.entity.AsyncTaskOutboxEntity> firstBatch =
                outboxService.claimBatch("publisher-A", 10);
        List<com.openagentflow.entity.AsyncTaskOutboxEntity> secondBatch =
                outboxService.claimBatch("publisher-B", 10);

        assertThat(firstBatch).hasSize(1);
        assertThat(firstBatch.get(0).getId()).isEqualTo(outboxId);
        // 已被 publisher-A 领取（sending + 新鲜锁），第二个发布器拿不到同一条消息。
        assertThat(secondBatch).isEmpty();

        Map<String, Object> claimed = jdbcTemplate.queryForMap(
                "SELECT status, locked_by FROM async_task_outbox WHERE id = ?", outboxId);
        assertThat(claimed.get("status")).isEqualTo("sending");
        assertThat(claimed.get("locked_by")).isEqualTo("publisher-A");

        // Broker 确认后标记 sent，进入终态。
        outboxService.markSent(outboxId);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM async_task_outbox WHERE id = ?", String.class, outboxId);
        assertThat(status).isEqualTo("sent");
    }

    /** 走真实创建链路写入任务主表 + Outbox 待发消息。 */
    private String createTask() {
        return asyncTaskService.createTask(
                        "Kafka幂等集成测试任务",
                        "DOCUMENT_PROCESS",
                        "test",
                        null,
                        "demo",
                        null,
                        null,
                        Map.of("sample", "value"))
                .getId();
    }

    private Map<String, Object> taskRow(String taskId) {
        return jdbcTemplate.queryForMap(
                "SELECT status, locked_by, lock_version, current_stage FROM async_task WHERE id = ?", taskId);
    }

    private long countPendingOutbox(String taskId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM async_task_outbox
                WHERE task_id = ? AND status IN ('pending', 'sending', 'failed')
                """, Long.class, taskId);
        return count == null ? 0L : count;
    }

    private long longValue(Object value) {
        return ((Number) value).longValue();
    }
}