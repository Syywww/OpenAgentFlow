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

    /** 模型聊天客户端路由，覆盖所有协议（HTTP / 星火 WebSocket）。 */
    private final ModelChatClientRouter chatClientRouter;

    public RuntimeCancellationWatcher(StringRedisTemplate redisTemplate, ModelChatClientRouter chatClientRouter) {
        this.redisTemplate = redisTemplate;
        this.chatClientRouter = chatClientRouter;
    }

    /** 高频检查当前实例活动调用的停止令牌，并主动关闭各协议连接。 */
    @Scheduled(fixedDelayString = "${openagentflow.runtime.cancel-watch-ms:500}")
    public void cancelRequestedCalls() {
        for (String runId : chatClientRouter.activeRunIds()) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("oaf:runtime:cancel:" + runId))) {
                chatClientRouter.cancel(runId);
            }
        }
    }
}
