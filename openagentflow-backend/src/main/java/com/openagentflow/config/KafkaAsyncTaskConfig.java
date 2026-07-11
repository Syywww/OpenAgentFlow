package com.openagentflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Kafka 分布式异步任务基础配置。
 */
@Configuration
@EnableKafka
@EnableScheduling
@ConditionalOnProperty(prefix = "openagentflow.async-task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaAsyncTaskConfig {

    /**
     * 自动声明异步任务相关 Topic。
     *
     * @param properties 平台配置
     * @return Topic 定义数组
     */
    @Bean
    public KafkaAdmin.NewTopics asyncTaskTopics(OpenAgentFlowProperties properties) {
        OpenAgentFlowProperties.AsyncTask task = properties.getAsyncTask();
        int partitions = Math.max(1, task.getPartitions());
        return new KafkaAdmin.NewTopics(
                topic(task.getTopic(), partitions, "604800000"),
                topic(task.getRetryTopic5s(), partitions, "604800000"),
                topic(task.getRetryTopic30s(), partitions, "604800000"),
                topic(task.getDeadLetterTopic(), partitions, "2592000000")
        );
    }

    /**
     * 创建单个 Topic 定义。
     *
     * @param name Topic 名称
     * @param partitions 分区数
     * @param retentionMs 消息保留毫秒数
     * @return Topic 定义
     */
    private NewTopic topic(String name, int partitions, String retentionMs) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(1)
                .config("retention.ms", retentionMs)
                .build();
    }

    /**
     * 创建任务心跳调度器，避免长时间模型调用被误判为失联。
     *
     * @return 心跳调度器
     */
    @Bean("asyncTaskHeartbeatScheduler")
    public TaskScheduler asyncTaskHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("oaf-task-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }
}
