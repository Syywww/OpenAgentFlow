package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.delivery.DeliveryAcceptanceDtos;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 交付验收中心服务。
 * <p>聚合环境、核心链路、治理、监控、配置和数据状态，生成可交付报告。</p>
 */
@Service
public class DeliveryAcceptanceService {

    /** 最新 SQL 脚本版本，用于交付清单展示。 */
    private static final String LATEST_SQL_VERSION = "V029__demo_data_package.sql";

    /** 当前前端版本，和 package.json 保持一致。 */
    private static final String FRONTEND_VERSION = "0.1.0";

    /** JDBC 工具，用于读取平台已有表的交付状态。 */
    private final JdbcTemplate jdbcTemplate;

    /** Redis 客户端，用于检查 Redis 连通性。 */
    private final StringRedisTemplate redisTemplate;

    /** JSON 工具，用于保存报告快照。 */
    private final ObjectMapper objectMapper;

    public DeliveryAcceptanceService(JdbcTemplate jdbcTemplate,
                                     StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询交付验收总览。
     *
     * @return 总览信息
     */
    public DeliveryAcceptanceDtos.Overview overview() {
        assertCanView();
        List<DeliveryAcceptanceDtos.CheckItem> checks = buildChecks();
        return buildOverview(checks, latestReport(), manifest());
    }

    /**
     * 查询当前交付检查项。
     *
     * @return 检查项列表
     */
    public List<DeliveryAcceptanceDtos.CheckItem> checklist() {
        assertCanView();
        return buildChecks();
    }

    /**
     * 生成新的交付报告。
     *
     * @return 报告详情
     */
    @Transactional(rollbackFor = Exception.class)
    public DeliveryAcceptanceDtos.ReportDetail runAcceptance() {
        assertCanManage();
        List<DeliveryAcceptanceDtos.CheckItem> checks = buildChecks();
        List<DeliveryAcceptanceDtos.RiskItem> risks = buildRisks(checks);
        DeliveryAcceptanceDtos.Manifest manifest = manifest();
        DeliveryAcceptanceDtos.Overview overview = buildOverview(checks, null, manifest);
        DeliveryAcceptanceDtos.ReportDetail report = new DeliveryAcceptanceDtos.ReportDetail();
        report.setId(UUID.randomUUID().toString());
        report.setReportCode("delivery_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
        report.setReportName("OpenAgentFlow 交付验收报告");
        report.setOverallStatus(overview.getOverallStatus());
        report.setScore(overview.getScore());
        report.setPassedCount(overview.getPassedCount());
        report.setWarningCount(overview.getWarningCount());
        report.setFailedCount(overview.getFailedCount());
        report.setCreatedAt(LocalDateTime.now());
        report.setOverview(overview);
        report.setChecks(checks);
        report.setRisks(risks);
        report.setManifest(manifest);
        saveReport(report);
        return report;
    }

    /**
     * 分页查询交付报告列表。
     *
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 报告分页
     */
    public PageResult<DeliveryAcceptanceDtos.ReportSummary> listReports(Integer pageNo, Integer pageSize) {
        assertCanView();
        int current = pageNo == null ? 1 : Math.max(1, pageNo);
        int size = pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize));
        long total = count("select count(1) from delivery_acceptance_report");
        List<DeliveryAcceptanceDtos.ReportSummary> records = jdbcTemplate.query("""
                        select id, report_code, report_name, overall_status, score,
                               passed_count, warning_count, failed_count, created_at
                        from delivery_acceptance_report
                        order by created_at desc
                        limit ?, ?
                        """,
                (rs, rowNum) -> {
                    DeliveryAcceptanceDtos.ReportSummary item = new DeliveryAcceptanceDtos.ReportSummary();
                    item.setId(rs.getString("id"));
                    item.setReportCode(rs.getString("report_code"));
                    item.setReportName(rs.getString("report_name"));
                    item.setOverallStatus(rs.getString("overall_status"));
                    item.setScore(rs.getBigDecimal("score"));
                    item.setPassedCount(rs.getInt("passed_count"));
                    item.setWarningCount(rs.getInt("warning_count"));
                    item.setFailedCount(rs.getInt("failed_count"));
                    item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return item;
                },
                (current - 1) * size, size);
        return new PageResult<>(records, total, current, size);
    }

