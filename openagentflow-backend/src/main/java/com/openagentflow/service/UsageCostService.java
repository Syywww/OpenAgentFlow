package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.task.AsyncTaskDtos;
import com.openagentflow.domain.usage.UsageDtos;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.entity.ModelUsageQuotaEntity;
import com.openagentflow.entity.RuntimeCostDailyEntity;
import com.openagentflow.entity.RuntimeRunEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.ModelUsageQuotaMapper;
import com.openagentflow.mapper.RuntimeCostDailyMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 成本与用量中心服务。
 *
 * <p>统一负责 Token 成本计算、配额拦截、日报累计和前端统计查询。</p>
 */
@Service
public class UsageCostService implements DistributedTaskHandler {

    /** 金额计算保留小数位。 */
    private static final int COST_SCALE = 6;

    /** 配额百分比保留小数位。 */
    private static final int RATE_SCALE = 2;

    /** 配额规则 Mapper。 */
    private final ModelUsageQuotaMapper modelUsageQuotaMapper;

    /** 成本日报 Mapper。 */
    private final RuntimeCostDailyMapper runtimeCostDailyMapper;

    /** JDBC 工具，用于聚合查询和原子更新。 */
    private final JdbcTemplate jdbcTemplate;

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    public UsageCostService(ModelUsageQuotaMapper modelUsageQuotaMapper,
                            RuntimeCostDailyMapper runtimeCostDailyMapper,
                            JdbcTemplate jdbcTemplate,
                            AsyncTaskService asyncTaskService) {
        this.modelUsageQuotaMapper = modelUsageQuotaMapper;
        this.runtimeCostDailyMapper = runtimeCostDailyMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * 查询成本中心首页聚合数据。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 聚合数据
     */
    public UsageDtos.ConsoleData getConsole(LocalDate startDate, LocalDate endDate) {
        UsageDtos.ConsoleData data = new UsageDtos.ConsoleData();
        data.setOverview(getOverview(startDate, endDate));
        data.setDaily(listDailyUsage(startDate, endDate));
        data.setModelBreakdown(listBreakdown("model", startDate, endDate, 8));
        data.setAgentBreakdown(listBreakdown("agent", startDate, endDate, 8));
        return data;
    }

    /**
     * 查询用量总览。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 总览数据
     */
    public UsageDtos.Overview getOverview(LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
        String sql = """
                SELECT COUNT(1) call_count,
                       COALESCE(SUM(CASE WHEN c.success = 1 THEN 1 ELSE 0 END), 0) success_count,
                       COALESCE(SUM(CASE WHEN c.success = 0 THEN 1 ELSE 0 END), 0) failure_count,
                       COALESCE(SUM(c.prompt_tokens), 0) prompt_tokens,
                       COALESCE(SUM(c.completion_tokens), 0) completion_tokens,
                       COALESCE(SUM(c.total_tokens), 0) total_tokens,
                       COALESCE(SUM(c.cost_amount), 0) total_cost,
                       COALESCE(AVG(c.latency_ms), 0) avg_latency_ms
                FROM runtime_llm_call c
                WHERE c.created_at >= ? AND c.created_at < ?
                """;
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, range.start(), range.end());
        UsageDtos.Overview overview = new UsageDtos.Overview();
        overview.setCallCount(longValue(row.get("call_count")));
        overview.setSuccessCount(longValue(row.get("success_count")));
        overview.setFailureCount(longValue(row.get("failure_count")));
        overview.setPromptTokens(longValue(row.get("prompt_tokens")));
        overview.setCompletionTokens(longValue(row.get("completion_tokens")));
        overview.setTotalTokens(longValue(row.get("total_tokens")));
        overview.setTotalCost(decimalValue(row.get("total_cost")));
        overview.setAvgLatencyMs(decimalValue(row.get("avg_latency_ms")));
        List<UsageDtos.QuotaSummary> quotas = listQuotas();
        overview.setQuotaRuleCount(quotas.size());
        overview.setQuotaRiskCount((int) quotas.stream().filter(this::isQuotaRisk).count());
        return overview;
    }

