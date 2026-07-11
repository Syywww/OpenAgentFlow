package com.openagentflow.service;

import com.openagentflow.entity.AsyncTaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kafka 任务补偿投递调度器。
 *
 * <p>用于修复 MySQL 提交成功但 Kafka 发送失败，以及 Worker 宕机后心跳超时的任务。</p>
 */
@Service
@ConditionalOnProperty(prefix = "openagentflow.async-task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTaskRecoveryScheduler {

    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(KafkaTaskRecoveryScheduler.class);

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    /** Kafka 工具类。 */
    private final KafkaTaskClient kafkaTaskClient;

    public KafkaTaskRecoveryScheduler(AsyncTaskService asyncTaskService,
                                      KafkaTaskClient kafkaTaskClient) {
        this.asyncTaskService = asyncTaskService;
        this.kafkaTaskClient = kafkaTaskClient;
    }

    /**
     * 每分钟扫描并补偿最多100条失联任务。
     */
    @Scheduled(initialDelay = 30000L, fixedDelay = 60000L)
    public void recover() {
        List<AsyncTaskEntity> tasks = asyncTaskService.findRecoverableTasks(100);
        for (AsyncTaskEntity task : tasks) {
            try {
                kafkaTaskClient.publish(task);
                log.info("Kafka 任务已补偿投递：taskId={}, taskType={}", task.getId(), task.getTaskType());
            } catch (Exception exception) {
                log.warn("Kafka 任务补偿投递失败：taskId={}, error={}", task.getId(), exception.getMessage());
            }
        }
    }
}
