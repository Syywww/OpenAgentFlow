package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.api.PageResult;
import com.openagentflow.domain.prompt.PromptDtos;
import com.openagentflow.entity.PromptTemplateEntity;
import com.openagentflow.entity.PromptTemplateVersionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.PromptTemplateMapper;
import com.openagentflow.mapper.PromptTemplateVersionMapper;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 模板中心应用服务。
 *
 * <p>负责 Prompt 模板 CRUD、变量解析、版本发布、版本回滚和复制能力。</p>
 */
@Service
public class PromptTemplateService {

    /** Prompt 变量占位符，例如 {{user_input}}。 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

    /** Prompt 模板 Mapper。 */
    private final PromptTemplateMapper promptTemplateMapper;

    /** Prompt 模板版本 Mapper。 */
    private final PromptTemplateVersionMapper promptTemplateVersionMapper;

    /** JDBC 工具，用于分页统计和聚合指标。 */
    private final JdbcTemplate jdbcTemplate;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** Prompt编译器，用于发布前变量、注入和敏感信息检查。 */
    private final PromptCompiler promptCompiler;

    public PromptTemplateService(PromptTemplateMapper promptTemplateMapper,
                                 PromptTemplateVersionMapper promptTemplateVersionMapper,
                                 JdbcTemplate jdbcTemplate,
                                 ObjectMapper objectMapper,
                                 PromptCompiler promptCompiler) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.promptTemplateVersionMapper = promptTemplateVersionMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.promptCompiler = promptCompiler;
    }

    /**
     * 查询 Prompt 模板中心概览。
     *
     * @return 概览指标
     */
    public PromptDtos.Overview getOverview() {
        PromptDtos.Overview overview = new PromptDtos.Overview();
        overview.templateCount = count("SELECT COUNT(1) FROM prompt_template");
        overview.publishedCount = count("SELECT COUNT(1) FROM prompt_template WHERE status = 'published'");
        overview.draftCount = count("SELECT COUNT(1) FROM prompt_template WHERE status = 'draft'");
        overview.versionCount = count("SELECT COUNT(1) FROM prompt_template_version");
        overview.runningExperimentCount = count("SELECT COUNT(1) FROM prompt_experiment WHERE status='running'");
        overview.productionReleaseCount = count("SELECT COUNT(1) FROM prompt_environment_release WHERE environment='production' AND status='active'");
        overview.activeBindingCount = count("SELECT COUNT(1) FROM prompt_binding WHERE enabled=1");
        return overview;
    }

    /**
     * 分页查询 Prompt 模板。
     *
     * @param promptType Prompt 类型
     * @param status 模板状态
     * @param keyword 搜索关键字
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 模板分页
     */
    public PageResult<PromptDtos.TemplateSummary> listTemplates(String promptType,
                                                                String status,
                                                                String keyword,
                                                                Integer pageNo,
                                                                Integer pageSize) {
        int current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (current - 1) * size;

        LambdaQueryWrapper<PromptTemplateEntity> wrapper = buildTemplateWrapper(promptType, status, keyword);
        Long total = promptTemplateMapper.selectCount(wrapper);
        List<PromptTemplateEntity> records = promptTemplateMapper.selectList(wrapper
                .orderByDesc(PromptTemplateEntity::getUpdatedAt)
                .last("LIMIT " + size + " OFFSET " + offset));
        return new PageResult<>(records.stream().map(this::toSummary).toList(), total, current, size);
    }

    /**
     * 查询 Prompt 模板详情。
     *
     * @param id 模板ID
     * @return 模板详情
     */
    public PromptDtos.TemplateDetail getTemplate(String id) {
        PromptTemplateEntity entity = requireTemplate(id);
        PromptDtos.TemplateDetail detail = new PromptDtos.TemplateDetail();
        copySummary(toSummary(entity), detail);
        detail.versions = listVersions(id);
        return detail;
    }

    /**
     * 创建 Prompt 模板。
     *
     * @param request 创建请求
     * @return 模板详情
     */
    @Transactional(rollbackFor = Exception.class)
    public PromptDtos.TemplateDetail createTemplate(PromptDtos.TemplateRequest request) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setId(newId());
        entity.setWorkspaceId(WorkspaceContextHolder.current());
        fillTemplate(entity, request, true);
        entity.setOwnerUserId(currentUserId());
        entity.setVersion(0L);
        promptTemplateMapper.insert(entity);
        return getTemplate(entity.getId());
    }

    /**
     * 更新 Prompt 模板。
     *
     * @param id 模板ID
     * @param request 更新请求
     * @return 模板详情
     */
    @Transactional(rollbackFor = Exception.class)
    public PromptDtos.TemplateDetail updateTemplate(String id, PromptDtos.TemplateRequest request) {
        PromptTemplateEntity entity = requireTemplate(id);
        fillTemplate(entity, request, false);
        entity.setVersion(entity.getVersion() == null ? 1L : entity.getVersion() + 1);
        promptTemplateMapper.updateById(entity);
        return getTemplate(entity.getId());
    }

    /**
     * 删除 Prompt 模板。
     *
     * @param id 模板ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String id) {
        requireTemplate(id);
        promptTemplateMapper.deleteById(id);
    }

    /**
     * 发布 Prompt 模板并保存版本快照。
     *
     * @param id 模板ID
     * @param request 发布请求
     * @return 模板详情
     */
    @Transactional(rollbackFor = Exception.class)
    public PromptDtos.TemplateDetail publishTemplate(String id, PromptDtos.PublishRequest request) {
        PromptTemplateEntity entity = requireTemplate(id);
        String versionNo = StringUtils.hasText(request == null ? null : request.versionNo)
                ? request.versionNo.trim()
                : nextVersionNo(entity.getId());
        ensureVersionNoAvailable(entity.getId(), versionNo);
        PromptTemplateVersionEntity publishedVersion = saveVersion(entity, versionNo, request == null ? null : request.changeNote);
        entity.setStatus("published");
        // 首个版本可作为初始稳定基线；后续版本必须经过环境晋级与生产门禁后才能替换稳定版。
        if (!StringUtils.hasText(entity.getStableVersionId())) {
            entity.setStableVersionId(publishedVersion.getId());
        }
        entity.setVersion(entity.getVersion() == null ? 1L : entity.getVersion() + 1);
        promptTemplateMapper.updateById(entity);
        return getTemplate(entity.getId());
    }

    /**
     * 回滚 Prompt 模板到指定历史版本，并生成新的回滚版本记录。
     *
     * @param id 模板ID
     * @param versionId 版本ID
     * @return 模板详情
     */
    @Transactional(rollbackFor = Exception.class)
    public PromptDtos.TemplateDetail rollbackTemplate(String id, String versionId) {
        PromptTemplateEntity entity = requireTemplate(id);
        PromptTemplateVersionEntity version = requireVersion(versionId);
        if (!id.equals(version.getTemplateId())) {
            throw new BusinessException("PROMPT_VERSION_MISMATCH", "版本不属于当前模板");
        }

        // 回滚会把历史版本内容写回当前模板，同时新增一个版本快照，方便后续审计和再次回滚。
        entity.setContent(version.getContent());
        entity.setVariables(version.getVariables());
        entity.setVariableSchema(StringUtils.hasText(version.getVariableSchema())
                ? version.getVariableSchema() : version.getVariables());
        entity.setStatus("draft");
        entity.setVersion(entity.getVersion() == null ? 1L : entity.getVersion() + 1);
        promptTemplateMapper.updateById(entity);
        saveVersion(entity, nextVersionNo(entity.getId()), "回滚到版本 " + version.getVersionNo());
        return getTemplate(entity.getId());
    }

    /**
     * 复制 Prompt 模板。
     *
     * @param id 来源模板ID
     * @param request 复制请求
     * @return 新模板详情
     */
    @Transactional(rollbackFor = Exception.class)
    public PromptDtos.TemplateDetail copyTemplate(String id, PromptDtos.CopyRequest request) {
        PromptTemplateEntity source = requireTemplate(id);
        PromptTemplateEntity copy = new PromptTemplateEntity();
        copy.setId(newId());
        copy.setWorkspaceId(source.getWorkspaceId());
        copy.setTemplateCode(uniqueCode(StringUtils.hasText(request == null ? null : request.templateCode)
                ? request.templateCode
                : source.getTemplateCode() + "-copy"));
        copy.setTemplateName(StringUtils.hasText(request == null ? null : request.templateName)
                ? request.templateName
                : source.getTemplateName() + " 副本");
        copy.setPromptType(source.getPromptType());
        copy.setContent(source.getContent());
        copy.setVariables(source.getVariables());
        copy.setVariableSchema(source.getVariableSchema());
        copy.setCurrentEnvironment("development");
        copy.setRiskLevel(source.getRiskLevel());
        copy.setDescription(source.getDescription());
        copy.setStatus("draft");
        copy.setOwnerUserId(currentUserId());
        copy.setVersion(0L);
        promptTemplateMapper.insert(copy);
        return getTemplate(copy.getId());
    }

    /**
     * 查询模板历史版本。
     *
     * @param templateId 模板ID
     * @return 版本列表
     */
    public List<PromptDtos.VersionSummary> listVersions(String templateId) {
        return promptTemplateVersionMapper.selectList(new LambdaQueryWrapper<PromptTemplateVersionEntity>()
                        .eq(PromptTemplateVersionEntity::getTemplateId, templateId)
                        .orderByDesc(PromptTemplateVersionEntity::getCreatedAt))
                .stream()
                .map(this::toVersionSummary)
                .toList();
    }

    /**
     * 填充模板实体。
     *
     * @param entity 模板实体
     * @param request 保存请求
     * @param create 是否为创建
     */
    private void fillTemplate(PromptTemplateEntity entity, PromptDtos.TemplateRequest request, boolean create) {
        if (request == null || !StringUtils.hasText(request.templateName) || !StringUtils.hasText(request.content)) {
            throw new BusinessException("PROMPT_TEMPLATE_INVALID", "模板名称和内容不能为空");
        }
        String promptType = StringUtils.hasText(request.promptType) ? request.promptType.trim().toLowerCase() : "system";
        if (create) {
            entity.setTemplateCode(uniqueCode(StringUtils.hasText(request.templateCode)
                    ? request.templateCode
                    : promptType + "-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())));
        } else if (StringUtils.hasText(request.templateCode)) {
            entity.setTemplateCode(uniqueCodeForUpdate(entity.getId(), request.templateCode));
        }
        entity.setTemplateName(request.templateName.trim());
        entity.setPromptType(promptType);
        entity.setContent(request.content);
        entity.setVariables(normalizeVariables(request.variables, request.content));
        entity.setVariableSchema(normalizeVariables(
                StringUtils.hasText(request.variableSchema) ? request.variableSchema : request.variables,
                request.content));
        if (create) {
            entity.setCurrentEnvironment("development");
        }
        entity.setRiskLevel(StringUtils.hasText(request.riskLevel) ? request.riskLevel : "low");
        entity.setDescription(request.description);
        entity.setStatus(StringUtils.hasText(request.status) ? request.status : "draft");
    }

    /**
     * 保存模板版本快照。
     *
     * @param entity 模板实体
     * @param versionNo 版本号
     * @param changeNote 变更说明
     */
    private PromptTemplateVersionEntity saveVersion(PromptTemplateEntity entity, String versionNo, String changeNote) {
        // 发布前执行统一编译检查，密钥明文或注入语句会阻断进入稳定版本。
        var validation = promptCompiler.compile(entity.getContent(), entity.getVariableSchema(), Map.of(), List.of(), false);
        if (!validation.warnings.isEmpty()) {
            throw new BusinessException("PROMPT_RELEASE_SECURITY_BLOCKED", String.join("；", validation.warnings));
        }
        PromptTemplateVersionEntity version = new PromptTemplateVersionEntity();
        version.setId(newId());
        version.setTemplateId(entity.getId());
        version.setVersionNo(versionNo);
        version.setContent(entity.getContent());
        version.setVariables(entity.getVariables());
        version.setVariableSchema(entity.getVariableSchema());
        version.setContentHash(validation.contentHash);
        version.setValidationStatus("passed");
        version.setValidationResult(toJson(Map.of(
                "warnings", validation.warnings,
                "missingVariables", validation.missingVariables,
                "estimatedTokens", validation.estimatedTokens)));
        version.setEnvironment("development");
        version.setPublishedAt(LocalDateTime.now());
        version.setChangeNote(StringUtils.hasText(changeNote) ? changeNote : "发布 Prompt 模板版本");
        version.setCreatedBy(currentUserId());
        promptTemplateVersionMapper.insert(version);
        return version;
    }

    /**
     * 构造模板查询条件。
     *
     * @param promptType Prompt 类型
     * @param status 模板状态
     * @param keyword 搜索关键字
     * @return 查询条件
     */
    private LambdaQueryWrapper<PromptTemplateEntity> buildTemplateWrapper(String promptType, String status, String keyword) {
        LambdaQueryWrapper<PromptTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(promptType) && !"all".equalsIgnoreCase(promptType)) {
            wrapper.eq(PromptTemplateEntity::getPromptType, promptType);
        }
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(PromptTemplateEntity::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(PromptTemplateEntity::getTemplateName, keyword)
                    .or()
                    .like(PromptTemplateEntity::getTemplateCode, keyword)
                    .or()
                    .like(PromptTemplateEntity::getDescription, keyword));
        }
        return wrapper;
    }

    /**
     * 规范化变量定义。
     *
     * @param variables 前端传入变量JSON
     * @param content Prompt 内容
     * @return JSON 字符串
     */
    private String normalizeVariables(String variables, String content) {
        if (StringUtils.hasText(variables)) {
            try {
                // 变量字段必须是合法 JSON，避免前端误填后污染模板版本。
                objectMapper.readTree(variables);
                return variables;
            } catch (JsonProcessingException ex) {
                throw new BusinessException("PROMPT_VARIABLES_INVALID", "变量定义必须是合法 JSON");
            }
        }
        List<Map<String, String>> parsed = extractVariables(content).stream()
                .map(name -> Map.of("name", name))
                .toList();
        return toJson(parsed);
    }

    /**
     * 从 Prompt 内容中提取变量名。
     *
     * @param content Prompt 内容
     * @return 去重后的变量名列表
     */
    private List<String> extractVariables(String content) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content == null ? "" : content);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return new ArrayList<>(names);
    }

    /**
     * 把模板实体转换为摘要。
     *
     * @param entity 模板实体
     * @return 模板摘要
     */
    private PromptDtos.TemplateSummary toSummary(PromptTemplateEntity entity) {
        PromptDtos.TemplateSummary summary = new PromptDtos.TemplateSummary();
        summary.id = entity.getId();
        summary.templateCode = entity.getTemplateCode();
        summary.templateName = entity.getTemplateName();
        summary.promptType = entity.getPromptType();
        summary.promptTypeLabel = promptTypeLabel(entity.getPromptType());
        summary.content = entity.getContent();
        summary.variables = entity.getVariables();
        summary.variableSchema = entity.getVariableSchema();
        summary.stableVersionId = entity.getStableVersionId();
        summary.currentEnvironment = entity.getCurrentEnvironment();
        summary.riskLevel = entity.getRiskLevel();
        summary.variableNames = readVariableNames(entity.getVariables(), entity.getContent());
        summary.description = entity.getDescription();
        summary.status = entity.getStatus();
        summary.statusLabel = statusLabel(entity.getStatus());
        summary.versionCount = count("SELECT COUNT(1) FROM prompt_template_version WHERE template_id = ?", entity.getId());
        summary.latestVersionNo = latestVersionNo(entity.getId());
        summary.bindingCount = count("SELECT COUNT(1) FROM prompt_binding WHERE template_id=? AND enabled=1", entity.getId());
        summary.ownerUserId = entity.getOwnerUserId();
        summary.createdAt = entity.getCreatedAt();
        summary.updatedAt = entity.getUpdatedAt();
        return summary;
    }

    /**
     * 把版本实体转换为摘要。
     *
     * @param entity 版本实体
     * @return 版本摘要
     */
    private PromptDtos.VersionSummary toVersionSummary(PromptTemplateVersionEntity entity) {
        PromptDtos.VersionSummary summary = new PromptDtos.VersionSummary();
        summary.id = entity.getId();
        summary.templateId = entity.getTemplateId();
        summary.versionNo = entity.getVersionNo();
        summary.content = entity.getContent();
        summary.variables = entity.getVariables();
        summary.variableSchema = entity.getVariableSchema();
        summary.contentHash = entity.getContentHash();
        summary.validationStatus = entity.getValidationStatus();
        summary.validationResult = entity.getValidationResult();
        summary.qualityScore = entity.getQualityScore();
        summary.environment = entity.getEnvironment();
        summary.publishedAt = entity.getPublishedAt();
        summary.variableNames = readVariableNames(entity.getVariables(), entity.getContent());
        summary.changeNote = entity.getChangeNote();
        summary.createdBy = entity.getCreatedBy();
        summary.createdAt = entity.getCreatedAt();
        return summary;
    }

    /**
     * 复制摘要字段到详情对象。
     *
     * @param source 摘要
     * @param target 详情
     */
    private void copySummary(PromptDtos.TemplateSummary source, PromptDtos.TemplateDetail target) {
        target.id = source.id;
        target.templateCode = source.templateCode;
        target.templateName = source.templateName;
        target.promptType = source.promptType;
        target.promptTypeLabel = source.promptTypeLabel;
        target.content = source.content;
        target.variables = source.variables;
        target.variableSchema = source.variableSchema;
        target.stableVersionId = source.stableVersionId;
        target.currentEnvironment = source.currentEnvironment;
        target.riskLevel = source.riskLevel;
        target.variableNames = source.variableNames;
        target.description = source.description;
        target.status = source.status;
        target.statusLabel = source.statusLabel;
        target.versionCount = source.versionCount;
        target.latestVersionNo = source.latestVersionNo;
        target.bindingCount = source.bindingCount;
        target.ownerUserId = source.ownerUserId;
        target.createdAt = source.createdAt;
        target.updatedAt = source.updatedAt;
    }

    /**
     * 从变量 JSON 中读取变量名，读取失败时回退到内容解析。
     *
     * @param variables 变量 JSON
     * @param content Prompt 内容
     * @return 变量名列表
     */
    @SuppressWarnings("unchecked")
    private List<String> readVariableNames(String variables, String content) {
        if (!StringUtils.hasText(variables)) {
            return extractVariables(content);
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(variables, List.class);
            return rows.stream()
                    .map(row -> String.valueOf(row.getOrDefault("name", "")))
                    .filter(StringUtils::hasText)
                    .toList();
        } catch (Exception ex) {
            return extractVariables(content);
        }
    }

    /**
     * 查询模板最新版本号。
     *
     * @param templateId 模板ID
     * @return 最新版本号
     */
    private String latestVersionNo(String templateId) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT version_no
                FROM prompt_template_version
                WHERE template_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, String.class, templateId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /**
     * 生成下一个版本号。
     *
     * @param templateId 模板ID
     * @return 版本号
     */
    private String nextVersionNo(String templateId) {
        long next = count("SELECT COUNT(1) FROM prompt_template_version WHERE template_id = ?", templateId) + 1;
        return "v" + next;
    }

    /**
     * 确保版本号在模板内唯一。
     *
     * @param templateId 模板ID
     * @param versionNo 版本号
     */
    private void ensureVersionNoAvailable(String templateId, String versionNo) {
        Long count = count("SELECT COUNT(1) FROM prompt_template_version WHERE template_id = ? AND version_no = ?", templateId, versionNo);
        if (count > 0) {
            throw new BusinessException("PROMPT_VERSION_EXISTS", "版本号已存在");
        }
    }

    /**
     * 获取模板实体，不存在时抛出业务异常。
     *
     * @param id 模板ID
     * @return 模板实体
     */
    private PromptTemplateEntity requireTemplate(String id) {
        PromptTemplateEntity entity = promptTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("PROMPT_TEMPLATE_NOT_FOUND", "Prompt 模板不存在");
        }
        return entity;
    }

    /**
     * 获取模板版本实体，不存在时抛出业务异常。
     *
     * @param id 版本ID
     * @return 版本实体
     */
    private PromptTemplateVersionEntity requireVersion(String id) {
        PromptTemplateVersionEntity entity = promptTemplateVersionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("PROMPT_VERSION_NOT_FOUND", "Prompt 模板版本不存在");
        }
        return entity;
    }

    /**
     * 生成唯一模板编码。
     *
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueCode(String baseCode) {
        String normalized = baseCode.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "-");
        String candidate = normalized;
        int index = 1;
        while (count("SELECT COUNT(1) FROM prompt_template WHERE template_code = ?", candidate) > 0) {
            candidate = normalized + "-" + index++;
        }
        return candidate;
    }

    /**
     * 更新时生成唯一模板编码。
     *
     * @param id 当前模板ID
     * @param baseCode 基础编码
     * @return 唯一编码
     */
    private String uniqueCodeForUpdate(String id, String baseCode) {
        String normalized = baseCode.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "-");
        Long count = count("SELECT COUNT(1) FROM prompt_template WHERE template_code = ? AND id <> ?", normalized, id);
        if (count > 0) {
            throw new BusinessException("PROMPT_CODE_EXISTS", "模板编码已存在");
        }
        return normalized;
    }

    /**
     * 统计 SQL 查询结果。
     *
     * @param sql SQL
     * @param args 参数
     * @return 统计值
     */
    private Long count(String sql, Object... args) {
        Number number = jdbcTemplate.queryForObject(sql, Number.class, args);
        return number == null ? 0L : number.longValue();
    }

    /**
     * 获取当前用户ID。
     *
     * @return 用户ID
     */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    /**
     * 生成UUID字符串。
     *
     * @return UUID
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 序列化 JSON。
     *
     * @param value 数据对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("JSON_SERIALIZE_FAILED", "JSON 序列化失败");
        }
    }

    /**
     * Prompt 类型中文标签。
     *
     * @param value 类型值
     * @return 中文标签
     */
    private String promptTypeLabel(String value) {
        return switch (value == null ? "" : value) {
            case "user" -> "User Prompt";
            case "rag" -> "RAG Prompt";
            case "tool" -> "Tool Prompt";
            case "evaluation" -> "Evaluation Prompt";
            case "workflow" -> "Workflow Prompt";
            default -> "System Prompt";
        };
    }

    /**
     * 状态中文标签。
     *
     * @param value 状态值
     * @return 中文标签
     */
    private String statusLabel(String value) {
        return switch (value == null ? "" : value) {
            case "published" -> "已发布";
            case "archived" -> "已归档";
            default -> "草稿";
        };
    }
}
