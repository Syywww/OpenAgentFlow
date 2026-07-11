package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Agent、Prompt、工作流和知识索引发布质量门禁服务。 */
@Service
public class ReleaseGateService {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    public ReleaseGateService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /**
     * 执行发布门禁，不达标时写入明细并阻止发布事务。
     *
     * @return 门禁执行数据
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessException.class)
    public Map<String, Object> assertCanRelease(String resourceType,
                                                String resourceId,
                                                String workspaceId,
                                                String targetVersion) {
        List<Map<String, Object>> waivers = jdbcTemplate.queryForList("""
                SELECT * FROM release_gate_waiver WHERE resource_type=? AND resource_id=?
                  AND status='approved' AND expires_at>NOW(3) ORDER BY approved_at DESC LIMIT 1
                """, resourceType, resourceId);
        if (!waivers.isEmpty()) {
            return Map.of("passed", true, "waived", true, "waiverId", waivers.getFirst().get("id"),
                    "reason", String.valueOf(waivers.getFirst().get("reason")));
        }
        List<Map<String, Object>> policies = jdbcTemplate.queryForList("""
                SELECT * FROM release_gate_policy WHERE resource_type=? AND enabled=1
                  AND (workspace_id=? OR workspace_id IS NULL)
                ORDER BY workspace_id IS NULL ASC LIMIT 1
                """, resourceType, workspaceId);
        if (policies.isEmpty()) return Map.of("passed", true, "reason", "未配置发布门禁");
        Map<String, Object> policy = policies.getFirst();
        Metrics metrics = "agent".equals(resourceType) ? agentMetrics(resourceId) : new Metrics(1D, 0D, 0, true, true, true);
        double minScore = number(policy.get("min_eval_score"));
        double maxFailure = number(policy.get("max_failure_rate"));
        int maxLatency = ((Number) policy.get("max_p95_latency_ms")).intValue();
        boolean requireSecurity = Boolean.TRUE.equals(policy.get("require_security_pass"))
                || policy.get("require_security_pass") instanceof Number number && number.intValue() == 1;
        boolean requireCost = Boolean.TRUE.equals(policy.get("require_cost_budget"))
                || policy.get("require_cost_budget") instanceof Number number && number.intValue() == 1;
        boolean passed = metrics.evalScore >= minScore && metrics.failureRate <= maxFailure
                && metrics.p95LatencyMs <= maxLatency
                && (!requireSecurity || metrics.securityPassed) && (!requireCost || metrics.costPassed);
        String executionId = UUID.randomUUID().toString();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("minEvalScore", minScore);
        detail.put("maxFailureRate", maxFailure);
        detail.put("maxP95LatencyMs", maxLatency);
        detail.put("sampleAvailable", metrics.sampleAvailable);
        jdbcTemplate.update("""
                INSERT INTO release_gate_execution
                  (id,policy_id,workspace_id,resource_type,resource_id,target_version,status,eval_score,failure_rate,
                   p95_latency_ms,security_passed,cost_passed,detail_json,requested_by,finished_at,created_at)
                VALUES (?,?,?,?,?,?, ?,?,?,?,?,?,?,?,NOW(3),NOW(3))
                """, executionId, policy.get("id"), workspaceId, resourceType, resourceId, targetVersion,
                passed ? "passed" : "blocked", metrics.evalScore, metrics.failureRate, metrics.p95LatencyMs,
                metrics.securityPassed, metrics.costPassed, json(detail), currentUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executionId", executionId);
        result.put("passed", passed);
        result.put("evalScore", metrics.evalScore);
        result.put("failureRate", metrics.failureRate);
        result.put("p95LatencyMs", metrics.p95LatencyMs);
        result.put("securityPassed", metrics.securityPassed);
        result.put("costPassed", metrics.costPassed);
        if (!passed) throw new BusinessException("RELEASE_GATE_BLOCKED", "发布门禁未通过，请先完成评测并处理安全、稳定性或成本问题");
        return result;
    }

    /** 查询资源最近门禁执行。 */
    public List<Map<String, Object>> executions(String resourceType, String resourceId) {
        return jdbcTemplate.queryForList("SELECT * FROM release_gate_execution WHERE resource_type=? AND resource_id=? ORDER BY created_at DESC LIMIT 100", resourceType, resourceId);
    }

