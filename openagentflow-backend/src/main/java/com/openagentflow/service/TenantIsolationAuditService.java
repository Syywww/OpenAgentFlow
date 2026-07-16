package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.security.TenantIsolationPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** MySQL 与跨存储租户命名空间隔离巡检服务。 */
@Service
public class TenantIsolationAuditService {

    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    public TenantIsolationAuditService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** 扫描核心表字段和跨存储命名约束，并保存开放问题。 */
    public List<Map<String, Object>> scan() {
        List<Map<String, Object>> issues = new ArrayList<>();
        for (String table : jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema=DATABASE() AND table_type='BASE TABLE'
                """, String.class)) {
            if (!TenantIsolationPolicy.requiresTenantCondition(table)) continue;
            Integer columns = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM information_schema.columns
                    WHERE table_schema=DATABASE() AND table_name=? AND column_name='workspace_id'
                    """, Integer.class, table);
            if (columns == null || columns == 0) issues.add(save("mysql", table, "workspace_column_missing", "critical",
                    Map.of("table", table, "expectedColumn", "workspace_id")));
        }
        // 跨存储统一采用 oaf/{workspaceId}/... 或 oaf_{workspaceId}_...，实际客户端在写入时复用该约定。
        for (String scope : List.of("redis", "milvus", "minio", "opensearch", "kafka")) {
            issues.add(Map.of("auditScope", scope, "status", "policy_enabled",
                    "namespacePattern", scope.equals("minio") ? "oaf/{workspaceId}/..." : "oaf_{workspaceId}_..."));
        }
        return issues;
    }

    /** 查询未处理隔离问题。 */
    public List<Map<String, Object>> openIssues() {
        return jdbcTemplate.queryForList("SELECT * FROM tenant_isolation_audit WHERE status='open' ORDER BY detected_at DESC LIMIT 200");
    }

    private Map<String, Object> save(String scope, String resource, String type, String severity, Map<String, Object> evidence) {
        String id = UUID.randomUUID().toString();
        try {
            jdbcTemplate.update("""
                    INSERT INTO tenant_isolation_audit
                      (id,audit_scope,resource_type,issue_type,severity,evidence_json,status,detected_at,created_at,updated_at)
                    VALUES (?,?,?,?,?,CAST(? AS JSON),'open',NOW(3),NOW(3),NOW(3))
                    """, id, scope, resource, type, severity, objectMapper.writeValueAsString(evidence));
        } catch (Exception exception) { throw new IllegalStateException("租户隔离巡检结果保存失败", exception); }
        return jdbcTemplate.queryForMap("SELECT * FROM tenant_isolation_audit WHERE id=?", id);
    }
}
