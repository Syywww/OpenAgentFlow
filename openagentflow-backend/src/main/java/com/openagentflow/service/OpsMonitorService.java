package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.ops.OpsMonitorDtos;
import com.openagentflow.domain.vector.VectorStoreStatus;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.entity.OpsAlertEventEntity;
import com.openagentflow.entity.OpsAlertRuleEntity;
import com.openagentflow.entity.OpsHealthCheckEntity;
import com.openagentflow.entity.OpsNotifyChannelEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.ModelProviderMapper;
import com.openagentflow.mapper.OpsAlertEventMapper;
import com.openagentflow.mapper.OpsAlertRuleMapper;
import com.openagentflow.mapper.OpsHealthCheckMapper;
import com.openagentflow.mapper.OpsNotifyChannelMapper;
import com.openagentflow.security.AuthUserDetails;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 运营监控与告警中心服务。
 */
@Service
public class OpsMonitorService {

    /** 告警规则 Mapper。 */
    private final OpsAlertRuleMapper alertRuleMapper;

    /** 告警事件 Mapper。 */
    private final OpsAlertEventMapper alertEventMapper;

    /** 巡检项 Mapper。 */
    private final OpsHealthCheckMapper healthCheckMapper;

    /** 通知渠道 Mapper。 */
    private final OpsNotifyChannelMapper notifyChannelMapper;

    /** 模型供应商 Mapper。 */
    private final ModelProviderMapper modelProviderMapper;

    /** JDBC 工具，用于聚合统计已有业务表。 */
    private final JdbcTemplate jdbcTemplate;

    /** Redis 客户端，用于健康巡检。 */
    private final StringRedisTemplate redisTemplate;

