package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Agent Runtime 分布式控制服务。
 *
 * <p>取消令牌写入Redis以便任意Runtime实例立即读取，同时写入MySQL保证控制动作可审计。</p>
 */
@Service
public class RuntimeControlService {

    /** 取消令牌键前缀。 */
    private static final String CANCEL_KEY_PREFIX = "oaf:runtime:cancel:";

    /** Redis客户端。 */
    private final StringRedisTemplate redisTemplate;

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 模型聊天客户端路由，用于主动关闭当前 JVM 中所有协议的活动调用。 */
    private final ModelChatClientRouter chatClientRouter;

    public RuntimeControlService(StringRedisTemplate redisTemplate,
                                 JdbcTemplate jdbcTemplate,
                                 ModelChatClientRouter chatClientRouter) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.chatClientRouter = chatClientRouter;
    }

    /**
     * 请求停止指定运行。
     *
     * @param runId 运行ID
     * @return 控制结果
     */
    public Map<String, Object> cancel(String runId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM runtime_run WHERE id = ?", Integer.class, runId);
        if (count == null || count == 0) {
            throw new BusinessException("RUNTIME_RUN_NOT_FOUND", "运行不存在");
        }
        String commandId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CANCEL_KEY_PREFIX + runId, "1", Duration.ofHours(6));
        boolean localCallInterrupted = chatClientRouter.cancel(runId);
        jdbcTemplate.update("UPDATE runtime_run SET cancel_requested = 1 WHERE id = ? AND status IN ('RUNNING','running')", runId);
        jdbcTemplate.update("""
                INSERT INTO runtime_control_command
                  (id, run_id, command_type, command_payload, status, requested_by, created_at)
                VALUES (?, ?, 'cancel', JSON_OBJECT('source', 'api'), 'pending', ?, NOW(3))
                """, commandId, runId, currentUserId());
        return Map.of("runId", runId, "commandId", commandId, "cancelRequested", true,
                "localCallInterrupted", localCallInterrupted);
    }

    /**
     * 检查运行是否被请求停止。
     *
     * @param runId 运行ID
     * @return 是否应停止
     */
    public boolean isCancellationRequested(String runId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CANCEL_KEY_PREFIX + runId));
    }

    /**
     * Runtime确认控制指令已生效。
     *
     * @param runId 运行ID
     * @param executorId 执行器ID
     */
    public void acknowledgeCancellation(String runId, String executorId) {
        jdbcTemplate.update("""
                UPDATE runtime_control_command
                SET status = 'applied', applied_by = ?, applied_at = NOW(3)
                WHERE run_id = ? AND command_type = 'cancel' AND status = 'pending'
                """, executorId, runId);
    }

    /** 获取当前登录用户ID。 */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