    /**
     * 查询交付报告详情。
     *
     * @param id 报告 ID
     * @return 报告详情
     */
    public DeliveryAcceptanceDtos.ReportDetail getReport(String id) {
        assertCanView();
        List<DeliveryAcceptanceDtos.ReportDetail> list = jdbcTemplate.query("""
                        select *
                        from delivery_acceptance_report
                        where id = ?
                        limit 1
                        """,
                (rs, rowNum) -> {
                    DeliveryAcceptanceDtos.ReportDetail report = new DeliveryAcceptanceDtos.ReportDetail();
                    report.setId(rs.getString("id"));
                    report.setReportCode(rs.getString("report_code"));
                    report.setReportName(rs.getString("report_name"));
                    report.setOverallStatus(rs.getString("overall_status"));
                    report.setScore(rs.getBigDecimal("score"));
                    report.setPassedCount(rs.getInt("passed_count"));
                    report.setWarningCount(rs.getInt("warning_count"));
                    report.setFailedCount(rs.getInt("failed_count"));
                    report.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    report.setOverview(readJson(rs.getString("summary_json"), DeliveryAcceptanceDtos.Overview.class));
                    report.setChecks(readJson(rs.getString("checklist_json"), new TypeReference<List<DeliveryAcceptanceDtos.CheckItem>>() {
                    }));
                    report.setRisks(readJson(rs.getString("risk_json"), new TypeReference<List<DeliveryAcceptanceDtos.RiskItem>>() {
                    }));
                    report.setManifest(readJson(rs.getString("manifest_json"), DeliveryAcceptanceDtos.Manifest.class));
                    return report;
                }, id);
        if (list.isEmpty()) {
            throw new BusinessException("DELIVERY_REPORT_NOT_FOUND", "交付报告不存在");
        }
        return list.get(0);
    }

    /**
     * 构建交付检查项。
     *
     * @return 检查项列表
     */
    private List<DeliveryAcceptanceDtos.CheckItem> buildChecks() {
        List<DeliveryAcceptanceDtos.CheckItem> checks = new ArrayList<>();
        checks.add(checkMysql());
        checks.add(checkRedis());
        checks.add(checkMilvus());
        checks.add(checkModelConfigured());
        checks.add(checkAgentReady());
        checks.add(checkKnowledgeReady());
        checks.add(checkToolReady());
        checks.add(checkWorkflowReady());
        checks.add(checkTraceReady());
        checks.add(checkEvaluationReady());
        checks.add(checkCostReady());
        checks.add(checkOpsReady());
        checks.add(checkRiskReady());
        checks.add(checkAsyncTaskReady());
        checks.add(checkMemoryProductionReady());
        checks.add(checkProductionSecrets());
        checks.add(checkDemoData());
        return checks;
    }

