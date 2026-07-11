package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime流式事件持久缓冲与断线续传服务。
 */
@Service
public class RuntimeEventStreamService {

    /** Redis字符串客户端。 */
    private final StringRedisTemplate redisTemplate;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    /** Runtime异步执行器。 */
    private final TaskExecutor taskExecutor;

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 当前实例发射器与Run ID的绑定。 */
    private final Map<SseEmitter, String> emitterRuns = new ConcurrentHashMap<>();

    public RuntimeEventStreamService(StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     @Qualifier("agentRuntimeExecutor") TaskExecutor taskExecutor,
                                     JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 绑定原始SSE连接。 */
    public void bind(SseEmitter emitter, String runId) {
        emitterRuns.put(emitter, runId);
        emitter.onCompletion(() -> emitterRuns.remove(emitter));
        emitter.onError(error -> emitterRuns.remove(emitter));
    }

    /** 持久化并发送事件，返回全局单调递增序号。 */
    public long publish(SseEmitter emitter, String name, Object data) {
        String runId = emitterRuns.get(emitter);
        long sequence = 0L;
        if (runId != null) {
            try {
                sequence = nextSequence(runId);
                append(runId, sequence, name, data);
            } catch (Exception ignored) {
                // 缓冲异常时保持原始SSE可用，Redis恢复后后续事件仍可继续写入。
            }
        }
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event().name(name)
                    .data(data, MediaType.APPLICATION_JSON);
            if (sequence > 0) event.id(String.valueOf(sequence));
            emitter.send(event);
        } catch (Exception ignored) {
            // 客户端断开不影响后台模型生成，事件已进入Redis，可由新连接续传。
        }
        return sequence;
    }

    /** 创建续传连接，先重放历史事件，再跟随运行中的新增事件。 */
    public SseEmitter resume(String runId, long afterSequence) {
        SseEmitter emitter = new SseEmitter(180_000L);
        taskExecutor.execute(() -> follow(runId, Math.max(0L, afterSequence), emitter));
        return emitter;
    }

    /** 按序轮询Redis缓冲，终态且无新事件时结束连接。 */
    private void follow(String runId, long afterSequence, SseEmitter emitter) {
        long cursor = afterSequence;
        try {
            while (true) {
                List<Map<String, Object>> events = replay(runId, cursor);
                for (Map<String, Object> event : events) {
                    long sequence = ((Number) event.get("sequence")).longValue();
                    emitter.send(SseEmitter.event().id(String.valueOf(sequence))
                            .name(String.valueOf(event.get("name")))
                            .data(event.get("data"), MediaType.APPLICATION_JSON));
                    cursor = sequence;
                }
                if (events.isEmpty() && isTerminal(runId)) break;
                Thread.sleep(300L);
            }
            emitter.complete();
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    /** 读取指定序号之后的缓冲事件。 */
    private List<Map<String, Object>> replay(String runId, long afterSequence) {
        List<String> entries = redisTemplate.opsForList().range(eventKey(runId), 0, -1);
        if (entries == null) return List.of();
        return entries.stream().map(this::readEvent)
                .filter(item -> ((Number) item.getOrDefault("sequence", 0L)).longValue() > afterSequence)
                .toList();
    }

    /** 原子生成Run内事件序号。 */
    private long nextSequence(String runId) {
        Long value = redisTemplate.opsForValue().increment(sequenceKey(runId));
        redisTemplate.expire(sequenceKey(runId), Duration.ofHours(6));
        return value == null ? 1L : value;
    }

    /** 写入有界Redis列表，防止长会话无限占用内存。 */
    private void append(String runId, long sequence, String name, Object data) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("sequence", sequence);
            event.put("name", name);
            event.put("data", data);
            String key = eventKey(runId);
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(event));
            redisTemplate.opsForList().trim(key, -4000, -1);
            redisTemplate.expire(key, Duration.ofHours(6));
        } catch (Exception exception) {
            throw new IllegalStateException("Runtime事件缓冲写入失败：" + exception.getMessage(), exception);
        }
    }

    /** 解析单条事件。 */
    private Map<String, Object> readEvent(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /** 判断Run是否已经进入终态。 */
    private boolean isTerminal(String runId) {
        List<String> statuses = jdbcTemplate.query("SELECT status FROM runtime_run WHERE id=?",
                (rs, rowNum) -> rs.getString(1), runId);
        return statuses.isEmpty() || List.of("success", "failed", "cancelled", "timeout").contains(statuses.getFirst().toLowerCase());
    }

    private String eventKey(String runId) { return "oaf:runtime:events:" + runId; }
    private String sequenceKey(String runId) { return "oaf:runtime:event-seq:" + runId; }
}
