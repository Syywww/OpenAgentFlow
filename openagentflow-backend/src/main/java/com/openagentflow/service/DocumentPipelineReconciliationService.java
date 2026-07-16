package com.openagentflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档分布式流水线自动对账服务。
 *
 * <p>定期核对DAG预期条目、MySQL分片、已同步向量和最后活跃时间，防止部分成功长期不可见。</p>
 */
@Service
@ConditionalOnProperty(prefix = "openagentflow.document-pipeline", name = "reconcile-enabled",
        havingValue = "true", matchIfMissing = true)
public class DocumentPipelineReconciliationService {

    /** JDBC数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** 单轮最多扫描文档数。 */
    private final int batchSize;

    /** 流水线无更新后判定停滞的分钟数。 */
    private final int staleMinutes;

    public DocumentPipelineReconciliationService(
            JdbcTemplate jdbcTemplate,
            @Value("${openagentflow.document-pipeline.reconcile-batch-size:100}") int batchSize,
            @Value("${openagentflow.document-pipeline.reconcile-stale-minutes:15}") int staleMinutes) {
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = Math.max(10, Math.min(batchSize, 1000));
        this.staleMinutes = Math.max(5, staleMinutes);
    }

    /** 按固定间隔扫描当前有效流水线并更新治理问题。 */
    @Scheduled(initialDelayString = "${openagentflow.document-pipeline.reconcile-delay-ms:60000}",
            fixedDelayString = "${openagentflow.document-pipeline.reconcile-delay-ms:60000}")
    @Transactional
    public void reconcile() {
        List<PipelineSnapshot> documents = jdbcTemplate.query("""
                SELECT d.id,d.kb_id,d.current_pipeline_root_id,d.pipeline_generation,d.parse_status,d.updated_at,
                       CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(d.metadata,'$.expectedChunkCount')),'0') AS UNSIGNED) expected_chunks,
                       CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(d.metadata,'$.expectedEmbeddingCount')),'0') AS UNSIGNED) expected_embeddings
                FROM knowledge_document d
                WHERE d.current_pipeline_root_id IS NOT NULL
                  AND d.parse_status IN ('pending','processing','parsed','failed')
                ORDER BY d.updated_at ASC LIMIT ?
                """, (resultSet, rowNum) -> new PipelineSnapshot(
                resultSet.getString("id"), resultSet.getString("kb_id"),
                resultSet.getString("current_pipeline_root_id"), resultSet.getLong("pipeline_generation"),
                resultSet.getString("parse_status"), resultSet.getObject("updated_at", LocalDateTime.class),
                resultSet.getLong("expected_chunks"), resultSet.getLong("expected_embeddings")), batchSize);
        for (PipelineSnapshot document : documents) {
            reconcileDocument(document);
        }
    }

    /** 核对单份文档并幂等新增或关闭问题。 */
    private void reconcileDocument(PipelineSnapshot document) {
        long chunks = count("SELECT COUNT(1) FROM knowledge_chunk WHERE document_id=?", document.documentId());
        long embeddings = count("""
                SELECT COUNT(1) FROM knowledge_embedding e
                JOIN knowledge_chunk c ON c.id=e.chunk_id
                WHERE c.document_id=? AND e.sync_status='synced'
                """, document.documentId());
        boolean terminal = "parsed".equals(document.parseStatus()) || "failed".equals(document.parseStatus());
        reconcileCount(document, "CHUNK_COUNT_MISMATCH", document.expectedChunks(), chunks,
                terminal && document.expectedChunks() > 0);
        reconcileCount(document, "EMBEDDING_COUNT_MISMATCH", document.expectedEmbeddings(), embeddings,
                terminal && document.expectedEmbeddings() > 0);

        long activeNodes = count("""
                SELECT COUNT(1) FROM document_pipeline_node
                WHERE root_task_id=? AND status IN ('pending','queued','running')
                """, document.rootTaskId());
        boolean stale = activeNodes > 0 && document.updatedAt() != null
                && document.updatedAt().isBefore(LocalDateTime.now().minusMinutes(staleMinutes));
        if (stale) {
            upsertIssue(document, "PIPELINE_STALLED", activeNodes, activeNodes,
                    Map.of("activeNodeCount", activeNodes, "staleMinutes", staleMinutes));
        } else {
            resolveIssue(document, "PIPELINE_STALLED");
        }
    }

    /** 根据预期和实际数量维护一种数量问题。 */
    private void reconcileCount(PipelineSnapshot document,
                                String issueType,
                                long expected,
                                long actual,
                                boolean shouldCheck) {
        if (shouldCheck && expected != actual) {
            upsertIssue(document, issueType, expected, actual,
                    Map.of("parseStatus", document.parseStatus()));
        } else {
            resolveIssue(document, issueType);
        }
    }

    /** 幂等写入未关闭的对账问题，并刷新最近发现时间。 */
    private void upsertIssue(PipelineSnapshot document,
                             String issueType,
                             long expected,
                             long actual,
                             Map<String, Object> detail) {
        jdbcTemplate.update("""
                INSERT INTO document_pipeline_reconcile_issue
                  (id,document_id,kb_id,root_task_id,pipeline_generation,issue_type,severity,
                   expected_count,actual_count,detail_json,status,first_detected_at,last_detected_at,created_at,updated_at)
                VALUES (?,?,?,?,?,?,'high',?,?,JSON_OBJECT('summary',?),'open',NOW(3),NOW(3),NOW(3),NOW(3))
                ON DUPLICATE KEY UPDATE expected_count=VALUES(expected_count),actual_count=VALUES(actual_count),
                  detail_json=VALUES(detail_json),status='open',resolved_at=NULL,last_detected_at=NOW(3),updated_at=NOW(3)
                """, UUID.randomUUID().toString(), document.documentId(), document.kbId(), document.rootTaskId(),
                document.generation(), issueType, expected, actual, detail.toString());
    }

    /** 数量恢复正常后自动关闭当前代际对应的问题。 */
    private void resolveIssue(PipelineSnapshot document, String issueType) {
        jdbcTemplate.update("""
                UPDATE document_pipeline_reconcile_issue
                SET status='resolved',resolved_at=NOW(3),updated_at=NOW(3)
                WHERE document_id=? AND root_task_id=? AND issue_type=? AND status='open'
                """, document.documentId(), document.rootTaskId(), issueType);
    }

    /** 执行计数查询并把空值转换为零。 */
    private long count(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    /**
     * 单份文档的流水线快照。
     *
     * @param documentId 文档ID
     * @param kbId 知识库ID
     * @param rootTaskId 当前根任务ID
     * @param generation 当前代次
     * @param parseStatus 解析状态
     * @param updatedAt 最近更新时间
     * @param expectedChunks 预期分片数
     * @param expectedEmbeddings 预期向量数
     */
    private record PipelineSnapshot(String documentId, String kbId, String rootTaskId, long generation,
                                    String parseStatus, LocalDateTime updatedAt,
                                    long expectedChunks, long expectedEmbeddings) { }
}
