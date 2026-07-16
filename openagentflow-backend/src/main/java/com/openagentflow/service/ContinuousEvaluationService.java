package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 资源持续评测服务。
 * 负责生成黄金基线、比较候选版本并保存可追溯的指标退化明细。
 */
@Service
public class ContinuousEvaluationService {

    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    public ContinuousEvaluationService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** 将一个成功评测任务固化为资源的当前黄金基线。 */
    @Transactional
    public Map<String, Object> createBaseline(String resourceType, String resourceId, String workspaceId,
                                               String evalTaskId, String baselineName, String resourceVersion,
                                               String userId) {
        assertTaskBelongsToResource(evalTaskId, resourceType, resourceId);
        MetricSnapshot snapshot = snapshot(evalTaskId);
        if (snapshot.sampleCount() == 0 || snapshot.metrics().isEmpty()) {
            throw new BusinessException("EVALUATION_BASELINE_EMPTY", "评测任务没有可用于创建基线的有效得分");
        }
        String id = UUID.randomUUID().toString();
        // 同一资源只保留一个生效基线，旧基线仍保留用于审计和回溯。
        jdbcTemplate.update("UPDATE evaluation_baseline SET active=0 WHERE resource_type=? AND resource_id=? AND active=1",
                resourceType, resourceId);
        jdbcTemplate.update("""
                INSERT INTO evaluation_baseline
                  (id,workspace_id,resource_type,resource_id,resource_version,eval_task_id,baseline_name,
                   metric_values,overall_score,sample_count,active,created_by,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,CAST(? AS JSON),?,?,1,?,NOW(3),NOW(3))
                """, id, workspaceId, resourceType, resourceId, resourceVersion, evalTaskId,
                baselineName, json(snapshot.metrics()), snapshot.overallScore(), snapshot.sampleCount(), userId);
        return jdbcTemplate.queryForMap("SELECT * FROM evaluation_baseline WHERE id=?", id);
    }

