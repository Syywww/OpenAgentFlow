package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.task.AsyncTaskMessage;
import com.openagentflow.entity.AsyncTaskEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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

    /** Transactional Outbox 服务。 */
    private final AsyncTaskOutboxService outboxService;

    public KafkaTaskClient(KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper,
                           AsyncTaskOutboxService outboxService) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }

    /**
     * 投递新任务到主 Topic。
     *
     * @param task 任务实体
     */
    public void publish(AsyncTaskEntity task) {
        outboxService.enqueueInitial(task);
    }

    /**
     * 投递任务到对应重试 Topic。
     *
     * @param task 任务实体
     * @param attempt 当前重试次数
     * @param delay 延迟时间
     * @param error 上次错误
     */
    public void publishRetry(AsyncTaskEntity task, int attempt, java.time.Duration delay, String error) {
        outboxService.enqueueRetry(task, attempt, delay, error);
    }

    /**
     * 投递最终失败消息到死信 Topic。
     *
     * @param task 任务实体
     * @param attempt 最终尝试次数
     * @param error 最终错误
     */
    public void publishDeadLetter(AsyncTaskEntity task, int attempt, String error) {
        outboxService.enqueueDeadLetter(task, attempt, error);
    }

    /**
     * 反序列化 Kafka 任务消息。
     *
     * @param payload JSON 消息
     * @return 任务消息
     */
    public AsyncTaskMessage parse(String payload) {
        try {
            AsyncTaskMessage message = objectMapper.readValue(payload, AsyncTaskMessage.class);
            if (message.getSchemaVersion() == null || message.getSchemaVersion() < 1
                    || message.getSchemaVersion() > AsyncTaskOutboxService.CURRENT_SCHEMA_VERSION) {
                throw new IllegalArgumentException("不支持的任务消息Schema版本：" + message.getSchemaVersion());
            }
            if (message.getTaskId() == null || message.getTaskId().isBlank()
                    || message.getTaskType() == null || message.getTaskType().isBlank()) {
                throw new IllegalArgumentException("任务消息缺少taskId或taskType");
            }
            return message;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Kafka 任务消息格式错误：" + exception.getMessage(), exception);
        }
    }

    /**
     * 可靠发送任务消息，等待 Broker 确认后再更新 MySQL 投递状态。
     */
    public void sendNow(String topic, String messageKey, String payload) {
        try {
            // Outbox 发布器等待 Broker ACK，成功后才会把消息状态更新为 sent。
            kafkaTemplate.send(topic, messageKey, payload).get(15, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka 任务投递失败：" + exception.getMessage(), exception);
        }
    }
}