    /**
     * MySQL 连通性检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkMysql() {
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            return item("mysql", "MySQL 数据库", "环境组件", "passed", "MySQL 可访问", "保持备份与最小权限账号配置", false, result, 1, Map.of("database", databaseName()));
        } catch (Exception exception) {
            return item("mysql", "MySQL 数据库", "环境组件", "failed", "MySQL 不可访问：" + exception.getMessage(), "检查本地 MySQL 服务、账号密码和数据库 openagentflow", true, 0, 1, Map.of());
        }
    }

    /**
     * Redis 连通性检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkRedis() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            String pong = connection.ping();
            return item("redis", "Redis 缓存", "环境组件", "PONG".equalsIgnoreCase(pong) ? "passed" : "warning", "Redis 返回：" + pong, "生产环境建议启用密码和持久化策略", false, pong, "PONG", Map.of());
        } catch (Exception exception) {
            return item("redis", "Redis 缓存", "环境组件", "failed", "Redis 不可访问：" + exception.getMessage(), "检查 Redis 服务和连接配置", true, "failed", "PONG", Map.of());
        }
    }

    /**
     * Milvus 状态检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkMilvus() {
        long vectorRows = count("select count(1) from knowledge_embedding where vector_primary_key is not null or embedding_json is not null");
        long fallbackRows = count("select count(1) from knowledge_embedding where vector_primary_key is null and embedding_json is not null");
        long connectionCount = count("select count(1) from vector_store_connection");
        if (vectorRows <= 0) {
            return item("milvus", "Milvus 向量库", "环境组件", "warning", "尚无可用向量数据", "上传知识库文档并完成 Embedding 与向量写入", false, vectorRows, ">0", mapOf("connectionCount", connectionCount, "fallbackRows", fallbackRows));
        }
        return item("milvus", "Milvus 向量库", "环境组件", fallbackRows > 0 ? "warning" : "passed", fallbackRows > 0 ? "存在 MySQL 向量兜底数据" : "向量数据可用", "生产交付建议确保 Milvus 可用并完成向量同步", false, vectorRows, ">0", mapOf("connectionCount", connectionCount, "fallbackRows", fallbackRows));
    }

    /**
     * 模型配置检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkModelConfigured() {
        long enabledModels = count("select count(1) from model_config where enabled = 1 and model_type = 'chat'");
        long enabledKeys = count("select count(1) from model_api_key where enabled = 1");
        String status = enabledModels > 0 && enabledKeys > 0 ? "passed" : "failed";
        return item("model_config", "模型与密钥", "核心链路", status,
                status.equals("passed") ? "已配置可用聊天模型和 API Key" : "缺少启用的聊天模型或 API Key",
                "在系统设置中配置模型供应商、聊天模型和 API Key",
                true, enabledModels + "/" + enabledKeys, "模型>0且Key>0", mapOf("enabledModels", enabledModels, "enabledKeys", enabledKeys));
    }

    /**
     * Agent 就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkAgentReady() {
        long activeAgents = count("select count(1) from agent where deleted_at is null and status in ('active','published')");
        return item("agent_ready", "Agent 配置", "核心链路", activeAgents > 0 ? "passed" : "failed",
                activeAgents > 0 ? "存在可运行 Agent" : "缺少可运行 Agent",
                "创建并发布至少一个 Agent，绑定模型和 Prompt",
                true, activeAgents, ">0", Map.of("activeAgents", activeAgents));
    }

    /**
     * 知识库就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkKnowledgeReady() {
        long kbCount = count("select count(1) from knowledge_base where deleted_at is null and status = 'active'");
        long chunkCount = count("select count(1) from knowledge_chunk where status = 'active'");
        long embeddingCount = count("select count(1) from knowledge_embedding");
        String status = kbCount > 0 && chunkCount > 0 && embeddingCount > 0 ? "passed" : "warning";
        return item("knowledge_ready", "RAG 知识库", "核心链路", status,
                status.equals("passed") ? "知识库、切片和向量已准备" : "知识库链路尚未完整准备",
                "上传演示文档并确认解析、切片、Embedding、向量写入完成",
                false, kbCount + "/" + chunkCount + "/" + embeddingCount, "知识库/切片/向量均>0", mapOf("kbCount", kbCount, "chunkCount", chunkCount, "embeddingCount", embeddingCount));
    }

    /**
     * 工具中心就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkToolReady() {
        long toolCount = count("select count(1) from tool_definition where deleted_at is null and enabled = 1");
        return item("tool_ready", "工具中心", "核心链路", toolCount > 0 ? "passed" : "warning",
                toolCount > 0 ? "存在启用工具" : "尚未配置启用工具",
                "至少配置一个低风险 REST API 或 Webhook 工具用于演示 Tool Calling",
                false, toolCount, ">0", Map.of("toolCount", toolCount));
    }

    /**
     * 工作流就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkWorkflowReady() {
        long workflowCount = count("select count(1) from workflow_definition where deleted_at is null");
        long publishedCount = count("select count(1) from workflow_definition where deleted_at is null and status = 'published'");
        return item("workflow_ready", "工作流编排", "核心链路", workflowCount > 0 ? "passed" : "warning",
                publishedCount > 0 ? "存在已发布工作流" : "工作流可用但未发布",
                "创建并发布一个包含 LLM/RAG/工具节点的示例工作流",
                false, workflowCount + "/" + publishedCount, "工作流>0", mapOf("workflowCount", workflowCount, "publishedCount", publishedCount));
    }

    /**
     * Trace 就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkTraceReady() {
        long runCount = count("select count(1) from runtime_run");
        long stepCount = count("select count(1) from runtime_trace_step");
        return item("trace_ready", "运行 Trace", "可观测", runCount > 0 && stepCount > 0 ? "passed" : "warning",
                runCount > 0 ? "存在运行链路数据" : "尚无运行链路数据",
                "在调试台执行一次 Agent 对话并查看 Trace",
                false, runCount + "/" + stepCount, "运行和步骤均>0", mapOf("runCount", runCount, "stepCount", stepCount));
    }

    /**
     * 评测中心就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkEvaluationReady() {
        long datasetCount = count("select count(1) from eval_dataset where deleted_at is null");
        long taskCount = count("select count(1) from eval_task");
        long judgeMetric = count("select count(1) from eval_metric where metric_code = 'llm_judge_overall'");
        return item("evaluation_ready", "模型评测", "质量治理", datasetCount > 0 && judgeMetric > 0 ? "passed" : "warning",
                datasetCount > 0 ? "评测集与 Judge 指标可用" : "尚未创建评测集",
                "创建样本集并运行一次 LLM-as-Judge 评测",
                false, datasetCount + "/" + taskCount, "评测集>0", mapOf("datasetCount", datasetCount, "taskCount", taskCount, "judgeMetric", judgeMetric));
    }

    /**
     * 成本中心就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkCostReady() {
        long priceConfigCount = count("select count(1) from model_price_config where enabled = 1");
        long modelPriceCount = count("select count(1) from model_config where status = 'enabled' and (input_price_per_1k > 0 or output_price_per_1k > 0)");
        long quotaCount = count("select count(1) from model_usage_quota where enabled = 1");
        long priceCount = Math.max(priceConfigCount, modelPriceCount);
        return item("cost_ready", "成本与配额", "运营治理", priceCount > 0 ? "passed" : "warning",
                priceCount > 0 ? "模型价格配置可用" : "缺少启用的模型价格配置",
                "配置模型价格和配额规则，确保成本不为 0",
                false, priceCount + "/" + quotaCount, "价格配置>0", mapOf("priceConfigCount", priceConfigCount, "modelPriceCount", modelPriceCount, "quotaCount", quotaCount));
    }

    /**
     * 运营监控就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkOpsReady() {
        long checkCount = count("select count(1) from ops_health_check");
        long ruleCount = count("select count(1) from ops_alert_rule where enabled = 1");
        long openAlerts = count("select count(1) from ops_alert_event where status in ('open','acknowledged')");
        String status = checkCount > 0 && ruleCount > 0 ? (openAlerts > 0 ? "warning" : "passed") : "warning";
        return item("ops_ready", "运营监控", "运营治理", status,
                openAlerts > 0 ? "存在待处理告警" : "巡检项和告警规则已配置",
                "处理打开的告警，并配置通知渠道",
                false, checkCount + "/" + ruleCount + "/" + openAlerts, "巡检项和规则>0", mapOf("checkCount", checkCount, "ruleCount", ruleCount, "openAlerts", openAlerts));
    }

    /**
     * 风险治理就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkRiskReady() {
        long openRisks = count("select count(1) from risk_governance_event where status in ('open','pending','acknowledged')");
        long highRiskTools = count("select count(1) from tool_definition where deleted_at is null and risk_level = 'high' and enabled = 1");
        return item("risk_ready", "审计与风险治理", "安全治理", openRisks > 0 || highRiskTools > 0 ? "warning" : "passed",
                openRisks > 0 ? "存在未闭环风险事件" : "当前无打开风险事件",
                "处理高风险工具、MCP 能力和待确认事项",
                false, openRisks + "/" + highRiskTools, "打开风险=0", mapOf("openRisks", openRisks, "highRiskTools", highRiskTools));
    }

    /**
     * 异步任务就绪检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkAsyncTaskReady() {
        long activeTasks = count("select count(1) from async_task where status in ('pending','running')");
        long failedTasks = count("select count(1) from async_task where status = 'failed'");
        String status = activeTasks > 0 || failedTasks > 0 ? "warning" : "passed";
        return item("async_task_ready", "异步任务中心", "运营治理", status,
                status.equals("passed") ? "暂无积压或失败任务" : "存在积压或失败任务",
                "进入任务中心处理失败任务，确认长任务没有卡住",
                false, activeTasks + "/" + failedTasks, "积压和失败均=0", mapOf("activeTasks", activeTasks, "failedTasks", failedTasks));
    }

    /** Memory生产能力就绪检查。 */
    private DeliveryAcceptanceDtos.CheckItem checkMemoryProductionReady() {
        long policies = count("select count(1) from memory_policy where status='enabled'");
        long syncFailed = count("select count(1) from agent_memory where status='active' and sync_status='failed'");
        long openHighIssues = count("select count(1) from memory_governance_issue where status='open' and severity in ('high','critical')");
        boolean ready = policies > 0 && syncFailed == 0 && openHighIssues == 0;
        return item("memory_production", "Memory生产能力", "AI治理", ready ? "passed" : "warning",
                ready ? "Memory策略、向量同步和治理状态正常" : "Memory存在策略缺失、向量失败或高风险治理问题",
                "进入Memory中心执行治理扫描和向量重建，并确认默认空间策略已启用",
                false, policies + "/" + syncFailed + "/" + openHighIssues,
                "启用策略>0、向量失败=0、高风险问题=0",
                mapOf("policies", policies, "syncFailed", syncFailed, "openHighIssues", openHighIssues));
    }

