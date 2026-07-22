package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.template.TemplateDtos;
import com.openagentflow.entity.AsyncTaskEntity;
import com.openagentflow.entity.KnowledgeChunkEntity;
import com.openagentflow.entity.KnowledgeEmbeddingEntity;
import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 解决方案模板异步安装、升级、补偿和卸载服务。 */
@Service
public class TemplateInstallService {

    /** 数据库工具。 */ private final JdbcTemplate jdbcTemplate;
    /** JSON工具。 */ private final ObjectMapper objectMapper;
    /** 模板发布服务。 */ private final SolutionTemplateService templateService;
    /** 异步任务中心。 */ private final AsyncTaskService asyncTaskService;
    /** 工作空间治理服务。 */ private final WorkspaceGovernanceService workspaceGovernanceService;
    /** 当前用户与Agent权限服务。 */ private final AgentAccessService agentAccessService;
    /** MinIO对象服务。 */ private final SharedObjectStorageService objectStorageService;
    /** Milvus向量服务。 */ private final MilvusKnowledgeVectorService vectorService;

    public TemplateInstallService(JdbcTemplate jdbcTemplate,
                                  ObjectMapper objectMapper,
                                  SolutionTemplateService templateService,
                                  AsyncTaskService asyncTaskService,
                                  WorkspaceGovernanceService workspaceGovernanceService,
                                  AgentAccessService agentAccessService,
                                  SharedObjectStorageService objectStorageService,
                                  MilvusKnowledgeVectorService vectorService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.templateService = templateService;
        this.asyncTaskService = asyncTaskService;
        this.workspaceGovernanceService = workspaceGovernanceService;
        this.agentAccessService = agentAccessService;
        this.objectStorageService = objectStorageService;
        this.vectorService = vectorService;
    }

