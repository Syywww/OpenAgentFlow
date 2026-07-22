package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.config.OpenAgentFlowProperties;
import com.openagentflow.domain.task.AsyncTaskDtos;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.AsyncTaskLogEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.AsyncTaskLogMapper;
import com.openagentflow.mapper.AsyncTaskMapper;
import com.openagentflow.security.AuthUserDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 异步任务中心应用服务。
 */
@Service
public class AsyncTaskService {

    /** 异步任务 Mapper。 */
    private final AsyncTaskMapper asyncTaskMapper;

    /** 异步任务日志 Mapper。 */
    private final AsyncTaskLogMapper asyncTaskLogMapper;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    /** JDBC 工具，用于轻量统计和工作空间名称查询。 */
    private final JdbcTemplate jdbcTemplate;

    /** 平台配置，用于读取 Kafka 任务重试策略。 */
    private final OpenAgentFlowProperties properties;

    /** Transactional Outbox 服务。 */
    private final AsyncTaskOutboxService outboxService;

    /** 结构化任务阶段服务。 */
    private final AsyncTaskStageService stageService;

    public AsyncTaskService(AsyncTaskMapper asyncTaskMapper,
                            AsyncTaskLogMapper asyncTaskLogMapper,
                            ObjectMapper objectMapper,
                            JdbcTemplate jdbcTemplate,
                            OpenAgentFlowProperties properties,
                            AsyncTaskOutboxService outboxService,
                            AsyncTaskStageService stageService) {
        this.asyncTaskMapper = asyncTaskMapper;
        this.asyncTaskLogMapper = asyncTaskLogMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.outboxService = outboxService;
        this.stageService = stageService;
    }

    /**
     * 创建异步任务记录。
     *
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param bizType 业务类型
     * @param bizId 业务对象ID
     * @param sourceTable 来源表
     * @param sourceId 来源记录ID
     * @param workspaceId 工作空间ID
     * @param payload 请求参数
     * @return 新建任务实体
     */
    @Transactional(rollbackFor = Exception.class)
    public AsyncTaskEntity createTask(String taskName,
                                      String taskType,
                                      String bizType,
                                      String bizId,
                                      String sourceTable,
                                      String sourceId,
                                      String workspaceId,
                                      Map<String, Object> payload) {
        String userId = currentUserId();
        AsyncTaskEntity task = new AsyncTaskEntity();
        task.setId(newId());
        task.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        task.setTaskCode(uniqueTaskCode(taskType));
        task.setTaskName(taskName);
        task.setTaskType(taskType);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setSourceTable(sourceTable);
        task.setSourceId(sourceId);
        task.setWorkspaceId(workspaceId);
        task.setOwnerUserId(userId);
        task.setStatus("pending");
        task.setPriority(5);
        task.setProgressPercent(BigDecimal.ZERO);
        task.setCurrentStage("accepted");
        task.setCurrentMessage("任务已进入异步队列");
        task.setTotalSteps(6);
        task.setFinishedSteps(0);
        task.setRetryCount(0);
        task.setMaxRetries(Math.max(0, properties.getAsyncTask().getMaxRetries()));
        task.setCancelRequested(false);
        task.setRequestPayload(toJson(payload));
        task.setCreatedBy(userId);
        asyncTaskMapper.insert(task);
        appendLog(task.getId(), "info", "accepted", "任务已创建并进入队列", payload, 0);
        // 任务主表与待发送消息在同一事务提交，消除 MySQL 与 Kafka 双写窗口。
        outboxService.enqueueInitial(task);
        return task;
    }

