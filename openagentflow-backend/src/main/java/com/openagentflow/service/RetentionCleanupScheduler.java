package com.openagentflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 高增长业务表分批保留清理器。
 *
 * <p>每次只删除有限行，避免一次全表删除造成长事务、主从延迟和InnoDB历史版本堆积。</p>
 */
@Service
@ConditionalOnProperty(prefix = "openagentflow.lifecycle", name = "retention-enabled", havingValue = "true", matchIfMissing = true)
public class RetentionCleanupScheduler {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 单次清理上限。 */
    private final int batchSize;

    /** Trace保留天数。 */
    private final int traceRetentionDays;

    /** 异步任务日志保留天数。 */
    private final int taskLogRetentionDays;

    public RetentionCleanupScheduler(JdbcTemplate jdbcTemplate,
                                     @Value("${openagentflow.lifecycle.cleanup-batch-size:2000}") int batchSize,
                                     @Value("${openagentflow.lifecycle.trace-retention-days:90}") int traceRetentionDays,
                                     @Value("${openagentflow.lifecycle.task-log-retention-days:30}") int taskLogRetentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = Math.max(100, Math.min(batchSize, 10000));
        this.traceRetentionDays = Math.max(7, traceRetentionDays);
        this.taskLogRetentionDays = Math.max(7, taskLogRetentionDays);
    }

    /** 每小时执行一轮小批量清理。 */
    @Scheduled(cron = "${openagentflow.lifecycle.cleanup-cron:0 17 * * * *}")
    public void cleanup() {
        deleteBatch("async_task_log", "created_at", taskLogRetentionDays);
        deleteBatch("async_task_outbox", "sent_at", 7, "status='sent'");
        deleteBatch("runtime_llm_call", "created_at", traceRetentionDays);
        deleteBatch("runtime_trace_step", "created_at", traceRetentionDays);
        deleteBatch("runtime_control_command", "created_at", traceRetentionDays);
    }

    private void deleteBatch(String table, String timeColumn, int retentionDays) {
        deleteBatch(table, timeColumn, retentionDays, "1=1");
    }

    private void deleteBatch(String table, String timeColumn, int retentionDays, String extraCondition) {
        // 表名、字段名和附加条件只来自代码常量，不接收外部输入。
        String sql = "DELETE FROM " + table + " WHERE " + extraCondition
                + " AND " + timeColumn + " < DATE_SUB(NOW(3), INTERVAL ? DAY) LIMIT " + batchSize;
        try {
            jdbcTemplate.update(sql, retentionDays);
        } catch (Exception ignored) {
            // 兼容裁剪部署中不存在的可选表，下一轮继续处理其他保留对象。
        }
    }
}