    /**
     * 生产密钥检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkProductionSecrets() {
        String jwtSecret = System.getenv("OAF_JWT_SECRET");
        boolean usingDefault = !StringUtils.hasText(jwtSecret) || jwtSecret.length() < 32;
        return item("production_secrets", "生产密钥", "安全治理", usingDefault ? "warning" : "passed",
                usingDefault ? "未检测到足够强度的生产 JWT Secret" : "生产 JWT Secret 已通过环境变量配置",
                "生产环境必须通过 OAF_JWT_SECRET 配置高强度密钥",
                false, usingDefault ? "weak-or-default" : "configured", "configured", Map.of());
    }

    /**
     * 演示数据检查。
     *
     * @return 检查项
     */
    private DeliveryAcceptanceDtos.CheckItem checkDemoData() {
        long agents = count("select count(1) from agent where deleted_at is null and agent_code in ('customer-support-agent','order-analyst-agent','quality-review-agent')");
        long knowledge = count("select count(1) from knowledge_base where deleted_at is null and kb_code = 'product-manual-kb'");
        long chunks = count("select count(1) from knowledge_chunk where kb_id = '40000000-0000-0000-0000-000000000001' and status = 'active'");
        long tools = count("select count(1) from tool_definition where deleted_at is null and tool_code in ('demo_order_status_rest','demo_customer_event_webhook','demo_readonly_order_sql')");
        long workflows = count("select count(1) from workflow_definition where deleted_at is null and workflow_code = 'demo-customer-service-flow'");
        long datasets = count("select count(1) from eval_dataset where deleted_at is null and dataset_code = 'demo-customer-service-eval'");
        long teams = count("select count(1) from agent_team where team_code = 'demo-customer-service-squad' and status = 'published'");
        long prompts = count("select count(1) from prompt_template where template_code in ('demo-customer-service-system','demo-trusted-rag-answer')");
        long memories = count("select count(1) from agent_memory where memory_key in ('customer_support_agent_long_term_template','demo_customer_support_preference') and status = 'active'");
        boolean ready = agents >= 3 && knowledge > 0 && chunks >= 4 && tools >= 3 && workflows > 0 && datasets > 0 && teams > 0 && prompts >= 2 && memories >= 1;
        return item("demo_data", "演示数据", "交付清单", ready ? "passed" : "warning",
                ready ? "演示样例包已覆盖 Agent、知识库、工具、工作流、评测集和协作团队" : "演示样例包不完整",
                "运行 scripts/init-demo-data.ps1 补齐 P33 演示样例包",
                false,
                agents + "/" + knowledge + "/" + chunks + "/" + tools + "/" + workflows + "/" + datasets + "/" + teams,
                "Agent>=3、分片>=4、工具>=3、工作流/评测集/团队均>0",
                mapOf("agents", agents, "knowledge", knowledge, "chunks", chunks, "tools", tools, "workflows", workflows, "datasets", datasets, "teams", teams, "prompts", prompts, "memories", memories));
    }

