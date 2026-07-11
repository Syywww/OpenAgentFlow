package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.KnowledgeChunkEntity;
import com.openagentflow.entity.KnowledgeEmbeddingEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Milvus蓝绿物理集合异步构建服务。 */
@Service
public class KnowledgeIndexBuildService implements DistributedTaskHandler {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON工具。 */
    private final ObjectMapper objectMapper;

    /** Milvus服务。 */
    private final MilvusKnowledgeVectorService milvusService;

    public KnowledgeIndexBuildService(JdbcTemplate jdbcTemplate,
                                      ObjectMapper objectMapper,
                                      MilvusKnowledgeVectorService milvusService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.milvusService = milvusService;
    }

    /** 返回索引构建任务类型。 */
    @Override
    public String taskType() { return "KNOWLEDGE_INDEX_BUILD"; }

    /** 分批把MySQL向量检查点导入新物理集合。 */
    @Override
    public Map<String, Object> executeDistributedTask(AsyncTaskEntity task) {
        Map<String, Object> payload = map(task.getRequestPayload());
        String versionId = text(payload.get("versionId"));
        String kbId = text(payload.get("kbId"));
        String collectionName = text(payload.get("collectionName"));
        List<Integer> dimensions = jdbcTemplate.queryForList("SELECT DISTINCT embedding_dim FROM knowledge_embedding WHERE kb_id=? AND embedding_json IS NOT NULL", Integer.class, kbId);
        if (dimensions.size() != 1) throw new IllegalStateException("索引版本要求知识库只使用一种向量维度，当前维度数量：" + dimensions.size());
        int dimension = dimensions.getFirst();
        String cursor = "";
        int total = 0;
        while (true) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT e.id embedding_id,e.chunk_id,e.model_id,e.vector_primary_key,e.embedding_json,e.content_hash,
                           c.document_id,c.chunk_no,c.parent_chunk_id,c.chunk_level,c.content,c.status
                    FROM knowledge_embedding e JOIN knowledge_chunk c ON c.id=e.chunk_id
                    WHERE e.kb_id=? AND e.embedding_json IS NOT NULL AND e.id>? ORDER BY e.id LIMIT 500
                    """, kbId, cursor);
            if (rows.isEmpty()) break;
            List<KnowledgeEmbeddingEntity> embeddings = new ArrayList<>();
            List<KnowledgeChunkEntity> chunks = new ArrayList<>();
            List<List<Double>> vectors = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                KnowledgeEmbeddingEntity embedding = new KnowledgeEmbeddingEntity();
                embedding.setId(text(row.get("embedding_id")));
                embedding.setChunkId(text(row.get("chunk_id")));
                embedding.setKbId(kbId);
                embedding.setModelId(text(row.get("model_id")));
                embedding.setVectorPrimaryKey(text(row.get("vector_primary_key")));
                embedding.setContentHash(text(row.get("content_hash")));
                KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
                chunk.setId(text(row.get("chunk_id")));
                chunk.setKbId(kbId);
                chunk.setDocumentId(text(row.get("document_id")));
                chunk.setChunkNo(row.get("chunk_no") instanceof Number n ? n.intValue() : 0);
                chunk.setParentChunkId(text(row.get("parent_chunk_id")));
                chunk.setChunkLevel(text(row.get("chunk_level")));
                chunk.setContent(text(row.get("content")));
                chunk.setStatus(text(row.get("status")));
                embeddings.add(embedding);
                chunks.add(chunk);
                vectors.add(vector(text(row.get("embedding_json"))));
            }
            milvusService.upsertKnowledgeChunks(collectionName, embeddings, chunks, vectors);
            total += rows.size();
            cursor = text(rows.getLast().get("embedding_id"));
        }
        jdbcTemplate.update("UPDATE knowledge_index_version SET status='ready',dimension=?,chunk_count=? WHERE id=?", dimension, total, versionId);
        return Map.of("versionId", versionId, "dimension", dimension, "chunkCount", total, "status", "ready");
    }

    private Map<String, Object> map(String json) {
        try { return objectMapper.readValue(json == null ? "{}" : json, new TypeReference<>() { }); }
        catch (Exception ignored) { return Map.of(); }
    }
    private List<Double> vector(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() { }); }
        catch (Exception exception) { throw new IllegalStateException("向量检查点解析失败", exception); }
    }
    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
}
