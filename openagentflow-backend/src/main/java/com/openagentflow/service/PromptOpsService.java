package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;
import com.openagentflow.entity.PromptBindingEntity;
import com.openagentflow.entity.PromptEnvironmentReleaseEntity;
import com.openagentflow.entity.PromptRuntimeMetricEntity;
import com.openagentflow.entity.PromptTemplateEntity;
import com.openagentflow.entity.PromptTemplateVersionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.PromptBindingMapper;
import com.openagentflow.mapper.PromptEnvironmentReleaseMapper;
import com.openagentflow.mapper.PromptRuntimeMetricMapper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Prompt 预览、差异、影响面、环境晋级和运行指标治理服务。 */
@Service
public class PromptOpsService {

    /** Prompt 模板 Mapper。 */ private final PromptTemplateMapper templateMapper;
    /** Prompt 版本 Mapper。 */ private final PromptTemplateVersionMapper versionMapper;
    /** Prompt 绑定 Mapper。 */ private final PromptBindingMapper bindingMapper;
    /** Prompt 环境发布 Mapper。 */ private final PromptEnvironmentReleaseMapper releaseMapper;
    /** Prompt 运行指标 Mapper。 */ private final PromptRuntimeMetricMapper metricMapper;
    /** 统一 Prompt Runtime。 */ private final PromptRuntimeService promptRuntimeService;
    /** 生产发布门禁服务。 */ private final ReleaseGateService releaseGateService;
    /** JDBC 查询工具。 */ private final JdbcTemplate jdbcTemplate;

