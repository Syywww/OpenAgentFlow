package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.task.AsyncTaskMessage;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.AsyncTaskOutboxEntity;
import com.openagentflow.mapper.AsyncTaskOutboxMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 异步任务 Transactional Outbox 服务。
 */
@Service
public class AsyncTaskOutboxService {

    /** 当前消息 Schema 版本。 */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /** Outbox Mapper。 */
    private final AsyncTaskOutboxMapper outboxMapper;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** JDBC 工具，用于原子领取消息。 */
    private final JdbcTemplate jdbcTemplate;

    /** Kafka 异步任务配置。 */
    private final OpenAgentFlowProperties.AsyncTask properties;

    /** 任务 Topic 路由器。 */
    private final AsyncTaskTopicRouter topicRouter;

    public AsyncTaskOutboxService(AsyncTaskOutboxMapper outboxMapper,
                                  ObjectMapper objectMapper,
                                  JdbcTemplate jdbcTemplate,
                                  OpenAgentFlowProperties openAgentFlowProperties,
                                  AsyncTaskTopicRouter topicRouter) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = openAgentFlowProperties.getAsyncTask();
        this.topicRouter = topicRouter;
    }

    /**
     * 在当前数据库事务中创建首次投递消息。
     *
     * @param task 异步任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void enqueueInitial(AsyncTaskEntity task) {
        enqueue(task, topicRouter.primaryTopic(task.getTaskType()), value(task.getRetryCount()), Duration.ZERO, null);
    }

    /**
     * 在当前数据库事务中创建延迟重试消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public void enqueueRetry(AsyncTaskEntity task, int attempt, Duration delay, String error) {
        String topic = topicRouter.retryTopic(task.getTaskType(), attempt);
        enqueue(task, topic, attempt, delay, error);
    }

    /**
     * 在当前数据库事务中创建死信消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public void enqueueDeadLetter(AsyncTaskEntity task, int attempt, String error) {
        enqueue(task, properties.getDeadLetterTopic(), attempt, Duration.ZERO, error);
    }

    /**
     * 为旧任务或失联任务补充待发送消息，存在未完成 Outbox 时不重复创建。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean enqueueRecovery(AsyncTaskEntity task) {
        Long pending = outboxMapper.selectCount(new LambdaQueryWrapper<AsyncTaskOutboxEntity>()
                .eq(AsyncTaskOutboxEntity::getTaskId, task.getId())
                .in(AsyncTaskOutboxEntity::getStatus, List.of("pending", "sending", "failed")));
        if (pending != null && pending > 0) {
            return false;
        }
        enqueueInitial(task);
        return true;
    }

    /**
     * 原子领取一批可发送 Outbox 消息。
     */
    public List<AsyncTaskOutboxEntity> claimBatch(String publisherId, int batchSize) {
        // 每个时间列保持单一时钟来源，避免应用与数据库时区不一致时领取失效：
        // available_at 由 JVM 写入（enqueue/markFailed），用 JVM 时间比较；
        // locked_at 由 MySQL NOW(3) 写入，也用 MySQL 时间比较。
        List<AsyncTaskOutboxEntity> candidates = outboxMapper.selectList(new LambdaQueryWrapper<AsyncTaskOutboxEntity>()
                .in(AsyncTaskOutboxEntity::getStatus, List.of("pending", "failed", "sending"))
                .le(AsyncTaskOutboxEntity::getAvailableAt, LocalDateTime.now())
                .and(item -> item.ne(AsyncTaskOutboxEntity::getStatus, "sending")
                        .or()
                        .isNull(AsyncTaskOutboxEntity::getLockedAt)
                        .or()
                        .apply("locked_at < DATE_SUB(NOW(3), INTERVAL 2 MINUTE)"))
                .orderByAsc(AsyncTaskOutboxEntity::getAvailableAt)
                .orderByAsc(AsyncTaskOutboxEntity::getCreatedAt)
                .last("limit " + Math.max(1, Math.min(batchSize, 500))));
        List<AsyncTaskOutboxEntity> claimed = new ArrayList<>();
        for (AsyncTaskOutboxEntity candidate : candidates) {
            int changed = jdbcTemplate.update("""
                    UPDATE async_task_outbox
                    SET status = 'sending', locked_by = ?, locked_at = NOW(3), updated_at = NOW(3)
                    WHERE id = ?
                      AND attempt_count < max_attempts
                      AND available_at <= ?
                      AND (status IN ('pending', 'failed')
                           OR (status = 'sending' AND (locked_at IS NULL OR locked_at < DATE_SUB(NOW(3), INTERVAL 2 MINUTE)))
                      )
                    """, publisherId, candidate.getId(), LocalDateTime.now());
            if (changed > 0) {
                claimed.add(outboxMapper.selectById(candidate.getId()));
            }
        }
        return claimed;
    }

    /**
     * 标记 Broker 已确认消息。
     */
    public void markSent(String outboxId) {
        jdbcTemplate.update("""
                UPDATE async_task_outbox
                SET status = 'sent', sent_at = NOW(3), locked_by = NULL, locked_at = NULL,
                    last_error = NULL, updated_at = NOW(3)
                WHERE id = ?
                """, outboxId);
    }

    /**
     * 标记发送失败并按指数退避安排下次发送。
     *
     * @return 是否已超过最大发送次数
     */
    public boolean markFailed(AsyncTaskOutboxEntity outbox, String error) {
        int nextAttempt = value(outbox.getAttemptCount()) + 1;
        int maxAttempts = Math.max(1, value(outbox.getMaxAttempts()));
        boolean dead = nextAttempt >= maxAttempts;
        long delaySeconds = Math.min(60L, 1L << Math.min(nextAttempt, 6));
        jdbcTemplate.update("""
                UPDATE async_task_outbox
                SET status = ?, attempt_count = ?, available_at = ?, last_error = ?,
                    locked_by = NULL, locked_at = NULL, updated_at = NOW(3)
                WHERE id = ?
                """,
                dead ? "dead" : "failed",
                nextAttempt,
                LocalDateTime.now().plusSeconds(delaySeconds),
                limit(error, 4000),
                outbox.getId());
        return dead;
    }

    /**
     * 清理已成功发送且超过保留期的 Outbox 数据。
     */
    public int purgeSent(int retentionDays) {
        return jdbcTemplate.update("DELETE FROM async_task_outbox WHERE status = 'sent' AND sent_at < DATE_SUB(NOW(3), INTERVAL ? DAY)",
                Math.max(1, retentionDays));
    }

    private void enqueue(AsyncTaskEntity task, String topic, int attempt, Duration delay, String error) {
        try {
            Instant now = Instant.now();
            AsyncTaskMessage message = new AsyncTaskMessage();
            message.setSchemaVersion(CURRENT_SCHEMA_VERSION);
            message.setTraceId(task.getTraceId());
            message.setMessageId(UUID.randomUUID().toString());
            message.setTaskId(task.getId());
            message.setTaskType(task.getTaskType());
            message.setAttempt(Math.max(0, attempt));
            message.setCreatedAt(now);
            message.setNotBeforeAt(now.plus(delay == null ? Duration.ZERO : delay));
            message.setLastError(error);

            AsyncTaskOutboxEntity outbox = new AsyncTaskOutboxEntity();
            outbox.setId(UUID.randomUUID().toString());
            outbox.setTaskId(task.getId());
            outbox.setMessageId(message.getMessageId());
            outbox.setTopicName(topic);
            outbox.setMessageKey(task.getId());
            outbox.setSchemaVersion(CURRENT_SCHEMA_VERSION);
            outbox.setTraceId(task.getTraceId());
            outbox.setPayloadJson(objectMapper.writeValueAsString(message));
            outbox.setStatus("pending");
            outbox.setAttemptCount(0);
            outbox.setMaxAttempts(Math.max(3, properties.getOutboxMaxAttempts()));
            // 可投递时间跟随 JVM 时钟（与 Kafka 消息 notBeforeAt 同源），领取比较必须用 JVM 时间（见 claimBatch）。
            outbox.setAvailableAt(LocalDateTime.ofInstant(message.getNotBeforeAt(), ZoneId.systemDefault()));
            outboxMapper.insert(outbox);
        } catch (Exception exception) {
            throw new IllegalStateException("创建 Kafka Outbox 消息失败：" + exception.getMessage(), exception);
        }
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