    /** 对资源最近一次成功评测执行黄金基线回归比较。 */
    @Transactional
    public RegressionResult compareLatest(String resourceType, String resourceId, String workspaceId,
                                           String targetVersion, double maxRegression, String userId) {
        List<Map<String, Object>> baselines = jdbcTemplate.queryForList("""
                SELECT * FROM evaluation_baseline
                WHERE resource_type=? AND resource_id=? AND active=1
                  AND (workspace_id=? OR workspace_id IS NULL)
                ORDER BY workspace_id IS NULL ASC,created_at DESC LIMIT 1
                """, resourceType, resourceId, workspaceId);
        if (baselines.isEmpty()) {
            return new RegressionResult(false, null, null, List.of(), "尚未创建黄金评测基线");
        }
        Map<String, Object> baseline = baselines.getFirst();
        String taskId = latestSuccessfulTask(resourceType, resourceId);
        if (taskId == null) {
            return new RegressionResult(false, String.valueOf(baseline.get("id")), null,
                    List.of(), "没有找到该资源的成功候选评测任务");
        }
        Map<String, Double> baselineMetrics = parseMetrics(baseline.get("metric_values"));
        MetricSnapshot candidate = snapshot(taskId);
        EvaluationRegressionPolicy.Result comparison = EvaluationRegressionPolicy.compare(
                baselineMetrics, candidate.metrics(), maxRegression);
        String regressionId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO evaluation_regression
                  (id,workspace_id,baseline_id,candidate_task_id,resource_type,resource_id,target_version,status,
                   baseline_metrics,candidate_metrics,regression_detail,created_by,created_at)
                VALUES (?,?,?,?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON),CAST(? AS JSON),?,NOW(3))
                """, regressionId, workspaceId, baseline.get("id"), taskId, resourceType, resourceId, targetVersion,
                comparison.passed() ? "passed" : "blocked", json(baselineMetrics), json(candidate.metrics()),
                json(comparison.regressions()), userId);
        String reason = comparison.passed() ? "候选版本未超过允许退化阈值" : "存在超过阈值的退化指标";
        return new RegressionResult(comparison.passed(), String.valueOf(baseline.get("id")),
                regressionId, comparison.regressions(), reason);
    }

    /** 查询资源的黄金基线列表。 */
    public List<Map<String, Object>> listBaselines(String resourceType, String resourceId) {
        return jdbcTemplate.queryForList("""
                SELECT * FROM evaluation_baseline WHERE resource_type=? AND resource_id=?
                ORDER BY active DESC,created_at DESC LIMIT 100
                """, resourceType, resourceId);
    }

    /** 聚合评测任务下每个指标的平均得分。 */
    private MetricSnapshot snapshot(String taskId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.metric_code,AVG(s.score) metric_value
                FROM eval_task_run r
                JOIN eval_score s ON s.task_run_id=r.id
                JOIN eval_metric m ON m.id=s.metric_id
                WHERE r.task_id=? AND r.status='success' AND s.score IS NOT NULL
                GROUP BY m.metric_code
                """, taskId);
        Map<String, Double> metrics = new LinkedHashMap<>();
        rows.forEach(row -> metrics.put(String.valueOf(row.get("metric_code")),
                ((Number) row.get("metric_value")).doubleValue()));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM eval_task_run WHERE task_id=? AND status='success'", Integer.class, taskId);
        double overall = metrics.values().stream().mapToDouble(Double::doubleValue).average().orElse(0D);
        return new MetricSnapshot(metrics, overall, count == null ? 0 : count);
    }

    /** 找出与资源类型匹配的最近成功任务。 */
    private String latestSuccessfulTask(String resourceType, String resourceId) {
        String predicate = switch (resourceType) {
            case "agent" -> "agent_id=?";
            case "workflow" -> "workflow_id=?";
            case "prompt" -> "prompt_template_id=?";
            case "knowledge" -> "JSON_UNQUOTE(JSON_EXTRACT(eval_config,'$.knowledgeBaseId'))=?";
            default -> throw new BusinessException("RELEASE_RESOURCE_TYPE_INVALID", "不支持的发布资源类型");
        };
        List<String> rows = jdbcTemplate.query(
                "SELECT id FROM eval_task WHERE status='success' AND " + predicate + " ORDER BY finished_at DESC LIMIT 1",
                (rs, rowNum) -> rs.getString(1), resourceId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** 确认基线来源任务与目标资源匹配，防止跨资源套用高分任务。 */
    private void assertTaskBelongsToResource(String taskId, String resourceType, String resourceId) {
        String latest = latestSuccessfulTask(resourceType, resourceId);
        if (latest == null) {
            throw new BusinessException("EVALUATION_TASK_NOT_FOUND", "目标资源没有成功评测任务");
        }
        Integer matched = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM eval_task WHERE id=? AND status='success'",
                Integer.class, taskId);
        if (matched == null || matched == 0) {
            throw new BusinessException("EVALUATION_TASK_INVALID", "指定评测任务不存在或尚未成功");
        }
        // 通过资源条件再次筛选，不能仅依赖任务状态。
        if (!taskId.equals(latest)) {
            String candidate = latestSuccessfulTaskForId(taskId, resourceType, resourceId);
            if (candidate == null) throw new BusinessException("EVALUATION_TASK_RESOURCE_MISMATCH", "评测任务不属于目标资源");
        }
    }

    private String latestSuccessfulTaskForId(String taskId, String resourceType, String resourceId) {
        String predicate = switch (resourceType) {
            case "agent" -> "agent_id=?";
            case "workflow" -> "workflow_id=?";
            case "prompt" -> "prompt_template_id=?";
            case "knowledge" -> "JSON_UNQUOTE(JSON_EXTRACT(eval_config,'$.knowledgeBaseId'))=?";
            default -> throw new BusinessException("RELEASE_RESOURCE_TYPE_INVALID", "不支持的发布资源类型");
        };
        List<String> rows = jdbcTemplate.query("SELECT id FROM eval_task WHERE id=? AND status='success' AND " + predicate,
                (rs, rowNum) -> rs.getString(1), taskId, resourceId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private Map<String, Double> parseMetrics(Object value) {
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<>() { });
        } catch (Exception exception) {
            throw new BusinessException("EVALUATION_BASELINE_CORRUPTED", "黄金基线指标无法解析");
        }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new BusinessException("JSON_SERIALIZE_FAILED", "治理数据序列化失败"); }
    }

    /** 评测指标快照。 */
    private record MetricSnapshot(Map<String, Double> metrics, double overallScore, int sampleCount) { }

    /** 黄金基线回归比较结果。 */
    public record RegressionResult(boolean passed, String baselineId, String regressionId,
                                   List<Map<String, Object>> regressions, String reason) { }
}
