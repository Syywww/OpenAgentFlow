package com.openagentflow.service;

import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.entity.AsyncTaskOutboxEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka Transactional Outbox 发布器。
 */
@Service
@ConditionalOnExpression("'${openagentflow.async-task.enabled:true}' == 'true' && '${openagentflow.async-task.publisher-enabled:true}' == 'true'")
public class KafkaOutboxPublisher {

    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxPublisher.class);

    /** 当前发布器ID。 */
    private final String publisherId = buildPublisherId();

    /** Outbox 服务。 */
    private final AsyncTaskOutboxService outboxService;

    /** Kafka 工具类。 */
    private final KafkaTaskClient kafkaTaskClient;

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    /** 异步任务配置。 */
    private final OpenAgentFlowProperties.AsyncTask properties;

    public KafkaOutboxPublisher(AsyncTaskOutboxService outboxService,
                                KafkaTaskClient kafkaTaskClient,
                                AsyncTaskService asyncTaskService,
                                OpenAgentFlowProperties openAgentFlowProperties) {
        this.outboxService = outboxService;
        this.kafkaTaskClient = kafkaTaskClient;
        this.asyncTaskService = asyncTaskService;
        this.properties = openAgentFlowProperties.getAsyncTask();
    }

    /**
     * 高频领取待发送消息，等待 Broker ACK 后更新 Outbox 和任务投递状态。
     */
    @Scheduled(initialDelayString = "${openagentflow.async-task.outbox-initial-delay-ms:1000}",
            fixedDelayString = "${openagentflow.async-task.outbox-poll-ms:500}")
    public void publishPending() {
        List<AsyncTaskOutboxEntity> messages = outboxService.claimBatch(publisherId, properties.getOutboxBatchSize());
        for (AsyncTaskOutboxEntity outbox : messages) {
            try {
                kafkaTaskClient.sendNow(outbox.getTopicName(), outbox.getMessageKey(), outbox.getPayloadJson());
                outboxService.markSent(outbox.getId());
                asyncTaskService.markEnqueued(outbox.getTaskId(), outbox.getTopicName(), outbox.getMessageId());
                asyncTaskService.appendLog(outbox.getTaskId(), "info", "enqueued", "Outbox 消息已投递 Kafka",
                        Map.of("topic", outbox.getTopicName(), "messageId", outbox.getMessageId()), null);
            } catch (Exception exception) {
                boolean dead = outboxService.markFailed(outbox, rootMessage(exception));
                asyncTaskService.appendLog(outbox.getTaskId(), dead ? "error" : "warn", "outbox_retry",
                        dead ? "Outbox 超过最大发送次数" : "Outbox 发送失败，等待自动重试",
                        Map.of("error", rootMessage(exception)), null);
                log.warn("Kafka Outbox 发送失败：outboxId={}, taskId={}, dead={}, error={}",
                        outbox.getId(), outbox.getTaskId(), dead, rootMessage(exception));
            }
        }
    }

    /**
     * 每天清理已成功发送的历史 Outbox，避免业务库无限增长。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeSent() {
        int purged = outboxService.purgeSent(properties.getOutboxRetentionDays());
        if (purged > 0) {
            log.info("已清理过期 Kafka Outbox：count={}", purged);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static String buildPublisherId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-outbox-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception ignored) {
            return "outbox-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
