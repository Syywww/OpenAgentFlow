package com.openagentflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.entity.AsyncTaskEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 跨MySQL、Milvus和对象存储的数据一致性巡检服务。 */
@Service
public class DataConsistencyService implements DistributedTaskHandler {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 共享对象存储服务。 */
    private final SharedObjectStorageService objectStorageService;

    /** OpenSearch索引服务。 */
    private final KeywordSearchService keywordSearchService;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    public DataConsistencyService(JdbcTemplate jdbcTemplate,
                                  SharedObjectStorageService objectStorageService,
                                  KeywordSearchService keywordSearchService,
                                  ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectStorageService = objectStorageService;
        this.keywordSearchService = keywordSearchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String taskType() { return "DATA_CONSISTENCY_REPAIR"; }

    /** 定期识别缺失向量、孤儿向量映射和长期处理中档。 */
    @Scheduled(cron = "${openagentflow.lifecycle.consistency-cron:0 35 2 * * *}")
    public void scan() {
        detect("""
                SELECT d.id resource_id,k.workspace_id,'knowledge_document' resource_type,'milvus' storage_type,
                       'missing_synced_vector' issue_type,'high' severity
                FROM knowledge_document d JOIN knowledge_base k ON k.id=d.kb_id
                WHERE d.parse_status='parsed' AND NOT EXISTS (
                  SELECT 1 FROM knowledge_chunk c JOIN knowledge_embedding e ON e.chunk_id=c.id
                  WHERE c.document_id=d.id AND e.sync_status='synced') LIMIT 500
                """);
        activeStorageScan();
        detect("""
                SELECT e.id resource_id,k.workspace_id,'knowledge_embedding' resource_type,'mysql' storage_type,
                       'orphan_embedding' issue_type,'medium' severity
                FROM knowledge_embedding e LEFT JOIN knowledge_chunk c ON c.id=e.chunk_id
                LEFT JOIN knowledge_base k ON k.id=e.kb_id WHERE c.id IS NULL LIMIT 500
                """);
        detect("""
                SELECT d.id resource_id,k.workspace_id,'knowledge_document' resource_type,'mysql' storage_type,
                       'processing_timeout' issue_type,'high' severity
                FROM knowledge_document d JOIN knowledge_base k ON k.id=d.kb_id
                WHERE d.parse_status='processing' AND d.uploaded_at<DATE_SUB(NOW(),INTERVAL 2 HOUR) LIMIT 500
                """);
    }

    /** 查询未解决问题。 */
    public List<Map<String, Object>> issues() {
        return jdbcTemplate.queryForList("SELECT * FROM data_consistency_issue WHERE status<>'resolved' ORDER BY detected_at DESC LIMIT 500");
    }

    /** 执行单个一致性问题的幂等修复。 */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        try {
            Map<String, Object> payload = objectMapper.readValue(task.getRequestPayload(), new com.fasterxml.jackson.core.type.TypeReference<>() { });
            String issueId = String.valueOf(payload.get("issueId"));
            Map<String, Object> issue = jdbcTemplate.queryForMap("SELECT * FROM data_consistency_issue WHERE id=?", issueId);
            String issueType = String.valueOf(issue.get("issue_type"));
            String resourceId = String.valueOf(issue.get("resource_id"));
            if ("orphan_embedding".equals(issueType)) {
                jdbcTemplate.update("DELETE FROM knowledge_embedding WHERE id=?", resourceId);
            } else if ("missing_opensearch_document".equals(issueType)) {
                reindexDocument(resourceId);
            } else if ("processing_timeout".equals(issueType) || "missing_synced_vector".equals(issueType)) {
                jdbcTemplate.update("UPDATE knowledge_document SET parse_status='pending',parse_error=NULL WHERE id=?", resourceId);
            } else if ("missing_object".equals(issueType)) {
                throw new IllegalStateException("原始对象缺失无法自动重建，需要从备份恢复");
            }
            jdbcTemplate.update("UPDATE data_consistency_issue SET status='resolved',resolved_at=NOW(3),resolution=? WHERE id=?",
                    "分布式修复任务已完成", issueId);
            return Map.of("issueId", issueId, "status", "resolved", "issueType", issueType);
        } catch (Exception exception) {
            throw new IllegalStateException("一致性问题修复失败：" + exception.getMessage(), exception);
        }
    }

    /** 主动访问对象存储与OpenSearch，识别控制面状态正常但数据面缺失的问题。 */
    private void activeStorageScan() {
        List<Map<String, Object>> documents = jdbcTemplate.queryForList("""
                SELECT d.id,d.kb_id,d.storage_bucket,d.storage_key,k.workspace_id,d.parse_status
                FROM knowledge_document d JOIN knowledge_base k ON k.id=d.kb_id
                WHERE d.deleted_at IS NULL ORDER BY d.uploaded_at DESC LIMIT 500
                """);
        for (Map<String, Object> document : documents) {
            String documentId = String.valueOf(document.get("id"));
            String workspaceId = String.valueOf(document.get("workspace_id"));
            if (!objectStorageService.exists(String.valueOf(document.get("storage_bucket")), String.valueOf(document.get("storage_key")))) {
                saveActiveIssue(workspaceId, documentId, "object_storage", "missing_object", "critical");
            }
            if (keywordSearchService.isEnabled() && "parsed".equalsIgnoreCase(String.valueOf(document.get("parse_status")))) {
                try {
                    if (keywordSearchService.documentChunkCount(String.valueOf(document.get("kb_id")), documentId) == 0L) {
                        saveActiveIssue(workspaceId, documentId, "opensearch", "missing_opensearch_document", "high");
                    }
                } catch (Exception exception) {
                    saveActiveIssue(workspaceId, documentId, "opensearch", "opensearch_unreachable", "high");
                }
            }
        }
    }

    /** 将MySQL分片重新批量写入OpenSearch。 */
    private void reindexDocument(String documentId) {
        List<com.openagentflow.entity.KnowledgeChunkEntity> chunks = jdbcTemplate.query(
                "SELECT * FROM knowledge_chunk WHERE document_id=? AND status='active'",
                new org.springframework.jdbc.core.BeanPropertyRowMapper<>(com.openagentflow.entity.KnowledgeChunkEntity.class), documentId);
        if (!chunks.isEmpty()) keywordSearchService.indexChunks(chunks.getFirst().getKbId(), chunks);
    }

    /** 保存主动探测问题。 */
    private void saveActiveIssue(String workspaceId, String resourceId, String storageType, String issueType, String severity) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO data_consistency_issue
                  (id,workspace_id,resource_type,resource_id,storage_type,issue_type,severity,status,evidence_json,detected_at,created_at)
                VALUES (?,?, 'knowledge_document',?,?,?,?, 'open',JSON_OBJECT('source','active_probe'),NOW(3),NOW(3))
                """, UUID.randomUUID().toString(), workspaceId, resourceId, storageType, issueType, severity);
    }

    private void detect(String sql) {
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
            jdbcTemplate.update("""
                    INSERT IGNORE INTO data_consistency_issue
                      (id,workspace_id,resource_type,resource_id,storage_type,issue_type,severity,status,evidence_json,detected_at,created_at)
                    VALUES (?,?,?,?,?,?,?,'open',JSON_OBJECT('source','scheduled_scan'),NOW(3),NOW(3))
                    """, UUID.randomUUID().toString(), row.get("workspace_id"), row.get("resource_type"), row.get("resource_id"),
                    row.get("storage_type"), row.get("issue_type"), row.get("severity"));
        }
    }
}
