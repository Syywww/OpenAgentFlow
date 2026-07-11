package com.openagentflow.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 平台生产指标服务。
 *
 * <p>统一暴露Outbox积压、任务积压、Runtime并发和文档阶段耗时，供Prometheus与KEDA使用。</p>
 */
@Service
public class PlatformMetricsService {

    /** 指标注册中心。 */
    private final MeterRegistry registry;

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** Outbox待发送数量。 */
    private final AtomicLong outboxPending = new AtomicLong();

    /** 最老Outbox等待秒数。 */
    private final AtomicLong outboxOldestAge = new AtomicLong();

    /** 异步任务积压数量。 */
    private final AtomicLong taskBacklog = new AtomicLong();

    /** Runtime执行中数量。 */
    private final AtomicLong runtimeRunning = new AtomicLong();

    /** 未解决SLO违规数量。 */
    private final AtomicLong sloViolations = new AtomicLong();

    /** 未解决一致性问题数量。 */
    private final AtomicLong consistencyIssues = new AtomicLong();

    /** 物理文档DAG积压节点数量。 */
    private final AtomicLong documentDagBacklog = new AtomicLong();

    public PlatformMetricsService(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        this.registry = registry;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 注册平台Gauge。 */
    @PostConstruct
    public void register() {
        Gauge.builder("openagentflow_outbox_pending", outboxPending, AtomicLong::get).register(registry);
        Gauge.builder("openagentflow_outbox_oldest_age_seconds", outboxOldestAge, AtomicLong::get).register(registry);
        Gauge.builder("openagentflow_async_task_backlog", taskBacklog, AtomicLong::get).register(registry);
        Gauge.builder("openagentflow_runtime_running", runtimeRunning, AtomicLong::get).register(registry);
        Gauge.builder("openagentflow_slo_violations", sloViolations, AtomicLong::get).register(registry);
        Gauge.builder("openagentflow_consistency_issues", consistencyIssues, AtomicLong::get).register(registry);
        Gauge.builder("openagentflow_document_dag_backlog", documentDagBacklog, AtomicLong::get).register(registry);
    }

    /** 从数据库刷新低基数运营指标。 */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${openagentflow.observability.refresh-ms:15000}")
    public void refresh() {
        outboxPending.set(number("SELECT COUNT(1) FROM async_task_outbox WHERE status IN ('pending','failed','sending')"));
        outboxOldestAge.set(number("SELECT COALESCE(TIMESTAMPDIFF(SECOND, MIN(created_at), NOW()), 0) FROM async_task_outbox WHERE status IN ('pending','failed','sending')"));
        taskBacklog.set(number("SELECT COUNT(1) FROM async_task WHERE status IN ('pending','running')"));
        runtimeRunning.set(number("SELECT COUNT(1) FROM runtime_run WHERE status IN ('RUNNING','running')"));
        sloViolations.set(number("SELECT COUNT(1) FROM platform_slo_violation WHERE status IN ('open','acknowledged')"));
        consistencyIssues.set(number("SELECT COUNT(1) FROM data_consistency_issue WHERE status IN ('open','repairing')"));
        documentDagBacklog.set(number("SELECT COUNT(1) FROM document_pipeline_node WHERE status IN ('pending','queued','running')"));
    }

    /** 获取文档阶段计时器。 */
    public Timer documentStageTimer(String stage) {
        return registry.timer("openagentflow_document_stage_duration", "stage", stage);
    }

    /** 累加安全拦截次数。 */
    public void incrementSecurityBlock(String reason) {
        Counter.builder("openagentflow_security_blocks_total").tag("reason", reason).register(registry).increment();
    }

    private long number(String sql) {
        try {
            Number result = jdbcTemplate.queryForObject(sql, Number.class);
            return result == null ? 0L : result.longValue();
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