    public PromptOpsService(PromptTemplateMapper templateMapper,
                            PromptTemplateVersionMapper versionMapper,
                            PromptBindingMapper bindingMapper,
                            PromptEnvironmentReleaseMapper releaseMapper,
                            PromptRuntimeMetricMapper metricMapper,
                            PromptRuntimeService promptRuntimeService,
                            ReleaseGateService releaseGateService,
                            JdbcTemplate jdbcTemplate) {
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.bindingMapper = bindingMapper;
        this.releaseMapper = releaseMapper;
        this.metricMapper = metricMapper;
        this.promptRuntimeService = promptRuntimeService;
        this.releaseGateService = releaseGateService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 使用真实编译链预览最终 Prompt、变量来源、分层和风险警告。 */
    public PromptRuntimeDtos.CompileResult preview(String templateId, PromptRuntimeDtos.PreviewRequest request) {
        PromptTemplateEntity template = requireTemplate(templateId);
        PromptRuntimeDtos.PreviewRequest actual = request == null ? new PromptRuntimeDtos.PreviewRequest() : request;
        actual.templateId = templateId;
        actual.resourceType = "preview";
        actual.resourceId = templateId;
        actual.variableSchema = firstText(actual.variableSchema, template.getVariableSchema());
        actual.strict = actual.strict == null || actual.strict;
        if (!StringUtils.hasText(actual.versionId)) {
            actual.bindingMode = "MANUAL";
            actual.content = firstText(actual.content, template.getContent());
        } else {
            actual.bindingMode = "LOCKED";
        }
        return promptRuntimeService.compile(actual);
    }

    /** 对比两个 Prompt 版本的内容行和变量 Schema。 */
    public PromptRuntimeDtos.VersionDiff diff(String templateId, String fromVersionId, String toVersionId) {
        requireTemplate(templateId);
        PromptTemplateVersionEntity from = requireVersion(templateId, fromVersionId);
        PromptTemplateVersionEntity to = requireVersion(templateId, toVersionId);
        Set<String> fromLines = new LinkedHashSet<>(List.of(normalizeContent(from.getContent()).split("\\R", -1)));
        Set<String> toLines = new LinkedHashSet<>(List.of(normalizeContent(to.getContent()).split("\\R", -1)));
        PromptRuntimeDtos.VersionDiff result = new PromptRuntimeDtos.VersionDiff();
        result.fromVersionId = fromVersionId;
        result.toVersionId = toVersionId;
        result.addedLines = toLines.stream().filter(line -> !fromLines.contains(line)).toList();
        result.removedLines = fromLines.stream().filter(line -> !toLines.contains(line)).toList();
        result.variableSchemaChanged = !normalizeContent(firstText(from.getVariableSchema(), from.getVariables()))
                .equals(normalizeContent(firstText(to.getVariableSchema(), to.getVariables())));
        return result;
    }

    /** 查询模板影响的 Agent、工作流、RAG、工具和评测资源。 */
    public List<PromptRuntimeDtos.ImpactItem> impacts(String templateId) {
        requireTemplate(templateId);
        return bindingMapper.selectList(new LambdaQueryWrapper<PromptBindingEntity>()
                        .eq(PromptBindingEntity::getTemplateId, templateId)
                        .eq(PromptBindingEntity::getEnabled, true)
                        .orderByAsc(PromptBindingEntity::getResourceType))
                .stream().map(this::toImpact).toList();
    }

    /** 将指定版本晋级到开发、测试或生产环境，并支持生产灰度比例。 */
    @Transactional
    public PromptRuntimeDtos.EnvironmentRelease promote(String templateId, PromptRuntimeDtos.PromotionRequest request) {
        PromptTemplateEntity template = requireTemplate(templateId);
        if (request == null || !StringUtils.hasText(request.versionId)) {
            throw new BusinessException("PROMPT_RELEASE_VERSION_REQUIRED", "Prompt 晋级版本不能为空");
        }
        PromptTemplateVersionEntity version = requireVersion(templateId, request.versionId);
        String environment = normalizeEnvironment(request.environment);
        int grayPercent = request.grayPercent == null ? 100 : Math.max(0, Math.min(100, request.grayPercent));
        String workspaceId = firstText(WorkspaceContextHolder.current(), template.getWorkspaceId());
        if (!StringUtils.hasText(workspaceId)) {
            throw new BusinessException("WORKSPACE_REQUIRED", "Prompt 环境晋级必须归属工作空间");
        }
        if ("production".equals(environment)) {
            releaseGateService.assertCanRelease("prompt", templateId, workspaceId, version.getVersionNo());
        }

        // 同一环境仅保留当前活动候选，历史发布改为已替代，便于回滚审计。
        jdbcTemplate.update("""
                UPDATE prompt_environment_release SET status='superseded', updated_at=NOW(3)
                WHERE template_id=? AND environment=? AND status='active' AND version_id<>?
                """, templateId, environment, version.getId());
        PromptEnvironmentReleaseEntity release = releaseMapper.selectOne(new LambdaQueryWrapper<PromptEnvironmentReleaseEntity>()
                .eq(PromptEnvironmentReleaseEntity::getTemplateId, templateId)
                .eq(PromptEnvironmentReleaseEntity::getVersionId, version.getId())
                .eq(PromptEnvironmentReleaseEntity::getEnvironment, environment)
                .last("limit 1"));
        if (release == null) {
            release = new PromptEnvironmentReleaseEntity();
            release.setId(UUID.randomUUID().toString());
            release.setWorkspaceId(workspaceId);
            release.setTemplateId(templateId);
            release.setVersionId(version.getId());
        }
        release.setEnvironment(environment);
        release.setStatus("active");
        release.setGrayPercent(grayPercent);
        release.setReleaseNote(request.releaseNote);
        release.setPromotedBy(currentUserId());
        release.setPromotedAt(LocalDateTime.now());
        release.setUpdatedAt(LocalDateTime.now());
        if (releaseMapper.selectById(release.getId()) == null) {
            releaseMapper.insert(release);
        } else {
            releaseMapper.updateById(release);
        }

        version.setEnvironment(environment);
        version.setPublishedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        template.setCurrentEnvironment(environment);
        if ("production".equals(environment) && grayPercent == 100) {
            template.setStableVersionId(version.getId());
            template.setStatus("published");
        }
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
        return toRelease(release);
    }

    /** 查询模板的多环境发布历史。 */
    public List<PromptRuntimeDtos.EnvironmentRelease> releases(String templateId) {
        requireTemplate(templateId);
        return releaseMapper.selectList(new LambdaQueryWrapper<PromptEnvironmentReleaseEntity>()
                        .eq(PromptEnvironmentReleaseEntity::getTemplateId, templateId)
                        .orderByDesc(PromptEnvironmentReleaseEntity::getPromotedAt))
                .stream().map(this::toRelease).toList();
    }

    /** 按版本聚合调用量、成功率、质量、耗时、Token 和成本。 */
    public List<PromptRuntimeDtos.VersionMetric> metrics(String templateId) {
        requireTemplate(templateId);
        List<PromptRuntimeMetricEntity> metrics = metricMapper.selectList(new LambdaQueryWrapper<PromptRuntimeMetricEntity>()
                .eq(PromptRuntimeMetricEntity::getTemplateId, templateId));
        Map<String, List<PromptRuntimeMetricEntity>> grouped = new LinkedHashMap<>();
        for (PromptRuntimeMetricEntity metric : metrics) {
            grouped.computeIfAbsent(String.valueOf(metric.getVersionId()), ignored -> new ArrayList<>()).add(metric);
        }
        Map<String, PromptTemplateVersionEntity> versions = new LinkedHashMap<>();
        versionMapper.selectList(new LambdaQueryWrapper<PromptTemplateVersionEntity>()
                        .eq(PromptTemplateVersionEntity::getTemplateId, templateId))
                .forEach(version -> versions.put(version.getId(), version));
        return grouped.entrySet().stream().map(entry -> toMetric(entry.getKey(), versions.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(item -> String.valueOf(item.versionNo), Comparator.reverseOrder()))
                .toList();
    }

    /** 转换版本聚合指标。 */
    private PromptRuntimeDtos.VersionMetric toMetric(String versionId,
                                                      PromptTemplateVersionEntity version,
                                                      List<PromptRuntimeMetricEntity> items) {
        PromptRuntimeDtos.VersionMetric result = new PromptRuntimeDtos.VersionMetric();
        result.versionId = "null".equals(versionId) ? null : versionId;
        result.versionNo = version == null ? "手工 Prompt" : version.getVersionNo();
        result.callCount = (long) items.size();
        long successCount = items.stream().filter(item -> Boolean.TRUE.equals(item.getSuccess())).count();
        result.successRate = percentage(successCount, items.size());
        result.avgQualityScore = items.stream().filter(item -> item.getQualityScore() != null)
                .mapToDouble(item -> item.getQualityScore().doubleValue()).average().orElse(0D);
        result.avgLatencyMs = items.stream().mapToInt(item -> item.getLatencyMs() == null ? 0 : item.getLatencyMs())
                .average().orElse(0D);
        result.totalTokens = items.stream().mapToLong(item -> item.getTokenCount() == null ? 0 : item.getTokenCount()).sum();
        result.totalCost = items.stream().map(item -> item.getCostAmount() == null ? BigDecimal.ZERO : item.getCostAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return result;
    }

    /** 转换资源影响项并补充资源名称。 */
    private PromptRuntimeDtos.ImpactItem toImpact(PromptBindingEntity binding) {
        PromptRuntimeDtos.ImpactItem item = new PromptRuntimeDtos.ImpactItem();
        item.resourceType = binding.getResourceType();
        item.resourceId = binding.getResourceId();
        item.resourceName = resourceName(binding.getResourceType(), binding.getResourceId());
        item.bindingMode = binding.getBindingMode();
        item.versionId = binding.getVersionId();
        return item;
    }

    /** 根据资源类型读取展示名称，未知类型回退为资源编号。 */
    private String resourceName(String type, String id) {
        Map<String, String> tableColumns = Map.of(
                "agent", "agent:agent_name",
                "workflow", "workflow_definition:workflow_name",
                "rag", "knowledge_base:kb_name",
                "knowledge_base", "knowledge_base:kb_name",
                "tool", "tool_definition:tool_name",
                "evaluation", "eval_task:task_name",
                "eval", "eval_task:task_name"
        );
        String config = tableColumns.get(String.valueOf(type).toLowerCase(Locale.ROOT));
        if (config == null) {
            return id;
        }
        String[] pair = config.split(":");
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT " + pair[1] + " FROM " + pair[0] + " WHERE id=? LIMIT 1", String.class, id);
            return names.isEmpty() ? id : names.getFirst();
        } catch (Exception ignored) {
            return id;
        }
    }

    /** 转换环境发布摘要。 */
    private PromptRuntimeDtos.EnvironmentRelease toRelease(PromptEnvironmentReleaseEntity entity) {
        PromptRuntimeDtos.EnvironmentRelease result = new PromptRuntimeDtos.EnvironmentRelease();
        result.id = entity.getId();
        result.templateId = entity.getTemplateId();
        result.versionId = entity.getVersionId();
        result.environment = entity.getEnvironment();
        result.status = entity.getStatus();
        result.grayPercent = entity.getGrayPercent();
        result.promotedBy = entity.getPromotedBy();
        result.promotedAt = entity.getPromotedAt();
        return result;
    }

    /** 查询并校验 Prompt 模板。 */
    private PromptTemplateEntity requireTemplate(String templateId) {
        PromptTemplateEntity entity = templateMapper.selectById(templateId);
        if (entity == null) {
            throw new BusinessException("PROMPT_TEMPLATE_NOT_FOUND", "Prompt 模板不存在");
        }
        return entity;
    }

    /** 查询并校验 Prompt 版本归属。 */
    private PromptTemplateVersionEntity requireVersion(String templateId, String versionId) {
        if (!StringUtils.hasText(versionId)) {
            throw new BusinessException("PROMPT_VERSION_REQUIRED", "Prompt 版本不能为空");
        }
        PromptTemplateVersionEntity entity = versionMapper.selectById(versionId);
        if (entity == null || !templateId.equals(entity.getTemplateId())) {
            throw new BusinessException("PROMPT_VERSION_NOT_FOUND", "Prompt 版本不存在");
        }
        return entity;
    }

    /** 规范化目标环境。 */
    private String normalizeEnvironment(String environment) {
        String value = firstText(environment, "development").toLowerCase(Locale.ROOT);
        if (!List.of("development", "testing", "production").contains(value)) {
            throw new BusinessException("PROMPT_ENVIRONMENT_INVALID", "Prompt 环境仅支持 development、testing、production");
        }
        return value;
    }

    /** 获取当前用户编号。 */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUserDetails details
                ? details.getUserId() : null;
    }

    /** 计算百分比并保留两位小数。 */
    private double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0D : BigDecimal.valueOf(numerator * 100D / denominator)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** 获取第一个非空文本。 */
    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    /** 统一空内容，避免版本差异空指针。 */
    private String normalizeContent(String content) {
        return content == null ? "" : content.replace("\r\n", "\n");
    }
}
