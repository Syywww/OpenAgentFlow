package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 跨MySQL、对象存储与检索索引的数据生命周期服务。
 */
@Service
public class DataLifecycleService implements DistributedTaskHandler {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 对象存储服务。 */
    private final SharedObjectStorageService objectStorageService;

    /** 异步任务服务。 */
    private final AsyncTaskService asyncTaskService;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    /** Milvus删除服务。 */
    private final MilvusKnowledgeVectorService milvusService;

    /** OpenSearch删除服务。 */
    private final KeywordSearchService keywordSearchService;

    public DataLifecycleService(JdbcTemplate jdbcTemplate,
                                SharedObjectStorageService objectStorageService,
                                AsyncTaskService asyncTaskService,
                                ObjectMapper objectMapper,
                                MilvusKnowledgeVectorService milvusService,
                                KeywordSearchService keywordSearchService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectStorageService = objectStorageService;
        this.asyncTaskService = asyncTaskService;
        this.objectMapper = objectMapper;
        this.milvusService = milvusService;
        this.keywordSearchService = keywordSearchService;
    }

    /** 提交文档彻底清理作业。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitDocumentPurge(String documentId) {
        var documents = jdbcTemplate.queryForList("""
                SELECT d.id, d.kb_id, d.storage_bucket, d.storage_key, k.workspace_id
                FROM knowledge_document d JOIN knowledge_base k ON k.id=d.kb_id WHERE d.id=?
                """, documentId);
        if (documents.isEmpty()) {
            throw new BusinessException("DOCUMENT_NOT_FOUND", "文档不存在");
        }
        Map<String, Object> document = documents.getFirst();
        String jobId = UUID.randomUUID().toString();
        Map<String, Object> targets = Map.of(
                "bucket", text(document.get("storage_bucket")),
                "objectKey", text(document.get("storage_key")),
                "kbId", text(document.get("kb_id")));
        jdbcTemplate.update("""
                INSERT INTO data_lifecycle_job
                  (id, workspace_id, resource_type, resource_id, action_type, status, storage_targets, created_at)
                VALUES (?, ?, 'knowledge_document', ?, 'purge', 'pending', ?, NOW(3))
                """, jobId, document.get("workspace_id"), documentId, json(targets));
        AsyncTaskEntity task = asyncTaskService.createTask("彻底清理知识文档", "DATA_LIFECYCLE_PURGE",
                "knowledge_document", documentId, "data_lifecycle_job", jobId,
                text(document.get("workspace_id")), Map.of("jobId", jobId, "documentId", documentId));
        return Map.of("jobId", jobId, "taskId", task.getId(), "status", "pending");
    }

    /** 返回生命周期任务类型。 */
    @Override
    public String taskType() {
        return "DATA_LIFECYCLE_PURGE";
    }

    /** 执行跨存储文档清理。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        Map<String, Object> payload = map(task.getRequestPayload());
        String jobId = text(payload.get("jobId"));
        String documentId = text(payload.get("documentId"));
        Map<String, Object> job = jdbcTemplate.queryForMap("SELECT * FROM data_lifecycle_job WHERE id=?", jobId);
        Map<String, Object> targets = map(text(job.get("storage_targets")));
        jdbcTemplate.update("UPDATE data_lifecycle_job SET status='running' WHERE id=?", jobId);

        // 先清理对象存储，随后在一个数据库事务内删除向量映射、分片与文档元数据。
        if (!text(targets.get("objectKey")).isBlank()) {
            objectStorageService.delete(text(targets.get("bucket")), text(targets.get("objectKey")));
        }
        List<Map<String, Object>> vectorTargets = jdbcTemplate.queryForList("""
                SELECT DISTINCT COALESCE(e.milvus_collection_name,k.milvus_collection_name) collection_name,e.embedding_dim
                FROM knowledge_embedding e JOIN knowledge_chunk c ON c.id=e.chunk_id
                JOIN knowledge_base k ON k.id=c.kb_id WHERE c.document_id=? AND e.embedding_dim IS NOT NULL
                """, documentId);
        for (Map<String, Object> vectorTarget : vectorTargets) {
            milvusService.deleteDocument(text(vectorTarget.get("collection_name")),
                    ((Number) vectorTarget.get("embedding_dim")).intValue(), documentId);
        }
        keywordSearchService.deleteDocument(text(targets.get("kbId")), documentId);
        int embeddings = jdbcTemplate.update("DELETE e FROM knowledge_embedding e JOIN knowledge_chunk c ON c.id=e.chunk_id WHERE c.document_id=?", documentId);
        int chunks = jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE document_id=?", documentId);
        int documents = jdbcTemplate.update("DELETE FROM knowledge_document WHERE id=?", documentId);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("mysqlEmbeddings", embeddings);
        counts.put("mysqlChunks", chunks);
        counts.put("mysqlDocuments", documents);
        counts.put("objectDeleted", !text(targets.get("objectKey")).isBlank());
        counts.put("vectorCleanupMode", "milvus-delete-and-compaction");
        jdbcTemplate.update("UPDATE data_lifecycle_job SET status='success', deleted_counts=?, finished_at=NOW(3) WHERE id=?", json(counts), jobId);
        return counts;
    }

    private Map<String, Object> map(String json) {
        try { return objectMapper.readValue(json == null ? "{}" : json, new TypeReference<>() {}); }
        catch (Exception ignored) { return Map.of(); }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("生命周期参数序列化失败", exception); }
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
}