    /**
     * 根据检查项构建总览。
     *
     * @param checks 检查项
     * @param latest 最近报告
     * @param manifest 交付清单
     * @return 总览
     */
    private DeliveryAcceptanceDtos.Overview buildOverview(List<DeliveryAcceptanceDtos.CheckItem> checks,
                                                          DeliveryAcceptanceDtos.ReportSummary latest,
                                                          DeliveryAcceptanceDtos.Manifest manifest) {
        int passed = (int) checks.stream().filter(item -> "passed".equals(item.getStatus())).count();
        int warning = (int) checks.stream().filter(item -> "warning".equals(item.getStatus())).count();
        int failed = (int) checks.stream().filter(item -> "failed".equals(item.getStatus())).count();
        boolean blockingFailed = checks.stream().anyMatch(item -> "failed".equals(item.getStatus()) && Boolean.TRUE.equals(item.getBlocking()));
        DeliveryAcceptanceDtos.Overview overview = new DeliveryAcceptanceDtos.Overview();
        overview.setPassedCount(passed);
        overview.setWarningCount(warning);
        overview.setFailedCount(failed);
        overview.setScore(score(checks));
        overview.setOverallStatus(blockingFailed ? "failed" : warning > 0 || failed > 0 ? "warning" : "ready");
        overview.setModuleCount(10);
        overview.setComponentCount(3);
        overview.setLatestReportAt(latest == null ? null : latest.getCreatedAt());
        overview.setLatestReportCode(latest == null ? null : latest.getReportCode());
        overview.setMetrics(mapOf(
                "agentCount", count("select count(1) from agent where deleted_at is null"),
                "knowledgeBaseCount", count("select count(1) from knowledge_base where deleted_at is null"),
                "workflowCount", count("select count(1) from workflow_definition where deleted_at is null"),
                "toolCount", count("select count(1) from tool_definition where deleted_at is null"),
                "todayRunCount", count("select count(1) from runtime_run where created_at >= curdate()"),
                "openAlertCount", count("select count(1) from ops_alert_event where status in ('open','acknowledged')")));
        overview.setManifest(manifest);
        return overview;
    }

