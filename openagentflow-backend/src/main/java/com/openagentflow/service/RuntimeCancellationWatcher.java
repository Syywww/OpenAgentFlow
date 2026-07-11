package com.openagentflow.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Runtime实例分布式强取消观察器。 */
@Service
@ConditionalOnProperty(prefix = "openagentflow.runtime", name = "accept-traffic", havingValue = "true", matchIfMissing = true)
public class RuntimeCancellationWatcher {

    /** Redis客户端。 */
    private final StringRedisTemplate redisTemplate;

    /** 模型HTTP客户端。 */
    private final OpenAiCompatibleClient modelClient;

    public RuntimeCancellationWatcher(StringRedisTemplate redisTemplate, OpenAiCompatibleClient modelClient) {
        this.redisTemplate = redisTemplate;
        this.modelClient = modelClient;
    }

    /** 高频检查当前实例活动调用的停止令牌，并主动关闭HTTP请求。 */
    @Scheduled(fixedDelayString = "${openagentflow.runtime.cancel-watch-ms:500}")
    public void cancelRequestedCalls() {
        for (String runId : modelClient.activeRunIds()) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("oaf:runtime:cancel:" + runId))) {
                modelClient.cancel(runId);
            }
        }
    }
}
