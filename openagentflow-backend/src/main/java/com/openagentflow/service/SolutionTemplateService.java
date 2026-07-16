package com.openagentflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.template.TemplateDtos;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** 企业解决方案模板发布、审核和运营服务。 */
@Service
public class SolutionTemplateService {

    /** 快照中疑似敏感明文的检查规则。 */
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|authorization|password|secret|access[_-]?token)\\s*[:=]\\s*[^,}\\s]{6,}");

    /** JDBC工具。 */ private final JdbcTemplate jdbcTemplate;
    /** JSON工具。 */ private final ObjectMapper objectMapper;
    /** MinIO共享对象存储。 */ private final SharedObjectStorageService objectStorageService;

    public SolutionTemplateService(JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper,
                                   SharedObjectStorageService objectStorageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.objectStorageService = objectStorageService;
    }

    /** 查询模板市场运营概览。 */
    public TemplateDtos.Overview overview() {
        TemplateDtos.Overview result = new TemplateDtos.Overview();
        result.publishedCount = count("SELECT COUNT(1) FROM agent_template WHERE visibility='public' AND status='published' AND deleted_at IS NULL");
        result.favoriteCount = count("SELECT COUNT(1) FROM agent_template_favorite WHERE user_id=?", currentUserId());
        result.installedCount = count("SELECT COUNT(1) FROM agent_template_install WHERE installed_by=? AND install_status='success'", currentUserId());
        result.pendingReviewCount = count("SELECT COUNT(1) FROM agent_template_version WHERE status='pending'");
        result.pendingReportCount = count("SELECT COUNT(1) FROM agent_template_report WHERE status='pending'");
        result.runningInstallCount = count("SELECT COUNT(1) FROM agent_template_install WHERE installed_by=? AND install_status IN ('pending','running')", currentUserId());
        return result;
    }

    /** 查询模板作者公开主页及其已上架作品。 */
    public TemplateDtos.AuthorProfile authorProfile(String userId) {
        Map<String, Object> user = single("SELECT id,display_name,username,avatar_url FROM iam_user WHERE id=? AND deleted_at IS NULL", userId);
        if (user == null) {
            throw new BusinessException("TEMPLATE_AUTHOR_NOT_FOUND", "模板作者不存在");
        }
        TemplateDtos.AuthorProfile result = new TemplateDtos.AuthorProfile();
        result.userId = text(user.get("id"));
        result.authorName = firstText(text(user.get("display_name")), text(user.get("username")));
        result.avatarUrl = text(user.get("avatar_url"));
        result.templates = jdbcTemplate.queryForList("""
                SELECT t.*,v.version_no,0 favorite FROM agent_template t
                LEFT JOIN agent_template_version v ON v.id=t.current_version_id
                WHERE t.author_user_id=? AND t.visibility='public' AND t.status='published' AND t.deleted_at IS NULL
                ORDER BY t.recommended DESC,t.trend_score DESC,t.published_at DESC
                """, userId).stream().map(this::mapSummary).toList();
        result.publishedTemplateCount = result.templates.size();
        result.totalInstallCount = result.templates.stream().mapToLong(item -> item.installCount).sum();
        result.totalFavoriteCount = result.templates.stream().mapToLong(item -> item.favoriteCount).sum();
        result.averageRating = result.templates.isEmpty() ? BigDecimal.ZERO : result.templates.stream()
                .map(item -> item.averageRating).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(result.templates.size()), 2, java.math.RoundingMode.HALF_UP);
        return result;
    }

    /** 分页查询公开模板或当前用户收藏。 */
    public PageResult<TemplateDtos.TemplateSummary> listPublic(String category,
                                                               String keyword,
                                                               String sort,
                                                               boolean favoriteOnly,
                                                               int pageNo,
                                                               int pageSize) {
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, Math.min(100, pageSize));
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE t.visibility='public' AND t.status='published' AND t.deleted_at IS NULL ");
        if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category)) {
            where.append(" AND t.category=? ");
            args.add(category);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (t.template_name LIKE ? OR t.template_code LIKE ? OR t.description LIKE ? OR JSON_SEARCH(t.tags,'one',?) IS NOT NULL) ");
            String like = "%" + keyword.trim() + "%";
            args.add(like); args.add(like); args.add(like); args.add("%" + keyword.trim() + "%");
        }
        if (favoriteOnly) {
            where.append(" AND EXISTS (SELECT 1 FROM agent_template_favorite f WHERE f.template_id=t.id AND f.user_id=?) ");
            args.add(currentUserId());
        }
        String orderBy = switch (String.valueOf(sort).toLowerCase(Locale.ROOT)) {
            case "latest" -> "t.published_at DESC";
            case "rating" -> "t.average_rating DESC,t.rating_count DESC";
            case "installs" -> "t.install_count DESC";
            default -> "t.recommended DESC,t.trend_score DESC,t.published_at DESC";
        };
        long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_template t" + where, Long.class, args.toArray());
        args.add(safeSize);
        args.add((safePage - 1) * safeSize);
        List<TemplateDtos.TemplateSummary> records = jdbcTemplate.query("""
                SELECT t.*,v.version_no,
                  EXISTS(SELECT 1 FROM agent_template_favorite f WHERE f.template_id=t.id AND f.user_id=?) favorite
                FROM agent_template t LEFT JOIN agent_template_version v ON v.id=t.current_version_id
                """ + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
                prepared -> {
                    int index = 1;
                    prepared.setString(index++, currentUserId());
                    for (Object arg : args) prepared.setObject(index++, arg);
                },
                (rs, rowNum) -> mapSummary(resultSetMap(rs)));
        return new PageResult<>(records, total, safePage, safeSize);
    }

    /** 查询模板详情、版本、资源和评论。 */
    public TemplateDtos.TemplateDetail detail(String templateId) {
        Map<String, Object> template = requireTemplate(templateId);
        if (!canView(template)) {
            throw new BusinessException("TEMPLATE_VIEW_FORBIDDEN", "无权查看该工作空间模板");
        }
        TemplateDtos.TemplateDetail detail = new TemplateDtos.TemplateDetail();
        copySummary(mapSummary(template), detail);
        detail.workspaceId = text(template.get("workspace_id"));
        detail.licenseCode = text(template.get("license_code"));
        detail.compatibility = text(template.get("compatibility"));
        detail.dependencyManifest = jsonMap(template.get("dependency_manifest"));
        detail.versions = jdbcTemplate.queryForList("SELECT * FROM agent_template_version WHERE template_id=? ORDER BY created_at DESC", templateId)
                .stream().map(this::mapVersion).toList();
        if (StringUtils.hasText(detail.currentVersionId)) {
            detail.resources = resources(detail.currentVersionId);
        }
        detail.comments = comments(templateId);
        detail.canReview = hasSuccessfulInstall(templateId, currentUserId());
        detail.canManage = isManager() || currentUserId().equals(text(template.get("author_user_id")));
        return detail;
    }

    /** 查询当前工作空间或当前用户创建的模板。 */
    public List<TemplateDtos.TemplateSummary> listManaged() {
        String workspaceId = WorkspaceContextHolder.current();
        return jdbcTemplate.queryForList("""
                SELECT t.*,v.version_no,0 favorite FROM agent_template t
                LEFT JOIN agent_template_version v ON v.id=t.current_version_id
                WHERE t.deleted_at IS NULL AND (t.author_user_id=? OR t.workspace_id=?)
                ORDER BY t.updated_at DESC
                """, currentUserId(), workspaceId).stream().map(this::mapSummary).toList();
    }

    /** 创建工作空间私有解决方案模板草稿。 */
    @Transactional
    public TemplateDtos.TemplateDetail create(TemplateDtos.TemplateRequest request) {
        validateTemplateRequest(request);
        String id = UUID.randomUUID().toString();
        String workspaceId = firstText(request.workspaceId, WorkspaceContextHolder.current());
        if (!StringUtils.hasText(workspaceId)) {
            throw new BusinessException("WORKSPACE_REQUIRED", "创建解决方案模板必须选择工作空间");
        }
        String code = uniqueCode(firstText(request.templateCode, slug(request.templateName)));
        jdbcTemplate.update("""
                INSERT INTO agent_template
                  (id,workspace_id,template_code,template_name,template_type,visibility,review_status,author_user_id,
                   author_name,license_code,compatibility,category,description,icon,cover_url,tags,agent_snapshot,
                   prompt_snapshot,tool_snapshot,knowledge_snapshot,dependency_manifest,recommended,status,created_by)
                VALUES (?,?,?,?,?,'workspace','draft',?,?,?,?,?,?,?,?,?,JSON_OBJECT(),JSON_OBJECT(),JSON_OBJECT(),
                        JSON_OBJECT(),JSON_OBJECT(),0,'draft',?)
                """, id, workspaceId, code, request.templateName.trim(), firstText(request.templateType, "solution"),
                currentUserId(), currentUserName(), firstText(request.licenseCode, "Apache-2.0"), request.compatibility,
                firstText(request.category, "其他"), request.description, firstText(request.icon, "Blocks"), request.coverUrl,
                toJson(request.tags), currentUserId());
        return detail(id);
    }

    /** 更新尚未公开的模板基础信息。 */
    @Transactional
    public TemplateDtos.TemplateDetail update(String templateId, TemplateDtos.TemplateRequest request) {
        Map<String, Object> template = requireManageable(templateId);
        validateTemplateRequest(request);
        jdbcTemplate.update("""
                UPDATE agent_template SET template_name=?,template_type=?,category=?,description=?,icon=?,cover_url=?,
                  tags=?,license_code=?,compatibility=?,version=version+1,updated_at=NOW(3)
                WHERE id=?
                """, request.templateName.trim(), firstText(request.templateType, "solution"),
                firstText(request.category, "其他"), request.description, firstText(request.icon, "Blocks"),
                request.coverUrl, toJson(request.tags), firstText(request.licenseCode, "Apache-2.0"),
                request.compatibility, template.get("id"));
        return detail(templateId);
    }

    /** 软删除未上架模板。 */
    @Transactional
    public void delete(String templateId) {
        Map<String, Object> template = requireManageable(templateId);
        if ("published".equals(text(template.get("status")))) {
            throw new BusinessException("TEMPLATE_PUBLISHED", "公开上架模板请先下架后再删除");
        }
        jdbcTemplate.update("UPDATE agent_template SET status='deleted',deleted_at=NOW(3),updated_at=NOW(3) WHERE id=?", templateId);
    }

    /** 自动分析入口Agent与团队依赖。 */
    public List<TemplateDtos.ResourceReference> analyzeDependencies(TemplateDtos.PublishRequest request) {
        if (request == null || (request.entryAgentIds.isEmpty() && request.entryTeamIds.isEmpty())) {
            throw new BusinessException("TEMPLATE_ENTRY_REQUIRED", "解决方案模板至少需要一个入口Agent或Agent团队");
        }
        LinkedHashMap<String, TemplateDtos.ResourceReference> resources = new LinkedHashMap<>();
        Set<String> agentIds = new LinkedHashSet<>(request.entryAgentIds);
        for (String teamId : request.entryTeamIds) {
            addReference(resources, "team", teamId, resourceName("team", teamId), true);
            jdbcTemplate.queryForList("SELECT agent_id FROM agent_team_member WHERE team_id=? AND enabled=1", teamId)
                    .forEach(row -> agentIds.add(text(row.get("agent_id"))));
        }
        for (String agentId : agentIds) {
            Map<String, Object> agent = single("SELECT * FROM agent WHERE id=? AND deleted_at IS NULL", agentId);
            if (agent == null) throw new BusinessException("TEMPLATE_AGENT_NOT_FOUND", "入口Agent不存在：" + agentId);
            addReference(resources, "agent", agentId, text(agent.get("agent_name")), true);
            addIfText(resources, "prompt", text(agent.get("system_prompt_template_id")), true);
            jdbcTemplate.queryForList("SELECT tool_id FROM agent_tool_binding WHERE agent_id=? AND enabled=1", agentId)
                    .forEach(row -> addIfText(resources, "tool", text(row.get("tool_id")), true));
            jdbcTemplate.queryForList("SELECT knowledge_base_id FROM agent_knowledge_binding WHERE agent_id=? AND enabled=1", agentId)
                    .forEach(row -> addIfText(resources, "knowledge", text(row.get("knowledge_base_id")), true));
            jdbcTemplate.queryForList("SELECT workflow_id FROM agent_workflow_binding WHERE agent_id=? AND enabled=1", agentId)
                    .forEach(row -> addIfText(resources, "workflow", text(row.get("workflow_id")), true));
            jdbcTemplate.queryForList("SELECT id FROM agent_memory WHERE agent_id=? AND deleted_at IS NULL AND status='enabled'", agentId)
                    .forEach(row -> addIfText(resources, "memory", text(row.get("id")), false));
        }
        for (TemplateDtos.ResourceReference included : request.includeResources) {
            addReference(resources, included.resourceType, included.resourceId,
                    firstText(included.resourceName, resourceName(included.resourceType, included.resourceId)), included.required);
        }
        for (TemplateDtos.ResourceReference excluded : request.excludeResources) {
            resources.remove(key(excluded.resourceType, excluded.resourceId));
        }
        expandKnowledgeResources(resources);
        expandPromptAndToolResources(resources);
        return new ArrayList<>(resources.values());
    }

    /** 创建不可变模板版本、MinIO包和自动检查结果。 */
    @Transactional
    public TemplateDtos.VersionSummary publishVersion(String templateId, TemplateDtos.PublishRequest request) {
        Map<String, Object> template = requireManageable(templateId);
        if (request == null || !StringUtils.hasText(request.versionNo)) {
            throw new BusinessException("TEMPLATE_VERSION_REQUIRED", "模板版本号不能为空");
        }
        TemplatePackagePolicy.compareVersions(request.versionNo, request.versionNo);
        List<Map<String, Object>> existingVersions = jdbcTemplate.queryForList(
                "SELECT version_no FROM agent_template_version WHERE template_id=? ORDER BY created_at DESC LIMIT 1", templateId);
        if (!existingVersions.isEmpty()) {
            String latest = text(existingVersions.getFirst().get("version_no"));
            if (TemplatePackagePolicy.compareVersions(request.versionNo, latest) <= 0) {
                throw new BusinessException("TEMPLATE_VERSION_NOT_INCREASED", "新模板版本必须高于当前版本 " + latest);
            }
            if (TemplatePackagePolicy.isBreakingUpgrade(latest, request.versionNo) && !request.breakingChange) {
                throw new BusinessException("TEMPLATE_BREAKING_CHANGE_REQUIRED", "提高主版本号必须声明为破坏性升级");
            }
            if (request.breakingChange && !TemplatePackagePolicy.isBreakingUpgrade(latest, request.versionNo)) {
                throw new BusinessException("TEMPLATE_MAJOR_VERSION_REQUIRED", "破坏性升级必须提高主版本号");
            }
        }
        if (!StringUtils.hasText(request.changeLog) || !StringUtils.hasText(request.compatibilityStatement)) {
            throw new BusinessException("TEMPLATE_RELEASE_NOTE_REQUIRED", "模板版本必须填写更新说明和兼容性声明");
        }
        List<TemplateDtos.ResourceReference> references = analyzeDependencies(request);
        List<ResourcePackage> packageResources = references.stream().map(this::snapshotResource)
                .sorted(Comparator.comparingInt(ResourcePackage::sortOrder)).toList();
        Map<String, Object> dependencyGraph = dependencyGraph(packageResources);
        boolean rawSecretDetected = packageResources.stream().anyMatch(ResourcePackage::secretDetected);
        boolean dependenciesPassed = packageResources.stream().noneMatch(item -> item.snapshot().isEmpty())
                && packageResources.stream().anyMatch(item -> "agent".equals(item.type()) || "team".equals(item.type()));
        boolean licensePassed = List.of("apache-2.0", "mit", "bsd-3-clause", "cc-by-4.0")
                .contains(text(template.get("license_code")).toLowerCase(Locale.ROOT));
        boolean toolRiskPassed = packageResources.stream().filter(item -> "tool".equals(item.type()))
                .allMatch(item -> !"high".equalsIgnoreCase(text(item.snapshot().get("risk_level")))
                        || truth(item.snapshot().get("require_confirm")));
        boolean promptRiskPassed = packageResources.stream().filter(item -> "prompt".equals(item.type()))
                .noneMatch(item -> containsPromptInjection(text(item.snapshot().get("content"))));
        boolean automaticPassed = !rawSecretDetected && dependenciesPassed && licensePassed && toolRiskPassed && promptRiskPassed;
        Map<String, Object> securityResult = mapOf(
                "passed", automaticPassed, "sensitive", !rawSecretDetected, "dependency", dependenciesPassed,
                "license", licensePassed, "toolRisk", toolRiskPassed, "promptRisk", promptRiskPassed);
        Map<String, Object> runtimeResult = mapOf("passed", dependenciesPassed, "minimumRuntime", dependenciesPassed,
                "entryCount", request.entryAgentIds.size() + request.entryTeamIds.size());
        String versionId = UUID.randomUUID().toString();
        Map<String, Object> packagePayload = mapOf(
                "schemaVersion", 1, "templateId", templateId, "versionId", versionId, "versionNo", request.versionNo,
                "resources", packageResources, "dependencyGraph", dependencyGraph);
        byte[] packageBytes = toJson(packagePayload).getBytes(StandardCharsets.UTF_8);
        String packageHash = sha256(packageBytes);
        String objectKey = "templates/" + templateId + "/" + request.versionNo + "/solution-package.json";
        SharedObjectStorageService.StoredObject stored = objectStorageService.putStream(
                objectKey, new java.io.ByteArrayInputStream(packageBytes), packageBytes.length, "application/json");
        String status = automaticPassed ? (request.submitForPublicReview ? "pending" : "published") : "rejected";
        jdbcTemplate.update("""
                INSERT INTO agent_template_version
                  (id,template_id,version_no,version_name,change_log,compatibility_statement,breaking_change,
                   resource_manifest,dependency_graph,security_scan_result,runtime_check_result,package_bucket,
                   package_key,package_hash,package_size,status,submitted_by,submitted_at,published_by,published_at,created_by)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(3),?,?,?)
                """, versionId, templateId, request.versionNo, request.versionName, request.changeLog,
                request.compatibilityStatement, request.breakingChange, toJson(resourceCountMap(packageResources)),
                toJson(dependencyGraph), toJson(securityResult), toJson(runtimeResult), stored.bucket(), stored.objectKey(),
                packageHash, stored.size(), status, currentUserId(),
                "published".equals(status) ? currentUserId() : null,
                "published".equals(status) ? Timestamp.valueOf(LocalDateTime.now()) : null, currentUserId());
        for (ResourcePackage resource : packageResources) {
            jdbcTemplate.update("""
                    INSERT INTO agent_template_resource
                      (id,template_version_id,resource_type,source_resource_id,resource_code,resource_name,
                       resource_snapshot,content_hash,parent_resource_id,dependency_ids,object_manifest,sort_order,required)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, resource.id(), versionId, resource.type(), resource.sourceId(), resource.code(), resource.name(),
                    toJson(databaseSnapshot(resource)), resource.contentHash(),
                    resource.parentId(), toJson(resource.dependencies()), toJson(resource.objects()), resource.sortOrder(), resource.required());
        }
        jdbcTemplate.update("""
                INSERT INTO agent_template_review
                  (id,template_id,template_version_id,review_type,review_status,risk_level,checklist_result,review_comment,reviewer_user_id)
                VALUES (?,?,?,'automatic',?,?,?,?,?)
                """, UUID.randomUUID().toString(), templateId, versionId,
                automaticPassed ? "passed" : "rejected", automaticPassed ? "low" : "high", toJson(securityResult),
                automaticPassed ? "自动检查通过" : "自动检查未通过，禁止进入公开审核", currentUserId());
        jdbcTemplate.update("""
                UPDATE agent_template SET review_status=?,dependency_manifest=?,package_bucket=?,package_key=?,
                  package_hash=?,package_size=?,updated_at=NOW(3) WHERE id=?
                """, status, toJson(resourceCountMap(packageResources)), stored.bucket(), stored.objectKey(), packageHash, stored.size(), templateId);
        if ("published".equals(status)) {
            activateVersion(templateId, versionId, "workspace");
        }
        return mapVersion(single("SELECT * FROM agent_template_version WHERE id=?", versionId));
    }

    /** 管理员人工审核公开版本。 */
    @Transactional
    public TemplateDtos.VersionSummary review(String versionId, TemplateDtos.ReviewRequest request) {
        requireAuthority("template:review");
        Map<String, Object> version = requireVersion(versionId);
        if (!"pending".equals(text(version.get("status")))) {
            throw new BusinessException("TEMPLATE_REVIEW_STATE_INVALID", "只有待审核版本可以执行人工审核");
        }
        String action = request == null ? "" : text(request.action).toLowerCase(Locale.ROOT);
        String status = switch (action) {
            case "approve" -> "published";
            case "reject" -> "rejected";
            case "changes_required" -> "changes_required";
            default -> throw new BusinessException("TEMPLATE_REVIEW_ACTION_INVALID", "审核动作仅支持approve、reject、changes_required");
        };
        jdbcTemplate.update("UPDATE agent_template_version SET status=?,published_by=?,published_at=? WHERE id=?",
                status, "published".equals(status) ? currentUserId() : null,
                "published".equals(status) ? Timestamp.valueOf(LocalDateTime.now()) : null, versionId);
        jdbcTemplate.update("""
                INSERT INTO agent_template_review
                  (id,template_id,template_version_id,review_type,review_status,risk_level,checklist_result,
                   review_comment,reviewer_user_id)
                VALUES (?,?,?,'manual',?,?,?,?,?)
                """, UUID.randomUUID().toString(), version.get("template_id"), versionId,
                "published".equals(status) ? "passed" : status, firstText(request.riskLevel, "low"), "{}",
                request.comment, currentUserId());
        if ("published".equals(status)) {
            activateVersion(text(version.get("template_id")), versionId, "public");
            markUpgradeAvailable(text(version.get("template_id")), versionId);
        } else {
            jdbcTemplate.update("UPDATE agent_template SET review_status=?,updated_at=NOW(3) WHERE id=?", status, version.get("template_id"));
        }
        return mapVersion(requireVersion(versionId));
    }

    /** 收藏或取消收藏模板。 */
    @Transactional
    public boolean toggleFavorite(String templateId) {
        requirePublicTemplate(templateId);
        int deleted = jdbcTemplate.update("DELETE FROM agent_template_favorite WHERE template_id=? AND user_id=?", templateId, currentUserId());
        boolean favorite = deleted == 0;
        if (favorite) {
            jdbcTemplate.update("INSERT INTO agent_template_favorite(id,template_id,user_id) VALUES (?,?,?)",
                    UUID.randomUUID().toString(), templateId, currentUserId());
        }
        refreshFavoriteCount(templateId);
        return favorite;
    }

    /** 成功安装用户提交或修改唯一评分评论。 */
    @Transactional
    public void rate(String templateId, TemplateDtos.RatingRequest request) {
        String userId = currentUserId();
        Map<String, Object> install = successfulInstall(templateId, userId);
        if (install == null) throw new BusinessException("TEMPLATE_REVIEW_INSTALL_REQUIRED", "成功安装模板后才能评分评论");
        if (request == null || request.rating < 1 || request.rating > 5 || !StringUtils.hasText(request.comment)) {
            throw new BusinessException("TEMPLATE_REVIEW_INVALID", "评分必须为1到5分且评论不能为空");
        }
        jdbcTemplate.update("""
                INSERT INTO agent_template_rating(id,template_id,user_id,install_id,rating)
                VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE rating=VALUES(rating),install_id=VALUES(install_id),updated_at=NOW(3)
                """, UUID.randomUUID().toString(), templateId, userId, install.get("id"), request.rating);
        List<Map<String, Object>> roots = jdbcTemplate.queryForList(
                "SELECT id FROM agent_template_comment WHERE template_id=? AND user_id=? AND parent_comment_id IS NULL", templateId, userId);
        if (roots.isEmpty()) {
            jdbcTemplate.update("""
                    INSERT INTO agent_template_comment(id,template_id,user_id,install_id,comment_content)
                    VALUES (?,?,?,?,?)
                    """, UUID.randomUUID().toString(), templateId, userId, install.get("id"), request.comment.trim());
        } else {
            jdbcTemplate.update("UPDATE agent_template_comment SET comment_content=?,status='visible',updated_at=NOW(3) WHERE id=?",
                    request.comment.trim(), roots.getFirst().get("id"));
        }
        refreshRating(templateId);
    }

    /** 模板作者或管理员回复评论。 */
    @Transactional
    public void reply(String templateId, String parentCommentId, TemplateDtos.ReplyRequest request) {
        Map<String, Object> template = requireTemplate(templateId);
        boolean author = currentUserId().equals(text(template.get("author_user_id")));
        boolean admin = isManager();
        if (!author && !admin) throw new BusinessException("TEMPLATE_REPLY_FORBIDDEN", "只有模板作者或管理员可以回复评论");
        if (request == null || !StringUtils.hasText(request.content)) throw new BusinessException("TEMPLATE_REPLY_REQUIRED", "回复内容不能为空");
        Map<String, Object> parent = single("SELECT id FROM agent_template_comment WHERE id=? AND template_id=?", parentCommentId, templateId);
        if (parent == null) throw new BusinessException("TEMPLATE_COMMENT_NOT_FOUND", "评论不存在");
        jdbcTemplate.update("""
                INSERT INTO agent_template_comment
                  (id,template_id,user_id,parent_comment_id,comment_content,author_reply,admin_reply)
                VALUES (?,?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), templateId, currentUserId(), parentCommentId,
                request.content.trim(), author, admin);
    }

    /** 提交模板举报。 */
    @Transactional
    public void report(String templateId, TemplateDtos.ReportRequest request) {
        requirePublicTemplate(templateId);
        if (request == null || !StringUtils.hasText(request.reportType) || !StringUtils.hasText(request.reason)) {
            throw new BusinessException("TEMPLATE_REPORT_INVALID", "举报类型和原因不能为空");
        }
        jdbcTemplate.update("""
                INSERT INTO agent_template_report
                  (id,template_id,reporter_user_id,report_type,report_reason,evidence)
                VALUES (?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), templateId, currentUserId(), request.reportType,
                request.reason.trim(), toJson(request.evidence));
        jdbcTemplate.update("UPDATE agent_template SET report_count=report_count+1 WHERE id=?", templateId);
    }

    /** 查询模板举报治理队列。 */
    public List<TemplateDtos.ReportSummary> reports(String status) {
        requireAuthority("template:operate");
        String safeStatus = StringUtils.hasText(status) ? status : "pending";
        return jdbcTemplate.queryForList("""
                SELECT r.*,t.template_name,COALESCE(u.display_name,u.username) reporter_name
                FROM agent_template_report r
                JOIN agent_template t ON t.id=r.template_id
                LEFT JOIN iam_user u ON u.id=r.reporter_user_id
                WHERE (?='all' OR r.status=?) ORDER BY r.created_at DESC
                LIMIT 200
                """, safeStatus, safeStatus).stream().map(row -> {
            TemplateDtos.ReportSummary item = new TemplateDtos.ReportSummary();
            item.id=text(row.get("id")); item.templateId=text(row.get("template_id"));
            item.templateName=text(row.get("template_name")); item.reporterUserId=text(row.get("reporter_user_id"));
            item.reporterName=text(row.get("reporter_name")); item.reportType=text(row.get("report_type"));
            item.reason=text(row.get("report_reason")); item.evidence=jsonList(row.get("evidence"));
            item.status=text(row.get("status")); item.resolution=text(row.get("resolution"));
            item.handledBy=text(row.get("handled_by")); item.createdAt=dateTime(row.get("created_at"));
            item.handledAt=dateTime(row.get("handled_at")); return item;
        }).toList();
    }

    /** 处置模板举报并可同步下架存在风险的模板。 */
    @Transactional
    public TemplateDtos.ReportSummary resolveReport(String reportId, TemplateDtos.ReportResolutionRequest request) {
        requireAuthority("template:operate");
        if (request == null || !List.of("resolved", "rejected").contains(request.action)
                || !StringUtils.hasText(request.resolution)) {
            throw new BusinessException("TEMPLATE_REPORT_RESOLUTION_INVALID", "请填写有效的举报处理动作和结论");
        }
        Map<String,Object> report=single("SELECT * FROM agent_template_report WHERE id=?",reportId);
        if(report==null) throw new BusinessException("TEMPLATE_REPORT_NOT_FOUND","模板举报不存在");
        jdbcTemplate.update("UPDATE agent_template_report SET status=?,resolution=?,handled_by=?,handled_at=NOW(3) WHERE id=?",
                request.action, request.resolution.trim(), currentUserId(), reportId);
        if (request.offlineTemplate && "resolved".equals(request.action)) {
            jdbcTemplate.update("UPDATE agent_template SET status='offline',recommended=0,updated_at=NOW(3) WHERE id=?", report.get("template_id"));
        }
        return reports("all").stream().filter(item -> item.id.equals(reportId)).findFirst()
                .orElseThrow(() -> new BusinessException("TEMPLATE_REPORT_NOT_FOUND", "模板举报不存在"));
    }

    /** 运营人员设置推荐或上下架。 */
    @Transactional
    public TemplateDtos.TemplateDetail operate(String templateId, Boolean recommended, String status) {
        requireAuthority("template:operate");
        if (StringUtils.hasText(status) && !List.of("published", "offline").contains(status)) {
            throw new BusinessException("TEMPLATE_OPERATION_STATUS_INVALID", "运营状态仅支持published或offline");
        }
        jdbcTemplate.update("UPDATE agent_template SET recommended=COALESCE(?,recommended),status=COALESCE(?,status),updated_at=NOW(3) WHERE id=?",
                recommended, StringUtils.hasText(status) ? status : null, templateId);
        return detail(templateId);
    }

    /** 查询待人工审核版本。 */
    public List<TemplateDtos.VersionSummary> pendingReviews() {
        requireAuthority("template:review");
        return jdbcTemplate.queryForList("SELECT * FROM agent_template_version WHERE status='pending' ORDER BY submitted_at ASC")
                .stream().map(this::mapVersion).toList();
    }

    /** 查询版本资源清单。 */
    public List<TemplateDtos.ResourceSummary> resources(String versionId) {
        return jdbcTemplate.queryForList("SELECT * FROM agent_template_resource WHERE template_version_id=? ORDER BY sort_order,resource_type", versionId)
                .stream().map(this::mapResource).toList();
    }

    /** 供安装服务读取模板原始数据。 */
    Map<String, Object> requireTemplate(String templateId) {
        Map<String, Object> template = single("SELECT t.*,v.version_no,0 favorite FROM agent_template t LEFT JOIN agent_template_version v ON v.id=t.current_version_id WHERE t.id=? AND t.deleted_at IS NULL", templateId);
        if (template == null) throw new BusinessException("TEMPLATE_NOT_FOUND", "解决方案模板不存在");
        return template;
    }

    /** 供安装服务读取版本数据。 */
    Map<String, Object> requireVersion(String versionId) {
        Map<String, Object> version = single("SELECT * FROM agent_template_version WHERE id=?", versionId);
        if (version == null) throw new BusinessException("TEMPLATE_VERSION_NOT_FOUND", "模板版本不存在");
        return version;
    }

    /** 供安装服务读取资源快照。 */
    List<Map<String, Object>> versionResourceRows(String versionId) {
        return jdbcTemplate.queryForList("SELECT * FROM agent_template_resource WHERE template_version_id=? ORDER BY sort_order,id", versionId);
    }

    /** 将模板版本激活为当前版本。 */
    private void activateVersion(String templateId, String versionId, String visibility) {
        jdbcTemplate.update("""
                UPDATE agent_template SET current_version_id=?,visibility=?,review_status='approved',status='published',
                  published_at=COALESCE(published_at,NOW(3)),updated_at=NOW(3) WHERE id=?
                """, versionId, visibility, templateId);
        Map<String, Object> version = requireVersion(versionId);
        jdbcTemplate.update("""
                UPDATE agent_template SET dependency_manifest=?,package_bucket=?,package_key=?,package_hash=?,package_size=?
                WHERE id=?
                """, version.get("resource_manifest"), version.get("package_bucket"), version.get("package_key"),
                version.get("package_hash"), version.get("package_size"), templateId);
    }

    /** 新版本发布后标记所有旧安装实例可升级。 */
    private void markUpgradeAvailable(String templateId, String currentVersionId) {
        jdbcTemplate.update("UPDATE agent_template_install SET upgrade_available=(template_version_id<>?),updated_at=NOW(3) WHERE template_id=? AND install_status='success'",
                currentVersionId, templateId);
        String notificationId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO notification(id,notification_type,title,content,severity,resource_type,resource_id,payload,created_by)
                SELECT ?,'template_upgrade','解决方案模板有新版本',CONCAT(t.template_name,' 已发布新版本'),'info',
                       'agent_template',t.id,JSON_OBJECT('templateId',t.id,'versionId',?),NULL
                FROM agent_template t WHERE t.id=?
                """, notificationId, currentVersionId, templateId);
        jdbcTemplate.update("""
                INSERT IGNORE INTO notification_recipient(id,notification_id,user_id)
                SELECT UUID(),?,installed_by FROM agent_template_install
                WHERE template_id=? AND install_status='success' AND installed_by IS NOT NULL
                """, notificationId, templateId);
    }

    /** MySQL清单移除大体量向量正文，完整数据仅保存在MinIO模板包。 */
    private Map<String, Object> databaseSnapshot(ResourcePackage resource) {
        Map<String, Object> snapshot = new LinkedHashMap<>(TemplatePackagePolicy.sanitizeSnapshot(resource.snapshot()));
        if ("embedding".equals(resource.type())) {
            snapshot.remove("embedding_json");
            snapshot.remove("embedding_blob_base64");
            snapshot.put("vector_payload_location", "package:" + resource.id());
        }
        return snapshot;
    }

    /** 展开知识库下的文档、切片和向量依赖。 */
    private void expandKnowledgeResources(LinkedHashMap<String, TemplateDtos.ResourceReference> resources) {
        List<TemplateDtos.ResourceReference> knowledgeBases = resources.values().stream()
                .filter(item -> "knowledge".equals(item.resourceType)).toList();
        for (TemplateDtos.ResourceReference kb : knowledgeBases) {
            jdbcTemplate.queryForList("SELECT id,doc_name FROM knowledge_document WHERE kb_id=?", kb.resourceId)
                    .forEach(row -> addReference(resources, "document", text(row.get("id")), text(row.get("doc_name")), true));
            jdbcTemplate.queryForList("SELECT id,COALESCE(title,CONCAT('分片-',chunk_no)) name FROM knowledge_chunk WHERE kb_id=?", kb.resourceId)
                    .forEach(row -> addReference(resources, "chunk", text(row.get("id")), text(row.get("name")), true));
            jdbcTemplate.queryForList("SELECT id,CONCAT('向量-',id) name FROM knowledge_embedding WHERE kb_id=?", kb.resourceId)
                    .forEach(row -> addReference(resources, "embedding", text(row.get("id")), text(row.get("name")), true));
        }
    }

    /** 展开Prompt稳定版本和MCP工具服务器依赖。 */
    private void expandPromptAndToolResources(LinkedHashMap<String, TemplateDtos.ResourceReference> resources) {
        List<TemplateDtos.ResourceReference> prompts = resources.values().stream().filter(item -> "prompt".equals(item.resourceType)).toList();
        for (TemplateDtos.ResourceReference prompt : prompts) {
            jdbcTemplate.queryForList("SELECT stable_version_id FROM prompt_template WHERE id=?", prompt.resourceId).stream()
                    .map(row -> text(row.get("stable_version_id"))).filter(StringUtils::hasText)
                    .forEach(id -> addReference(resources, "prompt_version", id, resourceName("prompt_version", id), true));
        }
        List<TemplateDtos.ResourceReference> tools = resources.values().stream().filter(item -> "tool".equals(item.resourceType)).toList();
        for (TemplateDtos.ResourceReference tool : tools) {
            jdbcTemplate.queryForList("SELECT mcp_server_id FROM tool_definition WHERE id=?", tool.resourceId).stream()
                    .map(row -> text(row.get("mcp_server_id"))).filter(StringUtils::hasText)
                    .forEach(id -> addReference(resources, "mcp", id, resourceName("mcp", id), false));
        }
    }

    /** 将数据库资源转换成已清洗快照。 */
    private ResourcePackage snapshotResource(TemplateDtos.ResourceReference reference) {
        TableConfig config = tableConfig(reference.resourceType);
        Map<String, Object> row = single("SELECT * FROM " + config.table() + " WHERE id=?", reference.resourceId);
        if (row == null) throw new BusinessException("TEMPLATE_RESOURCE_NOT_FOUND", "模板资源不存在：" + reference.resourceType + "/" + reference.resourceId);
        List<Map<String, Object>> objects = new ArrayList<>();
        if ("document".equals(reference.resourceType) && StringUtils.hasText(text(row.get("storage_key")))) {
            objects.add(mapOf("bucket", row.get("storage_bucket"), "key", row.get("storage_key"),
                    "hash", row.get("file_hash"), "size", row.get("file_size")));
        }
        if (row.get("embedding_blob") instanceof byte[] bytes) {
            row.put("embedding_blob_base64", Base64.getEncoder().encodeToString(bytes));
            row.remove("embedding_blob");
        }
        if ("agent".equals(reference.resourceType)) {
            row.put("_tool_ids", jdbcTemplate.queryForList("SELECT tool_id FROM agent_tool_binding WHERE agent_id=? AND enabled=1", String.class, reference.resourceId));
            row.put("_knowledge_ids", jdbcTemplate.queryForList("SELECT knowledge_base_id FROM agent_knowledge_binding WHERE agent_id=? AND enabled=1", String.class, reference.resourceId));
            row.put("_workflow_ids", jdbcTemplate.queryForList("SELECT workflow_id FROM agent_workflow_binding WHERE agent_id=? AND enabled=1", String.class, reference.resourceId));
        }
        if ("team".equals(reference.resourceType)) {
            row.put("_members", jdbcTemplate.queryForList("SELECT * FROM agent_team_member WHERE team_id=? AND enabled=1", reference.resourceId));
        }
        boolean secretDetected = SECRET_PATTERN.matcher(toJson(row)).find();
        Map<String, Object> sanitized = TemplatePackagePolicy.sanitizeSnapshot(row);
        String hash = sha256(toJson(sanitized).getBytes(StandardCharsets.UTF_8));
        return new ResourcePackage(UUID.randomUUID().toString(), reference.resourceType, reference.resourceId,
                text(row.get(config.codeColumn())), firstText(reference.resourceName, text(row.get(config.nameColumn()))),
                sanitized, hash, parentId(reference.resourceType, row), dependencyIds(reference.resourceType, row),
                objects, config.sortOrder(), reference.required, secretDetected);
    }

    /** 构造模板资源依赖图。 */
    private Map<String, Object> dependencyGraph(List<ResourcePackage> resources) {
        List<Map<String, Object>> nodes = resources.stream().map(item -> mapOf(
                "id", item.id(), "type", item.type(), "sourceId", item.sourceId(), "name", item.name())).toList();
        List<Map<String, Object>> edges = new ArrayList<>();
        for (ResourcePackage resource : resources) {
            for (String dependency : resource.dependencies()) {
                edges.add(mapOf("source", resource.id(), "target", dependency));
            }
        }
        return mapOf("nodes", nodes, "edges", edges);
    }

    /** 按资源类型统计模板包内容。 */
    private Map<String, Integer> resourceCountMap(List<ResourcePackage> resources) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        resources.forEach(item -> counts.merge(item.type(), 1, Integer::sum));
        return counts;
    }

    /** 资源类型对应的数据库表、编码、名称和安装顺序。 */
    private TableConfig tableConfig(String type) {
        return switch (String.valueOf(type)) {
            case "prompt" -> new TableConfig("prompt_template", "template_code", "template_name", 10);
            case "prompt_version" -> new TableConfig("prompt_template_version", "version_no", "version_no", 15);
            case "mcp" -> new TableConfig("mcp_server", "server_code", "server_name", 20);
            case "tool" -> new TableConfig("tool_definition", "tool_code", "tool_name", 30);
            case "knowledge" -> new TableConfig("knowledge_base", "kb_code", "kb_name", 40);
            case "document" -> new TableConfig("knowledge_document", "id", "doc_name", 45);
            case "chunk" -> new TableConfig("knowledge_chunk", "id", "title", 50);
            case "embedding" -> new TableConfig("knowledge_embedding", "id", "id", 55);
            case "workflow" -> new TableConfig("workflow_definition", "workflow_code", "workflow_name", 60);
            case "agent" -> new TableConfig("agent", "agent_code", "agent_name", 70);
            case "team" -> new TableConfig("agent_team", "team_code", "team_name", 80);
            case "memory" -> new TableConfig("agent_memory", "memory_key", "memory_key", 90);
            default -> throw new BusinessException("TEMPLATE_RESOURCE_TYPE_INVALID", "不支持的模板资源类型：" + type);
        };
    }

    /** 查询资源展示名称。 */
    private String resourceName(String type, String id) {
        if (!StringUtils.hasText(id)) return "";
        TableConfig config = tableConfig(type);
        Map<String, Object> row = single("SELECT " + config.nameColumn() + " name FROM " + config.table() + " WHERE id=?", id);
        return row == null ? id : firstText(text(row.get("name")), id);
    }

    /** 从快照字段推导父资源。 */
    private String parentId(String type, Map<String, Object> row) {
        return switch (type) {
            case "prompt_version" -> text(row.get("template_id"));
            case "document" -> text(row.get("kb_id"));
            case "chunk" -> text(row.get("document_id"));
            case "embedding" -> text(row.get("chunk_id"));
            default -> null;
        };
    }

    /** 从快照字段推导资源依赖。 */
    private List<String> dependencyIds(String type, Map<String, Object> row) {
        List<String> values = new ArrayList<>();
        String parent = parentId(type, row);
        if (StringUtils.hasText(parent)) values.add(parent);
        if ("agent".equals(type)) {
            if (StringUtils.hasText(text(row.get("system_prompt_template_id")))) values.add(text(row.get("system_prompt_template_id")));
            if (StringUtils.hasText(text(row.get("model_id")))) values.add(text(row.get("model_id")));
        }
        if ("tool".equals(type) && StringUtils.hasText(text(row.get("mcp_server_id")))) values.add(text(row.get("mcp_server_id")));
        return values;
    }

    /** 映射模板摘要。 */
    private TemplateDtos.TemplateSummary mapSummary(Map<String, Object> row) {
        TemplateDtos.TemplateSummary item = new TemplateDtos.TemplateSummary();
        item.id = text(row.get("id")); item.templateCode = text(row.get("template_code"));
        item.templateName = text(row.get("template_name")); item.templateType = text(row.get("template_type"));
        item.visibility = text(row.get("visibility")); item.category = text(row.get("category"));
        item.description = text(row.get("description")); item.icon = text(row.get("icon"));
        item.coverUrl = text(row.get("cover_url")); item.tags = jsonList(row.get("tags"));
        item.authorUserId = text(row.get("author_user_id")); item.authorName = text(row.get("author_name"));
        item.currentVersion = text(row.get("version_no")); item.currentVersionId = text(row.get("current_version_id"));
        item.status = text(row.get("status")); item.reviewStatus = text(row.get("review_status"));
        item.recommended = truth(row.get("recommended")); item.favorite = truth(row.get("favorite"));
        item.installCount = longValue(row.get("install_count")); item.averageRating = decimal(row.get("average_rating"));
        item.ratingCount = longValue(row.get("rating_count")); item.favoriteCount = longValue(row.get("favorite_count"));
        item.trendScore = decimal(row.get("trend_score")); item.resourceCounts = jsonIntegerMap(row.get("dependency_manifest"));
        item.publishedAt = dateTime(row.get("published_at")); item.updatedAt = dateTime(row.get("updated_at"));
        return item;
    }

    /** 复制摘要字段到详情。 */
    private void copySummary(TemplateDtos.TemplateSummary source, TemplateDtos.TemplateDetail target) {
        target.id=source.id; target.templateCode=source.templateCode; target.templateName=source.templateName;
        target.templateType=source.templateType; target.visibility=source.visibility; target.category=source.category;
        target.description=source.description; target.icon=source.icon; target.coverUrl=source.coverUrl; target.tags=source.tags;
        target.authorUserId=source.authorUserId; target.authorName=source.authorName; target.currentVersion=source.currentVersion;
        target.currentVersionId=source.currentVersionId; target.status=source.status; target.reviewStatus=source.reviewStatus;
        target.recommended=source.recommended; target.favorite=source.favorite; target.installCount=source.installCount;
        target.averageRating=source.averageRating; target.ratingCount=source.ratingCount; target.favoriteCount=source.favoriteCount;
        target.trendScore=source.trendScore; target.resourceCounts=source.resourceCounts; target.publishedAt=source.publishedAt;
        target.updatedAt=source.updatedAt;
    }

    /** 映射版本摘要。 */
    private TemplateDtos.VersionSummary mapVersion(Map<String, Object> row) {
        TemplateDtos.VersionSummary item = new TemplateDtos.VersionSummary();
        item.id=text(row.get("id")); item.templateId=text(row.get("template_id")); item.versionNo=text(row.get("version_no"));
        item.versionName=text(row.get("version_name")); item.changeLog=text(row.get("change_log"));
        item.compatibilityStatement=text(row.get("compatibility_statement")); item.breakingChange=truth(row.get("breaking_change"));
        item.status=text(row.get("status")); item.packageHash=text(row.get("package_hash")); item.packageSize=longValue(row.get("package_size"));
        item.securityScanResult=jsonMap(row.get("security_scan_result")); item.runtimeCheckResult=jsonMap(row.get("runtime_check_result"));
        item.submittedAt=dateTime(row.get("submitted_at")); item.publishedAt=dateTime(row.get("published_at"));
        item.createdAt=dateTime(row.get("created_at")); return item;
    }

    /** 映射模板资源摘要。 */
    private TemplateDtos.ResourceSummary mapResource(Map<String, Object> row) {
        TemplateDtos.ResourceSummary item = new TemplateDtos.ResourceSummary();
        item.id=text(row.get("id")); item.resourceType=text(row.get("resource_type"));
        item.sourceResourceId=text(row.get("source_resource_id")); item.resourceCode=text(row.get("resource_code"));
        item.resourceName=text(row.get("resource_name")); item.contentHash=text(row.get("content_hash"));
        item.required=truth(row.get("required")); item.dependencyIds=jsonList(row.get("dependency_ids"));
        item.sortOrder=intValue(row.get("sort_order")); return item;
    }

    /** 查询评论及评分。 */
    private List<TemplateDtos.CommentSummary> comments(String templateId) {
        return jdbcTemplate.queryForList("""
                SELECT c.*,COALESCE(u.display_name,u.username) user_name,r.rating
                FROM agent_template_comment c LEFT JOIN iam_user u ON u.id=c.user_id
                LEFT JOIN agent_template_rating r ON r.template_id=c.template_id AND r.user_id=c.user_id
                WHERE c.template_id=? AND c.status='visible' ORDER BY c.created_at ASC
                """, templateId).stream().map(row -> {
            TemplateDtos.CommentSummary item = new TemplateDtos.CommentSummary();
            item.id=text(row.get("id")); item.userId=text(row.get("user_id")); item.userName=text(row.get("user_name"));
            item.parentCommentId=text(row.get("parent_comment_id")); item.content=text(row.get("comment_content"));
            item.authorReply=truth(row.get("author_reply")); item.adminReply=truth(row.get("admin_reply"));
            item.rating=row.get("rating") == null ? null : intValue(row.get("rating")); item.createdAt=dateTime(row.get("created_at"));
            return item;
        }).toList();
    }

    /** 刷新评分聚合和趋势分。 */
    private void refreshRating(String templateId) {
        jdbcTemplate.update("""
                UPDATE agent_template t SET
                  average_rating=COALESCE((SELECT AVG(rating) FROM agent_template_rating WHERE template_id=t.id),0),
                  rating_count=(SELECT COUNT(1) FROM agent_template_rating WHERE template_id=t.id),
                  trend_score=install_count*1.0+favorite_count*2.0+rating_count*3.0+average_rating*5.0
                WHERE t.id=?
                """, templateId);
    }

    /** 刷新收藏聚合和趋势分。 */
    private void refreshFavoriteCount(String templateId) {
        jdbcTemplate.update("""
                UPDATE agent_template t SET favorite_count=(SELECT COUNT(1) FROM agent_template_favorite WHERE template_id=t.id),
                  trend_score=install_count*1.0+favorite_count*2.0+rating_count*3.0+average_rating*5.0 WHERE t.id=?
                """, templateId);
    }

    /** 校验模板创建请求。 */
    private void validateTemplateRequest(TemplateDtos.TemplateRequest request) {
        if (request == null || !StringUtils.hasText(request.templateName)) {
            throw new BusinessException("TEMPLATE_NAME_REQUIRED", "模板名称不能为空");
        }
    }

    /** 查询并校验可管理模板。 */
    private Map<String, Object> requireManageable(String templateId) {
        Map<String, Object> template = requireTemplate(templateId);
        if (!isManager() && !currentUserId().equals(text(template.get("author_user_id")))) {
            throw new BusinessException("TEMPLATE_MANAGE_FORBIDDEN", "无权管理该解决方案模板");
        }
        return template;
    }

    /** 查询并校验公开模板。 */
    private Map<String, Object> requirePublicTemplate(String templateId) {
        Map<String, Object> template = requireTemplate(templateId);
        if (!"public".equals(text(template.get("visibility"))) || !"published".equals(text(template.get("status")))) {
            throw new BusinessException("TEMPLATE_NOT_PUBLIC", "模板尚未公开上架");
        }
        return template;
    }

    /** 判断是否可查看模板。 */
    private boolean canView(Map<String, Object> template) {
        return "public".equals(text(template.get("visibility"))) || isManager()
                || currentUserId().equals(text(template.get("author_user_id")))
                || String.valueOf(WorkspaceContextHolder.current()).equals(text(template.get("workspace_id")));
    }

    /** 判断用户是否成功安装过模板。 */
    private boolean hasSuccessfulInstall(String templateId, String userId) {
        return successfulInstall(templateId, userId) != null;
    }

    /** 查询用户最近一次成功安装。 */
    private Map<String, Object> successfulInstall(String templateId, String userId) {
        return single("SELECT * FROM agent_template_install WHERE template_id=? AND installed_by=? AND install_status='success' ORDER BY created_at DESC LIMIT 1", templateId, userId);
    }

    /** 权限校验。 */
    private void requireAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(item -> authority.equals(item.getAuthority()) || "ROLE_SUPER_ADMIN".equals(item.getAuthority()) || "ROLE_ADMIN".equals(item.getAuthority()));
        if (!allowed) throw new BusinessException("TEMPLATE_PERMISSION_FORBIDDEN", "缺少模板治理权限：" + authority);
    }

    /** 判断当前用户是否管理员。 */
    private boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream().anyMatch(item ->
                List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "template:review", "template:operate").contains(item.getAuthority()));
    }

    /** 当前用户ID。 */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUserDetails details ? details.getUserId() : "";
    }

    /** 当前用户展示名称。 */
    private String currentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails details) {
            return firstText(details.getUser().getDisplayName(), details.getUsername());
        }
        return "";
    }

    /** 唯一模板编码。 */
    private String uniqueCode(String base) {
        String value = firstText(base, "solution").replaceAll("[^a-zA-Z0-9_-]", "-").toLowerCase(Locale.ROOT);
        if (count("SELECT COUNT(1) FROM agent_template WHERE template_code=?", value) == 0) return value;
        return value + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** 名称转简单编码。 */
    private String slug(String name) {
        return "solution-" + Integer.toUnsignedString(String.valueOf(name).hashCode(), 36);
    }

    /** 添加非空资源引用。 */
    private void addIfText(LinkedHashMap<String, TemplateDtos.ResourceReference> target, String type, String id, boolean required) {
        if (StringUtils.hasText(id)) addReference(target, type, id, resourceName(type, id), required);
    }

    /** 幂等添加资源引用。 */
    private void addReference(LinkedHashMap<String, TemplateDtos.ResourceReference> target,
                              String type, String id, String name, boolean required) {
        if (!StringUtils.hasText(type) || !StringUtils.hasText(id)) return;
        target.computeIfAbsent(key(type, id), ignored -> {
            TemplateDtos.ResourceReference item = new TemplateDtos.ResourceReference();
            item.resourceType=type; item.resourceId=id; item.resourceName=name; item.required=required; return item;
        });
    }

    /** 资源引用键。 */
    private String key(String type, String id) { return type + ":" + id; }

    /** 查询单行。 */
    private Map<String, Object> single(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** 结果集转Map，保留数据库列名。 */
    private Map<String, Object> resultSetMap(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        java.sql.ResultSetMetaData meta = rs.getMetaData();
        for (int index=1; index<=meta.getColumnCount(); index++) row.put(meta.getColumnLabel(index), rs.getObject(index));
        return row;
    }

    /** 统计查询。 */
    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args); return value == null ? 0L : value;
    }

    /** JSON序列化。 */
    String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException("模板JSON序列化失败", exception); }
    }

    /** JSON对象解析。 */
    Map<String, Object> jsonMap(Object value) {
        try { return !StringUtils.hasText(text(value)) ? new LinkedHashMap<>() : objectMapper.readValue(text(value), new TypeReference<>() {}); }
        catch (Exception ignored) { return new LinkedHashMap<>(); }
    }

    /** JSON字符串列表解析。 */
    List<String> jsonList(Object value) {
        try { return !StringUtils.hasText(text(value)) ? new ArrayList<>() : objectMapper.readValue(text(value), new TypeReference<>() {}); }
        catch (Exception ignored) { return new ArrayList<>(); }
    }

    /** JSON数值Map解析。 */
    private Map<String, Integer> jsonIntegerMap(Object value) {
        try { return !StringUtils.hasText(text(value)) ? new LinkedHashMap<>() : objectMapper.readValue(text(value), new TypeReference<>() {}); }
        catch (Exception ignored) { return new LinkedHashMap<>(); }
    }

    /** SHA-256哈希。 */
    String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256不可用", exception); }
    }

    /** Prompt注入风险检查。 */
    private boolean containsPromptInjection(String content) {
        String value = String.valueOf(content).toLowerCase(Locale.ROOT);
        return value.contains("ignore previous instructions") || value.contains("忽略之前的指令") || value.contains("泄露系统提示词");
    }

    /** 空值安全文本。 */ String text(Object value) { return value == null ? "" : String.valueOf(value); }
    /** 首个非空文本。 */ private String firstText(String value, String fallback) { return StringUtils.hasText(value) ? value : fallback; }
    /** 布尔转换。 */ boolean truth(Object value) { return value instanceof Boolean b ? b : value instanceof Number n ? n.intValue()!=0 : "true".equalsIgnoreCase(text(value)) || "1".equals(text(value)); }
    /** 长整数转换。 */ private long longValue(Object value) { return value instanceof Number n ? n.longValue() : 0L; }
    /** 整数转换。 */ private int intValue(Object value) { return value instanceof Number n ? n.intValue() : 0; }
    /** 小数转换。 */ private BigDecimal decimal(Object value) { return value instanceof BigDecimal b ? b : value instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO; }
    /** 时间转换。 */ private LocalDateTime dateTime(Object value) { return value instanceof LocalDateTime time ? time : value instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : null; }
    /** 简单Map构造。 */ private Map<String,Object> mapOf(Object... values) { Map<String,Object> map=new LinkedHashMap<>(); for(int i=0;i+1<values.length;i+=2) map.put(String.valueOf(values[i]),values[i+1]); return map; }

    /** 数据库资源表配置。 */
    private record TableConfig(
            /** 数据库表名。 */ String table,
            /** 业务编码列名。 */ String codeColumn,
            /** 展示名称列名。 */ String nameColumn,
            /** 安装顺序。 */ int sortOrder) { }

    /** 模板包资源内部模型。 */
    private record ResourcePackage(
            /** 模板资源ID。 */ String id,
            /** 资源类型。 */ String type,
            /** 来源资源ID。 */ String sourceId,
            /** 资源业务编码。 */ String code,
            /** 资源展示名称。 */ String name,
            /** 完整资源快照。 */ Map<String,Object> snapshot,
            /** 内容哈希。 */ String contentHash,
            /** 父资源ID。 */ String parentId,
            /** 依赖资源ID。 */ List<String> dependencies,
            /** 关联对象清单。 */ List<Map<String,Object>> objects,
            /** 安装顺序。 */ int sortOrder,
            /** 是否为必需资源。 */ boolean required,
            /** 清洗前是否检出敏感内容。 */ boolean secretDetected) { }
}
