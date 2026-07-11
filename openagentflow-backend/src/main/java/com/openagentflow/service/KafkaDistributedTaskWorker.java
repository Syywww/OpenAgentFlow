package com.openagentflow.service;

import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.task.AsyncTaskMessage;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.AuthUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kafka 分布式异步任务消费者。
 */
@Service
@ConditionalOnProperty(prefix = "openagentflow.async-task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaDistributedTaskWorker {

    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(KafkaDistributedTaskWorker.class);

    /** 当前 Worker 唯一标识。 */
    private final String workerId = buildWorkerId();

    /** 任务处理器映射。 */
    private final Map<String, DistributedTaskHandler> handlers;

    /** Kafka 工具类。 */
    private final KafkaTaskClient kafkaTaskClient;

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    /** 任务配置。 */
    private final OpenAgentFlowProperties.AsyncTask properties;

    /** 心跳调度器。 */
    private final TaskScheduler heartbeatScheduler;

    /** 用户加载服务，用于恢复任务创建人的权限上下文。 */
    private final AuthUserDetailsService authUserDetailsService;

    public KafkaDistributedTaskWorker(List<DistributedTaskHandler> handlers,
                                      KafkaTaskClient kafkaTaskClient,
                                      AsyncTaskService asyncTaskService,
                                      OpenAgentFlowProperties openAgentFlowProperties,
                                      @Qualifier("asyncTaskHeartbeatScheduler") TaskScheduler heartbeatScheduler,
                                      AuthUserDetailsService authUserDetailsService) {
        this.handlers = handlers.stream().collect(Collectors.toMap(
                DistributedTaskHandler::taskType,
                Function.identity(),
                (left, right) -> {
                    throw new IllegalStateException("重复的分布式任务处理器：" + left.taskType());
                },
                LinkedHashMap::new));
        this.kafkaTaskClient = kafkaTaskClient;
        this.asyncTaskService = asyncTaskService;
        this.properties = openAgentFlowProperties.getAsyncTask();
        this.heartbeatScheduler = heartbeatScheduler;
        this.authUserDetailsService = authUserDetailsService;
    }

    /**
     * 消费主任务和两级重试 Topic。
     *
     * @param payload 消息JSON
     * @param acknowledgment Kafka 手动确认对象
     */
    @KafkaListener(
            topics = {"${openagentflow.async-task.topic}", "${openagentflow.async-task.retry-topic-5s}", "${openagentflow.async-task.retry-topic-30s}"},
            groupId = "${openagentflow.async-task.consumer-group}")
    public void consume(String payload, Acknowledgment acknowledgment) {
        AsyncTaskMessage message;
        try {
            message = kafkaTaskClient.parse(payload);
        } catch (Exception exception) {
            // 无法识别任务ID的坏消息不能无限阻塞分区，输出错误后确认并交由 Kafka 日志审计。
            log.error("收到无法解析的 Kafka 任务消息，已跳过：payload={}, error={}", payload, exception.getMessage());
            acknowledgment.acknowledge();
            return;
        }
        if (message.getNotBeforeAt() != null && message.getNotBeforeAt().isAfter(Instant.now())) {
            Duration remaining = Duration.between(Instant.now(), message.getNotBeforeAt());
            acknowledgment.nack(remaining.compareTo(Duration.ofSeconds(30)) > 0 ? Duration.ofSeconds(30) : remaining);
            return;
        }

        AsyncTaskEntity task = asyncTaskService.findById(message.getTaskId());
        if (task == null || isTerminal(task.getStatus())) {
            acknowledgment.acknowledge();
            return;
        }
        if (!asyncTaskService.tryClaim(task.getId(), workerId)) {
            // 其他 Worker 已经领取或任务状态已变化，重复消息直接确认。
            acknowledgment.acknowledge();
            return;
        }

        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
                () -> asyncTaskService.heartbeat(task.getId(), workerId), Duration.ofSeconds(20));
        try {
            restoreSecurityContext(task);
            DistributedTaskHandler handler = handlers.get(task.getTaskType());
            if (handler == null) {
                throw new IllegalStateException("未注册任务处理器：" + task.getTaskType());
            }
            Map<String, Object> result = handler.executeDistributedTask(asyncTaskService.findById(task.getId()));
            AsyncTaskEntity latest = asyncTaskService.findById(task.getId());
            if (latest != null && !"canceled".equals(latest.getStatus())) {
                asyncTaskService.markSuccess(task.getId(), "Kafka 分布式任务执行完成", result == null ? Map.of() : result);
            }
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            handleFailure(task, message, exception, acknowledgment);
        } finally {
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 消费死信 Topic，确保最终失败状态已经落库。
     */
    @KafkaListener(
            topics = "${openagentflow.async-task.dead-letter-topic}",
            groupId = "${openagentflow.async-task.consumer-group}-dlt")
    public void consumeDeadLetter(String payload, Acknowledgment acknowledgment) {
        AsyncTaskMessage message;
        try {
            message = kafkaTaskClient.parse(payload);
        } catch (Exception exception) {
            log.error("收到无法解析的 Kafka 死信消息，已跳过：payload={}, error={}", payload, exception.getMessage());
            acknowledgment.acknowledge();
            return;
        }
        if (asyncTaskService.findById(message.getTaskId()) != null) {
            asyncTaskService.markDeadLetter(message.getTaskId(), message.getLastError());
        }
        acknowledgment.acknowledge();
    }

    /**
     * 按重试次数路由到五秒、三十秒重试 Topic 或死信 Topic。
     */
    private void handleFailure(AsyncTaskEntity task,
                               AsyncTaskMessage message,
                               Exception exception,
                               Acknowledgment acknowledgment) {
        String error = rootMessage(exception);
        AsyncTaskEntity latest = asyncTaskService.findById(task.getId());
        int nextAttempt = Math.max(value(message.getAttempt()), latest == null ? 0 : value(latest.getRetryCount())) + 1;
        if (latest != null && Boolean.TRUE.equals(latest.getCancelRequested())) {
            asyncTaskService.markCanceled(task.getId(), "任务执行期间收到取消请求");
            acknowledgment.acknowledge();
            return;
        }
        if (nextAttempt <= Math.max(0, properties.getMaxRetries())) {
            Duration delay = nextAttempt == 1 ? Duration.ofSeconds(5) : Duration.ofSeconds(30);
            LocalDateTime nextRetryAt = LocalDateTime.now().plus(delay);
            asyncTaskService.markRetryPending(task.getId(), nextRetryAt, error);
            kafkaTaskClient.publishRetry(asyncTaskService.findById(task.getId()), nextAttempt, delay, error);
        } else {
            kafkaTaskClient.publishDeadLetter(asyncTaskService.findById(task.getId()), nextAttempt, error);
            asyncTaskService.markDeadLetter(task.getId(), error);
        }
        log.warn("Kafka 任务执行失败：taskId={}, taskType={}, attempt={}, error={}",
                task.getId(), task.getTaskType(), nextAttempt, error);
        acknowledgment.acknowledge();
    }

    private boolean isTerminal(String status) {
        return List.of("success", "failed", "canceled", "dead_letter").contains(status);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static String buildWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception ignored) {
            return "worker-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    /**
     * 恢复任务创建人的 Spring Security 上下文，保证资源级权限与前台提交时一致。
     */
    private void restoreSecurityContext(AsyncTaskEntity task) {
        String userId = task.getOwnerUserId() == null ? task.getCreatedBy() : task.getOwnerUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("异步任务缺少创建用户，无法恢复权限上下文");
        }
        AuthUserDetails userDetails = authUserDetailsService.loadUserById(userId);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