    /** 创建幂等的Kafka异步安装任务。 */
    @Transactional
    public TemplateDtos.InstallSummary install(String templateId, TemplateDtos.InstallRequest request) {
        Map<String, Object> template = templateService.requireTemplate(templateId);
        if (!"published".equals(templateService.text(template.get("status")))) {
            throw new BusinessException("TEMPLATE_NOT_INSTALLABLE", "只有已上架模板可以安装");
        }
        if (request == null || !StringUtils.hasText(request.workspaceId)) {
            throw new BusinessException("TEMPLATE_INSTALL_WORKSPACE_REQUIRED", "安装模板必须选择目标工作空间");
        }
        String userId = currentUserId();
        workspaceGovernanceService.assertCanManageWorkspace(request.workspaceId);
        String idempotencyKey = firstText(request.idempotencyKey,
                templateId + ":" + request.workspaceId + ":" + userId + ":" + UUID.randomUUID());
        Map<String, Object> existing = single("SELECT * FROM agent_template_install WHERE idempotency_key=?", idempotencyKey);
        if (existing != null) return mapInstall(existing);
        String versionId = firstText(request.templateVersionId, templateService.text(template.get("current_version_id")));
        Map<String, Object> version = templateService.requireVersion(versionId);
        if (!"published".equals(templateService.text(version.get("status")))) {
            throw new BusinessException("TEMPLATE_VERSION_NOT_INSTALLABLE", "所选模板版本尚未发布");
        }
        String installId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO agent_template_install
                  (id,template_id,workspace_id,template_version_id,idempotency_key,install_status,progress_percent,
                   current_stage,current_message,name_prefix,model_mapping,embedding_model_id,credentials_ready,
                   installed_by,install_config,installed_manifest)
                VALUES (?,?,?,?,?,'pending',0,'accepted','安装任务已进入Kafka队列',?,?,?,?,?,?,JSON_OBJECT())
                """, installId, templateId, request.workspaceId, versionId, idempotencyKey,
                firstText(request.namePrefix, ""), toJson(request.modelMapping), request.embeddingModelId,
                request.credentialsReady, userId, toJson(request));
        AsyncTaskEntity task = asyncTaskService.createTask(
                "安装解决方案模板：" + template.get("template_name"), "TEMPLATE_INSTALL", "agent_template_install",
                installId, "agent_template_install", installId, request.workspaceId,
                Map.of("installId", installId, "templateId", templateId, "versionId", versionId));
        jdbcTemplate.update("UPDATE agent_template_install SET install_task_id=?,updated_at=NOW(3) WHERE id=?", task.getId(), installId);
        return getInstall(installId);
    }

    /** 查询当前用户的模板安装实例。 */
    public List<TemplateDtos.InstallSummary> listMine() {
        return jdbcTemplate.queryForList("""
                SELECT i.*,t.template_name,v.version_no,latest.version_no latest_version_no
                FROM agent_template_install i JOIN agent_template t ON t.id=i.template_id
                LEFT JOIN agent_template_version v ON v.id=i.template_version_id
                LEFT JOIN agent_template_version latest ON latest.id=t.current_version_id
                WHERE i.installed_by=? ORDER BY i.created_at DESC
                """, currentUserId()).stream().map(this::mapInstall).toList();
    }

    /** 查询单个安装实例。 */
    public TemplateDtos.InstallSummary getInstall(String installId) {
        Map<String, Object> row = single("""
                SELECT i.*,t.template_name,v.version_no,latest.version_no latest_version_no
                FROM agent_template_install i JOIN agent_template t ON t.id=i.template_id
                LEFT JOIN agent_template_version v ON v.id=i.template_version_id
                LEFT JOIN agent_template_version latest ON latest.id=t.current_version_id WHERE i.id=?
                """, installId);
        if (row == null) throw new BusinessException("TEMPLATE_INSTALL_NOT_FOUND", "模板安装实例不存在");
        if (!currentUserId().equals(templateService.text(row.get("installed_by")))) {
            workspaceGovernanceService.assertCanManageWorkspace(templateService.text(row.get("workspace_id")));
        }
        return mapInstall(row);
    }

    /** Worker执行完整资源复制。 */
    public Map<String, Object> executeInstall(AsyncTaskEntity task) {
        Map<String, Object> payload = jsonMap(task.getRequestPayload());
        String installId = templateService.text(payload.get("installId"));
        Map<String, Object> install = requireInstallRow(installId);
        String versionId = templateService.text(install.get("template_version_id"));
        List<Map<String, Object>> resources = new ArrayList<>(templateService.versionResourceRows(versionId));
        hydratePackageSnapshots(versionId, resources);
        if (resources.isEmpty()) {
            resources = seedAgentResources(install);
        }
        Map<String, String> idMapping = loadIdMapping(installId);
        List<VectorCopy> vectors = new ArrayList<>();
        updateInstall(installId, "running", 2, "prepare", "正在校验模板包与目标工作空间", null);
        asyncTaskService.updateProgress(task.getId(), "prepare", "正在校验模板包与目标工作空间", 2, Map.of("resourceCount", resources.size()));
        try {
            int total = Math.max(1, resources.size());
            for (int index = 0; index < resources.size(); index++) {
                asyncTaskService.assertActiveLease(task.getId());
                Map<String, Object> resource = resources.get(index);
                String sourceId = templateService.text(resource.get("source_resource_id"));
                String type = templateService.text(resource.get("resource_type"));
                String stage = "copy_" + type;
                int progress = 5 + (int) (((index + 1D) / total) * 80D);
                String message = "正在复制" + resourceLabel(type) + "：" + resource.get("resource_name");
                updateInstall(installId, "running", progress, stage, message, null);
                asyncTaskService.updateProgress(task.getId(), stage, message, progress, Map.of("resourceType", type));
                String targetId = copyResource(install, resource, idMapping, vectors);
                idMapping.put(sourceId, targetId);
                saveResourceMapping(installId, resource, targetId);
            }
            if (!vectors.isEmpty()) {
                updateInstall(installId, "running", 90, "milvus", "正在批量恢复兼容的Milvus向量", null);
                restoreVectors(vectors);
            }
            finalizeRelations(install, idMapping);
            String targetAgentId = firstTarget(installId, "agent");
            jdbcTemplate.update("""
                    UPDATE agent_template_install SET target_agent_id=?,install_status='success',progress_percent=100,
                      current_stage='completed',current_message='解决方案模板安装完成',completed_at=NOW(3),
                      installed_manifest=?,error_message=NULL,updated_at=NOW(3) WHERE id=?
                    """, targetAgentId, toJson(idMapping), installId);
            jdbcTemplate.update("""
                    UPDATE agent_template SET install_count=install_count+1,
                      trend_score=(install_count+1)*1.0+favorite_count*2.0+rating_count*3.0+average_rating*5.0
                    WHERE id=?
                    """, install.get("template_id"));
            return Map.of("installId", installId, "targetAgentId", firstText(targetAgentId, ""),
                    "resourceCount", idMapping.size(), "credentialsReady", templateService.truth(install.get("credentials_ready")));
        } catch (Exception exception) {
            rollbackCreatedResources(installId);
            updateInstall(installId, "rollback", 0, "rollback", "安装失败，已回滚本次创建的资源", rootMessage(exception));
            throw exception instanceof RuntimeException runtime ? runtime : new IllegalStateException(exception);
        }
    }

    /** 生成目标版本的三方差异与冲突项。 */
    @Transactional
    public List<TemplateDtos.UpgradeConflict> prepareUpgrade(String installId, String targetVersionId) {
        Map<String, Object> install = requireOwnedInstall(installId);
        Map<String, Object> targetVersion = templateService.requireVersion(targetVersionId);
        if (!install.get("template_id").equals(targetVersion.get("template_id"))) {
            throw new BusinessException("TEMPLATE_UPGRADE_VERSION_MISMATCH", "目标版本不属于当前模板");
        }
        String currentVersionNo = templateService.text(single("SELECT version_no FROM agent_template_version WHERE id=?", install.get("template_version_id")).get("version_no"));
        String targetVersionNo = templateService.text(targetVersion.get("version_no"));
        if (TemplatePackagePolicy.compareVersions(targetVersionNo, currentVersionNo) <= 0) {
            throw new BusinessException("TEMPLATE_UPGRADE_NOT_NEWER", "目标版本必须高于当前安装版本");
        }
        jdbcTemplate.update("DELETE FROM agent_template_upgrade_conflict WHERE install_id=? AND target_version_id=?", installId, targetVersionId);
        List<Map<String, Object>> installed = jdbcTemplate.queryForList("SELECT * FROM agent_template_install_resource WHERE install_id=?", installId);
        Map<String, Map<String, Object>> oldBySource = new LinkedHashMap<>();
        installed.forEach(row -> oldBySource.put(key(row), row));
        List<Map<String, Object>> newResources = templateService.versionResourceRows(targetVersionId);
        for (Map<String, Object> next : newResources) {
            Map<String, Object> old = oldBySource.get(key(next));
            String oldHash = old == null ? null : templateService.text(old.get("source_hash"));
            String localHash = old == null ? null : currentResourceHash(templateService.text(old.get("resource_type")), templateService.text(old.get("target_resource_id")));
            String newHash = templateService.text(next.get("content_hash"));
            String installedHash = old == null ? null : templateService.text(old.get("installed_hash"));
            String decision = old == null ? "use_new"
                    : localHash.equals(installedHash) ? "use_new"
                    : oldHash.equals(newHash) ? "keep_local" : "conflict";
            jdbcTemplate.update("""
                    INSERT INTO agent_template_upgrade_conflict
                      (id,install_id,target_version_id,template_resource_id,resource_type,target_resource_id,
                       old_hash,local_hash,new_hash,merge_decision,conflict_detail)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, UUID.randomUUID().toString(), installId, targetVersionId, next.get("id"), next.get("resource_type"),
                    old == null ? null : old.get("target_resource_id"), oldHash, localHash, newHash, decision,
                    toJson(Map.of("resourceName", next.get("resource_name"), "breakingUpgrade",
                            TemplatePackagePolicy.isBreakingUpgrade(currentVersionNo, targetVersionNo))));
        }
        return conflicts(installId, targetVersionId);
    }

    /** 提交三方冲突选择并创建异步升级任务。 */
    @Transactional
    public TemplateDtos.InstallSummary upgrade(String installId, TemplateDtos.UpgradeRequest request) {
        Map<String, Object> install = requireOwnedInstall(installId);
        if (request == null || !StringUtils.hasText(request.targetVersionId)) {
            throw new BusinessException("TEMPLATE_UPGRADE_VERSION_REQUIRED", "升级目标版本不能为空");
        }
        List<TemplateDtos.UpgradeConflict> conflicts = conflicts(installId, request.targetVersionId);
        if (conflicts.isEmpty()) conflicts = prepareUpgrade(installId, request.targetVersionId);
        for (TemplateDtos.UpgradeConflict conflict : conflicts) {
            String choice = request.conflictChoices.get(conflict.id);
            if ("conflict".equals(conflict.mergeDecision) && !List.of("use_new", "keep_local").contains(choice)) {
                throw new BusinessException("TEMPLATE_UPGRADE_CONFLICT_UNRESOLVED", "请处理全部三方升级冲突");
            }
            if (StringUtils.hasText(choice)) {
                jdbcTemplate.update("UPDATE agent_template_upgrade_conflict SET user_choice=?,resolved_by=?,resolved_at=NOW(3) WHERE id=?",
                        choice, currentUserId(), conflict.id);
            }
        }
        AsyncTaskEntity task = asyncTaskService.createTask("升级解决方案模板", "TEMPLATE_UPGRADE",
                "agent_template_install", installId, "agent_template_install", installId,
                templateService.text(install.get("workspace_id")), Map.of(
                        "installId", installId, "targetVersionId", request.targetVersionId));
        jdbcTemplate.update("UPDATE agent_template_install SET install_task_id=?,install_status='pending',progress_percent=0,current_stage='upgrade_accepted',current_message='升级任务已进入Kafka队列' WHERE id=?",
                task.getId(), installId);
        return getInstall(installId);
    }

    /** Worker执行模板升级，本地保留项不会被覆盖。 */
    public Map<String, Object> executeUpgrade(AsyncTaskEntity task) {
        Map<String, Object> payload = jsonMap(task.getRequestPayload());
        String installId = templateService.text(payload.get("installId"));
        String targetVersionId = templateService.text(payload.get("targetVersionId"));
        Map<String, Object> install = requireInstallRow(installId);
        Map<String, String> idMapping = loadIdMapping(installId);
        List<Map<String, Object>> conflicts = jdbcTemplate.queryForList(
                "SELECT * FROM agent_template_upgrade_conflict WHERE install_id=? AND target_version_id=? ORDER BY created_at", installId, targetVersionId);
        int total = Math.max(1, conflicts.size());
        for (int index = 0; index < conflicts.size(); index++) {
            Map<String, Object> conflict = conflicts.get(index);
            String decision = firstText(templateService.text(conflict.get("user_choice")), templateService.text(conflict.get("merge_decision")));
            if (List.of("keep_local", "same_change").contains(decision)) continue;
            Map<String, Object> resource = single("SELECT * FROM agent_template_resource WHERE id=?", conflict.get("template_resource_id"));
            if (resource == null) {
                throw new BusinessException("TEMPLATE_UPGRADE_RESOURCE_NOT_FOUND", "升级资源清单不存在");
            }
            hydratePackageSnapshots(targetVersionId, List.of(resource));
            String targetId = templateService.text(conflict.get("target_resource_id"));
            if (!StringUtils.hasText(targetId)) {
                targetId = copyResource(install, resource, idMapping, new ArrayList<>());
                saveResourceMapping(installId, resource, targetId);
            } else {
                updateResourceSnapshot(resource, targetId, idMapping, install);
                String installedHash = currentResourceHash(templateService.text(resource.get("resource_type")), targetId);
                jdbcTemplate.update("UPDATE agent_template_install_resource SET source_hash=?,installed_hash=?,current_hash=?,user_modified=0,updated_at=NOW(3) WHERE install_id=? AND target_resource_id=?",
                        resource.get("content_hash"), installedHash, installedHash, installId, targetId);
            }
            int progress = 10 + (int)(((index + 1D) / total) * 85D);
            updateInstall(installId, "running", progress, "upgrade", "正在合并模板新版本资源", null);
            asyncTaskService.updateProgress(task.getId(), "upgrade", "正在合并模板新版本资源", progress, Map.of());
        }
        jdbcTemplate.update("UPDATE agent_template_install SET template_version_id=?,upgrade_available=0,install_status='success',progress_percent=100,current_stage='completed',current_message='模板升级完成',completed_at=NOW(3),updated_at=NOW(3) WHERE id=?",
                targetVersionId, installId);
        return Map.of("installId", installId, "targetVersionId", targetVersionId);
    }

    /** 查询三方升级冲突。 */
    public List<TemplateDtos.UpgradeConflict> conflicts(String installId, String targetVersionId) {
        requireOwnedInstall(installId);
        return jdbcTemplate.queryForList("""
                SELECT c,r.resource_name FROM agent_template_upgrade_conflict c
                JOIN agent_template_resource r ON r.id=c.template_resource_id
                WHERE c.install_id=? AND c.target_version_id=? ORDER BY r.sort_order,r.resource_type
                """, installId, targetVersionId).stream().map(row -> {
            TemplateDtos.UpgradeConflict item = new TemplateDtos.UpgradeConflict();
            item.id=templateService.text(row.get("id")); item.resourceType=templateService.text(row.get("resource_type"));
            item.targetResourceId=templateService.text(row.get("target_resource_id")); item.resourceName=templateService.text(row.get("resource_name"));
            item.mergeDecision=templateService.text(row.get("merge_decision")); item.userChoice=templateService.text(row.get("user_choice"));
            item.oldHash=templateService.text(row.get("old_hash")); item.localHash=templateService.text(row.get("local_hash"));
            item.newHash=templateService.text(row.get("new_hash")); item.detail=jsonMap(row.get("conflict_detail")); return item;
        }).toList();
    }

    /** 默认只解除关联，可选删除未修改模板资源。 */
    @Transactional
    public void uninstall(String installId, TemplateDtos.UninstallRequest request) {
        requireOwnedInstall(installId);
        if (request != null && request.deleteUnmodifiedResources) {
            List<Map<String, Object>> resources = jdbcTemplate.queryForList("""
                    SELECT * FROM agent_template_install_resource WHERE install_id=? AND user_modified=0 AND user_created=0
                    ORDER BY installed_at DESC
                    """, installId);
            for (Map<String, Object> resource : resources) {
                String currentHash = currentResourceHash(templateService.text(resource.get("resource_type")), templateService.text(resource.get("target_resource_id")));
                if (currentHash.equals(templateService.text(resource.get("installed_hash")))) {
                    deleteResource(templateService.text(resource.get("resource_type")), templateService.text(resource.get("target_resource_id")), resource);
                }
            }
        }
        jdbcTemplate.update("UPDATE agent_template_install SET install_status='unlinked',current_stage='unlinked',current_message='模板关联已解除，用户修改和新增数据已保留',updated_at=NOW(3) WHERE id=?", installId);
    }

    /** 按模板资源类型复制数据库行并重写关联ID。 */
    private String copyResource(Map<String, Object> install,
                                Map<String, Object> resource,
                                Map<String, String> idMapping,
                                List<VectorCopy> vectors) {
        String type = templateService.text(resource.get("resource_type"));
        String sourceId = templateService.text(resource.get("source_resource_id"));
        String already = idMapping.get(sourceId);
        if (StringUtils.hasText(already)) return already;
        Map<String, Object> snapshot = jsonMap(resource.get("resource_snapshot"));
        String targetId = UUID.randomUUID().toString();
        Map<String, Object> values = prepareValues(type, snapshot, targetId, install, idMapping);
        if ("document".equals(type)) copyDocumentObject(values, install, targetId);
        insertDynamic(table(type), values);
        if ("agent".equals(type)) {
            jdbcTemplate.update("INSERT IGNORE INTO iam_resource_acl(id,resource_type,resource_id,subject_type,subject_id,permission_level,created_by) VALUES (?,'agent',?,'user',?,'owner',?)",
                    UUID.randomUUID().toString(), targetId, currentUserId(), currentUserId());
        }
        if ("embedding".equals(type)) collectVector(values, idMapping, vectors);
        return targetId;
    }

    /** 构造目标资源字段并移除敏感配置。 */
    private Map<String, Object> prepareValues(String type,
                                              Map<String, Object> snapshot,
                                              String targetId,
                                              Map<String, Object> install,
                                              Map<String, String> idMapping) {
        Map<String, Object> values = new LinkedHashMap<>(TemplatePackagePolicy.sanitizeSnapshot(snapshot));
        values.keySet().removeIf(key -> key.startsWith("_") || Set.of("created_at","updated_at","deleted_at","version").contains(key));
        values.put("id", targetId);
        String workspaceId = templateService.text(install.get("workspace_id"));
        String prefix = templateService.text(install.get("name_prefix"));
        String userId = currentUserId();
        if (columns(table(type)).contains("workspace_id")) values.put("workspace_id", workspaceId);
        if (columns(table(type)).contains("owner_user_id")) values.put("owner_user_id", userId);
        if (columns(table(type)).contains("created_by")) values.put("created_by", userId);
        rewriteRelation(values, "template_id", idMapping);
        rewriteRelation(values, "kb_id", idMapping);
        rewriteRelation(values, "document_id", idMapping);
        rewriteRelation(values, "chunk_id", idMapping);
        rewriteRelation(values, "parent_document_id", idMapping);
        rewriteRelation(values, "parent_chunk_id", idMapping);
        rewriteRelation(values, "system_prompt_template_id", idMapping);
        rewriteRelation(values, "system_prompt_version_id", idMapping);
        rewriteRelation(values, "mcp_server_id", idMapping);
        rewriteRelation(values, "agent_id", idMapping);
        values.replaceAll((key, value) -> value instanceof String text ? remapText(text, idMapping) : value);
        applyNamePrefix(type, values, prefix);
        applyModelMapping(type, values, install);
        if ("tool".equals(type)) {
            values.put("auth_config", "{}"); values.put("headers", "{}"); values.put("enabled", false); values.put("status", "draft");
        }
        if ("mcp".equals(type)) {
            values.put("auth_config", "{}"); values.put("env_vars", "{}"); values.put("status", "disabled");
        }
        if ("agent".equals(type)) {
            values.put("status", "draft"); values.put("visibility", "private"); values.put("published_version", null);
        }
        if ("workflow".equals(type)) {
            values.put("status", "draft"); values.put("visibility", "private"); values.put("published_version", null);
        }
        if ("knowledge".equals(type)) {
            values.put("status", "draft"); values.put("visibility", "private");
        }
        if ("embedding".equals(type)) {
            values.put("vector_primary_key", UUID.randomUUID().toString());
            values.put("sync_status", "pending"); values.put("last_synced_at", null);
            Object base64 = values.remove("embedding_blob_base64");
            if (base64 != null && StringUtils.hasText(String.valueOf(base64))) values.put("embedding_blob", Base64.getDecoder().decode(String.valueOf(base64)));
        }
        return filterColumns(table(type), values);
    }

    /** 将来源模型映射为目标工作空间模型，不复制供应商密钥。 */
    private void applyModelMapping(String type, Map<String, Object> values, Map<String, Object> install) {
        Map<String, Object> mapping = jsonMap(install.get("model_mapping"));
        if (values.containsKey("model_id")) {
            String source = templateService.text(values.get("model_id"));
            values.put("model_id", firstText(templateService.text(mapping.get(source)), templateService.text(mapping.get("default"))));
        }
        if ("knowledge".equals(type)) {
            values.put("embedding_model_id", StringUtils.hasText(templateService.text(install.get("embedding_model_id")))
                    ? install.get("embedding_model_id") : null);
        }
        if ("embedding".equals(type)) {
            values.put("model_id", StringUtils.hasText(templateService.text(install.get("embedding_model_id")))
                    ? install.get("embedding_model_id") : null);
        }
    }

    /** 添加资源名称前缀并确保业务编码唯一。 */
    private void applyNamePrefix(String type, Map<String, Object> values, String prefix) {
        String nameColumn = nameColumn(type);
        if (StringUtils.hasText(nameColumn) && values.get(nameColumn) != null && StringUtils.hasText(prefix)) {
            values.put(nameColumn, prefix + values.get(nameColumn));
        }
        String codeColumn = codeColumn(type);
        if (StringUtils.hasText(codeColumn) && values.get(codeColumn) != null) {
            values.put(codeColumn, templateService.text(values.get(codeColumn)) + "-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if ("prompt_version".equals(type)) values.put("version_no", templateService.text(values.get("version_no")) + "-installed");
    }

    /** 流式复制知识库原始文档到目标工作空间对象路径。 */
    private void copyDocumentObject(Map<String, Object> values, Map<String, Object> install, String targetId) {
        String sourceBucket = templateService.text(values.get("storage_bucket"));
        String sourceKey = templateService.text(values.get("storage_key"));
        if (!StringUtils.hasText(sourceKey)) return;
        String targetKey = "workspaces/" + install.get("workspace_id") + "/template-installs/" + install.get("id") + "/documents/" + targetId + "/" + sanitizeFileName(templateService.text(values.get("doc_name")));
        SharedObjectStorageService.StoredObject stored = objectStorageService.copy(sourceBucket, sourceKey, targetKey,
                "application/octet-stream", longValue(values.get("file_size")));
        values.put("storage_bucket", stored.bucket()); values.put("storage_key", stored.objectKey());
        values.put("file_hash", stored.contentHash()); values.put("file_size", stored.size());
    }

    /** 收集可直接恢复到Milvus的兼容向量。 */
    private void collectVector(Map<String, Object> values, Map<String, String> idMapping, List<VectorCopy> vectors) {
        String json = templateService.text(values.get("embedding_json"));
        if (!StringUtils.hasText(json) || !StringUtils.hasText(templateService.text(values.get("model_id")))) return;
        try {
            List<Double> vector = objectMapper.readValue(json, new TypeReference<>() {});
            Integer expectedDimension = expectedEmbeddingDimension(templateService.text(values.get("model_id")));
            if (expectedDimension != null && expectedDimension > 0 && expectedDimension != vector.size()) {
                return;
            }
            KnowledgeEmbeddingEntity embedding = new KnowledgeEmbeddingEntity();
            embedding.setId(templateService.text(values.get("id"))); embedding.setChunkId(templateService.text(values.get("chunk_id")));
            embedding.setKbId(templateService.text(values.get("kb_id"))); embedding.setModelId(templateService.text(values.get("model_id")));
            embedding.setVectorPrimaryKey(templateService.text(values.get("vector_primary_key"))); embedding.setEmbeddingDim(vector.size());
            KnowledgeChunkEntity chunk = new KnowledgeChunkEntity(); chunk.setId(templateService.text(values.get("chunk_id")));
            chunk.setKbId(templateService.text(values.get("kb_id"))); chunk.setDocumentId(findDocumentId(chunk.getId()));
            Map<String,Object> chunkRow=single("SELECT content FROM knowledge_chunk WHERE id=?",chunk.getId());
            chunk.setContent(chunkRow==null?"":templateService.text(chunkRow.get("content")));
            vectors.add(new VectorCopy(embedding, chunk, vector));
        } catch (Exception ignored) {
            // 向量快照不兼容时保留MySQL元数据，后续由重新向量化任务补齐。
        }
    }

    /** 按维度和集合批量恢复Milvus向量。 */
    private void restoreVectors(List<VectorCopy> vectors) {
        Map<String, List<VectorCopy>> grouped = new LinkedHashMap<>();
        for (VectorCopy item : vectors) {
            String key = item.embedding().getKbId() + ":" + item.vector().size();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        for (List<VectorCopy> batch : grouped.values()) {
            String collection = knowledgeCollection(batch.getFirst().embedding().getKbId());
            vectorService.upsertKnowledgeChunks(collection, batch.stream().map(VectorCopy::embedding).toList(),
                    batch.stream().map(VectorCopy::chunk).toList(), batch.stream().map(VectorCopy::vector).toList());
            for (VectorCopy item : batch) {
                jdbcTemplate.update("UPDATE knowledge_embedding SET sync_status='synced',last_synced_at=NOW(3) WHERE id=?", item.embedding().getId());
            }
        }
    }

    /** 安装完成后恢复Agent资源绑定和团队成员。 */
    private void finalizeRelations(Map<String, Object> install, Map<String, String> idMapping) {
        List<Map<String, Object>> mappings = jdbcTemplate.queryForList("SELECT * FROM agent_template_install_resource WHERE install_id=? AND install_status='success'", install.get("id"));
        for (Map<String, Object> mapping : mappings) {
            String type=templateService.text(mapping.get("resource_type"));
            Map<String,Object> resource=single("SELECT resource_snapshot FROM agent_template_resource WHERE id=?",mapping.get("template_resource_id"));
            Map<String,Object> snapshot=resource==null?Map.of():jsonMap(resource.get("resource_snapshot"));
            String targetId=templateService.text(mapping.get("target_resource_id"));
            if ("agent".equals(type)) {
                for (String source : stringList(snapshot.get("_tool_ids"))) if (idMapping.containsKey(source)) jdbcTemplate.update("INSERT IGNORE INTO agent_tool_binding(agent_id,tool_id,tool_config,require_confirm,enabled) VALUES (?,?,JSON_OBJECT(),0,1)",targetId,idMapping.get(source));
                for (String source : stringList(snapshot.get("_knowledge_ids"))) if (idMapping.containsKey(source)) jdbcTemplate.update("INSERT IGNORE INTO agent_knowledge_binding(agent_id,knowledge_base_id,retrieval_config,enabled) VALUES (?,?,JSON_OBJECT(),1)",targetId,idMapping.get(source));
                for (String source : stringList(snapshot.get("_workflow_ids"))) if (idMapping.containsKey(source)) jdbcTemplate.update("INSERT IGNORE INTO agent_workflow_binding(agent_id,workflow_id,trigger_mode,enabled) VALUES (?,?,'auto',1)",targetId,idMapping.get(source));
            }
            if ("team".equals(type)) {
                for (Map<String,Object> member : mapList(snapshot.get("_members"))) {
                    String agentId=idMapping.get(templateService.text(member.get("agent_id")));
                    if (StringUtils.hasText(agentId)) jdbcTemplate.update("INSERT IGNORE INTO agent_team_member(team_id,agent_id,member_role,handoff_policy,sort_order,enabled) VALUES (?,?,?,?,?,1)",targetId,agentId,member.get("member_role"),toJson(member.getOrDefault("handoff_policy", Map.of())),member.get("sort_order"));
                }
            }
            if ("workflow".equals(type)) {
                materializeWorkflowGraph(targetId, snapshot, idMapping);
            }
            if ("prompt_version".equals(type)) {
                String sourceTemplateId = templateService.text(snapshot.get("template_id"));
                String targetTemplateId = idMapping.get(sourceTemplateId);
                if (StringUtils.hasText(targetTemplateId)) {
                    jdbcTemplate.update("UPDATE prompt_template SET stable_version_id=?,status='published' WHERE id=?", targetId, targetTemplateId);
                }
            }
        }
    }

    /**
     * 将模板工作流快照中的节点和连线物化到执行表。
     *
     * <p>资源快照保存在工作流定义之外，安装时需要重写知识库、工具等来源ID，
     * 再写入真实节点表，确保安装后的独立副本可以直接进入设计器和执行引擎。</p>
     */
    private void materializeWorkflowGraph(String workflowId,
                                          Map<String, Object> snapshot,
                                          Map<String, String> idMapping) {
        jdbcTemplate.update("DELETE FROM workflow_edge WHERE workflow_id=?", workflowId);
        jdbcTemplate.update("DELETE FROM workflow_node WHERE workflow_id=?", workflowId);
        for (Map<String, Object> sourceNode : mapList(snapshot.get("_nodes"))) {
            Map<String, Object> node = jsonMap(remapText(toJson(sourceNode), idMapping));
            jdbcTemplate.update("""
                    INSERT INTO workflow_node
                      (id,workflow_id,node_key,node_name,node_type,position_x,position_y,
                       config_json,input_schema,output_schema,retry_policy,enabled)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    """, UUID.randomUUID().toString(), workflowId,
                    firstText(templateService.text(node.get("node_key")), UUID.randomUUID().toString()),
                    firstText(templateService.text(node.get("node_name")), "模板节点"),
                    firstText(templateService.text(node.get("node_type")), "LLM"),
                    decimalValue(node.get("position_x")), decimalValue(node.get("position_y")),
                    toJson(node.getOrDefault("config_json", Map.of())),
                    toJson(node.getOrDefault("input_schema", Map.of())),
                    toJson(node.getOrDefault("output_schema", Map.of())),
                    toJson(node.getOrDefault("retry_policy", Map.of())),
                    !Boolean.FALSE.equals(node.get("enabled")));
        }
        for (Map<String, Object> sourceEdge : mapList(snapshot.get("_edges"))) {
            Map<String, Object> edge = jsonMap(remapText(toJson(sourceEdge), idMapping));
            jdbcTemplate.update("""
                    INSERT INTO workflow_edge
                      (id,workflow_id,edge_key,source_node_key,target_node_key,condition_expr,label,metadata)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, UUID.randomUUID().toString(), workflowId,
                    firstText(templateService.text(edge.get("edge_key")), UUID.randomUUID().toString()),
                    edge.get("source_node_key"), edge.get("target_node_key"), edge.get("condition_expr"),
                    edge.get("label"), toJson(edge.getOrDefault("metadata", Map.of())));
        }
    }

    /** 读取模板画布坐标，缺省为零。 */
    private BigDecimal decimalValue(Object value) {
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try { return new BigDecimal(templateService.text(value)); }
        catch (Exception ignored) { return BigDecimal.ZERO; }
    }

    /** 将目标资源更新为新模板快照。 */
    private void updateResourceSnapshot(Map<String, Object> resource, String targetId, Map<String, String> idMapping, Map<String, Object> install) {
        String type=templateService.text(resource.get("resource_type"));
        Map<String,Object> values=prepareValues(type,jsonMap(resource.get("resource_snapshot")),targetId,install,idMapping);
        values.remove("id");
        if(values.isEmpty())return;
        String set=String.join(",",values.keySet().stream().map(key->"`"+key+"`=?").toList());
        List<Object> args=new ArrayList<>(values.values()); args.add(targetId);
        jdbcTemplate.update("UPDATE "+table(type)+" SET "+set+" WHERE id=?",args.toArray());
    }

    /** 保存安装资源映射。 */
    private void saveResourceMapping(String installId, Map<String, Object> resource, String targetId) {
        String installedHash = currentResourceHash(templateService.text(resource.get("resource_type")), targetId);
        jdbcTemplate.update("""
                INSERT INTO agent_template_install_resource
                  (id,install_id,template_resource_id,resource_type,source_resource_id,target_resource_id,
                   source_hash,installed_hash,current_hash,install_status,user_modified,user_created,object_manifest,installed_at)
                VALUES (?,?,?,?,?,?,?,?,?,'success',0,0,?,NOW(3))
                ON DUPLICATE KEY UPDATE target_resource_id=VALUES(target_resource_id),source_hash=VALUES(source_hash),
                  installed_hash=VALUES(installed_hash),current_hash=VALUES(current_hash),install_status='success',
                  error_message=NULL,installed_at=NOW(3),updated_at=NOW(3)
                """, UUID.randomUUID().toString(), installId, resource.get("id"), resource.get("resource_type"),
                resource.get("source_resource_id"), targetId, resource.get("content_hash"), installedHash,
                installedHash, firstText(templateService.text(resource.get("object_manifest")), "[]"));
    }

    /** 失败时按安装顺序逆序删除本次创建资源。 */
    private void rollbackCreatedResources(String installId) {
        List<Map<String,Object>> resources=jdbcTemplate.queryForList("SELECT * FROM agent_template_install_resource WHERE install_id=? AND install_status='success' ORDER BY installed_at DESC",installId);
        for(Map<String,Object> resource:resources){
            try { deleteResource(templateService.text(resource.get("resource_type")),templateService.text(resource.get("target_resource_id")),resource); }
            catch(Exception ignored){ }
            jdbcTemplate.update("UPDATE agent_template_install_resource SET install_status='rolled_back',updated_at=NOW(3) WHERE id=?",resource.get("id"));
        }
    }

    /**
     * 从MinIO模板包回填完整资源快照。
     * MySQL只保存轻量清单，向量正文等大字段在真正安装或升级时按需读取。
     */
    private void hydratePackageSnapshots(String versionId, List<Map<String, Object>> resources) {
        if (resources == null || resources.isEmpty()) return;
        Map<String, Object> version = templateService.requireVersion(versionId);
        String bucket = templateService.text(version.get("package_bucket"));
        String objectKey = templateService.text(version.get("package_key"));
        if (!StringUtils.hasText(objectKey)) return;
        byte[] packageBytes = objectStorageService.get(bucket, objectKey);
        String expectedHash = templateService.text(version.get("package_hash"));
        if (StringUtils.hasText(expectedHash) && !expectedHash.equalsIgnoreCase(templateService.sha256(packageBytes))) {
            throw new BusinessException("TEMPLATE_PACKAGE_HASH_MISMATCH", "模板包完整性检查失败，已阻止安装");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(packageBytes, new TypeReference<>() { });
            Map<String, Map<String, Object>> snapshots = new LinkedHashMap<>();
            for (Map<String, Object> packaged : mapList(payload.get("resources"))) {
                String key = templateService.text(packaged.get("type")) + ":" + templateService.text(packaged.get("sourceId"));
                snapshots.put(key, packaged);
            }
            for (Map<String, Object> resource : resources) {
                if (resource == null) continue;
                String key = templateService.text(resource.get("resource_type")) + ":" + templateService.text(resource.get("source_resource_id"));
                Map<String, Object> packaged = snapshots.get(key);
                if (packaged != null && packaged.get("snapshot") instanceof Map<?, ?>) {
                    resource.put("resource_snapshot", toJson(packaged.get("snapshot")));
                    resource.put("object_manifest", toJson(packaged.getOrDefault("objects", List.of())));
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("TEMPLATE_PACKAGE_INVALID", "模板包结构无法解析：" + exception.getMessage());
        }
    }

    /** 删除未修改的模板资源和对应MinIO对象。 */
    private void deleteResource(String type,String targetId,Map<String,Object> mapping){
        if(!StringUtils.hasText(targetId))return;
        if("document".equals(type)){
            Map<String,Object> row=single("SELECT storage_bucket,storage_key FROM knowledge_document WHERE id=?",targetId);
            if(row!=null&&StringUtils.hasText(templateService.text(row.get("storage_key")))) objectStorageService.delete(templateService.text(row.get("storage_bucket")),templateService.text(row.get("storage_key")));
        }
        jdbcTemplate.update("DELETE FROM "+table(type)+" WHERE id=?",targetId);
    }

    /** 动态插入经白名单过滤的资源快照。 */
    private void insertDynamic(String table,Map<String,Object> values){
        if(values.isEmpty())throw new IllegalStateException("模板资源快照没有可安装字段");
        String columns=String.join(",",values.keySet().stream().map(key->"`"+key+"`").toList());
        String placeholders=String.join(",",Collections.nCopies(values.size(),"?"));
        jdbcTemplate.update("INSERT INTO "+table+" ("+columns+") VALUES ("+placeholders+")",values.values().toArray());
    }

    /** 过滤数据库不存在的快照字段。 */
    private Map<String,Object> filterColumns(String table,Map<String,Object> values){
        Set<String> columns=columns(table); Map<String,Object> result=new LinkedHashMap<>();
        values.forEach((key,value)->{if(columns.contains(key))result.put(key,normalizeJdbcValue(value));}); return result;
    }

    /** 查询表字段白名单。 */
    private Set<String> columns(String table){
        return new java.util.LinkedHashSet<>(jdbcTemplate.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=?",String.class,table));
    }

    /** 将JSON数组或对象重新序列化为MySQL JSON文本。 */
    private Object normalizeJdbcValue(Object value){
        if(value instanceof Map<?,?>||value instanceof List<?>)return toJson(value);
        if(value instanceof String text&&text.matches("^\\d{4}-\\d{2}-\\d{2}T.*"))return text.replace('T',' ');
        return value;
    }

    /** 重新映射单个外键。 */
    private void rewriteRelation(Map<String,Object> values,String field,Map<String,String> mapping){
        String source=templateService.text(values.get(field)); if(StringUtils.hasText(source)&&mapping.containsKey(source))values.put(field,mapping.get(source));
    }

    /** 替换JSON文本中的全部来源资源ID。 */
    private String remapText(String text,Map<String,String> mapping){String result=text;for(Map.Entry<String,String> entry:mapping.entrySet())result=result.replace(entry.getKey(),entry.getValue());return result;}

    /** 加载已成功资源映射，支持Kafka重试幂等恢复。 */
    private Map<String,String> loadIdMapping(String installId){Map<String,String> result=new LinkedHashMap<>();jdbcTemplate.queryForList("SELECT source_resource_id,target_resource_id FROM agent_template_install_resource WHERE install_id=? AND install_status='success'",installId).forEach(row->result.put(templateService.text(row.get("source_resource_id")),templateService.text(row.get("target_resource_id"))));return result;}

    /** 种子模板没有显式资源表时，从模板Agent快照生成临时资源清单。 */
    private List<Map<String,Object>> seedAgentResources(Map<String,Object> install){
        Map<String,Object> template=templateService.requireTemplate(templateService.text(install.get("template_id")));
        Map<String,Object> snapshot=jsonMap(template.get("agent_snapshot")); List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> agent:mapList(snapshot.get("agents"))){String source=firstText(templateService.text(agent.get("resourceKey")),UUID.randomUUID().toString());Map<String,Object> row=new LinkedHashMap<>();row.put("id",UUID.randomUUID().toString());row.put("source_resource_id",source);row.put("resource_type","agent");row.put("resource_name",firstText(templateService.text(agent.get("agentName")),"模板Agent"));row.put("resource_snapshot",toJson(mapOf("id",source,"agent_code","template-agent","agent_name",row.get("resource_name"),"category",firstText(templateService.text(agent.get("category")),"通用"),"description",template.get("description"),"agent_type",firstText(templateService.text(agent.get("agentType")),"chat_agent"),"system_prompt",agent.get("systemPrompt"),"model_params",agent.get("modelParams"),"memory_strategy","none","prompt_binding_mode","MANUAL","prompt_variables","{}")));row.put("content_hash",templateService.sha256(templateService.text(row.get("resource_snapshot")).getBytes(StandardCharsets.UTF_8)));row.put("object_manifest","[]");result.add(row);}return result;
    }

    /** 更新安装状态并保持页面进度可见。 */
    private void updateInstall(String installId,String status,int progress,String stage,String message,String error){jdbcTemplate.update("UPDATE agent_template_install SET install_status=?,progress_percent=?,current_stage=?,current_message=?,error_message=?,updated_at=NOW(3) WHERE id=?",status,Math.max(0,Math.min(100,progress)),stage,message,error,installId);}

    /** 获取首个已安装目标资源。 */
    private String firstTarget(String installId,String type){Map<String,Object> row=single("SELECT target_resource_id FROM agent_template_install_resource WHERE install_id=? AND resource_type=? AND install_status='success' ORDER BY installed_at LIMIT 1",installId,type);return row==null?null:templateService.text(row.get("target_resource_id"));}

    /** 当前资源内容哈希，用于判断用户是否修改。 */
    private String currentResourceHash(String type,String targetId){if(!StringUtils.hasText(targetId))return "";Map<String,Object> row=single("SELECT * FROM "+table(type)+" WHERE id=?",targetId);if(row==null)return "";row.keySet().removeIf(key->Set.of("created_at","updated_at","deleted_at","version","last_synced_at").contains(key));return templateService.sha256(toJson(TemplatePackagePolicy.sanitizeSnapshot(row)).getBytes(StandardCharsets.UTF_8));}

    /** 资源类型数据库表。 */
    private String table(String type){return switch(type){case"prompt"->"prompt_template";case"prompt_version"->"prompt_template_version";case"mcp"->"mcp_server";case"tool"->"tool_definition";case"knowledge"->"knowledge_base";case"document"->"knowledge_document";case"chunk"->"knowledge_chunk";case"embedding"->"knowledge_embedding";case"workflow"->"workflow_definition";case"agent"->"agent";case"team"->"agent_team";case"memory"->"agent_memory";default->throw new BusinessException("TEMPLATE_RESOURCE_TYPE_INVALID","不支持的资源类型："+type);};}
    /** 资源名称字段。 */ private String nameColumn(String type){return switch(type){case"prompt"->"template_name";case"mcp"->"server_name";case"tool"->"tool_name";case"knowledge"->"kb_name";case"document"->"doc_name";case"chunk"->"title";case"workflow"->"workflow_name";case"agent"->"agent_name";case"team"->"team_name";default->null;};}
    /** 资源编码字段。 */ private String codeColumn(String type){return switch(type){case"prompt"->"template_code";case"mcp"->"server_code";case"tool"->"tool_code";case"knowledge"->"kb_code";case"workflow"->"workflow_code";case"agent"->"agent_code";case"team"->"team_code";default->null;};}
    /** 资源中文名。 */ private String resourceLabel(String type){return switch(type){case"prompt"->"Prompt";case"prompt_version"->"Prompt版本";case"mcp"->"MCP服务";case"tool"->"工具";case"knowledge"->"知识库";case"document"->"文档";case"chunk"->"切片";case"embedding"->"向量";case"workflow"->"工作流";case"agent"->"Agent";case"team"->"Agent团队";case"memory"->"Memory";default->type;};}
    /** 资源冲突键。 */ private String key(Map<String,Object> row){return templateService.text(row.get("resource_type"))+":"+templateService.text(row.get("source_resource_id"));}
    /** 查询并校验安装实例。 */ private Map<String,Object> requireInstallRow(String id){Map<String,Object> row=single("SELECT * FROM agent_template_install WHERE id=?",id);if(row==null)throw new BusinessException("TEMPLATE_INSTALL_NOT_FOUND","模板安装实例不存在");return row;}
    /** 查询并校验安装实例所有权。 */ private Map<String,Object> requireOwnedInstall(String id){Map<String,Object> row=requireInstallRow(id);if(!currentUserId().equals(templateService.text(row.get("installed_by"))))workspaceGovernanceService.assertCanManageWorkspace(templateService.text(row.get("workspace_id")));return row;}
    /** 安装摘要映射。 */ private TemplateDtos.InstallSummary mapInstall(Map<String,Object> row){TemplateDtos.InstallSummary item=new TemplateDtos.InstallSummary();item.id=templateService.text(row.get("id"));item.templateId=templateService.text(row.get("template_id"));item.templateName=templateService.text(row.get("template_name"));item.workspaceId=templateService.text(row.get("workspace_id"));item.templateVersionId=templateService.text(row.get("template_version_id"));item.versionNo=templateService.text(row.get("version_no"));item.installTaskId=templateService.text(row.get("install_task_id"));item.installStatus=templateService.text(row.get("install_status"));item.progressPercent=intValue(row.get("progress_percent"));item.currentStage=templateService.text(row.get("current_stage"));item.currentMessage=templateService.text(row.get("current_message"));item.targetAgentId=templateService.text(row.get("target_agent_id"));item.upgradeAvailable=templateService.truth(row.get("upgrade_available"));item.latestVersionNo=templateService.text(row.get("latest_version_no"));item.errorMessage=templateService.text(row.get("error_message"));item.createdAt=dateTime(row.get("created_at"));item.completedAt=dateTime(row.get("completed_at"));return item;}
    /** 查询单行。 */ private Map<String,Object> single(String sql,Object...args){List<Map<String,Object>> rows=jdbcTemplate.queryForList(sql,args);return rows.isEmpty()?null:rows.getFirst();}
    /** JSON解析。 */ private Map<String,Object> jsonMap(Object value){try{return !StringUtils.hasText(templateService.text(value))?new LinkedHashMap<>():objectMapper.readValue(templateService.text(value),new TypeReference<>(){});}catch(Exception ignored){return new LinkedHashMap<>();}}
    /** JSON序列化。 */ private String toJson(Object value){try{return objectMapper.writeValueAsString(value);}catch(Exception exception){throw new IllegalStateException("模板安装JSON序列化失败",exception);}}
    /** 当前用户。 */ private String currentUserId(){String id=agentAccessService.currentUserId();if(!StringUtils.hasText(id))throw new BusinessException("AUTH_REQUIRED","请先登录");return id;}
    /** 获取首个非空文本。 */ private String firstText(String value,String fallback){return StringUtils.hasText(value)?value:fallback;}
    /** 时间转换。 */ private LocalDateTime dateTime(Object value){return value instanceof LocalDateTime time?time:value instanceof Timestamp timestamp?timestamp.toLocalDateTime():null;}
    /** 数字转换。 */ private int intValue(Object value){return value instanceof Number n?n.intValue():0;} private long longValue(Object value){return value instanceof Number n?n.longValue():0L;}
    /** 关联文档。 */ private String findDocumentId(String chunkId){Map<String,Object> row=single("SELECT document_id FROM knowledge_chunk WHERE id=?",chunkId);return row==null?null:templateService.text(row.get("document_id"));}
    /** 知识库Milvus集合。 */ private String knowledgeCollection(String kbId){Map<String,Object> row=single("SELECT milvus_collection_name FROM knowledge_base WHERE id=?",kbId);return row==null?null:templateService.text(row.get("milvus_collection_name"));}
    /** 从目标Embedding模型默认参数读取向量维度，未声明时返回空并允许兼容导入。 */ private Integer expectedEmbeddingDimension(String modelId){Map<String,Object> row=single("SELECT default_params FROM model_config WHERE id=? AND model_type='embedding' AND status='enabled'",modelId);if(row==null)return null;Map<String,Object> params=jsonMap(row.get("default_params"));Object value=params.containsKey("dimensions")?params.get("dimensions"):params.get("embeddingDimension");return value instanceof Number number?number.intValue():null;}
    /** 文件名清洗。 */ private String sanitizeFileName(String name){return firstText(name,"document.bin").replaceAll("[\\\\/:*?\"<>|]","_");}
    /** 异常根消息。 */ private String rootMessage(Throwable error){Throwable current=error;while(current.getCause()!=null)current=current.getCause();return firstText(current.getMessage(),current.getClass().getSimpleName());}
    /** 字符串列表转换。 */ private List<String> stringList(Object value){if(value instanceof List<?> list)return list.stream().map(String::valueOf).toList();return List.of();}
    /** Map列表转换。 */ @SuppressWarnings("unchecked") private List<Map<String,Object>> mapList(Object value){if(value instanceof List<?> list)return list.stream().filter(Map.class::isInstance).map(item->(Map<String,Object>)item).toList();return List.of();}
    /** 简单Map构造。 */ private Map<String,Object> mapOf(Object...values){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i+1<values.length;i+=2)result.put(String.valueOf(values[i]),values[i+1]);return result;}

    /** 待批量写入Milvus的向量。 */
    private record VectorCopy(
            /** 向量元数据实体。 */ KnowledgeEmbeddingEntity embedding,
            /** 向量关联切片。 */ KnowledgeChunkEntity chunk,
            /** 浮点向量。 */ List<Double> vector) { }
}
