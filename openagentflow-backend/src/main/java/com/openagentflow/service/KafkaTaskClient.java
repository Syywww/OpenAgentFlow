package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.task.AsyncTaskMessage;
import com.openagentflow.entity.AsyncTaskEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 任务操作工具类。
 *
 * <p>统一封装任务消息序列化、分区键、可靠发送、重试 Topic 和死信 Topic，业务服务不直接操作 KafkaTemplate。</p>
 */
@Service
public class KafkaTaskClient {

    /** Kafka 发送模板。 */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    /** 异步任务配置。 */
    private final OpenAgentFlowProperties.AsyncTask properties;

    public KafkaTaskClient(KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper,
                           AsyncTaskService asyncTaskService,
                           OpenAgentFlowProperties openAgentFlowProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.asyncTaskService = asyncTaskService;
        this.properties = openAgentFlowProperties.getAsyncTask();
    }

    /**
     * 投递新任务到主 Topic。
     *
     * @param task 任务实体
     */
    public void publish(AsyncTaskEntity task) {
        publish(task, properties.getTopic(), value(task.getRetryCount()), Duration.ZERO, null);
    }

    /**
     * 投递任务到对应重试 Topic。
     *
     * @param task 任务实体
     * @param attempt 当前重试次数
     * @param delay 延迟时间
     * @param error 上次错误
     */
    public void publishRetry(AsyncTaskEntity task, int attempt, Duration delay, String error) {
        String topic = attempt <= 1 ? properties.getRetryTopic5s() : properties.getRetryTopic30s();
        publish(task, topic, attempt, delay, error);
    }

    /**
     * 投递最终失败消息到死信 Topic。
     *
     * @param task 任务实体
     * @param attempt 最终尝试次数
     * @param error 最终错误
     */
    public void publishDeadLetter(AsyncTaskEntity task, int attempt, String error) {
        publish(task, properties.getDeadLetterTopic(), attempt, Duration.ZERO, error);
    }

    /**
     * 反序列化 Kafka 任务消息。
     *
     * @param payload JSON 消息
     * @return 任务消息
     */
    public AsyncTaskMessage parse(String payload) {
        try {
            return objectMapper.readValue(payload, AsyncTaskMessage.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Kafka 任务消息格式错误：" + exception.getMessage(), exception);
        }
    }

    /**
     * 可靠发送任务消息，等待 Broker 确认后再更新 MySQL 投递状态。
     */
    private void publish(AsyncTaskEntity task,
                         String topic,
                         int attempt,
                         Duration delay,
                         String error) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            // 关闭 Kafka 任务开关时保留数据库任务，后续启用后由补偿调度器统一投递。
            asyncTaskService.appendLog(task.getId(), "warn", "queue_disabled",
                    "Kafka 分布式任务开关已关闭，任务暂存于 MySQL", java.util.Map.of(), null);
            return;
        }
        try {
            AsyncTaskMessage message = new AsyncTaskMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setTaskId(task.getId());
            message.setTaskType(task.getTaskType());
            message.setAttempt(Math.max(0, attempt));
            message.setCreatedAt(Instant.now());
            message.setNotBeforeAt(Instant.now().plus(delay == null ? Duration.ZERO : delay));
            message.setLastError(error);
            String payload = objectMapper.writeValueAsString(message);

            // 使用 taskId 作为消息 Key，同一任务始终进入同一分区，保证同任务消息有序。
            kafkaTemplate.send(topic, task.getId(), payload).get(15, TimeUnit.SECONDS);
            asyncTaskService.markEnqueued(task.getId(), topic, message.getMessageId());
            asyncTaskService.appendLog(task.getId(), "info", "enqueued", "任务已投递 Kafka",
                    java.util.Map.of("topic", topic, "messageId", message.getMessageId(), "attempt", attempt), null);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka 任务投递失败：" + exception.getMessage(), exception);
        }
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
