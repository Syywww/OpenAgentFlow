package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.workflow.WorkflowAdvancedDtos;
import com.openagentflow.domain.workflow.WorkflowDtos;
import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 工作流生产级增强服务。
 *
 * <p>该服务把模板、API 发布、人工确认、版本差异、调试入口和治理清单集中在一起，
 * 避免把工作流主 CRUD 服务继续膨胀。</p>
 */
@Service
public class WorkflowAdvancedService {

    /** JDBC 工具，用于轻量访问增强表和已有运行表。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** 工作流执行服务。 */
    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowAdvancedService(JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper,
                                   WorkflowExecutionService workflowExecutionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.workflowExecutionService = workflowExecutionService;
    }

    /**
     * 查询工作流增强总览。
     *
     * @return 总览数据
     */
    public WorkflowAdvancedDtos.Overview overview() {
        assertCanManageWorkflow();
        WorkflowAdvancedDtos.Overview overview = new WorkflowAdvancedDtos.Overview();
        overview.setWorkflowCount(count("SELECT COUNT(1) FROM workflow_definition WHERE deleted_at IS NULL"));
        overview.setPublishedCount(count("SELECT COUNT(1) FROM workflow_definition WHERE deleted_at IS NULL AND status = 'published'"));
        overview.setApiEndpointCount(count("SELECT COUNT(1) FROM workflow_api_endpoint WHERE enabled = 1"));
        overview.setPendingHumanTaskCount(count("SELECT COUNT(1) FROM workflow_human_task WHERE status = 'pending'"));
        overview.setTemplateCount(count("SELECT COUNT(1) FROM workflow_template WHERE enabled = 1"));
        overview.setTodayRunCount(count("SELECT COUNT(1) FROM workflow_run WHERE created_at >= DATE_SUB(NOW(3), INTERVAL 1 DAY)"));
        overview.setTodayFailedCount(count("SELECT COUNT(1) FROM workflow_run WHERE status = 'FAILED' AND created_at >= DATE_SUB(NOW(3), INTERVAL 1 DAY)"));
        overview.setCapabilities(capabilities());
        return overview;
    }

    /**
     * 查询 1-20 项工作流增强能力清单。
     *
     * @return 能力列表
     */
    public List<WorkflowAdvancedDtos.Capability> capabilities() {
        return List.of(
                capability("retry-timeout-failure", "节点重试/超时/失败分支", "执行引擎", "ready", "retryPolicy / failureStrategy", "节点级 retryCount、timeoutMs、failureStrategy、failureTargetNodeKey 已接入执行引擎。"),
                capability("human-confirm", "人工确认节点", "执行引擎", "ready", "nodeType=HUMAN", "高风险动作可进入 workflow_human_task，支持继续、拒绝和改参。"),
                capability("expression-mapping", "变量表达式与数据映射", "编排能力", "ready", "{{a.b[0]}}", "模板支持点路径和数组下标，工具参数 Map 会递归渲染。"),
                capability("condition-advanced", "条件节点增强", "编排能力", "ready", "conditionExpr", "支持 success、contains、equals、gt/gte/lt/lte、json:、AND、OR 和默认分支。"),
                capability("debug-mode", "工作流调试模式", "调试能力", "ready", "debugMode/startNodeKey/maxSteps/dryRun", "运行请求支持从指定节点开始、限制步数、空跑和逐步查看上下文。"),
                capability("template-center", "工作流模板中心", "复用能力", "ready", "workflow_template", "内置客服 RAG 工具流程和高风险确认流程模板。"),
                capability("trigger-event", "定时/Webhook/事件触发", "触发能力", "ready", "workflow_schedule / workflow-api", "已有调度表，API 发布端点支持外部 HTTP 触发。"),
                capability("io-schema", "输入输出 Schema", "开放能力", "ready", "input_schema/output_schema", "工作流定义支持标准入参和出参 Schema。"),
                capability("queue-execution", "执行队列化", "执行治理", "configured", "triggerType=async_task", "长流程可通过异步任务中心承接，当前接口保留异步触发类型。"),
                capability("parallel-join", "并行节点与汇聚节点", "编排能力", "ready", "nodeType=PARALLEL/JOIN", "并行节点会收集分支目标并按安全顺序执行，汇聚节点汇总上下文。"),
                capability("loop-batch", "循环/批处理节点", "编排能力", "ready", "nodeType=LOOP", "支持 items、itemPath、maxLoops，将批处理结果写回上下文。"),
                capability("version-diff", "版本差异对比", "版本治理", "ready", "/versions/diff", "可对比两个发布版本的节点、连线和策略摘要。"),
                capability("gray-release", "灰度发布", "发布治理", "configured", "releaseStrategy/grayPercent", "执行策略支持灰度比例和发布策略配置。"),
                capability("resource-permission", "权限与空间治理细化", "治理能力", "ready", "workspaceId/canManage", "沿用工作空间、资源归属和管理权限。"),
                capability("budget-control", "成本预算控制", "成本治理", "ready", "budgetTokens/budgetCost", "运行过程中累计 Token 和成本，超过预算后按策略阻断。"),
                capability("workflow-evaluation", "工作流质量评测", "质量治理", "configured", "eval_task.workflow_id", "评测任务表已支持 workflow_id，工作流可作为评测对象。"),
                capability("sub-workflow", "可复用子工作流", "复用能力", "ready", "nodeType=SUBFLOW", "子流程节点可按 workflowId 调用其他工作流。"),
                capability("api-publish", "工作流 API 发布", "开放能力", "ready", "workflow_api_endpoint", "工作流可发布为端点并通过 /workflow-api/{code} 调用。"),
                capability("node-plugin", "节点插件化", "扩展能力", "ready", "nodeType=PLUGIN", "插件节点可复用工具执行器或输出插件占位结果。"),
                capability("sandbox", "工作流运行沙箱", "安全能力", "ready", "sandboxLevel/sandboxPolicy", "节点可配置沙箱等级，高风险策略会写入策略命中日志。")
        );
    }