    /**
     * 构建风险提示。
     *
     * @param checks 检查项
     * @return 风险列表
     */
    private List<DeliveryAcceptanceDtos.RiskItem> buildRisks(List<DeliveryAcceptanceDtos.CheckItem> checks) {
        return checks.stream()
                .filter(item -> !"passed".equals(item.getStatus()))
                .map(item -> {
                    DeliveryAcceptanceDtos.RiskItem risk = new DeliveryAcceptanceDtos.RiskItem();
                    risk.setRiskLevel("failed".equals(item.getStatus()) && Boolean.TRUE.equals(item.getBlocking()) ? "high" : "warning".equals(item.getStatus()) ? "medium" : "low");
                    risk.setTitle(item.getCheckName());
                    risk.setDescription(item.getMessage());
                    risk.setSuggestion(item.getSuggestion());
                    risk.setSourceCheckCode(item.getCheckCode());
                    return risk;
                })
                .toList();
    }

    /**
     * 构建交付清单。
     *
     * @return 清单
     */
    private DeliveryAcceptanceDtos.Manifest manifest() {
        DeliveryAcceptanceDtos.Manifest manifest = new DeliveryAcceptanceDtos.Manifest();
        manifest.setAppName("OpenAgentFlow-Java");
        manifest.setBackendVersion(getClass().getPackage().getImplementationVersion() == null ? "0.1.0-SNAPSHOT" : getClass().getPackage().getImplementationVersion());
        manifest.setFrontendVersion(FRONTEND_VERSION);
        manifest.setJavaVersion(System.getProperty("java.version"));
        manifest.setDatabaseName(databaseName());
        manifest.setLatestSqlVersion(LATEST_SQL_VERSION);
        manifest.setDataComponents(mapOf("mysql", "openagentflow", "redis", "localhost:6379", "milvus", "localhost:19530"));
        manifest.setModuleCounts(mapOf(
                "agents", count("select count(1) from agent where deleted_at is null"),
                "knowledgeBases", count("select count(1) from knowledge_base where deleted_at is null"),
                "tools", count("select count(1) from tool_definition where deleted_at is null"),
                "workflows", count("select count(1) from workflow_definition where deleted_at is null"),
                "evaluationDatasets", count("select count(1) from eval_dataset where deleted_at is null"),
                "promptTemplates", count("select count(1) from prompt_template"),
                "agentTeams", count("select count(1) from agent_team"),
                "memories", count("select count(1) from agent_memory where status = 'active'"),
                "demoPackage", count("select count(1) from sys_config where config_key = 'demo.data.package.version'")));
        return manifest;
    }

    /**
     * 保存报告快照。
     *
     * @param report 报告详情
     */
    private void saveReport(DeliveryAcceptanceDtos.ReportDetail report) {
        jdbcTemplate.update("""
                        insert into delivery_acceptance_report(
                          id, report_code, report_name, overall_status, score,
                          passed_count, warning_count, failed_count,
                          summary_json, checklist_json, risk_json, manifest_json, created_by, created_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, cast(? as json), cast(? as json), cast(? as json), cast(? as json), ?, ?)
                        """,
                report.getId(),
                report.getReportCode(),
                report.getReportName(),
                report.getOverallStatus(),
                report.getScore(),
                report.getPassedCount(),
                report.getWarningCount(),
                report.getFailedCount(),
                toJson(report.getOverview()),
                toJson(report.getChecks()),
                toJson(report.getRisks()),
                toJson(report.getManifest()),
                currentUserId(),
                report.getCreatedAt());
    }