    /**
     * 幂等创建DAG子任务，并在同一事务写入Outbox。
     *
     * @param parentTask 父任务
     * @param taskName 子任务名称
     * @param taskType 子任务类型
     * @param shardNo 分片序号
     * @param shardTotal 分片总数
     * @param idempotencyKey 幂等键
     * @param payload 子任务参数
     * @return 已存在或新创建的子任务
     */
    @Transactional(rollbackFor = Exception.class)
    public AsyncTaskEntity createDagChildTask(AsyncTaskEntity parentTask,
                                               String taskName,
                                               String taskType,
                                               int shardNo,
                                               int shardTotal,
                                               String idempotencyKey,
                                               Map<String, Object> payload) {
        AsyncTaskEntity existing = asyncTaskMapper.selectOne(new LambdaQueryWrapper<AsyncTaskEntity>()
                .eq(AsyncTaskEntity::getIdempotencyKey, idempotencyKey)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        AsyncTaskEntity child = createTask(taskName, taskType,
                parentTask.getBizType(), parentTask.getBizId(), parentTask.getSourceTable(), parentTask.getSourceId(),
                parentTask.getWorkspaceId(), payload);
        child.setParentTaskId(parentTask.getId());
        child.setRootTaskId(parentTask.getRootTaskId() == null ? parentTask.getId() : parentTask.getRootTaskId());
        child.setTraceId(parentTask.getTraceId());
        child.setShardNo(Math.max(0, shardNo));
        child.setShardTotal(Math.max(1, shardTotal));
        child.setIdempotencyKey(idempotencyKey);
        asyncTaskMapper.updateById(child);
        return child;
    }

    /**
     * 查询DAG根任务下指定类型子任务的完成情况。
     *
     * @param rootTaskId 根任务ID
     * @param taskType 任务类型
     * @return 状态数量映射
     */
    public Map<String, Long> countDagChildren(String rootTaskId, String taskType) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT status, COUNT(1) AS total
                FROM async_task
                WHERE root_task_id = ? AND task_type = ?
                GROUP BY status
                """, rootTaskId, taskType);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("status")), ((Number) row.get("total")).longValue());
        }
        return result;
    }

    /**
     * 子任务完成后汇总DAG根任务；只有全部子任务成功才结束根任务。
     *
     * @param childTask 已完成子任务
     */
    public void completeDagParentIfReady(AsyncTaskEntity childTask) {
        if (childTask == null || !StringUtils.hasText(childTask.getRootTaskId())) {
            return;
        }
        String rootTaskId = childTask.getRootTaskId();
        Long unfinished = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM async_task
                WHERE root_task_id=? AND id<>? AND status NOT IN ('success','canceled','failed','dead_letter')
                """, Long.class, rootTaskId, rootTaskId);
        Long failed = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM async_task
                WHERE root_task_id=? AND status IN ('failed','dead_letter')
                """, Long.class, rootTaskId);
        if (unfinished != null && unfinished == 0L) {
            String status = failed != null && failed > 0 ? "failed" : "success";
            String message = "success".equals(status) ? "文档DAG全部阶段执行完成" : "文档DAG存在失败节点";
            jdbcTemplate.update("""
                    UPDATE async_task SET status=?, progress_percent=?, current_stage=?, current_message=?,
                      finished_at=NOW(3), locked_by=NULL, locked_at=NULL, heartbeat_at=NULL
                    WHERE id=? AND status='running'
                    """, status, "success".equals(status) ? 100 : 99, "success".equals(status) ? "done" : "failed", message, rootTaskId);
            appendLog(rootTaskId, "success".equals(status) ? "info" : "error",
                    "success".equals(status) ? "done" : "failed", message, Map.of("failedChildren", failed == null ? 0L : failed),
                    "success".equals(status) ? 100 : 99);
        }
    }

    /**
     * 分页查询异步任务。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<AsyncTaskDtos.Summary> listTasks(AsyncTaskDtos.Query query) {
        int pageNo = query.getPageNo() == null ? 1 : Math.max(1, query.getPageNo());
        // 未指定每页大小时统一按产品规范返回 10 条，避免异步任务列表一次加载过多。
        int pageSize = query.getPageSize() == null ? 10 : Math.max(1, Math.min(100, query.getPageSize()));
        LambdaQueryWrapper<AsyncTaskEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus()) && !"all".equalsIgnoreCase(query.getStatus())) {
            wrapper.eq(AsyncTaskEntity::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getTaskType()) && !"all".equalsIgnoreCase(query.getTaskType())) {
            wrapper.eq(AsyncTaskEntity::getTaskType, query.getTaskType());
        }
        if (StringUtils.hasText(query.getWorkspaceId()) && !"all".equalsIgnoreCase(query.getWorkspaceId())) {
            wrapper.eq(AsyncTaskEntity::getWorkspaceId, query.getWorkspaceId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(AsyncTaskEntity::getTaskName, keyword)
                    .or()
                    .like(AsyncTaskEntity::getTaskCode, keyword)
                    .or()
                    .like(AsyncTaskEntity::getBizId, keyword));
        }
        if (!isSystemManager()) {
            wrapper.eq(AsyncTaskEntity::getOwnerUserId, currentUserId());
        }
        wrapper.orderByDesc(AsyncTaskEntity::getCreatedAt);
        Page<AsyncTaskEntity> page = asyncTaskMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<AsyncTaskDtos.Summary> records = page.getRecords().stream().map(this::toSummary).toList();
        return new PageResult<>(records, page.getTotal(), pageNo, pageSize);
    }

    /**
     * 查询异步任务详情。
     *
     * @param id 任务ID
     * @return 任务详情
     */
    public AsyncTaskDtos.Detail getTask(String id) {
        AsyncTaskEntity task = requireTask(id);
        assertCanView(task);
        AsyncTaskDtos.Detail detail = new AsyncTaskDtos.Detail();
        copySummary(toSummary(task), detail);
        detail.setRequestPayload(parseMap(task.getRequestPayload()));
        detail.setResultPayload(parseMap(task.getResultPayload()));
        detail.setErrorCode(task.getErrorCode());
        detail.setLogs(asyncTaskLogMapper.selectList(new LambdaQueryWrapper<AsyncTaskLogEntity>()
                        .eq(AsyncTaskLogEntity::getTaskId, id)
                        .orderByAsc(AsyncTaskLogEntity::getCreatedAt))
                .stream()
                .map(this::toLogItem)
                .toList());
        List<AsyncTaskDtos.StageItem> stages = stageService.list(id);
        detail.setStages(stages.isEmpty() ? dagStages(id) : stages);
        return detail;
    }

    /** 把文档DAG节点转换为任务中心可直接展示的阶段时间线。 */
    private List<AsyncTaskDtos.StageItem> dagStages(String rootTaskId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT stage_code, shard_no, status, attempt_no, input_json, output_json,
                       error_message, started_at, finished_at
                FROM document_pipeline_node WHERE root_task_id=?
                ORDER BY FIELD(stage_code,'parse','chunk','embedding','persist','index'), shard_no
                """, rootTaskId);
        int[] order = {0};
        return rows.stream().map(row -> {
            AsyncTaskDtos.StageItem item = new AsyncTaskDtos.StageItem();
            String code = String.valueOf(row.get("stage_code"));
            item.setStageCode(code);
            item.setStageName(Map.of("parse", "解析文档", "chunk", "流式切片", "embedding", "生成向量",
                    "persist", "持久化分片", "index", "写入向量索引").getOrDefault(code, code));
            item.setStageOrder(++order[0]);
            item.setStatus(String.valueOf(row.get("status")));
            item.setInput(parseMap(row.get("input_json") == null ? null : String.valueOf(row.get("input_json"))));
            item.setOutput(parseMap(row.get("output_json") == null ? null : String.valueOf(row.get("output_json"))));
            item.setErrorMessage(row.get("error_message") == null ? null : String.valueOf(row.get("error_message")));
            if (row.get("started_at") instanceof java.sql.Timestamp started) item.setStartedAt(started.toLocalDateTime());
            if (row.get("finished_at") instanceof java.sql.Timestamp finished) item.setFinishedAt(finished.toLocalDateTime());
            return item;
        }).toList();
    }

    /**
     * 查询异步任务统计。
     *
     * @return 任务统计
     */
    public AsyncTaskDtos.Overview overview() {
        AsyncTaskDtos.Overview overview = new AsyncTaskDtos.Overview();
        overview.setTotalCount(countByStatus(null));
        overview.setPendingCount(countByStatus("pending"));
        overview.setRunningCount(countByStatus("running"));
        overview.setSuccessCount(countByStatus("success"));
        overview.setFailedCount(countByStatus("failed"));
        overview.setCanceledCount(countByStatus("canceled"));
        overview.setDeadLetterCount(countByStatus("dead_letter"));
        overview.setOutboxPendingCount(countSql("SELECT COUNT(1) FROM async_task_outbox WHERE status IN ('pending', 'sending', 'failed')"));
        overview.setOutboxDeadCount(countSql("SELECT COUNT(1) FROM async_task_outbox WHERE status = 'dead'"));
        return overview;
    }

    private long countSql(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }

    /**
     * 标记任务开始运行。
     *
     * @param taskId 任务ID
     */
    public void markRunning(String taskId) {
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus("running");
        task.setStartedAt(LocalDateTime.now());
        task.setCurrentStage("running");
        task.setCurrentMessage("任务开始执行");
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "info", "running", "任务开始执行", null, valueOf(task.getProgressPercent()));
    }

    /**
     * 更新任务进度并追加日志。
     *
     * @param taskId 任务ID
     * @param stage 阶段编码
     * @param message 阶段消息
     * @param progress 进度百分比
     * @param detail 日志详情
     */
    public void updateProgress(String taskId, String stage, String message, int progress, Map<String, Object> detail) {
        assertActiveLease(taskId);
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null || "canceled".equals(task.getStatus())) {
            return;
        }
        task.setStatus("running");
        task.setCurrentStage(stage);
        task.setCurrentMessage(message);
        task.setProgressPercent(BigDecimal.valueOf(Math.max(0, Math.min(100, progress))));
        task.setFinishedSteps(Math.max(valueOf(task.getFinishedSteps()), Math.min(valueOf(task.getTotalSteps()), progress / 16)));
        asyncTaskMapper.updateById(task);
        // 任务推进即代表 Worker 仍然存活，同时刷新心跳防止其他实例错误接管。
        jdbcTemplate.update("UPDATE async_task SET heartbeat_at = NOW(3) WHERE id = ? AND status = 'running'", taskId);
        appendLog(taskId, "info", stage, message, detail, progress);
    }

    /**
     * 标记任务执行成功。
     *
     * @param taskId 任务ID
     * @param message 成功消息
     * @param result 结果数据
     */
    public void markSuccess(String taskId, String message, Map<String, Object> result) {
        assertActiveLease(taskId);
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus("success");
        task.setProgressPercent(BigDecimal.valueOf(100));
        task.setCurrentStage("done");
        task.setCurrentMessage(message);
        task.setFinishedSteps(valueOf(task.getTotalSteps()));
        task.setResultPayload(toJson(result));
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setFinishedAt(LocalDateTime.now());
        task.setLockedBy(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(null);
        task.setNextRetryAt(null);
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "info", "done", message, result, 100);
    }

    /**
     * 标记任务执行失败。
     *
     * @param taskId 任务ID
     * @param errorCode 错误编码
     * @param errorMessage 错误消息
     */
    public void markFailed(String taskId, String errorCode, String errorMessage) {
        assertActiveLease(taskId);
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus("failed");
        task.setCurrentStage("failed");
        task.setCurrentMessage(errorMessage);
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        task.setFinishedAt(LocalDateTime.now());
        task.setLockedBy(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(null);
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "error", "failed", errorMessage, Map.of("errorCode", errorCode), valueOf(task.getProgressPercent()));
    }

    /**
     * 请求取消任务。
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    public AsyncTaskDtos.Detail cancelTask(String taskId) {
        AsyncTaskEntity task = requireTask(taskId);
        assertCanManage(task);
        if ("success".equals(task.getStatus()) || "failed".equals(task.getStatus()) || "canceled".equals(task.getStatus())) {
            return getTask(taskId);
        }
        task.setCancelRequested(true);
        task.setStatus("canceled");
        task.setCurrentStage("canceled");
        task.setCurrentMessage("用户已请求取消任务");
        task.setFinishedAt(LocalDateTime.now());
        task.setLockedBy(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(null);
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "warn", "canceled", "用户已请求取消任务，运行中的步骤会在下一个检查点停止", null, valueOf(task.getProgressPercent()));
        return getTask(taskId);
    }

    /**
     * 由异步执行线程标记任务已取消。
     *
     * @param taskId 任务ID
     * @param message 取消说明
     */
    public void markCanceled(String taskId, String message) {
        assertActiveLease(taskId);
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setCancelRequested(true);
        task.setStatus("canceled");
        task.setCurrentStage("canceled");
        task.setCurrentMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        task.setLockedBy(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(null);
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "warn", "canceled", message, null, valueOf(task.getProgressPercent()));
    }

    /**
     * 准备任务重试。
     *
     * @param taskId 任务ID
     * @return 任务实体
     */
    @Transactional(rollbackFor = Exception.class)
    public AsyncTaskEntity prepareRetry(String taskId) {
        AsyncTaskEntity task = requireTask(taskId);
        assertCanManage(task);
        if (!"failed".equals(task.getStatus()) && !"canceled".equals(task.getStatus()) && !"dead_letter".equals(task.getStatus())) {
            throw new BusinessException("TASK_RETRY_NOT_ALLOWED", "只有失败、已取消或死信任务可以重试");
        }
        int retryCount = valueOf(task.getRetryCount());
        int maxRetries = valueOf(task.getMaxRetries());
        boolean deadLetterRedrive = "dead_letter".equals(task.getStatus());
        if (retryCount >= maxRetries && !deadLetterRedrive) {
            throw new BusinessException("TASK_RETRY_LIMIT", "任务已达到最大重试次数");
        }
        task.setStatus("pending");
        task.setCancelRequested(false);
        // 死信人工重投开启一轮新的自动重试周期，其他人工重试沿用累计次数。
        task.setRetryCount(deadLetterRedrive ? 0 : retryCount + 1);
        task.setProgressPercent(BigDecimal.ZERO);
        task.setCurrentStage("retrying");
        task.setCurrentMessage("任务已重新进入队列");
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setLockedBy(null);
        task.setLockedAt(null);
        task.setHeartbeatAt(null);
        task.setNextRetryAt(null);
        task.setDeadLetterAt(null);
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "info", "retrying", "任务已重新进入队列", null, 0);
        outboxService.enqueueInitial(task);
        return task;
    }

    /**
     * 判断任务是否已请求取消。
     *
     * @param taskId 任务ID
     * @return 是否请求取消
     */
    public boolean isCancelRequested(String taskId) {
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        return task != null && Boolean.TRUE.equals(task.getCancelRequested());
    }

    /**
     * 根据ID查询任务实体。
     *
     * @param taskId 任务ID
     * @return 任务实体
     */
    public AsyncTaskEntity findById(String taskId) {
        return asyncTaskMapper.selectById(taskId);
    }

    /**
     * 使用 MySQL 条件更新原子领取任务，保证 Kafka 重复投递时只有一个 Worker 执行。
     *
     * @param taskId 任务ID
     * @param workerId Worker 实例ID
     * @return 领取成功后的执行代次，领取失败返回 null
     */
    public Long tryClaim(String taskId, String workerId) {
        long staleSeconds = Math.max(30L, properties.getAsyncTask().getStaleSeconds());
        int changed = jdbcTemplate.update("""
                UPDATE async_task
                SET status = 'running',
                    locked_by = ?,
                    locked_at = NOW(3),
                    heartbeat_at = NOW(3),
                    lock_version = lock_version + 1,
                    started_at = COALESCE(started_at, NOW(3)),
                    current_stage = 'running',
                    current_message = 'Kafka Worker 已领取任务',
                    next_retry_at = NULL
                WHERE id = ?
                  AND cancel_requested = 0
                  AND (
                    status = 'pending'
                    OR (status = 'running' AND (heartbeat_at IS NULL OR heartbeat_at < DATE_SUB(NOW(3), INTERVAL ? SECOND)))
                  )
                """, workerId, taskId, staleSeconds);
        if (changed > 0) {
            Long lockVersion = jdbcTemplate.queryForObject(
                    "SELECT lock_version FROM async_task WHERE id = ? AND locked_by = ?",
                    Long.class,
                    taskId,
                    workerId);
            appendLog(taskId, "info", "claimed", "Kafka Worker 已领取任务",
                    Map.of("workerId", workerId, "lockVersion", lockVersion == null ? 0L : lockVersion), null);
            return lockVersion;
        }
        return null;
    }

    /**
     * 刷新当前 Worker 的任务心跳。
     *
     * @param taskId 任务ID
     * @param workerId Worker 实例ID
     */
    public void heartbeat(String taskId, String workerId, long lockVersion) {
        jdbcTemplate.update("""
                UPDATE async_task
                SET heartbeat_at = NOW(3)
                WHERE id = ? AND locked_by = ? AND lock_version = ? AND status = 'running'
                """, taskId, workerId, lockVersion);
    }

    /**
     * 确认当前线程仍持有任务执行租约。
     */
    public void assertActiveLease(String taskId) {
        AsyncTaskExecutionContext.Lease lease = AsyncTaskExecutionContext.current();
        if (lease == null || !taskId.equals(lease.taskId())) {
            return;
        }
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM async_task
                WHERE id = ? AND status = 'running' AND locked_by = ? AND lock_version = ?
                """, Long.class, taskId, lease.workerId(), lease.lockVersion());
        if (count == null || count == 0L) {
            throw new IllegalStateException("TASK_LEASE_LOST：任务已被其他 Worker 接管");
        }
    }

    /**
     * 保存可恢复任务检查点。
     */
    public void saveCheckpoint(String taskId, Map<String, Object> checkpoint) {
        assertActiveLease(taskId);
        jdbcTemplate.update("UPDATE async_task SET checkpoint_json = CAST(? AS JSON), updated_at = NOW(3) WHERE id = ?",
                toJson(checkpoint == null ? Map.of() : checkpoint), taskId);
    }

    /**
     * 保存 Kafka 成功投递信息。
     *
     * @param taskId 任务ID
     * @param topic Topic 名称
     * @param messageId 消息ID
     */
    public void markEnqueued(String taskId, String topic, String messageId) {
        jdbcTemplate.update("""
                UPDATE async_task
                SET queue_topic = ?, kafka_message_id = ?, last_enqueued_at = NOW(3)
                WHERE id = ?
                """, topic, messageId, taskId);
    }

    /**
     * 标记任务等待下一次 Kafka 重试。
     *
     * @param taskId 任务ID
     * @param nextRetryAt 下次执行时间
     * @param errorMessage 本次错误
     */
    public void markRetryPending(String taskId, LocalDateTime nextRetryAt, String errorMessage) {
        assertActiveLease(taskId);
        jdbcTemplate.update("""
                UPDATE async_task
                SET status = 'pending',
                    retry_count = retry_count + 1,
                    current_stage = 'retry_waiting',
                    current_message = '任务执行失败，等待 Kafka 重试',
                    error_message = ?,
                    next_retry_at = ?,
                    locked_by = NULL,
                    locked_at = NULL,
                    heartbeat_at = NULL,
                    finished_at = NULL
                WHERE id = ?
                """, limitText(errorMessage, 4000), nextRetryAt, taskId);
        appendLog(taskId, "warn", "retry_waiting", "任务执行失败，等待 Kafka 重试",
                Map.of("nextRetryAt", nextRetryAt.toString(), "error", safeText(errorMessage)), null);
    }

    /**
     * 原子保存业务重试状态与 Kafka 重试 Outbox。
     */
    @Transactional(rollbackFor = Exception.class)
    public void scheduleRetry(String taskId,
                              LocalDateTime nextRetryAt,
                              String errorMessage,
                              int attempt,
                              java.time.Duration delay) {
        markRetryPending(taskId, nextRetryAt, errorMessage);
        AsyncTaskEntity task = requireTask(taskId);
        outboxService.enqueueRetry(task, attempt, delay, errorMessage);
    }

    /**
     * 标记消息已进入死信队列。
     *
     * @param taskId 任务ID
     * @param errorMessage 最终错误
     */
    public void markDeadLetter(String taskId, String errorMessage) {
        assertActiveLease(taskId);
        jdbcTemplate.update("""
                UPDATE async_task
                SET status = 'dead_letter',
                    current_stage = 'dead_letter',
                    current_message = '任务超过最大重试次数，已进入死信队列',
                    error_code = 'KAFKA_TASK_DEAD_LETTER',
                    error_message = ?,
                    dead_letter_at = NOW(3),
                    finished_at = NOW(3),
                    locked_by = NULL,
                    locked_at = NULL,
                    heartbeat_at = NULL
                WHERE id = ?
                """, limitText(errorMessage, 4000), taskId);
        appendLog(taskId, "error", "dead_letter", "任务超过最大重试次数，已进入死信队列",
                Map.of("error", safeText(errorMessage)), null);
    }

    /**
     * 原子保存最终失败状态与 Kafka 死信 Outbox。
     */
    @Transactional(rollbackFor = Exception.class)
    public void scheduleDeadLetter(String taskId, String errorMessage, int attempt) {
        markDeadLetter(taskId, errorMessage);
        outboxService.enqueueDeadLetter(requireTask(taskId), attempt, errorMessage);
    }

    /**
     * 查询需要补偿投递的待执行或失联任务。
     *
     * @param limit 最大返回数量
     * @return 待补偿任务
     */
    public List<AsyncTaskEntity> findRecoverableTasks(int limit) {
        long staleSeconds = Math.max(30L, properties.getAsyncTask().getStaleSeconds());
        return asyncTaskMapper.selectList(new LambdaQueryWrapper<AsyncTaskEntity>()
                .in(AsyncTaskEntity::getTaskType, List.of(
                        "DOCUMENT_PROCESS",
                        "KNOWLEDGE_VECTOR_REBUILD",
                        "EVALUATION_RUN",
                        "MCP_DISCOVERY",
                        "KNOWLEDGE_GOVERNANCE_SCAN",
                        "MEMORY_CLEANUP",
                        "USAGE_COST_RECALCULATION",
                        "TEMPLATE_INSTALL",
                        "TEMPLATE_UPGRADE",
                        "WORKFLOW_RUN"))
                .and(wrapper -> wrapper
                        .and(pending -> pending.eq(AsyncTaskEntity::getStatus, "pending")
                                .and(item -> item.isNull(AsyncTaskEntity::getNextRetryAt)
                                        .or()
                                        .le(AsyncTaskEntity::getNextRetryAt, LocalDateTime.now()))
                                .and(item -> item.isNull(AsyncTaskEntity::getLastEnqueuedAt)
                                        .or()
                                        .apply("last_enqueued_at < DATE_SUB(NOW(3), INTERVAL 60 SECOND)")))
                        .or(stale -> stale.eq(AsyncTaskEntity::getStatus, "running")
                                .apply("(heartbeat_at IS NULL OR heartbeat_at < DATE_SUB(NOW(3), INTERVAL {0} SECOND))", staleSeconds)))
                .orderByAsc(AsyncTaskEntity::getCreatedAt)
                .last("limit " + Math.max(1, Math.min(limit, 500))));
    }

    /**
     * 截断数据库错误字段，避免外部异常返回过长。
     */
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /**
     * 把空错误转换为空字符串，供不可空 Map 使用。
     */
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * 追加任务日志。
     *
     * @param taskId 任务ID
     * @param level 日志级别
     * @param stage 阶段编码
     * @param message 日志消息
     * @param detail 日志详情
     * @param progress 进度百分比
     */
    public void appendLog(String taskId, String level, String stage, String message, Map<String, Object> detail, Integer progress) {
        AsyncTaskLogEntity log = new AsyncTaskLogEntity();
        log.setId(newId());
        log.setTaskId(taskId);
        log.setLogLevel(StringUtils.hasText(level) ? level : "info");
        log.setStage(stage);
        log.setMessage(message);
        log.setDetailJson(toJson(detail));
        log.setProgressPercent(progress == null ? null : BigDecimal.valueOf(progress));
        asyncTaskLogMapper.insert(log);
    }

    /**
     * 查询任务实体，不存在时抛出业务异常。
     *
     * @param id 任务ID
     * @return 任务实体
     */
    private AsyncTaskEntity requireTask(String id) {
        AsyncTaskEntity task = asyncTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("TASK_NOT_FOUND", "异步任务不存在");
        }
        return task;
    }

    /**
     * 校验任务查看权限。
     *
     * @param task 任务实体
     */
    private void assertCanView(AsyncTaskEntity task) {
        if (isSystemManager()) {
            return;
        }
        String userId = currentUserId();
        if (!StringUtils.hasText(userId) || !userId.equals(task.getOwnerUserId())) {
            throw new BusinessException("TASK_FORBIDDEN", "没有查看该任务的权限");
        }
    }

    /**
     * 校验任务管理权限。
     *
     * @param task 任务实体
     */
    private void assertCanManage(AsyncTaskEntity task) {
        assertCanView(task);
    }

    /**
     * 统计指定状态任务数量。
     *
     * @param status 任务状态，空值表示全部
     * @return 任务数量
     */
    private Long countByStatus(String status) {
        LambdaQueryWrapper<AsyncTaskEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(AsyncTaskEntity::getStatus, status);
        }
        if (!isSystemManager()) {
            wrapper.eq(AsyncTaskEntity::getOwnerUserId, currentUserId());
        }
        return asyncTaskMapper.selectCount(wrapper);
    }

    /**
     * 转换任务摘要。
     *
     * @param task 任务实体
     * @return 任务摘要
     */
    private AsyncTaskDtos.Summary toSummary(AsyncTaskEntity task) {
        AsyncTaskDtos.Summary item = new AsyncTaskDtos.Summary();
        item.setId(task.getId());
        item.setTaskCode(task.getTaskCode());
        item.setTaskName(task.getTaskName());
        item.setTaskType(task.getTaskType());
        item.setTaskTypeLabel(taskTypeLabel(task.getTaskType()));
        item.setBizType(task.getBizType());
        item.setBizId(task.getBizId());
        item.setWorkspaceId(task.getWorkspaceId());
        item.setWorkspaceName(findWorkspaceName(task.getWorkspaceId()));
        item.setStatus(task.getStatus());
        item.setProgressPercent(valueOf(task.getProgressPercent()));
        item.setCurrentStage(task.getCurrentStage());
        item.setCurrentMessage(task.getCurrentMessage());
        item.setTotalSteps(task.getTotalSteps());
        item.setFinishedSteps(task.getFinishedSteps());
        item.setRetryCount(task.getRetryCount());
        item.setMaxRetries(task.getMaxRetries());
        item.setCancelRequested(task.getCancelRequested());
        item.setErrorMessage(task.getErrorMessage());
        item.setQueueTopic(task.getQueueTopic());
        item.setLockedBy(task.getLockedBy());
        item.setHeartbeatAt(task.getHeartbeatAt());
        item.setLockVersion(task.getLockVersion());
        item.setCheckpoint(parseMap(task.getCheckpointJson()));
        item.setNextRetryAt(task.getNextRetryAt());
        item.setDeadLetterAt(task.getDeadLetterAt());
        item.setStartedAt(task.getStartedAt());
        item.setFinishedAt(task.getFinishedAt());
        item.setCreatedAt(task.getCreatedAt());
        item.setUpdatedAt(task.getUpdatedAt());
        return item;
    }

    /**
     * 拷贝摘要字段到详情对象。
     *
     * @param source 摘要
     * @param target 详情
     */
    private void copySummary(AsyncTaskDtos.Summary source, AsyncTaskDtos.Detail target) {
        target.setId(source.getId());
        target.setTaskCode(source.getTaskCode());
        target.setTaskName(source.getTaskName());
        target.setTaskType(source.getTaskType());
        target.setTaskTypeLabel(source.getTaskTypeLabel());
        target.setBizType(source.getBizType());
        target.setBizId(source.getBizId());
        target.setWorkspaceId(source.getWorkspaceId());
        target.setWorkspaceName(source.getWorkspaceName());
        target.setStatus(source.getStatus());
        target.setProgressPercent(source.getProgressPercent());
        target.setCurrentStage(source.getCurrentStage());
        target.setCurrentMessage(source.getCurrentMessage());
        target.setTotalSteps(source.getTotalSteps());
        target.setFinishedSteps(source.getFinishedSteps());
        target.setRetryCount(source.getRetryCount());
        target.setMaxRetries(source.getMaxRetries());
        target.setCancelRequested(source.getCancelRequested());
        target.setErrorMessage(source.getErrorMessage());
        target.setQueueTopic(source.getQueueTopic());
        target.setLockedBy(source.getLockedBy());
        target.setHeartbeatAt(source.getHeartbeatAt());
        target.setLockVersion(source.getLockVersion());
        target.setCheckpoint(source.getCheckpoint());
        target.setNextRetryAt(source.getNextRetryAt());
        target.setDeadLetterAt(source.getDeadLetterAt());
        target.setStartedAt(source.getStartedAt());
        target.setFinishedAt(source.getFinishedAt());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    /**
     * 转换任务日志项。
     *
     * @param entity 日志实体
     * @return 日志项
     */
    private AsyncTaskDtos.LogItem toLogItem(AsyncTaskLogEntity entity) {
        AsyncTaskDtos.LogItem item = new AsyncTaskDtos.LogItem();
        item.setId(entity.getId());
        item.setLogLevel(entity.getLogLevel());
        item.setStage(entity.getStage());
        item.setMessage(entity.getMessage());
        item.setDetail(parseMap(entity.getDetailJson()));
        item.setProgressPercent(valueOf(entity.getProgressPercent()));
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    /**
     * 查询工作空间名称。
     *
     * @param workspaceId 工作空间ID
     * @return 工作空间名称
     */
    private String findWorkspaceName(String workspaceId) {
        if (!StringUtils.hasText(workspaceId)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT workspace_name FROM oaf_workspace WHERE id = ? LIMIT 1",
                String.class,
                workspaceId);
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 转换任务类型展示名称。
     *
     * @param taskType 任务类型
     * @return 展示名称
     */
    private String taskTypeLabel(String taskType) {
        if ("DOCUMENT_PROCESS".equals(taskType)) {
            return "知识文档处理";
        }
        if ("KNOWLEDGE_VECTOR_REBUILD".equals(taskType)) {
            return "知识库向量重建";
        }
        if ("EVALUATION_RUN".equals(taskType)) {
            return "评测批量运行";
        }
        if ("MCP_DISCOVERY".equals(taskType)) {
            return "MCP 能力发现";
        }
        if ("KNOWLEDGE_GOVERNANCE_SCAN".equals(taskType)) {
            return "知识治理扫描";
        }
        if ("MEMORY_CLEANUP".equals(taskType)) {
            return "Memory 治理清理";
        }
        if ("USAGE_COST_RECALCULATION".equals(taskType)) {
            return "历史成本重算";
        }
        if ("DATA_IMPORT".equals(taskType)) {
            return "数据导入";
        }
        if ("TEMPLATE_INSTALL".equals(taskType)) {
            return "解决方案模板安装";
        }
        if ("TEMPLATE_UPGRADE".equals(taskType)) {
            return "解决方案模板升级";
        }
        if ("WORKFLOW_RUN".equals(taskType)) {
            return "工作流异步运行";
        }
        return StringUtils.hasText(taskType) ? taskType : "未知任务";
    }

    /**
     * 生成唯一任务编码。
     *
     * @param taskType 任务类型
     * @return 唯一编码
     */
    private String uniqueTaskCode(String taskType) {
        return (StringUtils.hasText(taskType) ? taskType.toLowerCase().replace('_', '-') : "task")
                + "-" + System.currentTimeMillis()
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 判断当前用户是否系统管理员。
     *
     * @return 是否系统管理员
     */
    private boolean isSystemManager() {
        return hasAuthority("ROLE_super_admin") || hasAuthority("ROLE_admin") || hasAuthority("async-task:manage");
    }

    /**
     * 判断当前用户是否拥有指定权限。
     *
     * @param authority 权限标识
     * @return 是否拥有
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
     * 获取当前登录用户ID。
     *
     * @return 当前用户ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }

    /**
     * 解析 JSON Map。
     *
     * @param json JSON 字符串
     * @return Map 对象
     */
    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 转换为 JSON 字符串。
     *
     * @param value 原始对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            if (value == null) {
                return "{}";
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /**
     * 读取整数字段，空值兜底为 0。
     *
     * @param value 整数字段
     * @return 整数值
     */
    private int valueOf(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 读取进度字段，空值兜底为 0。
     *
     * @param value 进度字段
     * @return 整数进度
     */
    private int valueOf(BigDecimal value) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }
}
