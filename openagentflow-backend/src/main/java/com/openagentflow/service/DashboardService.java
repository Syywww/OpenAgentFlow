package com.openagentflow.service;

import com.openagentflow.domain.DashboardOverview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台全量概览服务。
 */
@Service
public class DashboardService {

    /** SQL 查询组件，用于跨多张业务表汇总工作台数据。 */
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询工作台全量概览数据。
     *
     * @return 工作台全量概览对象
     */
    public DashboardOverview getOverview() {
        DashboardOverview overview = new DashboardOverview();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        // 先聚合平台资源数量，保证顶部指标全部来自真实业务表。
        fillResourceMetrics(overview);
        // 再聚合今日运行质量和成本，这是工作台运营判断的核心数据。
        fillTodayRuntimeMetrics(overview, todayStart, tomorrowStart);
        // 补充任务、告警和健康状态，便于首页直接发现阻塞项。
        fillOperationalMetrics(overview);
        // 填充可钻取列表和趋势数据，前端不再依赖 mock。
        overview.setKnowledgeHealth(queryKnowledgeHealth());
        overview.setRunTrend(queryRunTrend(today.minusDays(6), tomorrowStart));
        overview.setRecentRuns(queryRecentRuns());
        overview.setModelUsage(queryModelUsage(today.minusDays(6).atStartOfDay()));
        overview.setTaskQueue(queryTaskQueue());
        overview.setOpenAlerts(queryOpenAlerts());
        overview.setHealthChecks(queryHealthChecks());
        overview.setInsights(buildInsights(overview));
        return overview;
    }

    /**
     * 填充平台资源指标。
     *
     * @param overview 工作台概览对象
     */
    private void fillResourceMetrics(DashboardOverview overview) {
        overview.setAgentCount(count("SELECT COUNT(1) FROM agent WHERE deleted_at IS NULL"));
        overview.setPublishedAgentCount(count("SELECT COUNT(1) FROM agent WHERE deleted_at IS NULL AND status = 'published'"));
        overview.setKnowledgeBaseCount(count("SELECT COUNT(1) FROM knowledge_base WHERE deleted_at IS NULL"));
        overview.setToolCount(count("SELECT COUNT(1) FROM tool_definition WHERE deleted_at IS NULL"));
        overview.setEnabledToolCount(count("""
                SELECT COUNT(1)
                FROM tool_definition
                WHERE deleted_at IS NULL
                  AND (enabled = 1 OR status IN ('enabled', 'active'))
                """));
        overview.setMcpServerCount(count("SELECT COUNT(1) FROM mcp_server WHERE deleted_at IS NULL"));
        overview.setWorkflowCount(count("SELECT COUNT(1) FROM workflow_definition WHERE deleted_at IS NULL"));
    }

