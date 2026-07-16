package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 单次请求资源画像与 SRE 诊断服务。 */
@Service
public class SreDiagnosticsService {

    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;

    public SreDiagnosticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 汇总一次运行的 LLM、RAG、工具、异步任务和 Trace 消耗。
     * 所有明细都由 runId 关联，便于从告警直接定位到瓶颈步骤。
     */
    public Map<String, Object> runResourceSummary(String runId) {
        List<Map<String, Object>> runs = jdbcTemplate.queryForList("""
                SELECT r.*,a.workspace_id FROM runtime_run r
                LEFT JOIN agent a ON a.id=r.agent_id WHERE r.id=? LIMIT 1
                """, runId);
        if (runs.isEmpty()) throw new BusinessException("RUN_NOT_FOUND", "运行不存在");
        Map<String, Object> run = runs.getFirst();
        String workspace = WorkspaceContextHolder.current();
        if (workspace != null && run.get("workspace_id") != null && !workspace.equals(String.valueOf(run.get("workspace_id")))) {
            throw new BusinessException("WORKSPACE_RESOURCE_FORBIDDEN", "运行不属于当前工作空间");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", metadataValue(run.get("metadata"), "requestId"));
        result.put("traceId", metadataValue(run.get("metadata"), "traceId"));
        result.put("runId", runId);
        result.put("workflowRunId", run.get("workflow_run_id"));
        result.put("status", run.get("status"));
        result.put("totalLatencyMs", run.get("latency_ms"));
        result.put("totalTokens", run.get("total_tokens"));
        result.put("totalCost", run.get("total_cost"));
        result.put("llm", one("""
                SELECT COUNT(1) calls,COALESCE(SUM(total_tokens),0) tokens,COALESCE(SUM(cost_amount),0) cost,
                       COALESCE(AVG(latency_ms),0) avgLatencyMs,COALESCE(MAX(latency_ms),0) maxLatencyMs,
                       COALESCE(SUM(success=0),0) failures FROM runtime_llm_call WHERE run_id=?
                """, runId));
        result.put("traceByType", jdbcTemplate.queryForList("""
                SELECT step_type,COUNT(1) stepCount,COALESCE(SUM(latency_ms),0) totalLatencyMs,
                       COALESCE(MAX(latency_ms),0) maxLatencyMs,COALESCE(SUM(status IN ('FAILED','ERROR')),0) failures
                FROM runtime_trace_step WHERE run_id=? GROUP BY step_type ORDER BY totalLatencyMs DESC
                """, runId));
        result.put("rag", one("""
                SELECT COUNT(1) steps,COALESCE(SUM(latency_ms),0) totalLatencyMs,COALESCE(MAX(latency_ms),0) maxLatencyMs
                FROM runtime_trace_step WHERE run_id=? AND UPPER(step_type) IN ('RAG','RETRIEVAL','KNOWLEDGE')
                """, runId));
        result.put("tools", one("""
                SELECT COUNT(1) calls,COALESCE(SUM(latency_ms),0) totalLatencyMs,
                       COALESCE(SUM(success=0),0) failures FROM tool_invocation_log WHERE run_id=?
                """, runId));
        result.put("tasks", jdbcTemplate.queryForList("""
                SELECT id taskId,task_type,status,retry_count,TIMESTAMPDIFF(MICROSECOND,created_at,
                       COALESCE(started_at,NOW(3)))/1000 queueWaitMs
                FROM async_task WHERE business_id=? OR id IN (
                  SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.taskId')) FROM runtime_run WHERE id=?
                ) ORDER BY created_at
                """, runId, runId));
        return result;
    }

    /** 查询当前工作空间最近一小时的四项黄金信号。 */
    public Map<String, Object> goldenSignals() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runtime", one("""
                SELECT COUNT(1) requests,COALESCE(AVG(latency_ms),0) avgLatencyMs,
                       COALESCE(SUM(status IN ('FAILED','ERROR'))/NULLIF(COUNT(1),0),0) errorRate,
                       COALESCE(SUM(total_tokens),0) tokens FROM runtime_run
                WHERE created_at>=DATE_SUB(NOW(3),INTERVAL 1 HOUR)
                """));
        result.put("asyncTasks", one("""
                SELECT COUNT(1) tasks,COALESCE(SUM(status='failed'),0) failures,
                       COALESCE(SUM(status IN ('pending','retrying')),0) backlog FROM async_task
                WHERE created_at>=DATE_SUB(NOW(3),INTERVAL 1 HOUR)
                """));
        return result;
    }

    private Map<String, Object> one(String sql, Object... args) {
        return jdbcTemplate.queryForMap(sql, args);
    }

    private Object metadataValue(Object metadata, String key) {
        if (metadata == null) return null;
        List<String> values = jdbcTemplate.query("SELECT JSON_UNQUOTE(JSON_EXTRACT(CAST(? AS JSON),?))",
                (rs, rowNum) -> rs.getString(1), String.valueOf(metadata), "$." + key);
        return values.isEmpty() ? null : values.getFirst();
    }
}