    /** 向量库状态服务。 */
    private final VectorStoreService vectorStoreService;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    public OpsMonitorService(OpsAlertRuleMapper alertRuleMapper,
                             OpsAlertEventMapper alertEventMapper,
                             OpsHealthCheckMapper healthCheckMapper,
                             OpsNotifyChannelMapper notifyChannelMapper,
                             ModelProviderMapper modelProviderMapper,
                             JdbcTemplate jdbcTemplate,
                             StringRedisTemplate redisTemplate,
                             VectorStoreService vectorStoreService,
                             ObjectMapper objectMapper) {
        this.alertRuleMapper = alertRuleMapper;
        this.alertEventMapper = alertEventMapper;
        this.healthCheckMapper = healthCheckMapper;
        this.notifyChannelMapper = notifyChannelMapper;
        this.modelProviderMapper = modelProviderMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.vectorStoreService = vectorStoreService;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询运营监控总览。
     *
     * @return 运营监控总览
     */
    public OpsMonitorDtos.Overview overview() {
        assertCanView();
        OpsMonitorDtos.Overview overview = new OpsMonitorDtos.Overview();
        overview.setOpenAlertCount(alertEventMapper.selectCount(new LambdaQueryWrapper<OpsAlertEventEntity>()
                .in(OpsAlertEventEntity::getStatus, List.of("open", "acknowledged"))));
        overview.setCriticalAlertCount(alertEventMapper.selectCount(new LambdaQueryWrapper<OpsAlertEventEntity>()
                .eq(OpsAlertEventEntity::getSeverity, "critical")
                .in(OpsAlertEventEntity::getStatus, List.of("open", "acknowledged"))));
        overview.setHealthyComponentCount(healthCheckMapper.selectCount(new LambdaQueryWrapper<OpsHealthCheckEntity>()
                .eq(OpsHealthCheckEntity::getStatus, "healthy")));
        overview.setUnhealthyComponentCount(healthCheckMapper.selectCount(new LambdaQueryWrapper<OpsHealthCheckEntity>()
                .in(OpsHealthCheckEntity::getStatus, List.of("warning", "unhealthy"))));
        overview.setApiFailureRate(metricValue("api_failure_rate", 60));
        overview.setModelFailureRate(metricValue("model_failure_rate", 60));
        overview.setTaskBacklogCount(metricValue("task_backlog_count", 60).longValue());
        overview.setTodayCost(metricValue("today_cost", 1440));
        overview.setTodayRunCount(metricValue("today_run_count", 1440).longValue());
        overview.setLastInspectionAt(lastInspectionAt());
        return overview;
    }

    /**
     * 查询健康矩阵，返回数据库中最近一次巡检结果。
     *
     * @return 健康组件列表
     */
    public List<OpsMonitorDtos.HealthItem> healthMatrix() {
        assertCanView();
        return healthCheckMapper.selectList(new LambdaQueryWrapper<OpsHealthCheckEntity>()
                        .orderByAsc(OpsHealthCheckEntity::getTargetType)
                        .orderByAsc(OpsHealthCheckEntity::getCheckCode))
                .stream()
                .map(this::toHealthItem)
                .toList();
    }

    /**
     * 执行一次手动巡检，并同步评估告警规则。
     *
     * @return 最新巡检结果
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OpsMonitorDtos.HealthItem> runInspection() {
        assertCanManage();
        LocalDateTime now = LocalDateTime.now();
        updateHealthCheck(checkMysql(now));
        updateHealthCheck(checkRedis(now));
        updateHealthCheck(checkMilvus(now));
        updateHealthCheck(checkModelProviders(now));
        updateHealthCheck(checkTaskQueue(now));
        updateHealthCheck(checkApiQuality(now));
        updateHealthCheck(checkModelQuality(now));
        evaluateAlertRules();
        return healthMatrix();
    }

    /**
     * 分页查询告警规则。
     *
     * @param enabled 是否启用
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 告警规则分页
     */
    public PageResult<OpsMonitorDtos.AlertRuleSummary> listRules(Boolean enabled, String keyword, Integer pageNo, Integer pageSize) {
        assertCanView();
        int current = pageNo == null ? 1 : Math.max(1, pageNo);
        // 运营中心所有列表默认 10 条，和全局列表分页规范保持一致。
        int size = pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize));
        LambdaQueryWrapper<OpsAlertRuleEntity> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(OpsAlertRuleEntity::getEnabled, enabled);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(item -> item.like(OpsAlertRuleEntity::getRuleName, value)
                    .or()
                    .like(OpsAlertRuleEntity::getRuleCode, value)
                    .or()
                    .like(OpsAlertRuleEntity::getMetricCode, value));
        }
        Long total = alertRuleMapper.selectCount(wrapper);
        wrapper.orderByDesc(OpsAlertRuleEntity::getUpdatedAt)
                .last("limit " + ((current - 1) * size) + "," + size);
        List<OpsMonitorDtos.AlertRuleSummary> records = alertRuleMapper.selectList(wrapper).stream().map(this::toRuleSummary).toList();
        return new PageResult<>(records, total, current, size);
    }

    /**
     * 创建告警规则。
     *
     * @param request 告警规则请求
     * @return 告警规则摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public OpsMonitorDtos.AlertRuleSummary createRule(OpsMonitorDtos.AlertRuleRequest request) {
        assertCanManage();
        OpsAlertRuleEntity entity = new OpsAlertRuleEntity();
        entity.setId(UUID.randomUUID().toString());
        fillRule(entity, request);
        entity.setCreatedBy(currentUserId());
        alertRuleMapper.insert(entity);
        return toRuleSummary(entity);
    }

    /**
     * 更新告警规则。
     *
     * @param id 告警规则ID
     * @param request 告警规则请求
     * @return 告警规则摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public OpsMonitorDtos.AlertRuleSummary updateRule(String id, OpsMonitorDtos.AlertRuleRequest request) {
        assertCanManage();
        OpsAlertRuleEntity entity = alertRuleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("OPS_RULE_NOT_FOUND", "告警规则不存在");
        }
        fillRule(entity, request);
        alertRuleMapper.updateById(entity);
        return toRuleSummary(entity);
    }

    /**
     * 删除告警规则。
     *
     * @param id 告警规则ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(String id) {
        assertCanManage();
        alertRuleMapper.deleteById(id);
    }

    /**
     * 分页查询告警事件。
     *
     * @param status 告警状态
     * @param severity 告警级别
     * @param keyword 关键字
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 告警事件分页
     */
    public PageResult<OpsMonitorDtos.AlertEventSummary> listEvents(String status,
                                                                   String severity,
                                                                   String keyword,
                                                                   Integer pageNo,
                                                                   Integer pageSize) {
        assertCanView();
        int current = pageNo == null ? 1 : Math.max(1, pageNo);
        // 运营中心所有列表默认 10 条，和全局列表分页规范保持一致。
        int size = pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize));
        LambdaQueryWrapper<OpsAlertEventEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(OpsAlertEventEntity::getStatus, status);
        }
        if (StringUtils.hasText(severity) && !"all".equalsIgnoreCase(severity)) {
            wrapper.eq(OpsAlertEventEntity::getSeverity, severity);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(item -> item.like(OpsAlertEventEntity::getAlertTitle, value)
                    .or()
                    .like(OpsAlertEventEntity::getRuleCode, value)
                    .or()
                    .like(OpsAlertEventEntity::getMetricCode, value));
        }
        Long total = alertEventMapper.selectCount(wrapper);
        wrapper.orderByDesc(OpsAlertEventEntity::getLastTriggeredAt)
                .last("limit " + ((current - 1) * size) + "," + size);
        List<OpsMonitorDtos.AlertEventSummary> records = alertEventMapper.selectList(wrapper).stream().map(this::toEventSummary).toList();
        return new PageResult<>(records, total, current, size);
    }

    /**
     * 处理告警事件。
     *
     * @param id 告警事件ID
     * @param request 处理请求
     * @return 告警事件摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public OpsMonitorDtos.AlertEventSummary handleEvent(String id, OpsMonitorDtos.AlertHandleRequest request) {
        assertCanManage();
        OpsAlertEventEntity entity = alertEventMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("OPS_ALERT_NOT_FOUND", "告警事件不存在");
        }
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "resolved");
        entity.setHandleNote(request.getHandleNote());
        entity.setHandledBy(currentUserId());
        entity.setHandledAt(LocalDateTime.now());
        alertEventMapper.updateById(entity);
        return toEventSummary(entity);
    }

    /**
     * 查询巡检项列表。
     *
     * @return 巡检项列表
     */
    public List<OpsMonitorDtos.HealthCheckSummary> listChecks() {
        assertCanView();
        return healthCheckMapper.selectList(new LambdaQueryWrapper<OpsHealthCheckEntity>()
                        .orderByAsc(OpsHealthCheckEntity::getTargetType)
                        .orderByAsc(OpsHealthCheckEntity::getCheckCode))
                .stream()
                .map(this::toCheckSummary)
                .toList();
    }

    /**
     * 查询通知渠道列表。
     *
     * @return 通知渠道列表
     */
    public List<OpsMonitorDtos.NotifyChannelSummary> listChannels() {
        assertCanView();
        return notifyChannelMapper.selectList(new LambdaQueryWrapper<OpsNotifyChannelEntity>()
                        .orderByAsc(OpsNotifyChannelEntity::getChannelType)
                        .orderByAsc(OpsNotifyChannelEntity::getChannelCode))
                .stream()
                .map(this::toChannelSummary)
                .toList();
    }

    /**
     * 根据请求填充告警规则实体。
     */
    private void fillRule(OpsAlertRuleEntity entity, OpsMonitorDtos.AlertRuleRequest request) {
        if (!StringUtils.hasText(request.getRuleCode()) || !StringUtils.hasText(request.getRuleName())) {
            throw new BusinessException("OPS_RULE_INVALID", "告警规则编码和名称不能为空");
        }
        entity.setRuleCode(request.getRuleCode().trim());
        entity.setRuleName(request.getRuleName().trim());
        entity.setMetricCode(request.getMetricCode());
        entity.setMetricSource(request.getMetricSource());
        entity.setOperator(StringUtils.hasText(request.getOperator()) ? request.getOperator() : ">=");
        entity.setThresholdValue(request.getThresholdValue() == null ? BigDecimal.ZERO : request.getThresholdValue());
        entity.setSeverity(StringUtils.hasText(request.getSeverity()) ? request.getSeverity() : "warning");
        entity.setWindowMinutes(request.getWindowMinutes() == null ? 60 : request.getWindowMinutes());
        entity.setCooldownMinutes(request.getCooldownMinutes() == null ? 30 : request.getCooldownMinutes());
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setNotifyChannels(StringUtils.hasText(request.getNotifyChannels()) ? request.getNotifyChannels() : "station");
        entity.setDescription(request.getDescription());
    }

    /**
     * 评估所有启用的告警规则。
     */
    private void evaluateAlertRules() {
        List<OpsAlertRuleEntity> rules = alertRuleMapper.selectList(new LambdaQueryWrapper<OpsAlertRuleEntity>()
                .eq(OpsAlertRuleEntity::getEnabled, true));
        for (OpsAlertRuleEntity rule : rules) {
            BigDecimal value = metricValue(rule.getMetricCode(), rule.getWindowMinutes());
            if (matches(rule.getOperator(), value, rule.getThresholdValue()) && outsideCooldown(rule, value)) {
                // 指标命中阈值时创建或更新一个打开中的告警，避免同一规则短时间刷屏。
                upsertAlertEvent(rule, value);
            }
        }
    }

    /**
     * 创建或更新告警事件。
     */
    private void upsertAlertEvent(OpsAlertRuleEntity rule, BigDecimal value) {
        LocalDateTime now = LocalDateTime.now();
        OpsAlertEventEntity event = alertEventMapper.selectOne(new LambdaQueryWrapper<OpsAlertEventEntity>()
                .eq(OpsAlertEventEntity::getRuleId, rule.getId())
                .in(OpsAlertEventEntity::getStatus, List.of("open", "acknowledged"))
                .last("limit 1"));
        if (event == null) {
            event = new OpsAlertEventEntity();
            event.setId(UUID.randomUUID().toString());
            event.setEventCode("ALERT-" + System.currentTimeMillis());
            event.setRuleId(rule.getId());
            event.setRuleCode(rule.getRuleCode());
            event.setFirstTriggeredAt(now);
            event.setTriggerCount(0);
            event.setStatus("open");
        }
        event.setAlertTitle(rule.getRuleName());
        event.setSeverity(rule.getSeverity());
        event.setMetricCode(rule.getMetricCode());
        event.setMetricSource(rule.getMetricSource());
        event.setMetricValue(value);
        event.setThresholdValue(rule.getThresholdValue());
        event.setAlertDetail("指标 " + rule.getMetricCode() + " 当前值 " + value + " 触发阈值 " + rule.getThresholdValue());
        event.setEvidenceJson(toJson(Map.of("operator", rule.getOperator(), "windowMinutes", rule.getWindowMinutes(), "notifyChannels", rule.getNotifyChannels())));
        event.setNotifyStatus("station".equalsIgnoreCase(rule.getNotifyChannels()) || rule.getNotifyChannels().contains("station") ? "sent" : "pending");
        event.setLastTriggeredAt(now);
        event.setTriggerCount((event.getTriggerCount() == null ? 0 : event.getTriggerCount()) + 1);
        if (event.getCreatedAt() == null) {
            alertEventMapper.insert(event);
        } else {
            alertEventMapper.updateById(event);
        }
        sendStationNotification(event);
    }

    /**
     * 写入站内通知并分发给管理员。
     */
    private void sendStationNotification(OpsAlertEventEntity event) {
        String notificationId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into notification(id, notification_type, title, content, severity, resource_type, resource_id, payload, created_by)
                values (?, 'ops_alert', ?, ?, ?, 'ops_alert_event', ?, ?, ?)
                """, notificationId, event.getAlertTitle(), event.getAlertDetail(), event.getSeverity(), event.getId(),
                toJson(Map.of("eventCode", event.getEventCode(), "metricCode", event.getMetricCode())), currentUserId());
        List<String> userIds = jdbcTemplate.queryForList("""
                select distinct u.id
                from iam_user u
                join iam_user_role ur on ur.user_id = u.id
                join iam_role r on r.id = ur.role_id
                where r.role_code in ('super_admin', 'admin') and u.status = 'enabled'
                """, String.class);
        for (String userId : userIds) {
            jdbcTemplate.update("insert into notification_recipient(id, notification_id, user_id) values (?, ?, ?)",
                    UUID.randomUUID().toString(), notificationId, userId);
        }
    }

    /**
     * 判断规则是否处于冷却期之外。
     */
    private boolean outsideCooldown(OpsAlertRuleEntity rule, BigDecimal value) {
        OpsAlertEventEntity event = alertEventMapper.selectOne(new LambdaQueryWrapper<OpsAlertEventEntity>()
                .eq(OpsAlertEventEntity::getRuleId, rule.getId())
                .in(OpsAlertEventEntity::getStatus, List.of("open", "acknowledged"))
                .orderByDesc(OpsAlertEventEntity::getLastTriggeredAt)
                .last("limit 1"));
        if (event == null || event.getLastTriggeredAt() == null) {
            return true;
        }
        int cooldown = rule.getCooldownMinutes() == null ? 30 : rule.getCooldownMinutes();
        return event.getLastTriggeredAt().plusMinutes(cooldown).isBefore(LocalDateTime.now())
                || value.compareTo(event.getMetricValue() == null ? BigDecimal.ZERO : event.getMetricValue()) != 0;
    }

    /**
     * 判断指标值是否命中规则。
     */
    private boolean matches(String operator, BigDecimal value, BigDecimal threshold) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        BigDecimal safeThreshold = threshold == null ? BigDecimal.ZERO : threshold;
        int compare = safeValue.compareTo(safeThreshold);
        return switch (operator == null ? ">=" : operator) {
            case ">" -> compare > 0;
            case "<" -> compare < 0;
            case "<=" -> compare <= 0;
            case "==" -> compare == 0;
            default -> compare >= 0;
        };
    }

    /**
     * 查询指标当前值。
     */
    private BigDecimal metricValue(String metricCode, Integer windowMinutes) {
        int minutes = windowMinutes == null ? 60 : Math.max(1, windowMinutes);
        return switch (metricCode == null ? "" : metricCode) {
            case "api_failure_rate" -> rate(
                    count("select count(1) from audit_operation_log where created_at >= date_sub(now(), interval ? minute) and success = 0", minutes),
                    count("select count(1) from audit_operation_log where created_at >= date_sub(now(), interval ? minute)", minutes));
            case "api_avg_latency_ms" -> decimal("select coalesce(avg(latency_ms), 0) from audit_operation_log where created_at >= date_sub(now(), interval ? minute)", minutes);
            case "model_failure_rate" -> rate(
                    count("select count(1) from runtime_llm_call where created_at >= date_sub(now(), interval ? minute) and success = 0", minutes),
                    count("select count(1) from runtime_llm_call where created_at >= date_sub(now(), interval ? minute)", minutes));
            case "model_avg_latency_ms" -> decimal("select coalesce(avg(latency_ms), 0) from runtime_llm_call where created_at >= date_sub(now(), interval ? minute)", minutes);
            case "task_backlog_count" -> decimal("select count(1) from async_task where status in ('pending', 'running')");
            case "task_failed_count" -> decimal("select count(1) from async_task where status = 'failed' and created_at >= date_sub(now(), interval ? minute)", minutes);
            case "open_risk_count" -> decimal("select count(1) from risk_governance_event where status in ('open', 'reviewing')");
            case "knowledge_issue_open_count" -> decimal("select count(1) from knowledge_governance_issue where status = 'open'");
            case "today_cost" -> decimal("select coalesce(sum(total_cost), 0) from runtime_cost_daily where stat_date = ?", LocalDate.now());
            case "today_run_count" -> decimal("select count(1) from runtime_run where created_at >= curdate()");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * 计算百分比。
     */
    private BigDecimal rate(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /**
     * 查询 Long 指标。
     */
    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    /**
     * 查询 BigDecimal 指标。
     */
    private BigDecimal decimal(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * MySQL 巡检。
     */
    private OpsHealthCheckEntity checkMysql(LocalDateTime now) {
        long start = System.currentTimeMillis();
        OpsHealthCheckEntity entity = baseCheck("mysql", "MySQL 数据库", "database", "mysql", now);
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            entity.setStatus("healthy");
            entity.setMessage("MySQL 连接正常");
        } catch (Exception exception) {
            entity.setStatus("unhealthy");
            entity.setMessage("MySQL 连接失败：" + exception.getMessage());
        }
        entity.setLatencyMs((int) (System.currentTimeMillis() - start));
        return entity;
    }

    /**
     * Redis 巡检。
     */
    private OpsHealthCheckEntity checkRedis(LocalDateTime now) {
        long start = System.currentTimeMillis();
        OpsHealthCheckEntity entity = baseCheck("redis", "Redis 缓存", "cache", "redis", now);
        try {
            String pong = redisTemplate.getConnectionFactory() == null ? null : redisTemplate.getConnectionFactory().getConnection().ping();
            entity.setStatus("PONG".equalsIgnoreCase(pong) ? "healthy" : "warning");
            entity.setMessage("Redis PING：" + (pong == null ? "无响应" : pong));
        } catch (Exception exception) {
            entity.setStatus("unhealthy");
            entity.setMessage("Redis 连接失败：" + exception.getMessage());
        }
        entity.setLatencyMs((int) (System.currentTimeMillis() - start));
        return entity;
    }

    /**
     * Milvus 巡检。
     */
    private OpsHealthCheckEntity checkMilvus(LocalDateTime now) {
        long start = System.currentTimeMillis();
        OpsHealthCheckEntity entity = baseCheck("milvus", "Milvus 向量库", "vector", "milvus", now);
        VectorStoreStatus status = vectorStoreService.getStatus();
        entity.setStatus(Boolean.TRUE.equals(status.getConnected()) ? "healthy" : "warning");
        entity.setMessage(status.getMessage());
        entity.setLatencyMs((int) (System.currentTimeMillis() - start));
        entity.setMetadataJson(toJson(Map.of("host", status.getHost(), "port", status.getPort(), "databaseName", status.getDatabaseName())));
        return entity;
    }

    /**
     * 模型供应商巡检。
     */
    private OpsHealthCheckEntity checkModelProviders(LocalDateTime now) {
        long start = System.currentTimeMillis();
        OpsHealthCheckEntity entity = baseCheck("model_providers", "模型供应商", "model", "model-provider", now);
        long unhealthy = modelProviderMapper.selectCount(new LambdaQueryWrapper<ModelProviderEntity>()
                .eq(ModelProviderEntity::getStatus, "enabled")
                .ne(ModelProviderEntity::getHealthStatus, "healthy"));
        long total = modelProviderMapper.selectCount(new LambdaQueryWrapper<ModelProviderEntity>()
                .eq(ModelProviderEntity::getStatus, "enabled"));
        entity.setStatus(unhealthy > 0 ? "warning" : "healthy");
        entity.setMessage("启用供应商 " + total + " 个，异常 " + unhealthy + " 个");
        entity.setLatencyMs((int) (System.currentTimeMillis() - start));
        entity.setMetadataJson(toJson(Map.of("total", total, "unhealthy", unhealthy)));
        return entity;
    }

    /**
     * 任务队列巡检。
     */
    private OpsHealthCheckEntity checkTaskQueue(LocalDateTime now) {
        OpsHealthCheckEntity entity = baseCheck("async_tasks", "异步任务队列", "task", "async-task", now);
        BigDecimal backlog = metricValue("task_backlog_count", 60);
        BigDecimal failed = metricValue("task_failed_count", 1440);
        entity.setStatus(backlog.compareTo(BigDecimal.valueOf(50)) >= 0 || failed.compareTo(BigDecimal.TEN) >= 0 ? "warning" : "healthy");
        entity.setMessage("积压 " + backlog.longValue() + " 个，近 24 小时失败 " + failed.longValue() + " 个");
        entity.setLatencyMs(0);
        entity.setMetadataJson(toJson(Map.of("backlog", backlog, "failed24h", failed)));
        return entity;
    }

    /**
     * API 质量巡检。
     */
    private OpsHealthCheckEntity checkApiQuality(LocalDateTime now) {
        OpsHealthCheckEntity entity = baseCheck("api_quality", "API 质量", "api", "api", now);
        BigDecimal failureRate = metricValue("api_failure_rate", 60);
        BigDecimal avgLatency = metricValue("api_avg_latency_ms", 60);
        entity.setStatus(failureRate.compareTo(BigDecimal.valueOf(10)) >= 0 || avgLatency.compareTo(BigDecimal.valueOf(3000)) >= 0 ? "warning" : "healthy");
        entity.setMessage("近 1 小时失败率 " + failureRate + "%，平均耗时 " + avgLatency.setScale(0, RoundingMode.HALF_UP) + "ms");
        entity.setLatencyMs(avgLatency.intValue());
        entity.setMetadataJson(toJson(Map.of("failureRate", failureRate, "avgLatencyMs", avgLatency)));
        return entity;
    }

    /**
     * 模型调用质量巡检。
     */
    private OpsHealthCheckEntity checkModelQuality(LocalDateTime now) {
        OpsHealthCheckEntity entity = baseCheck("model_quality", "模型调用质量", "model", "llm-call", now);
        BigDecimal failureRate = metricValue("model_failure_rate", 60);
        BigDecimal avgLatency = metricValue("model_avg_latency_ms", 60);
        entity.setStatus(failureRate.compareTo(BigDecimal.valueOf(10)) >= 0 || avgLatency.compareTo(BigDecimal.valueOf(20000)) >= 0 ? "warning" : "healthy");
        entity.setMessage("近 1 小时失败率 " + failureRate + "%，平均耗时 " + avgLatency.setScale(0, RoundingMode.HALF_UP) + "ms");
        entity.setLatencyMs(avgLatency.intValue());
        entity.setMetadataJson(toJson(Map.of("failureRate", failureRate, "avgLatencyMs", avgLatency)));
        return entity;
    }

    /**
     * 构建巡检基础对象。
     */
    private OpsHealthCheckEntity baseCheck(String code, String name, String targetType, String targetCode, LocalDateTime now) {
        OpsHealthCheckEntity entity = new OpsHealthCheckEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCheckCode(code);
        entity.setCheckName(name);
        entity.setTargetType(targetType);
        entity.setTargetCode(targetCode);
        entity.setLastCheckedAt(now);
        entity.setNextCheckAt(now.plusMinutes(5));
        entity.setCheckIntervalSeconds(300);
        entity.setEnabled(true);
        entity.setMetadataJson("{}");
        return entity;
    }

    /**
     * 更新巡检结果。
     */
    private void updateHealthCheck(OpsHealthCheckEntity check) {
        OpsHealthCheckEntity existing = healthCheckMapper.selectOne(new LambdaQueryWrapper<OpsHealthCheckEntity>()
                .eq(OpsHealthCheckEntity::getCheckCode, check.getCheckCode())
                .last("limit 1"));
        if (existing == null) {
            healthCheckMapper.insert(check);
            return;
        }
        existing.setCheckName(check.getCheckName());
        existing.setTargetType(check.getTargetType());
        existing.setTargetCode(check.getTargetCode());
        existing.setStatus(check.getStatus());
        existing.setMessage(check.getMessage());
        existing.setLatencyMs(check.getLatencyMs());
        existing.setLastCheckedAt(check.getLastCheckedAt());
        existing.setNextCheckAt(check.getNextCheckAt());
        existing.setCheckIntervalSeconds(check.getCheckIntervalSeconds());
        existing.setEnabled(check.getEnabled());
        existing.setMetadataJson(check.getMetadataJson());
        healthCheckMapper.updateById(existing);
    }

    /**
     * 最近一次巡检时间。
     */
    private LocalDateTime lastInspectionAt() {
        OpsHealthCheckEntity entity = healthCheckMapper.selectOne(new LambdaQueryWrapper<OpsHealthCheckEntity>()
                .orderByDesc(OpsHealthCheckEntity::getLastCheckedAt)
                .last("limit 1"));
        return entity == null ? null : entity.getLastCheckedAt();
    }

    /**
     * 转换告警规则摘要。
     */
    private OpsMonitorDtos.AlertRuleSummary toRuleSummary(OpsAlertRuleEntity entity) {
        OpsMonitorDtos.AlertRuleSummary summary = new OpsMonitorDtos.AlertRuleSummary();
        summary.setId(entity.getId());
        summary.setRuleCode(entity.getRuleCode());
        summary.setRuleName(entity.getRuleName());
        summary.setMetricCode(entity.getMetricCode());
        summary.setMetricSource(entity.getMetricSource());
        summary.setOperator(entity.getOperator());
        summary.setThresholdValue(entity.getThresholdValue());
        summary.setSeverity(entity.getSeverity());
        summary.setWindowMinutes(entity.getWindowMinutes());
        summary.setCooldownMinutes(entity.getCooldownMinutes());
        summary.setEnabled(entity.getEnabled());
        summary.setNotifyChannels(entity.getNotifyChannels());
        summary.setDescription(entity.getDescription());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    /**
     * 转换告警事件摘要。
     */
    private OpsMonitorDtos.AlertEventSummary toEventSummary(OpsAlertEventEntity entity) {
        OpsMonitorDtos.AlertEventSummary summary = new OpsMonitorDtos.AlertEventSummary();
        summary.setId(entity.getId());
        summary.setEventCode(entity.getEventCode());
        summary.setRuleCode(entity.getRuleCode());
        summary.setAlertTitle(entity.getAlertTitle());
        summary.setSeverity(entity.getSeverity());
        summary.setMetricCode(entity.getMetricCode());
        summary.setMetricSource(entity.getMetricSource());
        summary.setMetricValue(entity.getMetricValue());
        summary.setThresholdValue(entity.getThresholdValue());
        summary.setAlertDetail(entity.getAlertDetail());
        summary.setEvidence(readMap(entity.getEvidenceJson()));
        summary.setStatus(entity.getStatus());
        summary.setNotifyStatus(entity.getNotifyStatus());
        summary.setHandledBy(entity.getHandledBy());
        summary.setHandledAt(entity.getHandledAt());
        summary.setHandleNote(entity.getHandleNote());
        summary.setLastTriggeredAt(entity.getLastTriggeredAt());
        summary.setTriggerCount(entity.getTriggerCount());
        return summary;
    }

    /**
     * 转换健康矩阵项。
     */
    private OpsMonitorDtos.HealthItem toHealthItem(OpsHealthCheckEntity entity) {
        OpsMonitorDtos.HealthItem item = new OpsMonitorDtos.HealthItem();
        item.setCode(entity.getCheckCode());
        item.setName(entity.getCheckName());
        item.setType(entity.getTargetType());
        item.setStatus(entity.getStatus());
        item.setMessage(entity.getMessage());
        item.setLatencyMs(entity.getLatencyMs());
        item.setCheckedAt(entity.getLastCheckedAt());
        item.setMetadata(readMap(entity.getMetadataJson()));
        return item;
    }

    /**
     * 转换巡检项摘要。
     */
    private OpsMonitorDtos.HealthCheckSummary toCheckSummary(OpsHealthCheckEntity entity) {
        OpsMonitorDtos.HealthCheckSummary summary = new OpsMonitorDtos.HealthCheckSummary();
        summary.setId(entity.getId());
        summary.setCheckCode(entity.getCheckCode());
        summary.setCheckName(entity.getCheckName());
        summary.setTargetType(entity.getTargetType());
        summary.setTargetCode(entity.getTargetCode());
        summary.setStatus(entity.getStatus());
        summary.setMessage(entity.getMessage());
        summary.setLatencyMs(entity.getLatencyMs());
        summary.setLastCheckedAt(entity.getLastCheckedAt());
        summary.setNextCheckAt(entity.getNextCheckAt());
        summary.setEnabled(entity.getEnabled());
        summary.setMetadata(readMap(entity.getMetadataJson()));
        return summary;
    }

    /**
     * 转换通知渠道摘要。
     */
    private OpsMonitorDtos.NotifyChannelSummary toChannelSummary(OpsNotifyChannelEntity entity) {
        OpsMonitorDtos.NotifyChannelSummary summary = new OpsMonitorDtos.NotifyChannelSummary();
        summary.setId(entity.getId());
        summary.setChannelCode(entity.getChannelCode());
        summary.setChannelName(entity.getChannelName());
        summary.setChannelType(entity.getChannelType());
        summary.setEnabled(entity.getEnabled());
        summary.setLastTestStatus(entity.getLastTestStatus());
        summary.setLastTestMessage(entity.getLastTestMessage());
        summary.setLastTestAt(entity.getLastTestAt());
        return summary;
    }

    /**
     * 读取 JSON Map。
     */
    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return Map.of("raw", json);
        }
    }

    /**
     * 写入 JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    /**
     * 校验查看权限。
     */
    private void assertCanView() {
        if (!isOpsUser()) {
            throw new BusinessException("OPS_FORBIDDEN", "没有查看运营监控中心的权限");
        }
    }

    /**
     * 校验管理权限。
     */
    private void assertCanManage() {
        if (!isOpsManager()) {
            throw new BusinessException("OPS_FORBIDDEN", "没有管理运营监控中心的权限");
        }
    }

    /**
     * 判断当前用户是否运营中心可见用户。
     */
    private boolean isOpsUser() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("ops:monitor:view") || hasAuthority("ops:monitor:manage");
    }

    /**
     * 判断当前用户是否运营中心管理员。
     */
    private boolean isOpsManager() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("ops:monitor:manage");
    }

    /**
     * 判断是否拥有指定权限。
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    /**
     * 获取当前用户ID。
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }
}