    /**
     * 填充今日运行指标。
     *
     * @param overview 工作台概览对象
     * @param start 今日开始时间
     * @param end 明日开始时间
     */
    private void fillTodayRuntimeMetrics(DashboardOverview overview, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COUNT(1) AS run_count,
                       COALESCE(SUM(CASE WHEN UPPER(status) = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_count,
                       COALESCE(SUM(CASE WHEN UPPER(status) = 'FAILED' THEN 1 ELSE 0 END), 0) AS failure_count,
                       COALESCE(SUM(total_tokens), 0) AS token_count,
                       COALESCE(SUM(total_cost), 0) AS cost_amount,
                       COALESCE(AVG(latency_ms), 0) AS avg_latency_ms
                FROM runtime_run
                WHERE created_at >= ? AND created_at < ?
                """, start, end);
        long runCount = longValue(row.get("run_count"));
        long successCount = longValue(row.get("success_count"));
        overview.setTodayRunCount(runCount);
        overview.setTodaySuccessCount(successCount);
        overview.setTodayFailureCount(longValue(row.get("failure_count")));
        overview.setTodayTokenCount(longValue(row.get("token_count")));
        overview.setTodayCost(decimalValue(row.get("cost_amount")));
        overview.setTodayAvgLatencyMs(decimalValue(row.get("avg_latency_ms")).setScale(0, RoundingMode.HALF_UP));
        overview.setTodaySuccessRate(percent(successCount, runCount));
    }

    /**
     * 填充运营侧指标。
     *
     * @param overview 工作台概览对象
     */
    private void fillOperationalMetrics(DashboardOverview overview) {
        overview.setTaskBacklogCount(count("SELECT COUNT(1) FROM async_task WHERE status IN ('pending', 'running')"));
        overview.setOpenAlertCount(count("SELECT COUNT(1) FROM ops_alert_event WHERE status IN ('open', 'acknowledged')"));
        overview.setUnhealthyComponentCount(count("""
                SELECT COUNT(1)
                FROM ops_health_check
                WHERE enabled = 1
                  AND status IN ('warning', 'unhealthy', 'failed')
                """));
    }

    /**
     * 查询知识库健康概览。
     *
     * @return 知识库健康概览
     */
    private DashboardOverview.KnowledgeHealth queryKnowledgeHealth() {
        DashboardOverview.KnowledgeHealth health = new DashboardOverview.KnowledgeHealth();
        health.setDocumentCount(count("SELECT COUNT(1) FROM knowledge_document"));
        health.setParsedDocumentCount(count("SELECT COUNT(1) FROM knowledge_document WHERE parse_status = 'parsed'"));
        health.setFailedDocumentCount(count("SELECT COUNT(1) FROM knowledge_document WHERE parse_status = 'failed'"));
        health.setProcessingDocumentCount(count("SELECT COUNT(1) FROM knowledge_document WHERE parse_status IN ('pending', 'processing')"));
        health.setChunkCount(count("SELECT COUNT(1) FROM knowledge_chunk"));
        health.setEmbeddingCount(count("SELECT COUNT(1) FROM knowledge_embedding"));
        health.setOpenIssueCount(count("SELECT COUNT(1) FROM knowledge_governance_issue WHERE status = 'open'"));
        health.setHighRiskIssueCount(count("""
                SELECT COUNT(1)
                FROM knowledge_governance_issue
                WHERE status = 'open' AND severity IN ('high', 'critical')
                """));
        health.setUnsyncedEmbeddingCount(count("""
                SELECT COUNT(1)
                FROM knowledge_embedding
                WHERE sync_status IS NULL OR sync_status NOT IN ('synced', 'success')
                """));
        return health;
    }

    /**
     * 查询最近 7 天运行趋势。
     *
     * @param startDate 起始日期
     * @param endTime 结束时间
     * @return 运行趋势列表
     */
    private List<DashboardOverview.RunTrendItem> queryRunTrend(LocalDate startDate, LocalDateTime endTime) {
        Map<LocalDate, DashboardOverview.RunTrendItem> trendMap = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            DashboardOverview.RunTrendItem empty = new DashboardOverview.RunTrendItem();
            empty.setStatDate(date);
            empty.setRunCount(0L);
            empty.setSuccessCount(0L);
            empty.setFailureCount(0L);
            empty.setTokenCount(0L);
            empty.setCostAmount(BigDecimal.ZERO);
            trendMap.put(date, empty);
        }
        jdbcTemplate.query("""
                SELECT DATE(created_at) AS stat_date,
                       COUNT(1) AS run_count,
                       COALESCE(SUM(CASE WHEN UPPER(status) = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_count,
                       COALESCE(SUM(CASE WHEN UPPER(status) = 'FAILED' THEN 1 ELSE 0 END), 0) AS failure_count,
                       COALESCE(SUM(total_tokens), 0) AS token_count,
                       COALESCE(SUM(total_cost), 0) AS cost_amount
                FROM runtime_run
                WHERE created_at >= ? AND created_at < ?
                GROUP BY DATE(created_at)
                ORDER BY stat_date
                """, (rs) -> {
            LocalDate date = toLocalDate(rs.getObject("stat_date"));
            DashboardOverview.RunTrendItem item = trendMap.get(date);
            if (item != null) {
                // 只覆盖查询命中的日期，未命中的日期保留 0 值，前端趋势不会断档。
                item.setRunCount(rs.getLong("run_count"));
                item.setSuccessCount(rs.getLong("success_count"));
                item.setFailureCount(rs.getLong("failure_count"));
                item.setTokenCount(rs.getLong("token_count"));
                item.setCostAmount(nullToZero(rs.getBigDecimal("cost_amount")));
            }
        }, startDate.atStartOfDay(), endTime);
        return startDate.datesUntil(startDate.plusDays(7)).map(trendMap::get).toList();
    }

    /**
     * 查询最近运行记录。
     *
     * @return 最近运行列表
     */
    private List<DashboardOverview.RecentRunItem> queryRecentRuns() {
        return jdbcTemplate.query("""
                SELECT r.id,
                       r.run_no,
                       r.run_type,
                       COALESCE(a.agent_name, w.workflow_name, r.run_type, '未命名运行') AS target_name,
                       COALESCE(u.display_name, u.username, r.user_id, '系统') AS user_name,
                       r.status,
                       r.total_tokens,
                       r.total_cost,
                       r.latency_ms,
                       r.error_message,
                       r.started_at,
                       r.finished_at
                FROM runtime_run r
                LEFT JOIN agent a ON a.id = r.agent_id
                LEFT JOIN workflow_definition w ON w.id = r.workflow_id
                LEFT JOIN iam_user u ON u.id = r.user_id
                ORDER BY COALESCE(r.started_at, r.created_at) DESC
                LIMIT 10
                """, (rs, rowNum) -> {
            DashboardOverview.RecentRunItem item = new DashboardOverview.RecentRunItem();
            item.setId(rs.getString("id"));
            item.setRunNo(rs.getString("run_no"));
            item.setRunType(rs.getString("run_type"));
            item.setTargetName(rs.getString("target_name"));
            item.setUserName(rs.getString("user_name"));
            item.setStatus(rs.getString("status"));
            item.setStatusLabel(statusLabel(rs.getString("status")));
            item.setTotalTokens(rs.getLong("total_tokens"));
            item.setTotalCost(nullToZero(rs.getBigDecimal("total_cost")));
            item.setLatencyMs(rs.getLong("latency_ms"));
            item.setErrorMessage(rs.getString("error_message"));
            item.setStartedAt(toLocalDateTime(rs.getTimestamp("started_at")));
            item.setFinishedAt(toLocalDateTime(rs.getTimestamp("finished_at")));
            return item;
        });
    }

    /**
     * 查询模型使用排行。
     *
     * @param startTime 起始时间
     * @return 模型使用排行列表
     */
    private List<DashboardOverview.ModelUsageItem> queryModelUsage(LocalDateTime startTime) {
        long totalCalls = count("SELECT COUNT(1) FROM runtime_llm_call WHERE created_at >= ?", startTime);
        return jdbcTemplate.query("""
                SELECT COALESCE(c.model_id, 'unknown') AS model_id,
                       COALESCE(mc.model_name, mc.model_code, c.model_id, '未知模型') AS model_name,
                       COALESCE(mp.provider_name, c.provider_id, '未知服务商') AS provider_name,
                       COUNT(1) AS call_count,
                       COALESCE(SUM(CASE WHEN c.success = 1 THEN 1 ELSE 0 END), 0) AS success_count,
                       COALESCE(SUM(CASE WHEN c.success = 0 THEN 1 ELSE 0 END), 0) AS failure_count,
                       COALESCE(SUM(c.total_tokens), 0) AS total_tokens,
                       COALESCE(SUM(c.cost_amount), 0) AS total_cost,
                       COALESCE(AVG(c.latency_ms), 0) AS avg_latency_ms
                FROM runtime_llm_call c
                LEFT JOIN model_config mc ON mc.id = c.model_id
                LEFT JOIN model_provider mp ON mp.id = c.provider_id
                WHERE c.created_at >= ?
                GROUP BY c.model_id, mc.model_name, mc.model_code, mp.provider_name, c.provider_id
                ORDER BY call_count DESC
                LIMIT 6
                """, (rs, rowNum) -> {
            DashboardOverview.ModelUsageItem item = new DashboardOverview.ModelUsageItem();
            long callCount = rs.getLong("call_count");
            item.setModelId(rs.getString("model_id"));
            item.setModelName(rs.getString("model_name"));
            item.setProviderName(rs.getString("provider_name"));
            item.setCallCount(callCount);
            item.setSuccessCount(rs.getLong("success_count"));
            item.setFailureCount(rs.getLong("failure_count"));
            item.setTotalTokens(rs.getLong("total_tokens"));
            item.setTotalCost(nullToZero(rs.getBigDecimal("total_cost")));
            item.setAvgLatencyMs(nullToZero(rs.getBigDecimal("avg_latency_ms")).setScale(0, RoundingMode.HALF_UP));
            item.setUsagePercent(percent(callCount, totalCalls));
            return item;
        }, startTime);
    }

    /**
     * 查询排队和运行中的任务。
     *
     * @return 任务队列列表
     */
    private List<DashboardOverview.TaskQueueItem> queryTaskQueue() {
        return jdbcTemplate.query("""
                SELECT id,
                       task_name,
                       task_type,
                       status,
                       progress_percent,
                       current_message,
                       created_at,
                       updated_at
                FROM async_task
                WHERE status IN ('pending', 'running')
                ORDER BY CASE WHEN status = 'running' THEN 0 ELSE 1 END,
                         priority DESC,
                         created_at ASC
                LIMIT 6
                """, (rs, rowNum) -> {
            DashboardOverview.TaskQueueItem item = new DashboardOverview.TaskQueueItem();
            item.setId(rs.getString("id"));
            item.setTaskName(rs.getString("task_name"));
            item.setTaskType(rs.getString("task_type"));
            item.setStatus(rs.getString("status"));
            item.setProgressPercent(nullToZero(rs.getBigDecimal("progress_percent")));
            item.setCurrentMessage(rs.getString("current_message"));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return item;
        });
    }

    /**
     * 查询打开的告警事件。
     *
     * @return 打开告警列表
     */
    private List<DashboardOverview.AlertEventItem> queryOpenAlerts() {
        return jdbcTemplate.query("""
                SELECT id,
                       alert_title,
                       severity,
                       status,
                       metric_source,
                       metric_value,
                       threshold_value,
                       last_triggered_at
                FROM ops_alert_event
                WHERE status IN ('open', 'acknowledged')
                ORDER BY FIELD(severity, 'critical', 'high', 'medium', 'low'),
                         last_triggered_at DESC
                LIMIT 6
                """, (rs, rowNum) -> {
            DashboardOverview.AlertEventItem item = new DashboardOverview.AlertEventItem();
            item.setId(rs.getString("id"));
            item.setAlertTitle(rs.getString("alert_title"));
            item.setSeverity(rs.getString("severity"));
            item.setStatus(rs.getString("status"));
            item.setMetricSource(rs.getString("metric_source"));
            item.setMetricValue(nullToZero(rs.getBigDecimal("metric_value")));
            item.setThresholdValue(nullToZero(rs.getBigDecimal("threshold_value")));
            item.setLastTriggeredAt(toLocalDateTime(rs.getTimestamp("last_triggered_at")));
            return item;
        });
    }

    /**
     * 查询平台健康检查列表。
     *
     * @return 健康检查列表
     */
    private List<DashboardOverview.HealthCheckItem> queryHealthChecks() {
        return jdbcTemplate.query("""
                SELECT id,
                       check_name,
                       target_type,
                       target_code,
                       status,
                       message,
                       latency_ms,
                       last_checked_at
                FROM ops_health_check
                WHERE enabled = 1
                ORDER BY FIELD(status, 'unhealthy', 'failed', 'warning', 'healthy'),
                         last_checked_at DESC
                LIMIT 8
                """, (rs, rowNum) -> {
            DashboardOverview.HealthCheckItem item = new DashboardOverview.HealthCheckItem();
            item.setId(rs.getString("id"));
            item.setCheckName(rs.getString("check_name"));
            item.setTargetType(rs.getString("target_type"));
            item.setTargetCode(rs.getString("target_code"));
            item.setStatus(rs.getString("status"));
            item.setMessage(rs.getString("message"));
            item.setLatencyMs(rs.getLong("latency_ms"));
            item.setLastCheckedAt(toLocalDateTime(rs.getTimestamp("last_checked_at")));
            return item;
        });
    }

    /**
     * 生成工作台运营洞察。
     *
     * @param overview 工作台概览对象
     * @return 洞察列表
     */
    private List<DashboardOverview.InsightItem> buildInsights(DashboardOverview overview) {
        List<DashboardOverview.InsightItem> items = new java.util.ArrayList<>();
        if (overview.getOpenAlertCount() != null && overview.getOpenAlertCount() > 0) {
            items.add(insight("告警优先处理", "当前存在 " + overview.getOpenAlertCount() + " 个打开告警，建议先进入运营监控处理高优先级事件。", "danger"));
        }
        if (overview.getTaskBacklogCount() != null && overview.getTaskBacklogCount() > 0) {
            items.add(insight("任务队列积压", "仍有 " + overview.getTaskBacklogCount() + " 个任务排队或运行中，可进入任务中心查看文档解析、向量重建等进度。", "warning"));
        }
        DashboardOverview.KnowledgeHealth knowledge = overview.getKnowledgeHealth();
        if (knowledge.getOpenIssueCount() != null && knowledge.getOpenIssueCount() > 0) {
            items.add(insight("知识库治理待办", "知识治理中还有 " + knowledge.getOpenIssueCount() + " 个打开问题，其中高风险 " + knowledge.getHighRiskIssueCount() + " 个。", "warning"));
        }
        if (overview.getTodayRunCount() != null && overview.getTodayRunCount() > 0) {
            items.add(insight("今日调用概览", "今日运行 " + overview.getTodayRunCount() + " 次，成功率 " + overview.getTodaySuccessRate() + "%，Token 消耗 " + overview.getTodayTokenCount() + "。", "info"));
        }
        if (items.isEmpty()) {
            items.add(insight("平台状态平稳", "当前没有打开告警或积压任务。可以从工作台继续创建智能体、上传知识库或运行评测。", "success"));
        }
        return items;
    }

    /**
     * 创建洞察条目。
     *
     * @param title 标题
     * @param content 内容
     * @param tone 类型
     * @return 洞察条目
     */
    private DashboardOverview.InsightItem insight(String title, String content, String tone) {
        DashboardOverview.InsightItem item = new DashboardOverview.InsightItem();
        item.setTitle(title);
        item.setContent(content);
        item.setTone(tone);
        return item;
    }

    /**
     * 查询 Long 类型数量。
     *
     * @param sql SQL 语句
     * @param args SQL 参数
     * @return 数量
     */
    private Long count(String sql, Object... args) {
        Number number = jdbcTemplate.queryForObject(sql, Number.class, args);
        return number == null ? 0L : number.longValue();
    }

    /**
     * 计算百分比。
     *
     * @param part 分子
     * @param total 分母
     * @return 百分比数值
     */
    private BigDecimal percent(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    /**
     * BigDecimal 空值转零。
     *
     * @param value 原始值
     * @return 非空 BigDecimal
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 对象转 long。
     *
     * @param value 原始值
     * @return long 数值
     */
    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.hasText(value.toString())) {
            return Long.parseLong(value.toString());
        }
        return 0L;
    }

    /**
     * 对象转 BigDecimal。
     *
     * @param value 原始值
     * @return BigDecimal 数值
     */
    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value != null && StringUtils.hasText(value.toString())) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 对象转日期。
     *
     * @param value 原始值
     * @return 本地日期
     */
    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    /**
     * 时间戳转本地时间。
     *
     * @param timestamp 数据库时间戳
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 运行状态中文标签。
     *
     * @param status 原始状态
     * @return 中文标签
     */
    private String statusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "未知";
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            case "RUNNING" -> "运行中";
            case "PENDING" -> "排队中";
            default -> status;
        };
    }
}
