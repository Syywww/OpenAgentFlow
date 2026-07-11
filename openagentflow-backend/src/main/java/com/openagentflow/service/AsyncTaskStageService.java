package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.task.AsyncTaskDtos;
import com.openagentflow.entity.AsyncTaskStageEntity;
import com.openagentflow.mapper.AsyncTaskStageMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 异步任务结构化阶段服务。
 */
@Service
public class AsyncTaskStageService {

    /** 阶段 Mapper。 */
    private final AsyncTaskStageMapper stageMapper;

    /** JDBC 工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    public AsyncTaskStageService(AsyncTaskStageMapper stageMapper,
                                 JdbcTemplate jdbcTemplate,
                                 ObjectMapper objectMapper) {
        this.stageMapper = stageMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 标记阶段开始；相同任务和阶段在重试时覆盖为最新执行代次。
     */
    public void start(String taskId, String stageCode, String stageName, int order, Map<String, Object> input) {
        AsyncTaskExecutionContext.Lease lease = requireLease(taskId);
        jdbcTemplate.update("""
                INSERT INTO async_task_stage
                  (id, task_id, stage_code, stage_name, stage_order, shard_no, attempt_no, status, worker_id, lock_version,
                   input_json, started_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, 'running', ?, ?, CAST(? AS JSON), NOW(3), NOW(3), NOW(3))
                ON DUPLICATE KEY UPDATE
                  stage_name = VALUES(stage_name), stage_order = VALUES(stage_order), status = 'running',
                  worker_id = VALUES(worker_id), lock_version = VALUES(lock_version), input_json = VALUES(input_json),
                  output_json = NULL, error_message = NULL, started_at = NOW(3), finished_at = NULL, updated_at = NOW(3)
                """, UUID.randomUUID().toString(), taskId, stageCode, stageName, order, lease.lockVersion(),
                lease.workerId(), lease.lockVersion(), toJson(input));
        jdbcTemplate.update("""
                UPDATE document_pipeline_node n
                JOIN async_task t ON t.root_task_id=n.root_task_id
                SET n.status='running', n.attempt_no=n.attempt_no+1, n.started_at=NOW(3), n.updated_at=NOW(3)
                WHERE t.id=? AND n.task_id=t.id AND n.stage_code=?
                """, taskId, stageCode);
    }

    /**
     * 标记阶段成功，只允许当前执行代次提交。
     */
    public void succeed(String taskId, String stageCode, Map<String, Object> output) {
        AsyncTaskExecutionContext.Lease lease = requireLease(taskId);
        int changed = jdbcTemplate.update("""
                UPDATE async_task_stage
                SET status = 'success', output_json = CAST(? AS JSON), error_message = NULL,
                    finished_at = NOW(3), updated_at = NOW(3)
                WHERE task_id = ? AND stage_code = ? AND worker_id = ? AND lock_version = ?
                """, toJson(output), taskId, stageCode, lease.workerId(), lease.lockVersion());
        if (changed == 0) {
            throw new IllegalStateException("TASK_STAGE_LEASE_LOST：阶段已被新 Worker 接管");
        }
        jdbcTemplate.update("""
                UPDATE document_pipeline_node n
                JOIN async_task t ON t.root_task_id=n.root_task_id
                SET n.status='success', n.output_json=CAST(? AS JSON), n.finished_at=NOW(3), n.updated_at=NOW(3)
                WHERE t.id=? AND n.task_id=t.id AND n.stage_code=?
                """, toJson(output), taskId, stageCode);
        jdbcTemplate.update("""
                UPDATE document_pipeline_node next_node
                JOIN document_pipeline_node current_node ON current_node.root_task_id=next_node.root_task_id
                JOIN async_task t ON t.root_task_id=current_node.root_task_id
                SET next_node.dependency_count=GREATEST(0,next_node.dependency_count-1), next_node.updated_at=NOW(3)
                WHERE t.id=? AND current_node.task_id=t.id AND current_node.stage_code=? AND next_node.status='pending'
                  AND next_node.stage_code = CASE current_node.stage_code
                    WHEN 'parse' THEN 'chunk' WHEN 'chunk' THEN 'embedding'
                    WHEN 'embedding' THEN 'persist' WHEN 'persist' THEN 'index' ELSE '__none__' END
                """, taskId, stageCode);
    }

    /**
     * 标记阶段失败。
     */
    public void fail(String taskId, String stageCode, String error) {
        AsyncTaskExecutionContext.Lease lease = requireLease(taskId);
        jdbcTemplate.update("""
                UPDATE async_task_stage
                SET status = 'failed', error_message = ?, finished_at = NOW(3), updated_at = NOW(3)
                WHERE task_id = ? AND stage_code = ? AND worker_id = ? AND lock_version = ?
                """, limit(error, 4000), taskId, stageCode, lease.workerId(), lease.lockVersion());
        jdbcTemplate.update("""
                UPDATE document_pipeline_node n
                JOIN async_task t ON t.root_task_id=n.root_task_id
                SET n.status='failed', n.error_message=?, n.finished_at=NOW(3), n.updated_at=NOW(3)
                WHERE t.id=? AND n.task_id=t.id AND n.stage_code=?
                """, limit(error, 4000), taskId, stageCode);
    }

    /**
     * 查询任务阶段时间线。
     */
    public List<AsyncTaskDtos.StageItem> list(String taskId) {
        return stageMapper.selectList(new LambdaQueryWrapper<AsyncTaskStageEntity>()
                        .eq(AsyncTaskStageEntity::getTaskId, taskId)
                        .orderByAsc(AsyncTaskStageEntity::getStageOrder))
                .stream()
                .map(this::toItem)
                .toList();
    }

    private AsyncTaskExecutionContext.Lease requireLease(String taskId) {
        AsyncTaskExecutionContext.Lease lease = AsyncTaskExecutionContext.current();
        if (lease == null || !taskId.equals(lease.taskId())) {
            throw new IllegalStateException("任务阶段缺少 Worker 执行租约");
        }
        return lease;
    }

    private AsyncTaskDtos.StageItem toItem(AsyncTaskStageEntity entity) {
        AsyncTaskDtos.StageItem item = new AsyncTaskDtos.StageItem();
        item.setStageCode(entity.getStageCode());
        item.setStageName(entity.getStageName());
        item.setStageOrder(entity.getStageOrder());
        item.setStatus(entity.getStatus());
        item.setWorkerId(entity.getWorkerId());
        item.setLockVersion(entity.getLockVersion());
        item.setInput(parse(entity.getInputJson()));
        item.setOutput(parse(entity.getOutputJson()));
        item.setErrorMessage(entity.getErrorMessage());
        item.setStartedAt(entity.getStartedAt());
        item.setFinishedAt(entity.getFinishedAt());
        return item;
    }

    private Map<String, Object> parse(String json) {
        try {
            return json == null ? Map.of() : objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