    /**
     * 获取最近报告摘要。
     *
     * @return 最近报告
     */
    private DeliveryAcceptanceDtos.ReportSummary latestReport() {
        List<DeliveryAcceptanceDtos.ReportSummary> list = jdbcTemplate.query("""
                        select id, report_code, report_name, overall_status, score,
                               passed_count, warning_count, failed_count, created_at
                        from delivery_acceptance_report
                        order by created_at desc
                        limit 1
                        """,
                (rs, rowNum) -> {
                    DeliveryAcceptanceDtos.ReportSummary item = new DeliveryAcceptanceDtos.ReportSummary();
                    item.setId(rs.getString("id"));
                    item.setReportCode(rs.getString("report_code"));
                    item.setReportName(rs.getString("report_name"));
                    item.setOverallStatus(rs.getString("overall_status"));
                    item.setScore(rs.getBigDecimal("score"));
                    item.setPassedCount(rs.getInt("passed_count"));
                    item.setWarningCount(rs.getInt("warning_count"));
                    item.setFailedCount(rs.getInt("failed_count"));
                    item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return item;
                });
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 构建检查项。
     */
    private DeliveryAcceptanceDtos.CheckItem item(String code,
                                                  String name,
                                                  String category,
                                                  String status,
                                                  String message,
                                                  String suggestion,
                                                  boolean blocking,
                                                  Object actual,
                                                  Object expected,
                                                  Map<String, Object> detail) {
        DeliveryAcceptanceDtos.CheckItem item = new DeliveryAcceptanceDtos.CheckItem();
        item.setCheckCode(code);
        item.setCheckName(name);
        item.setCategory(category);
        item.setStatus(status);
        item.setMessage(message);
        item.setSuggestion(suggestion);
        item.setBlocking(blocking);
        item.setActualValue(actual);
        item.setExpectedValue(expected);
        item.setDetail(detail == null ? Map.of() : detail);
        return item;
    }

    /**
     * 计算交付评分。
     *
     * @param checks 检查项
     * @return 评分
     */
    private BigDecimal score(List<DeliveryAcceptanceDtos.CheckItem> checks) {
        if (checks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double total = checks.stream()
                .mapToDouble(item -> {
                    if ("passed".equals(item.getStatus())) {
                        return 100D;
                    }
                    if ("warning".equals(item.getStatus())) {
                        return 65D;
                    }
                    return 0D;
                })
                .sum();
        return BigDecimal.valueOf(total / checks.size()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 查询数量，失败时返回 0 避免交付中心整体不可用。
     */
    private long count(String sql) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class);
            return value == null ? 0 : value;
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * 当前数据库名称。
     */
    private String databaseName() {
        try {
            String value = jdbcTemplate.queryForObject("select database()", String.class);
            return value == null ? "openagentflow" : value;
        } catch (Exception ignored) {
            return "openagentflow";
        }
    }

    /**
     * JSON 序列化。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * JSON 反序列化。
     */
    private <T> T readJson(String raw, Class<T> type) {
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * JSON 反序列化。
     */
    private <T> T readJson(String raw, TypeReference<T> type) {
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 构建可包含空值的有序 Map。
     */
    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    /**
     * 校验查看权限。
     */
    private void assertCanView() {
        if (!hasAuthority("ROLE_super_admin")
                && !hasAuthority("ROLE_admin")
                && !hasAuthority("delivery:acceptance:view")
                && !hasAuthority("delivery:acceptance:manage")) {
            throw new BusinessException("DELIVERY_FORBIDDEN", "没有查看交付验收中心的权限");
        }
    }

    /**
     * 校验管理权限。
     */
    private void assertCanManage() {
        if (!hasAuthority("ROLE_super_admin")
                && !hasAuthority("ROLE_admin")
                && !hasAuthority("delivery:acceptance:manage")) {
            throw new BusinessException("DELIVERY_FORBIDDEN", "没有执行交付验收的权限");
        }
    }

    /**
     * 判断当前用户是否拥有权限。
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    /**
     * 当前用户 ID。
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails details) {
            return details.getUserId();
        }
        return null;
    }
}