    /**
     * 查询启用的工作流模板。
     *
     * @return 模板列表
     */
    public List<WorkflowAdvancedDtos.TemplateSummary> listTemplates() {
        assertCanManageWorkflow();
        return jdbcTemplate.query("""
                SELECT id, template_code, template_name, template_category, description, graph_json, variable_schema, default_policy
                FROM workflow_template
                WHERE enabled = 1
                ORDER BY updated_at DESC
                """, (rs, rowNum) -> toTemplate(rs));
    }

    /**
     * 查询 API 发布端点。
     *
     * @return API 端点列表
     */
    public List<WorkflowAdvancedDtos.ApiEndpointSummary> listApiEndpoints() {
        assertCanManageWorkflow();
        return jdbcTemplate.query("""
                SELECT endpoint.id, endpoint.workflow_id, workflow.workflow_name, endpoint.endpoint_code, endpoint.endpoint_name,
                       endpoint.auth_type, endpoint.rate_limit_per_minute, endpoint.enabled, endpoint.last_invoked_at
                FROM workflow_api_endpoint endpoint
                JOIN workflow_definition workflow ON workflow.id = endpoint.workflow_id
                WHERE workflow.deleted_at IS NULL
                ORDER BY endpoint.updated_at DESC
                """, (rs, rowNum) -> toApiEndpoint(rs));
    }

    /**
     * 发布或更新工作流 API 端点。
     *
     * @param workflowId 工作流 ID
     * @param request 发布请求
     * @return 端点摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowAdvancedDtos.ApiEndpointSummary publishApiEndpoint(String workflowId, WorkflowAdvancedDtos.ApiPublishRequest request) {
        assertCanManageWorkflow();
        if (!StringUtils.hasText(workflowId)) {
            throw new BusinessException("WORKFLOW_ID_EMPTY", "工作流 ID 不能为空");
        }
        String endpointCode = StringUtils.hasText(request == null ? null : request.getEndpointCode())
                ? request.getEndpointCode().trim()
                : "wf-" + workflowId.substring(0, Math.min(8, workflowId.length()));
        String endpointName = StringUtils.hasText(request == null ? null : request.getEndpointName())
                ? request.getEndpointName().trim()
                : "工作流 API";
        String authType = StringUtils.hasText(request == null ? null : request.getAuthType()) ? request.getAuthType() : "jwt";
        int rateLimit = request == null || request.getRateLimitPerMinute() == null ? 60 : Math.max(1, request.getRateLimitPerMinute());
        boolean enabled = request == null || request.getEnabled() == null || request.getEnabled();
        String exists = queryString("SELECT id FROM workflow_api_endpoint WHERE workflow_id = ? LIMIT 1", workflowId);
        if (StringUtils.hasText(exists)) {
            jdbcTemplate.update("""
                    UPDATE workflow_api_endpoint
                    SET endpoint_code = ?, endpoint_name = ?, auth_type = ?, rate_limit_per_minute = ?, enabled = ?, updated_at = NOW(3)
                    WHERE id = ?
                    """, endpointCode, endpointName, authType, rateLimit, enabled, exists);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO workflow_api_endpoint
                      (id, workflow_id, endpoint_code, endpoint_name, auth_type, api_secret, rate_limit_per_minute, enabled, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, newId(), workflowId, endpointCode, endpointName, authType, "", rateLimit, enabled, currentUserId());
        }
        jdbcTemplate.update("UPDATE workflow_definition SET api_enabled = ?, release_strategy = IF(release_strategy IS NULL, 'standard', release_strategy) WHERE id = ?",
                enabled, workflowId);
        return listApiEndpoints().stream()
                .filter(item -> workflowId.equals(item.getWorkflowId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("WORKFLOW_API_NOT_FOUND", "工作流 API 端点不存在"));
    }

    /**
     * 执行 API 发布端点。
     *
     * @param endpointCode 端点编码
     * @param request 运行请求
     * @return 运行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDtos.RunResult invokeApiEndpoint(String endpointCode, WorkflowDtos.RunRequest request) {
        String workflowId = queryString("""
                SELECT workflow_id FROM workflow_api_endpoint
                WHERE endpoint_code = ? AND enabled = 1
                LIMIT 1
                """, endpointCode);
        if (!StringUtils.hasText(workflowId)) {
            throw new BusinessException("WORKFLOW_API_NOT_FOUND", "工作流 API 端点不存在或未启用");
        }
        jdbcTemplate.update("UPDATE workflow_api_endpoint SET last_invoked_at = NOW(3) WHERE endpoint_code = ?", endpointCode);
        return workflowExecutionService.runWorkflow(workflowId, request == null ? new WorkflowDtos.RunRequest() : request, "api");
    }

    /**
     * 查询待处理人工确认任务。
     *
     * @return 人工任务列表
     */
    public List<WorkflowAdvancedDtos.HumanTaskSummary> listHumanTasks() {
        assertCanManageWorkflow();
        return jdbcTemplate.query("""
                SELECT id, workflow_run_id, task_name, status, decision, payload, created_at, expired_at
                FROM workflow_human_task
                WHERE status IN ('pending', 'approved', 'rejected', 'changed')
                ORDER BY created_at DESC
                LIMIT 100
                """, (rs, rowNum) -> toHumanTask(rs));
    }