    /** 创建限时发布豁免申请，默认待审批。 */
    public Map<String, Object> createWaiver(String resourceType, String resourceId, String reason, Integer hours) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO release_gate_waiver
                  (id,resource_type,resource_id,reason,status,requested_by,expires_at,created_at)
                VALUES (?,?,?,?,'pending',?,DATE_ADD(NOW(3),INTERVAL ? HOUR),NOW(3))
                """, id, resourceType, resourceId, reason, currentUserId(), Math.max(1, Math.min(72, hours == null ? 8 : hours)));
        return jdbcTemplate.queryForMap("SELECT * FROM release_gate_waiver WHERE id=?", id);
    }

    /** 审批发布豁免，仅由权限层允许的治理管理员调用。 */
    public Map<String, Object> approveWaiver(String id) {
        jdbcTemplate.update("UPDATE release_gate_waiver SET status='approved',approved_by=?,approved_at=NOW(3) WHERE id=? AND status='pending'",
                currentUserId(), id);
        return jdbcTemplate.queryForMap("SELECT * FROM release_gate_waiver WHERE id=?", id);
    }

    private Metrics agentMetrics(String agentId) {
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList("SELECT id FROM eval_task WHERE agent_id=? AND status='success' ORDER BY finished_at DESC LIMIT 1", agentId);
        if (tasks.isEmpty()) return new Metrics(0D, 1D, Integer.MAX_VALUE, false, true, false);
        String taskId = String.valueOf(tasks.getFirst().get("id"));
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COALESCE(AVG(s.score),0) eval_score,
                       COALESCE(SUM(r.status<>'success')/NULLIF(COUNT(DISTINCT r.id),0),1) failure_rate,
                       COALESCE(AVG(r.latency_ms),0) p95_latency
                FROM eval_task_run r LEFT JOIN eval_score s ON s.task_run_id=r.id WHERE r.task_id=?
                """, taskId);
        Long securityRisks = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ai_guardrail_event WHERE agent_id=? AND action_type='block' AND created_at>=DATE_SUB(NOW(),INTERVAL 30 DAY)", Long.class, agentId);
        Long exceededCostQuotas = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM model_usage_quota
                WHERE enabled=1 AND cost_limit IS NOT NULL AND cost_limit>0 AND cost_used>=cost_limit
                  AND ((UPPER(subject_type)='AGENT' AND subject_id=?) OR UPPER(subject_type) IN ('GLOBAL','SYSTEM'))
                """, Long.class, agentId);
        Long sampleCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM eval_task_run WHERE task_id=?", Long.class, taskId);
        int p95Offset = Math.max(0, (int) Math.ceil((sampleCount == null ? 0L : sampleCount) * 0.95D) - 1);
        List<Integer> p95Rows = jdbcTemplate.query("SELECT latency_ms FROM eval_task_run WHERE task_id=? ORDER BY latency_ms LIMIT 1 OFFSET ?",
                (rs, rowNum) -> rs.getInt(1), taskId, p95Offset);
        Integer p95 = p95Rows.isEmpty() ? 0 : p95Rows.getFirst();
        return new Metrics(number(row.get("eval_score")), number(row.get("failure_rate")),
                p95 == null ? 0 : p95,
                securityRisks == null || securityRisks == 0,
                exceededCostQuotas == null || exceededCostQuotas == 0, true);
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails
                ? userDetails.getUserId() : null;
    }
    private double number(Object value) { return value instanceof Number n ? n.doubleValue() : 0D; }
    private String json(Object value) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value); }
        catch (Exception ignored) { return "{}"; }
    }

    /** 门禁计算指标。 */
    private record Metrics(double evalScore, double failureRate, int p95LatencyMs,
                           boolean securityPassed, boolean costPassed, boolean sampleAvailable) { }
}