    /**
     * 查询每日用量趋势。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日趋势列表
     */
    public List<UsageDtos.DailyUsage> listDailyUsage(LocalDate startDate, LocalDate endDate) {
        DateRange range = normalizeRange(startDate, endDate);
        String sql = """
                SELECT DATE(c.created_at) stat_date,
                       COUNT(1) call_count,
                       COALESCE(SUM(CASE WHEN c.success = 1 THEN 1 ELSE 0 END), 0) success_count,
                       COALESCE(SUM(CASE WHEN c.success = 0 THEN 1 ELSE 0 END), 0) failure_count,
                       COALESCE(SUM(c.total_tokens), 0) total_tokens,
                       COALESCE(SUM(c.cost_amount), 0) total_cost
                FROM runtime_llm_call c
                WHERE c.created_at >= ? AND c.created_at < ?
                GROUP BY DATE(c.created_at)
                ORDER BY stat_date
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UsageDtos.DailyUsage item = new UsageDtos.DailyUsage();
            item.setStatDate(rs.getDate("stat_date").toLocalDate());
            item.setCallCount(rs.getLong("call_count"));
            item.setSuccessCount(rs.getLong("success_count"));
            item.setFailureCount(rs.getLong("failure_count"));
            item.setTotalTokens(rs.getLong("total_tokens"));
            item.setTotalCost(rs.getBigDecimal("total_cost"));
            return item;
        }, range.start(), range.end());
    }

    /**
     * 按维度拆分用量。
     *
     * @param dimension 维度：provider/model/agent/user/workflow/eval
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param limit 返回数量
     * @return 拆分列表
     */
    public List<UsageDtos.BreakdownItem> listBreakdown(String dimension, LocalDate startDate, LocalDate endDate, Integer limit) {
        DateRange range = normalizeRange(startDate, endDate);
        DimensionSql dimensionSql = dimensionSql(dimension);
        String sql = """
                SELECT %s id_value,
                       COALESCE(%s, '未命名') name_value,
                       COUNT(1) call_count,
                       COALESCE(SUM(c.total_tokens), 0) total_tokens,
                       COALESCE(SUM(c.cost_amount), 0) total_cost,
                       COALESCE(AVG(c.latency_ms), 0) avg_latency_ms
                FROM runtime_llm_call c
                LEFT JOIN runtime_run r ON r.id = c.run_id
                LEFT JOIN model_provider p ON p.id = c.provider_id
                LEFT JOIN model_config m ON m.id = c.model_id
                LEFT JOIN agent a ON a.id = r.agent_id
                LEFT JOIN workflow_definition w ON w.id = r.workflow_id
                LEFT JOIN iam_user u ON u.id = r.user_id
                LEFT JOIN eval_task_run etr ON etr.run_id = r.id
                LEFT JOIN eval_task et ON et.id = etr.task_id
                WHERE c.created_at >= ? AND c.created_at < ?
                GROUP BY %s, %s
                ORDER BY total_cost DESC, total_tokens DESC
                LIMIT ?
                """.formatted(dimensionSql.idExpression(), dimensionSql.nameExpression(), dimensionSql.idExpression(), dimensionSql.nameExpression());
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UsageDtos.BreakdownItem item = new UsageDtos.BreakdownItem();
            item.setId(rs.getString("id_value"));
            item.setName(rs.getString("name_value"));
            item.setCallCount(rs.getLong("call_count"));
            item.setTotalTokens(rs.getLong("total_tokens"));
            item.setTotalCost(rs.getBigDecimal("total_cost"));
            item.setAvgLatencyMs(rs.getBigDecimal("avg_latency_ms"));
            return item;
        }, range.start(), range.end(), limit == null ? 10 : Math.min(Math.max(limit, 1), 50));
    }

    /**
     * 分页查询调用明细。
     *
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param providerId 服务商 ID
     * @param modelId 模型 ID
     * @param agentId Agent ID
     * @param keyword 关键词
     * @return 分页明细
     */
    public PageResult<UsageDtos.CallDetail> listCallDetails(Integer pageNo,
                                                            Integer pageSize,
                                                            LocalDate startDate,
                                                            LocalDate endDate,
                                                            String providerId,
                                                            String modelId,
                                                            String agentId,
                                                            String keyword) {
        int current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        // 未指定每页大小时统一按产品规范返回 10 条，前端和开放接口保持一致。
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (current - 1) * size;
        DateRange range = normalizeRange(startDate, endDate);
        List<Object> params = new ArrayList<>();
        String where = buildCallWhere(range, providerId, modelId, agentId, keyword, params);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM runtime_llm_call c LEFT JOIN runtime_run r ON r.id = c.run_id " + where,
                Long.class,
                params.toArray());
        params.add(size);
        params.add(offset);
        String sql = """
                SELECT c.id, c.run_id, c.step_id, r.run_no, r.run_type,
                       p.provider_name, m.model_name, a.agent_name, w.workflow_name, u.display_name,
                       c.prompt_tokens, c.completion_tokens, c.total_tokens, c.cost_amount,
                       c.latency_ms, c.success, c.error_message, c.created_at
                FROM runtime_llm_call c
                LEFT JOIN runtime_run r ON r.id = c.run_id
                LEFT JOIN model_provider p ON p.id = c.provider_id
                LEFT JOIN model_config m ON m.id = c.model_id
                LEFT JOIN agent a ON a.id = r.agent_id
                LEFT JOIN workflow_definition w ON w.id = r.workflow_id
                LEFT JOIN iam_user u ON u.id = r.user_id
                %s
                ORDER BY c.created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(where);
        List<UsageDtos.CallDetail> records = jdbcTemplate.query(sql, (rs, rowNum) -> {
            UsageDtos.CallDetail item = new UsageDtos.CallDetail();
            item.setId(rs.getString("id"));
            item.setRunId(rs.getString("run_id"));
            item.setStepId(rs.getString("step_id"));
            item.setRunNo(rs.getString("run_no"));
            item.setRunType(rs.getString("run_type"));
            item.setProviderName(rs.getString("provider_name"));
            item.setModelName(rs.getString("model_name"));
            item.setAgentName(rs.getString("agent_name"));
            item.setWorkflowName(rs.getString("workflow_name"));
            item.setUserName(rs.getString("display_name"));
            item.setPromptTokens(rs.getInt("prompt_tokens"));
            item.setCompletionTokens(rs.getInt("completion_tokens"));
            item.setTotalTokens(rs.getInt("total_tokens"));
            item.setCostAmount(rs.getBigDecimal("cost_amount"));
            item.setLatencyMs(rs.getInt("latency_ms"));
            item.setSuccess(rs.getBoolean("success"));
            item.setErrorMessage(rs.getString("error_message"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            item.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return item;
        }, params.toArray());
        return new PageResult<>(records, total == null ? 0L : total, current, size);
    }

    /**
     * 查询配额规则。
     *
     * @return 配额规则列表
     */
    public List<UsageDtos.QuotaSummary> listQuotas() {
        return modelUsageQuotaMapper.selectList(new LambdaQueryWrapper<ModelUsageQuotaEntity>()
                        .orderByAsc(ModelUsageQuotaEntity::getSubjectType)
                        .orderByDesc(ModelUsageQuotaEntity::getCreatedAt))
                .stream()
                .peek(this::resetQuotaIfExpired)
                .map(this::toQuotaSummary)
                .toList();
    }

    /**
     * 创建配额规则。
     *
     * @param request 保存请求
     * @return 配额摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public UsageDtos.QuotaSummary createQuota(UsageDtos.QuotaRequest request) {
        ModelUsageQuotaEntity entity = new ModelUsageQuotaEntity();
        entity.setId(newId());
        fillQuota(entity, request);
        entity.setTokenUsed(0L);
        entity.setCostUsed(BigDecimal.ZERO);
        entity.setResetAt(nextResetAt(entity.getQuotaPeriod()));
        modelUsageQuotaMapper.insert(entity);
        return toQuotaSummary(entity);
    }

    /**
     * 更新配额规则。
     *
     * @param id 配额 ID
     * @param request 保存请求
     * @return 配额摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public UsageDtos.QuotaSummary updateQuota(String id, UsageDtos.QuotaRequest request) {
        ModelUsageQuotaEntity entity = requireQuota(id);
        fillQuota(entity, request);
        if (entity.getResetAt() == null) {
            entity.setResetAt(nextResetAt(entity.getQuotaPeriod()));
        }
        modelUsageQuotaMapper.updateById(entity);
        return toQuotaSummary(entity);
    }

    /**
     * 删除配额规则。
     *
     * @param id 配额 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuota(String id) {
        requireQuota(id);
        modelUsageQuotaMapper.deleteById(id);
    }

    /**
     * 模型调用前执行配额预检查。
     *
     * @param userId 用户 ID
     * @param agentId Agent ID
     * @param provider 服务商
     * @param model 模型
     * @param messages 模型消息
     * @param maxTokens 最大输出 Token
     */
    public void assertWithinQuota(String userId,
                                  String agentId,
                                  ModelProviderEntity provider,
                                  ModelConfigEntity model,
                                  List<ChatMessage> messages,
                                  Integer maxTokens) {
        long estimatedTokens = estimateTokens(messages, maxTokens);
        BigDecimal estimatedCost = estimateCost(model, messages, maxTokens);
        // 调用前只做预估拦截，真实用量在调用成功后再累计。
        for (ModelUsageQuotaEntity quota : matchedQuotas(userId, agentId, provider.getId(), model.getId())) {
            resetQuotaIfExpired(quota);
            long tokenAfter = safeLong(quota.getTokenUsed()) + estimatedTokens;
            BigDecimal costAfter = safeDecimal(quota.getCostUsed()).add(estimatedCost);
            if (quota.getTokenLimit() != null && quota.getTokenLimit() > 0 && tokenAfter > quota.getTokenLimit()) {
                throw new BusinessException("MODEL_TOKEN_QUOTA_EXCEEDED", "模型调用预计超过 Token 配额，请调整配额或更换模型");
            }
            if (quota.getCostLimit() != null && quota.getCostLimit().compareTo(BigDecimal.ZERO) > 0 && costAfter.compareTo(quota.getCostLimit()) > 0) {
                throw new BusinessException("MODEL_COST_QUOTA_EXCEEDED", "模型调用预计超过成本配额，请调整配额或更换模型");
            }
        }
    }

    /**
     * 计算一次模型调用的实际成本。
     *
     * @param model 模型配置
     * @param promptTokens 输入 Token
     * @param completionTokens 输出 Token
     * @return 成本金额
     */
    public BigDecimal calculateCost(ModelConfigEntity model, Integer promptTokens, Integer completionTokens) {
        BigDecimal inputPrice = safeDecimal(model.getInputPricePer1k());
        BigDecimal outputPrice = safeDecimal(model.getOutputPricePer1k());
        BigDecimal inputCost = inputPrice.multiply(BigDecimal.valueOf(promptTokens == null ? 0 : promptTokens))
                .divide(BigDecimal.valueOf(1000), COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputPrice.multiply(BigDecimal.valueOf(completionTokens == null ? 0 : completionTokens))
                .divide(BigDecimal.valueOf(1000), COST_SCALE, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 记录成功模型调用后的日报和配额已用值。
     *
     * @param run 运行记录
     * @param provider 服务商
     * @param model 模型
     * @param totalTokens 总 Token
     * @param cost 成本金额
     * @param success 是否成功
     * @param latencyMs 耗时
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordActualUsage(RuntimeRunEntity run,
                                  ModelProviderEntity provider,
                                  ModelConfigEntity model,
                                  Integer totalTokens,
                                  BigDecimal cost,
                                  Boolean success,
                                  Integer latencyMs) {
        BigDecimal safeCost = safeDecimal(cost);
        int safeTokens = totalTokens == null ? 0 : totalTokens;
        upsertDailyUsage(run, provider, model, safeTokens, safeCost, Boolean.TRUE.equals(success), latencyMs);
        incrementMatchedQuotas(run.getUserId(), run.getAgentId(), provider.getId(), model.getId(), safeTokens, safeCost);
    }

    /**
     * 导出调用明细 CSV。
     *
     * @param details 明细分页
     * @return CSV 文本
     */
    public String toCsv(List<UsageDtos.CallDetail> details) {
        StringBuilder builder = new StringBuilder("调用ID,运行ID,运行编号,服务商,模型,Agent,用户,输入Token,输出Token,总Token,成本,耗时ms,成功,错误,创建时间\n");
        for (UsageDtos.CallDetail item : details) {
            builder.append(csv(item.getId())).append(',')
                    .append(csv(item.getRunId())).append(',')
                    .append(csv(item.getRunNo())).append(',')
                    .append(csv(item.getProviderName())).append(',')
                    .append(csv(item.getModelName())).append(',')
                    .append(csv(item.getAgentName())).append(',')
                    .append(csv(item.getUserName())).append(',')
                    .append(item.getPromptTokens()).append(',')
                    .append(item.getCompletionTokens()).append(',')
                    .append(item.getTotalTokens()).append(',')
                    .append(item.getCostAmount()).append(',')
                    .append(item.getLatencyMs()).append(',')
                    .append(Boolean.TRUE.equals(item.getSuccess()) ? "是" : "否").append(',')
                    .append(csv(item.getErrorMessage())).append(',')
                    .append(csv(item.getCreatedAt() == null ? "" : item.getCreatedAt().toString()))
                    .append('\n');
        }
        return builder.toString();
    }

    /**
     * 按当前模型价格重算历史成本。
     *
     * <p>适用于先产生 Token、后补模型单价的情况。重算会同步 LLM 调用、Trace 步骤、运行总成本、成本日报和配额已用成本。</p>
     *
     * @return 更新的 LLM 调用记录数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int recalculateHistoricalCosts() {
        // 先按模型配置单价重算每条 LLM 调用明细成本。
        int updatedCalls = jdbcTemplate.update("""
                UPDATE runtime_llm_call c
                JOIN model_config m ON m.id = c.model_id
                SET c.cost_amount = ROUND(
                    (
                      COALESCE(c.prompt_tokens, 0) * COALESCE(m.input_price_per_1k, 0)
                      + COALESCE(c.completion_tokens, 0) * COALESCE(m.output_price_per_1k, 0)
                    ) / 1000,
                    6
                )
                """);

        // LLM Trace 步骤成本等于同一步骤下的 LLM 调用成本汇总。
        jdbcTemplate.update("""
                UPDATE runtime_trace_step s
                LEFT JOIN (
                    SELECT step_id, COALESCE(SUM(cost_amount), 0) AS step_cost
                    FROM runtime_llm_call
                    GROUP BY step_id
                ) x ON x.step_id = s.id
                SET s.cost_amount = COALESCE(x.step_cost, 0)
                WHERE s.step_type = 'LLM'
                """);

        // 运行总成本等于一次运行内所有 LLM 调用成本汇总。
        jdbcTemplate.update("""
                UPDATE runtime_run r
                LEFT JOIN (
                    SELECT run_id,
                           COALESCE(SUM(prompt_tokens), 0) AS prompt_tokens,
                           COALESCE(SUM(completion_tokens), 0) AS completion_tokens,
                           COALESCE(SUM(total_tokens), 0) AS total_tokens,
                           COALESCE(SUM(cost_amount), 0) AS total_cost
                    FROM runtime_llm_call
                    GROUP BY run_id
                ) x ON x.run_id = r.id
                SET r.prompt_tokens = COALESCE(x.prompt_tokens, r.prompt_tokens),
                    r.completion_tokens = COALESCE(x.completion_tokens, r.completion_tokens),
                    r.total_tokens = COALESCE(x.total_tokens, r.total_tokens),
                    r.total_cost = COALESCE(x.total_cost, 0)
                WHERE x.run_id IS NOT NULL
                """);

        rebuildCostDaily();
        rebuildQuotaUsage();
        return updatedCalls;
    }

    /**
     * 提交历史成本重算任务到 Kafka。
     *
     * @return 异步任务详情
     */
    public AsyncTaskDtos.Detail submitHistoricalCostRecalculation() {
        AsyncTaskEntity task = asyncTaskService.createTask(
                "按当前模型价格重算历史成本",
                "USAGE_COST_RECALCULATION",
                "usage_cost",
                null,
                "runtime_llm_call",
                null,
                null,
                Map.of("scope", "all_llm_calls"));
        return asyncTaskService.getTask(task.getId());
    }

    /**
     * 返回成本重算任务类型。
     */
    @Override
    public String taskType() {
        return "USAGE_COST_RECALCULATION";
    }

    /**
     * 在 Kafka Worker 中重算历史成本。
     */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        asyncTaskService.updateProgress(task.getId(), "cost_recalculation", "正在重算 LLM 调用和运行成本", 30, null);
        int updatedCalls = recalculateHistoricalCosts();
        return Map.of("updatedCalls", updatedCalls);
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     */
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.openagentflow.security.AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    private void rebuildCostDaily() {
        jdbcTemplate.update("DELETE FROM runtime_cost_daily");
        jdbcTemplate.update("""
                INSERT INTO runtime_cost_daily
                  (id, stat_date, provider_id, model_id, agent_id, workflow_id, run_count,
                   success_count, failure_count, total_tokens, total_cost, avg_latency_ms, created_at, updated_at)
                SELECT UUID(),
                       DATE(c.created_at),
                       c.provider_id,
                       c.model_id,
                       r.agent_id,
                       r.workflow_id,
                       COUNT(1),
                       COALESCE(SUM(CASE WHEN c.success = 1 THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN c.success = 0 THEN 1 ELSE 0 END), 0),
                       COALESCE(SUM(c.total_tokens), 0),
                       COALESCE(SUM(c.cost_amount), 0),
                       COALESCE(AVG(c.latency_ms), 0),
                       CURRENT_TIMESTAMP(3),
                       CURRENT_TIMESTAMP(3)
                FROM runtime_llm_call c
                LEFT JOIN runtime_run r ON r.id = c.run_id
                GROUP BY DATE(c.created_at), c.provider_id, c.model_id, r.agent_id, r.workflow_id
                """);
    }

    private void rebuildQuotaUsage() {
        for (ModelUsageQuotaEntity quota : modelUsageQuotaMapper.selectList(new LambdaQueryWrapper<ModelUsageQuotaEntity>())) {
            LocalDateTime periodStart = periodStart(quota.getQuotaPeriod());
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder("""
                    SELECT COALESCE(SUM(c.total_tokens), 0) AS total_tokens,
                           COALESCE(SUM(c.cost_amount), 0) AS total_cost
                    FROM runtime_llm_call c
                    LEFT JOIN runtime_run r ON r.id = c.run_id
                    """);
            if ("ROLE".equalsIgnoreCase(quota.getSubjectType())) {
                sql.append("""
                        LEFT JOIN iam_user_role ur ON ur.user_id = r.user_id
                        LEFT JOIN iam_role role ON role.id = ur.role_id
                        """);
            }
            sql.append(" WHERE c.created_at >= ? ");
            params.add(Timestamp.valueOf(periodStart));
            if (StringUtils.hasText(quota.getProviderId())) {
                sql.append(" AND c.provider_id = ? ");
                params.add(quota.getProviderId());
            }
            if (StringUtils.hasText(quota.getModelId())) {
                sql.append(" AND c.model_id = ? ");
                params.add(quota.getModelId());
            }
            appendQuotaSubjectWhere(sql, params, quota);
            Map<String, Object> row = jdbcTemplate.queryForMap(sql.toString(), params.toArray());
            quota.setTokenUsed(longValue(row.get("total_tokens")));
            quota.setCostUsed(decimalValue(row.get("total_cost")));
            quota.setResetAt(nextResetAt(quota.getQuotaPeriod()));
            modelUsageQuotaMapper.updateById(quota);
        }
    }

    private void appendQuotaSubjectWhere(StringBuilder sql, List<Object> params, ModelUsageQuotaEntity quota) {
        String subjectType = safeText(quota.getSubjectType()).toUpperCase();
        String subjectId = safeText(quota.getSubjectId());
        switch (subjectType) {
            case "USER" -> {
                sql.append(" AND r.user_id = ? ");
                params.add(subjectId);
            }
            case "ROLE" -> {
                sql.append(" AND (role.id = ? OR role.role_code = ? OR CONCAT('ROLE_', role.role_code) = ?) ");
                params.add(subjectId);
                params.add(subjectId);
                params.add(subjectId);
            }
            case "AGENT" -> {
                sql.append(" AND r.agent_id = ? ");
                params.add(subjectId);
            }
            case "PROVIDER" -> {
                sql.append(" AND c.provider_id = ? ");
                params.add(subjectId);
            }
            case "MODEL" -> {
                sql.append(" AND c.model_id = ? ");
                params.add(subjectId);
            }
            default -> {
                // GLOBAL 不追加主体条件。
            }
        }
    }

    private LocalDateTime periodStart(String period) {
        LocalDate today = LocalDate.now();
        if ("monthly".equalsIgnoreCase(period)) {
            return today.withDayOfMonth(1).atStartOfDay();
        }
        return today.atStartOfDay();
    }

    private String buildCallWhere(DateRange range,
                                  String providerId,
                                  String modelId,
                                  String agentId,
                                  String keyword,
                                  List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE c.created_at >= ? AND c.created_at < ? ");
        params.add(range.start());
        params.add(range.end());
        if (StringUtils.hasText(providerId) && !"all".equalsIgnoreCase(providerId)) {
            where.append(" AND c.provider_id = ? ");
            params.add(providerId);
        }
        if (StringUtils.hasText(modelId) && !"all".equalsIgnoreCase(modelId)) {
            where.append(" AND c.model_id = ? ");
            params.add(modelId);
        }
        if (StringUtils.hasText(agentId) && !"all".equalsIgnoreCase(agentId)) {
            where.append(" AND r.agent_id = ? ");
            params.add(agentId);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (r.run_no LIKE ? OR r.input_text LIKE ? OR r.output_text LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        return where.toString();
    }

    private void upsertDailyUsage(RuntimeRunEntity run,
                                  ModelProviderEntity provider,
                                  ModelConfigEntity model,
                                  int totalTokens,
                                  BigDecimal cost,
                                  boolean success,
                                  Integer latencyMs) {
        LocalDate statDate = LocalDate.now();
        RuntimeCostDailyEntity daily = findDaily(statDate, provider.getId(), model.getId(), run.getAgentId(), run.getWorkflowId());
        if (daily == null) {
            daily = new RuntimeCostDailyEntity();
            daily.setId(newId());
            daily.setStatDate(statDate);
            daily.setProviderId(provider.getId());
            daily.setModelId(model.getId());
            daily.setAgentId(run.getAgentId());
            daily.setWorkflowId(run.getWorkflowId());
            daily.setRunCount(0L);
            daily.setSuccessCount(0L);
            daily.setFailureCount(0L);
            daily.setTotalTokens(0L);
            daily.setTotalCost(BigDecimal.ZERO);
            daily.setAvgLatencyMs(BigDecimal.ZERO);
        }
        long oldRunCount = safeLong(daily.getRunCount());
        BigDecimal oldLatencyTotal = safeDecimal(daily.getAvgLatencyMs()).multiply(BigDecimal.valueOf(oldRunCount));
        daily.setRunCount(oldRunCount + 1);
        daily.setSuccessCount(safeLong(daily.getSuccessCount()) + (success ? 1 : 0));
        daily.setFailureCount(safeLong(daily.getFailureCount()) + (success ? 0 : 1));
        daily.setTotalTokens(safeLong(daily.getTotalTokens()) + totalTokens);
        daily.setTotalCost(safeDecimal(daily.getTotalCost()).add(cost));
        daily.setAvgLatencyMs(oldLatencyTotal.add(BigDecimal.valueOf(latencyMs == null ? 0 : latencyMs))
                .divide(BigDecimal.valueOf(daily.getRunCount()), 2, RoundingMode.HALF_UP));
        if (runtimeCostDailyMapper.selectById(daily.getId()) == null) {
            runtimeCostDailyMapper.insert(daily);
        } else {
            runtimeCostDailyMapper.updateById(daily);
        }
    }

    private RuntimeCostDailyEntity findDaily(LocalDate statDate, String providerId, String modelId, String agentId, String workflowId) {
        LambdaQueryWrapper<RuntimeCostDailyEntity> wrapper = new LambdaQueryWrapper<RuntimeCostDailyEntity>()
                .eq(RuntimeCostDailyEntity::getStatDate, statDate)
                .eq(RuntimeCostDailyEntity::getProviderId, providerId)
                .eq(RuntimeCostDailyEntity::getModelId, modelId);
        if (StringUtils.hasText(agentId)) {
            wrapper.eq(RuntimeCostDailyEntity::getAgentId, agentId);
        } else {
            wrapper.isNull(RuntimeCostDailyEntity::getAgentId);
        }
        if (StringUtils.hasText(workflowId)) {
            wrapper.eq(RuntimeCostDailyEntity::getWorkflowId, workflowId);
        } else {
            wrapper.isNull(RuntimeCostDailyEntity::getWorkflowId);
        }
        return runtimeCostDailyMapper.selectList(wrapper.last("limit 1")).stream().findFirst().orElse(null);
    }

    private void incrementMatchedQuotas(String userId, String agentId, String providerId, String modelId, int tokens, BigDecimal cost) {
        for (ModelUsageQuotaEntity quota : matchedQuotas(userId, agentId, providerId, modelId)) {
            resetQuotaIfExpired(quota);
            quota.setTokenUsed(safeLong(quota.getTokenUsed()) + tokens);
            quota.setCostUsed(safeDecimal(quota.getCostUsed()).add(cost));
            modelUsageQuotaMapper.updateById(quota);
        }
    }

    private List<ModelUsageQuotaEntity> matchedQuotas(String userId, String agentId, String providerId, String modelId) {
        return modelUsageQuotaMapper.selectList(new LambdaQueryWrapper<ModelUsageQuotaEntity>())
                .stream()
                .filter(quota -> providerMatched(quota, providerId))
                .filter(quota -> modelMatched(quota, modelId))
                .filter(quota -> subjectMatched(quota, userId, agentId, providerId, modelId))
                .toList();
    }

    private boolean subjectMatched(ModelUsageQuotaEntity quota, String userId, String agentId, String providerId, String modelId) {
        String subjectType = safeText(quota.getSubjectType()).toUpperCase();
        String subjectId = safeText(quota.getSubjectId());
        return switch (subjectType) {
            case "GLOBAL" -> true;
            case "USER" -> StringUtils.hasText(userId) && subjectId.equals(userId);
            case "ROLE" -> roleMatched(userId, subjectId);
            case "AGENT" -> StringUtils.hasText(agentId) && subjectId.equals(agentId);
            case "PROVIDER" -> StringUtils.hasText(providerId) && subjectId.equals(providerId);
            case "MODEL" -> StringUtils.hasText(modelId) && subjectId.equals(modelId);
            default -> false;
        };
    }

    private boolean roleMatched(String userId, String subjectId) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(subjectId)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM iam_user_role ur
                JOIN iam_role r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                  AND (r.id = ? OR r.role_code = ? OR CONCAT('ROLE_', r.role_code) = ?)
                """, Integer.class, userId, subjectId, subjectId, subjectId);
        return count != null && count > 0;
    }

    private boolean providerMatched(ModelUsageQuotaEntity quota, String providerId) {
        return !StringUtils.hasText(quota.getProviderId()) || quota.getProviderId().equals(providerId);
    }

    private boolean modelMatched(ModelUsageQuotaEntity quota, String modelId) {
        return !StringUtils.hasText(quota.getModelId()) || quota.getModelId().equals(modelId);
    }

    private void fillQuota(ModelUsageQuotaEntity entity, UsageDtos.QuotaRequest request) {
        entity.setSubjectType(StringUtils.hasText(request.getSubjectType()) ? request.getSubjectType().trim().toUpperCase() : "GLOBAL");
        entity.setSubjectId(StringUtils.hasText(request.getSubjectId()) ? request.getSubjectId().trim() : "00000000-0000-0000-0000-000000000000");
        entity.setProviderId(StringUtils.hasText(request.getProviderId()) ? request.getProviderId() : null);
        entity.setModelId(StringUtils.hasText(request.getModelId()) ? request.getModelId() : null);
        entity.setQuotaPeriod(StringUtils.hasText(request.getQuotaPeriod()) ? request.getQuotaPeriod().trim().toLowerCase() : "daily");
        entity.setTokenLimit(request.getTokenLimit());
        entity.setCostLimit(request.getCostLimit());
    }

    private void resetQuotaIfExpired(ModelUsageQuotaEntity quota) {
        if (quota.getResetAt() == null || quota.getResetAt().isAfter(LocalDateTime.now())) {
            return;
        }
        quota.setTokenUsed(0L);
        quota.setCostUsed(BigDecimal.ZERO);
        quota.setResetAt(nextResetAt(quota.getQuotaPeriod()));
        modelUsageQuotaMapper.updateById(quota);
    }

    private LocalDateTime nextResetAt(String period) {
        LocalDateTime now = LocalDateTime.now();
        if ("monthly".equalsIgnoreCase(period)) {
            return now.with(TemporalAdjusters.firstDayOfNextMonth()).toLocalDate().atStartOfDay();
        }
        return now.toLocalDate().plusDays(1).atStartOfDay();
    }

    private UsageDtos.QuotaSummary toQuotaSummary(ModelUsageQuotaEntity entity) {
        UsageDtos.QuotaSummary summary = new UsageDtos.QuotaSummary();
        summary.setId(entity.getId());
        summary.setSubjectType(entity.getSubjectType());
        summary.setSubjectId(entity.getSubjectId());
        summary.setProviderId(entity.getProviderId());
        summary.setModelId(entity.getModelId());
        summary.setQuotaPeriod(entity.getQuotaPeriod());
        summary.setTokenLimit(entity.getTokenLimit());
        summary.setCostLimit(entity.getCostLimit());
        summary.setTokenUsed(safeLong(entity.getTokenUsed()));
        summary.setCostUsed(safeDecimal(entity.getCostUsed()));
        summary.setTokenUsageRate(rate(summary.getTokenUsed(), summary.getTokenLimit()));
        summary.setCostUsageRate(rate(summary.getCostUsed(), summary.getCostLimit()));
        summary.setResetAt(entity.getResetAt());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    private boolean isQuotaRisk(UsageDtos.QuotaSummary quota) {
        return quota.getTokenUsageRate().compareTo(BigDecimal.valueOf(80)) >= 0
                || quota.getCostUsageRate().compareTo(BigDecimal.valueOf(80)) >= 0;
    }

    private BigDecimal rate(Long used, Long limit) {
        if (limit == null || limit <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(used == null ? 0 : used)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(limit), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal used, BigDecimal limit) {
        if (limit == null || limit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safeDecimal(used).multiply(BigDecimal.valueOf(100)).divide(limit, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private long estimateTokens(List<ChatMessage> messages, Integer maxTokens) {
        int charCount = messages == null ? 0 : messages.stream()
                .map(ChatMessage::getContent)
                .filter(StringUtils::hasText)
                .mapToInt(String::length)
                .sum();
        return Math.max(1, charCount / 4) + (maxTokens == null ? 0 : Math.max(maxTokens, 0));
    }

    private BigDecimal estimateCost(ModelConfigEntity model, List<ChatMessage> messages, Integer maxTokens) {
        int charCount = messages == null ? 0 : messages.stream()
                .map(ChatMessage::getContent)
                .filter(StringUtils::hasText)
                .mapToInt(String::length)
                .sum();
        int promptTokens = Math.max(1, charCount / 4);
        int completionTokens = maxTokens == null ? 0 : Math.max(maxTokens, 0);
        return calculateCost(model, promptTokens, completionTokens);
    }

    private ModelUsageQuotaEntity requireQuota(String id) {
        ModelUsageQuotaEntity entity = modelUsageQuotaMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("USAGE_QUOTA_NOT_FOUND", "配额规则不存在");
        }
        return entity;
    }

    private DimensionSql dimensionSql(String dimension) {
        return switch (safeText(dimension).toLowerCase()) {
            case "provider" -> new DimensionSql("c.provider_id", "p.provider_name");
            case "agent" -> new DimensionSql("r.agent_id", "a.agent_name");
            case "user" -> new DimensionSql("r.user_id", "u.display_name");
            case "workflow" -> new DimensionSql("r.workflow_id", "w.workflow_name");
            case "eval" -> new DimensionSql("et.id", "et.task_name");
            default -> new DimensionSql("c.model_id", "m.model_name");
        };
    }

    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(6) : startDate;
        return new DateRange(Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.plusDays(1).atTime(LocalTime.MIDNIGHT)));
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private record DateRange(Timestamp start, Timestamp end) {
    }

    private record DimensionSql(String idExpression, String nameExpression) {
    }
}
