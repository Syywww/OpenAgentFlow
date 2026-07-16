package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.entity.AsyncTaskEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 企业级文档处理DAG协调器。
 *
 * <p>根任务只负责建立持久化DAG和投递实际处理子任务，节点状态由结构化阶段服务同步。
 * 后续可将任一阶段替换为多个分片Worker，不改变上传接口和根任务聚合协议。</p>
 */
@Service
public class DocumentPipelineDagService implements DistributedTaskHandler {

    /** 延迟完成标识，通知Kafka Worker不要提前结束根任务。 */
    public static final String DEFERRED_RESULT_KEY = "__deferred";

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    /** 根节点、子任务和Outbox原子提交模板。 */
    private final TransactionTemplate transactionTemplate;

    public DocumentPipelineDagService(JdbcTemplate jdbcTemplate,
                                      AsyncTaskService asyncTaskService,
                                      ObjectMapper objectMapper,
                                      TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.asyncTaskService = asyncTaskService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /** 返回文档DAG根任务类型。 */
    @Override
    public String taskType() {
        return "DOCUMENT_DAG_ORCHESTRATE";
    }

    /** 创建持久化节点并幂等投递处理子任务。 */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity rootTask) {
        Map<String, Object> payload = map(rootTask.getRequestPayload());
        String documentId = text(payload.get("documentId"));
        String kbId = text(payload.get("kbId"));
        Map<String, Object> childHolder = new LinkedHashMap<>();
        transactionTemplate.executeWithoutResult(status -> {
            // 先递增文档代际并登记根任务，旧Worker从此无法继续写入。
            int updated = jdbcTemplate.update("""
                    UPDATE knowledge_document
                    SET pipeline_generation=IF(current_pipeline_root_id=?,pipeline_generation,pipeline_generation+1),
                        current_pipeline_root_id=?,
                        metadata=JSON_SET(COALESCE(metadata,JSON_OBJECT()),'$.currentPipelineRootId',?)
                    WHERE id=?
                    """, rootTask.getId(), rootTask.getId(), rootTask.getId(), documentId);
            if (updated != 1) throw new IllegalStateException("知识文档不存在：" + documentId);
            Long generation = jdbcTemplate.queryForObject(
                    "SELECT pipeline_generation FROM knowledge_document WHERE id=?", Long.class, documentId);
            Map<String, Object> fencedPayload = withRoot(payload, rootTask.getId());
            fencedPayload.put("pipelineGeneration", generation == null ? 0L : generation);
            jdbcTemplate.update("""
                    INSERT IGNORE INTO document_pipeline_node
                      (id,root_task_id,document_id,kb_id,stage_code,shard_no,shard_total,dependency_count,status,
                       idempotency_key,input_json,generation_no,expected_item_count,created_at,updated_at)
                    VALUES (?,?,?,?, 'parse',0,1,0,'queued',?,?,?,1,NOW(3),NOW(3))
                    """, UUID.randomUUID().toString(), rootTask.getId(), documentId, kbId,
                    rootTask.getId() + ":parse:0", json(fencedPayload), generation);
            AsyncTaskEntity child = asyncTaskService.createDagChildTask(rootTask,
                    "解析知识文档：" + text(payload.get("fileName")), "DOCUMENT_PIPELINE_PARSE", 0, 1,
                    rootTask.getId() + ":parse-task:0", fencedPayload);
            jdbcTemplate.update("UPDATE document_pipeline_node SET task_id=? WHERE root_task_id=? AND stage_code='parse'",
                    child.getId(), rootTask.getId());
            childHolder.put("id", child.getId());
        });
        String childTaskId = text(childHolder.get("id"));
        asyncTaskService.updateProgress(rootTask.getId(), "dispatched", "文档DAG已建立，等待阶段Worker执行", 5,
                Map.of("childTaskId", childTaskId, "stageCount", 5));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(DEFERRED_RESULT_KEY, true);
        result.put("rootTaskId", rootTask.getId());
        result.put("childTaskId", childTaskId);
        result.put("stageCount", 5);
        return result;
    }

    /** 为阶段参数补充根任务ID。 */
    private Map<String, Object> withRoot(Map<String, Object> payload, String rootTaskId) {
        Map<String, Object> result = new LinkedHashMap<>(payload);
        result.put("rootTaskId", rootTaskId);
        return result;
    }

    private Map<String, Object> map(String json) {
        try { return objectMapper.readValue(json == null ? "{}" : json, new TypeReference<>() {}); }
        catch (Exception ignored) { return Map.of(); }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("文档DAG参数序列化失败", exception); }
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
}
