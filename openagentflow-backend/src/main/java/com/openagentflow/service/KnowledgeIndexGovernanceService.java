package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库蓝绿索引版本治理服务。
 *
 * <p>构建时写入带版本号的物理集合，激活后由稳定别名承接查询，旧版本保留至回滚窗口结束。</p>
 */
@Service
public class KnowledgeIndexGovernanceService {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    /** Milvus集合发布服务。 */
    private final MilvusKnowledgeVectorService milvusService;

    /** 异步任务服务，用于提交索引构建。 */
    private final AsyncTaskService asyncTaskService;

    public KnowledgeIndexGovernanceService(JdbcTemplate jdbcTemplate,
                                           MilvusKnowledgeVectorService milvusService,
                                           AsyncTaskService asyncTaskService) {
        this.jdbcTemplate = jdbcTemplate;
        this.milvusService = milvusService;
        this.asyncTaskService = asyncTaskService;
    }

    /** 创建下一版索引元数据。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createVersion(String kbId) {
        Map<String, Object> kb;
        try {
            kb = jdbcTemplate.queryForMap("SELECT id, workspace_id, milvus_collection_name, embedding_model_id FROM knowledge_base WHERE id=?", kbId);
        } catch (Exception exception) {
            throw new BusinessException("KNOWLEDGE_BASE_NOT_FOUND", "知识库不存在");
        }
        Integer version = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM knowledge_index_version WHERE kb_id=?", Integer.class, kbId);
        int versionNo = version == null ? 1 : version;
        String alias = text(kb.get("milvus_collection_name"));
        if (alias.isBlank()) {
            alias = "kb_" + kbId.replace("-", "");
        }
        String physical = alias + "_v" + versionNo;
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO knowledge_index_version
                  (id,kb_id,workspace_id,version_no,collection_name,collection_alias,keyword_index_name,embedding_model_id,status,created_at)
                VALUES (?,?,?,?,?,?,?,?, 'building', NOW(3))
                """, id, kbId, kb.get("workspace_id"), versionNo, physical, alias,
                "oaf-kb-" + kbId + "-v" + versionNo, kb.get("embedding_model_id"));
        var task = asyncTaskService.createTask("构建知识库索引版本v" + versionNo, "KNOWLEDGE_INDEX_BUILD",
                "knowledge_index_version", id, "knowledge_index_version", id, text(kb.get("workspace_id")),
                Map.of("versionId", id, "kbId", kbId, "collectionName", physical));
        Map<String, Object> result = new java.util.LinkedHashMap<>(jdbcTemplate.queryForMap("SELECT * FROM knowledge_index_version WHERE id=?", id));
        result.put("asyncTaskId", task.getId());
        return result;
    }

    /** 原子激活已就绪版本并退役旧版本。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activate(String versionId) {
        Map<String, Object> version = jdbcTemplate.queryForMap("SELECT * FROM knowledge_index_version WHERE id=? FOR UPDATE", versionId);
        String status = text(version.get("status"));
        if (!List.of("ready", "active").contains(status)) {
            throw new BusinessException("INDEX_VERSION_NOT_READY", "只有已就绪索引版本可以激活");
        }
        String kbId = text(version.get("kb_id"));
        String collectionName = text(version.get("collection_name"));
        String alias = text(version.get("collection_alias"));
        Number dimension = version.get("dimension") instanceof Number number ? number : null;
        String physicalCollection = dimension == null ? collectionName : collectionName + "_d" + dimension.intValue();
        // 数据库状态切换前先执行Milvus原子别名切换，外部动作失败时不污染激活状态。
        milvusService.activateAlias(physicalCollection, alias);
        jdbcTemplate.update("UPDATE knowledge_index_version SET status='retired', retired_at=NOW(3) WHERE kb_id=? AND status='active' AND id<>?", kbId, versionId);
        jdbcTemplate.update("UPDATE knowledge_index_version SET status='active', activated_at=NOW(3), retired_at=NULL WHERE id=?", versionId);
        return jdbcTemplate.queryForMap("SELECT * FROM knowledge_index_version WHERE id=?", versionId);
    }

    /** 查询知识库全部索引版本。 */
    public List<Map<String, Object>> list(String kbId) {
        return jdbcTemplate.queryForList("SELECT * FROM knowledge_index_version WHERE kb_id=? ORDER BY version_no DESC", kbId);
    }

    /** 标记索引构建完成并写入容量数据。 */
    public Map<String, Object> markReady(String versionId, int dimension, long chunkCount) {
        jdbcTemplate.update("""
                UPDATE knowledge_index_version SET status='ready', dimension=?, chunk_count=?
                WHERE id=? AND status IN ('building','ready')
                """, Math.max(1, dimension), Math.max(0, chunkCount), versionId);
        return jdbcTemplate.queryForMap("SELECT * FROM knowledge_index_version WHERE id=?", versionId);
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
}
