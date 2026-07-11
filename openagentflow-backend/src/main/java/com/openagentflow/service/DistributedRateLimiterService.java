package com.openagentflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Redis 分布式速率与并发限制服务。
 */
@Service
public class DistributedRateLimiterService {

    /** 日志对象。 */
    private static final Logger log = LoggerFactory.getLogger(DistributedRateLimiterService.class);

    /** Redis 原子获取并发许可脚本。 */
    private static final DefaultRedisScript<Long> ACQUIRE_CONCURRENCY = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current >= tonumber(ARGV[1]) then return 0 end
            current = redis.call('INCR', KEYS[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return current
            """, Long.class);

    /** Redis 原子释放并发许可脚本。 */
    private static final DefaultRedisScript<Long> RELEASE_CONCURRENCY = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('DECR', KEYS[1]) or '0')
            if current <= 0 then redis.call('DEL', KEYS[1]) return 0 end
            return current
            """, Long.class);

    /** Redis 固定窗口 QPS 脚本。 */
    private static final DefaultRedisScript<Long> ACQUIRE_RATE = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('PEXPIRE', KEYS[1], 1000) end
            if current > tonumber(ARGV[1]) then return 0 end
            return current
            """, Long.class);

    /** Redis 客户端。 */
    private final StringRedisTemplate redisTemplate;

    /** Redis 不可用时的单实例并发保护。 */
    private final Map<String, Semaphore> localSemaphores = new ConcurrentHashMap<>();

    public DistributedRateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取一个可自动释放的分布式许可。
     */
    public Permit acquire(String resource, int qps, int maxConcurrency, Duration timeout) {
        String safeResource = resource.replaceAll("[^a-zA-Z0-9:_-]", "_");
        String concurrencyKey = "oaf:limit:concurrency:" + safeResource;
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                Long rateAllowed = redisTemplate.execute(ACQUIRE_RATE,
                        Collections.singletonList("oaf:limit:qps:" + safeResource),
                        String.valueOf(Math.max(1, qps)));
                if (rateAllowed != null && rateAllowed > 0) {
                    Long concurrencyAllowed = redisTemplate.execute(ACQUIRE_CONCURRENCY,
                            Collections.singletonList(concurrencyKey),
                            String.valueOf(Math.max(1, maxConcurrency)),
                            String.valueOf(Math.max(30_000L, timeout.toMillis() * 4)));
                    if (concurrencyAllowed != null && concurrencyAllowed > 0) {
                        return () -> redisTemplate.execute(RELEASE_CONCURRENCY, Collections.singletonList(concurrencyKey));
                    }
                }
            } catch (Exception exception) {
                // Redis 故障时仍使用本机信号量保护模型端点，避免无限并发。
                Semaphore semaphore = localSemaphores.computeIfAbsent(safeResource,
                        key -> new Semaphore(Math.max(1, maxConcurrency)));
                if (semaphore.tryAcquire()) {
                    log.warn("Redis 分布式限流不可用，已降级为本机并发限制：resource={}, error={}", safeResource, exception.getMessage());
                    return semaphore::release;
                }
            }
            sleep(50L);
        }
        throw new EmbeddingBackpressureException("Embedding 服务繁忙，等待分布式许可超时");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EmbeddingBackpressureException("Embedding 限流等待被中断");
        }
    }

    /**
     * 可自动释放的分布式许可。
     */
    @FunctionalInterface
    public interface Permit extends AutoCloseable {
        @Override
        void close();
    }
}
