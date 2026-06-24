package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
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

    public AsyncTaskService(AsyncTaskMapper asyncTaskMapper,
                            AsyncTaskLogMapper asyncTaskLogMapper,
                            ObjectMapper objectMapper,
                            JdbcTemplate jdbcTemplate) {
        this.asyncTaskMapper = asyncTaskMapper;
        this.asyncTaskLogMapper = asyncTaskLogMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
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
        task.setMaxRetries(1);
        task.setCancelRequested(false);
        task.setRequestPayload(toJson(payload));
        task.setCreatedBy(userId);
        asyncTaskMapper.insert(task);
        appendLog(task.getId(), "info", "accepted", "任务已创建并进入队列", payload, 0);
        return task;
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
        return detail;
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
        return overview;
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
        AsyncTaskEntity task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setCancelRequested(true);
        task.setStatus("canceled");
        task.setCurrentStage("canceled");
        task.setCurrentMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "warn", "canceled", message, null, valueOf(task.getProgressPercent()));
    }

    /**
     * 准备任务重试。
     *
     * @param taskId 任务ID
     * @return 任务实体
     */
    public AsyncTaskEntity prepareRetry(String taskId) {
        AsyncTaskEntity task = requireTask(taskId);
        assertCanManage(task);
        if (!"failed".equals(task.getStatus()) && !"canceled".equals(task.getStatus())) {
            throw new BusinessException("TASK_RETRY_NOT_ALLOWED", "只有失败或已取消的任务可以重试");
        }
        int retryCount = valueOf(task.getRetryCount());
        int maxRetries = valueOf(task.getMaxRetries());
        if (retryCount >= maxRetries) {
            throw new BusinessException("TASK_RETRY_LIMIT", "任务已达到最大重试次数");
        }
        task.setStatus("pending");
        task.setCancelRequested(false);
        task.setRetryCount(retryCount + 1);
        task.setProgressPercent(BigDecimal.ZERO);
        task.setCurrentStage("retrying");
        task.setCurrentMessage("任务已重新进入队列");
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        asyncTaskMapper.updateById(task);
        appendLog(taskId, "info", "retrying", "任务已重新进入队列", null, 0);
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
        if ("EVALUATION_RUN".equals(taskType)) {
            return "评测批量运行";
        }
        if ("MCP_DISCOVERY".equals(taskType)) {
            return "MCP 能力发现";
        }
        if ("DATA_IMPORT".equals(taskType)) {
            return "数据导入";
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
