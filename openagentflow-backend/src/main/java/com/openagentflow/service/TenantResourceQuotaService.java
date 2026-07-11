package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.time.Duration;
import java.util.UUID;

import java.util.Map;
import java.util.List;

/** 工作空间资源配额与基础设施隔离服务。 */
@Service
public class TenantResourceQuotaService {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 平台指标服务。 */
    private final PlatformMetricsService metricsService;

    /** Redis客户端，用于原子资源预占。 */
    private final StringRedisTemplate redisTemplate;

    public TenantResourceQuotaService(JdbcTemplate jdbcTemplate,
                                      PlatformMetricsService metricsService,
                                      StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.metricsService = metricsService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 校验工作空间是否仍有文档和存储配额。
     *
     * @param workspaceId 工作空间ID
     * @param uploadBytes 本次上传字节数
     */
    public void assertDocumentUploadAllowed(String workspaceId, long uploadBytes) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return;
        }
        Map<String, Object> quota = first("SELECT * FROM tenant_resource_quota WHERE workspace_id = ? AND enabled = 1", workspaceId);
        if (quota.isEmpty()) {
            return;
        }
        long maxUpload = number(quota.get("max_upload_bytes"));
        if (uploadBytes > maxUpload) {
            block(workspaceId, "upload_size", "文件超过工作空间单文件上传配额");
        }
        long documents = scalar("SELECT COUNT(1) FROM knowledge_document d JOIN knowledge_base k ON k.id=d.kb_id WHERE k.workspace_id = ?", workspaceId);
        if (documents >= number(quota.get("max_documents"))) {
            block(workspaceId, "document_count", "工作空间文档数量已达到配额");
        }
        long storage = scalar("SELECT COALESCE(SUM(d.file_size),0) FROM knowledge_document d JOIN knowledge_base k ON k.id=d.kb_id WHERE k.workspace_id = ?", workspaceId);
        if (storage + Math.max(0, uploadBytes) > number(quota.get("max_storage_bytes"))) {
            block(workspaceId, "storage", "工作空间对象存储用量已达到配额");
        }
    }

    /** 校验文件扩展名，阻止脚本和可执行文件进入解析链路。 */
    public void assertSafeDocumentType(String fileName) {
        String normalized = fileName == null ? "" : fileName.toLowerCase(java.util.Locale.ROOT);
        if (normalized.matches(".*\\.(exe|dll|bat|cmd|ps1|sh|jar|class|msi|scr|com)$")) {
            block(null, "dangerous_file_type", "不允许上传可执行文件或脚本文件");
        }
    }

    /**
     * 使用Redis Lua原子预占资源，避免并发请求同时穿透数据库配额检查。
     *
     * @param workspaceId 工作空间ID
     * @param resourceType 资源类型
     * @param resourceKey 业务幂等键
     * @param amount 预占数量
     * @param limit 配额上限
     * @param ttl 预占有效期
     * @return 预占ID
     */
    public String reserve(String workspaceId,
                          String resourceType,
                          String resourceKey,
                          long amount,
                          long limit,
                          Duration ttl) {
        String counterKey = "oaf:quota:" + workspaceId + ":" + resourceType;
        String reservationKey = counterKey + ":reservation:" + resourceKey;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>("""
                if redis.call('EXISTS', KEYS[2]) == 1 then return 1 end
                local current=tonumber(redis.call('GET',KEYS[1]) or '0')
                local amount=tonumber(ARGV[1]); local limit=tonumber(ARGV[2])
                if current+amount>limit then return 0 end
                redis.call('INCRBY',KEYS[1],amount)
                redis.call('SET',KEYS[2],amount,'PX',ARGV[3])
                return 1
                """, Long.class);
        Long allowed = redisTemplate.execute(script, List.of(counterKey, reservationKey),
                String.valueOf(Math.max(0, amount)), String.valueOf(Math.max(0, limit)), String.valueOf(Math.max(1000, ttl.toMillis())));
        if (!Long.valueOf(1).equals(allowed)) {
            block(workspaceId, resourceType, "工作空间资源并发预占超过配额");
        }
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT IGNORE INTO tenant_resource_reservation
                  (id,workspace_id,resource_type,resource_key,reserved_amount,status,expires_at,created_at,updated_at)
                VALUES (?,?,?,?,?,'reserved',DATE_ADD(NOW(3),INTERVAL ? SECOND),NOW(3),NOW(3))
                """, id, workspaceId, resourceType, resourceKey, amount, Math.max(1, ttl.toSeconds()));
        return id;
    }

    /** 提交资源预占，使其转为正式用量。 */
    public void commit(String reservationId) {
        jdbcTemplate.update("UPDATE tenant_resource_reservation SET status='committed',committed_at=NOW(3) WHERE id=? AND status='reserved'", reservationId);
    }

    private void block(String workspaceId, String reason, String message) {
        metricsService.incrementSecurityBlock(reason);
        jdbcTemplate.update("""
                INSERT INTO platform_security_event
                  (id,workspace_id,event_type,risk_level,resource_type,detail_json,handled,created_at)
                VALUES (UUID(),?,'resource_quota_block','high','knowledge_document',JSON_OBJECT('reason',?,'message',?),0,NOW(3))
                """, workspaceId, reason, message);
        throw new BusinessException("RESOURCE_QUOTA_EXCEEDED", message);
    }

    private Map<String, Object> first(String sql, Object... args) {
        var rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    private long scalar(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.MAX_VALUE;
    }
}