    /**
     * 处理人工确认任务。
     *
     * @param taskId 任务 ID
     * @param request 决策请求
     * @return 处理后的任务
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowAdvancedDtos.HumanTaskSummary decideHumanTask(String taskId, WorkflowAdvancedDtos.HumanTaskDecisionRequest request) {
        assertCanManageWorkflow();
        String decision = StringUtils.hasText(request == null ? null : request.getDecision()) ? request.getDecision() : "approved";
        String comment = request == null ? "" : request.getComment();
        String changedPayload = request == null || request.getChangedPayload() == null ? null : toJson(request.getChangedPayload());
        jdbcTemplate.update("""
                UPDATE workflow_human_task
                SET status = ?, decision = ?, comment = ?, payload = IFNULL(?, payload), completed_at = NOW(3)
                WHERE id = ?
                """, decision, decision, comment, changedPayload, taskId);
        return listHumanTasks().stream()
                .filter(item -> taskId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("WORKFLOW_HUMAN_TASK_NOT_FOUND", "人工确认任务不存在"));
    }

    /**
     * 对比两个工作流发布版本。
     *
     * @param workflowId 工作流 ID
     * @param leftVersion 左版本
     * @param rightVersion 右版本
     * @return 差异结果
     */
    public WorkflowAdvancedDtos.VersionDiff diffVersions(String workflowId, String leftVersion, String rightVersion) {
        assertCanManageWorkflow();
        String leftGraph = queryString("SELECT graph_json FROM workflow_version WHERE workflow_id = ? AND version_no = ? LIMIT 1", workflowId, leftVersion);
        String rightGraph = queryString("SELECT graph_json FROM workflow_version WHERE workflow_id = ? AND version_no = ? LIMIT 1", workflowId, rightVersion);
        Map<String, Object> left = parseMap(leftGraph);
        Map<String, Object> right = parseMap(rightGraph);
        Set<String> leftNodes = nodeIds(left);
        Set<String> rightNodes = nodeIds(right);
        Set<String> leftEdges = edgeIds(left);
        Set<String> rightEdges = edgeIds(right);
        List<String> changes = new ArrayList<>();
        for (String node : rightNodes) {
            if (!leftNodes.contains(node)) {
                changes.add("新增节点：" + node);
            }
        }
        for (String node : leftNodes) {
            if (!rightNodes.contains(node)) {
                changes.add("删除节点：" + node);
            }
        }
        for (String edge : rightEdges) {
            if (!leftEdges.contains(edge)) {
                changes.add("新增连线：" + edge);
            }
        }
        WorkflowAdvancedDtos.VersionDiff diff = new WorkflowAdvancedDtos.VersionDiff();
        diff.setLeftVersion(leftVersion);
        diff.setRightVersion(rightVersion);
        diff.setAddedNodes((int) rightNodes.stream().filter(item -> !leftNodes.contains(item)).count());
        diff.setRemovedNodes((int) leftNodes.stream().filter(item -> !rightNodes.contains(item)).count());
        diff.setChangedEdges((int) rightEdges.stream().filter(item -> !leftEdges.contains(item)).count());
        diff.setChanges(changes);
        return diff;
    }

    private WorkflowAdvancedDtos.Capability capability(String code, String name, String category, String status, String configKey, String description) {
        WorkflowAdvancedDtos.Capability capability = new WorkflowAdvancedDtos.Capability();
        capability.setCode(code);
        capability.setName(name);
        capability.setCategory(category);
        capability.setStatus(status);
        capability.setConfigKey(configKey);
        capability.setDescription(description);
        return capability;
    }

    private WorkflowAdvancedDtos.TemplateSummary toTemplate(ResultSet rs) throws java.sql.SQLException {
        WorkflowAdvancedDtos.TemplateSummary summary = new WorkflowAdvancedDtos.TemplateSummary();
        summary.setId(rs.getString("id"));
        summary.setTemplateCode(rs.getString("template_code"));
        summary.setTemplateName(rs.getString("template_name"));
        summary.setTemplateCategory(rs.getString("template_category"));
        summary.setDescription(rs.getString("description"));
        summary.setGraphJson(parseMap(rs.getString("graph_json")));
        summary.setVariableSchema(parseMap(rs.getString("variable_schema")));
        summary.setDefaultPolicy(parseMap(rs.getString("default_policy")));
        return summary;
    }

    private WorkflowAdvancedDtos.ApiEndpointSummary toApiEndpoint(ResultSet rs) throws java.sql.SQLException {
        WorkflowAdvancedDtos.ApiEndpointSummary summary = new WorkflowAdvancedDtos.ApiEndpointSummary();
        summary.setId(rs.getString("id"));
        summary.setWorkflowId(rs.getString("workflow_id"));
        summary.setWorkflowName(rs.getString("workflow_name"));
        summary.setEndpointCode(rs.getString("endpoint_code"));
        summary.setEndpointName(rs.getString("endpoint_name"));
        summary.setAuthType(rs.getString("auth_type"));
        summary.setRateLimitPerMinute(rs.getInt("rate_limit_per_minute"));
        summary.setEnabled(rs.getBoolean("enabled"));
        summary.setLastInvokedAt(toLocalDateTime(rs.getTimestamp("last_invoked_at")));
        return summary;
    }

    private WorkflowAdvancedDtos.HumanTaskSummary toHumanTask(ResultSet rs) throws java.sql.SQLException {
        WorkflowAdvancedDtos.HumanTaskSummary summary = new WorkflowAdvancedDtos.HumanTaskSummary();
        summary.setId(rs.getString("id"));
        summary.setWorkflowRunId(rs.getString("workflow_run_id"));
        summary.setTaskName(rs.getString("task_name"));
        summary.setStatus(rs.getString("status"));
        summary.setDecision(rs.getString("decision"));
        summary.setPayload(parseMap(rs.getString("payload")));
        summary.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        summary.setExpiredAt(toLocalDateTime(rs.getTimestamp("expired_at")));
        return summary;
    }

    private Integer count(String sql) {
        Number count = jdbcTemplate.queryForObject(sql, Number.class);
        return count == null ? 0 : count.intValue();
    }

    private String queryString(String sql, Object... args) {
        List<String> values = jdbcTemplate.queryForList(sql, String.class, args);
        return values.isEmpty() ? "" : values.get(0);
    }

    @SuppressWarnings("unchecked")
    private Set<String> nodeIds(Map<String, Object> graph) {
        Set<String> ids = new LinkedHashSet<>();
        Object nodes = graph.get("nodes");
        if (nodes instanceof List<?> list) {
            for (Object node : list) {
                if (node instanceof Map<?, ?> map) {
                    Object id = map.get("id");
                    ids.add(String.valueOf(id == null ? map.get("nodeKey") : id));
                }
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private Set<String> edgeIds(Map<String, Object> graph) {
        Set<String> ids = new LinkedHashSet<>();
        Object edges = graph.get("edges");
        if (edges instanceof List<?> list) {
            for (Object edge : list) {
                if (edge instanceof Map<?, ?> map) {
                    Object id = map.get("id");
                    ids.add(String.valueOf(id == null ? map.get("edgeKey") : id));
                }
            }
        }
        return ids;
    }

    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            }));
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        try {
            Object value = principal.getClass().getMethod("getId").invoke(principal);
            return value == null ? null : String.valueOf(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private void assertCanManageWorkflow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("UNAUTHORIZED", "请先登录");
        }
        boolean allowed = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> List.of("ROLE_super_admin", "ROLE_admin", "workflow:manage", "workflow:advanced:manage").contains(authority));
        if (!allowed) {
            throw new BusinessException("WORKFLOW_FORBIDDEN", "没有工作流高级治理权限");
        }
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }
}
