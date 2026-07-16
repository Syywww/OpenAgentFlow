package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;
import com.openagentflow.entity.PromptExperimentEntity;
import com.openagentflow.entity.PromptExperimentVariantEntity;
import com.openagentflow.entity.PromptTemplateEntity;
import com.openagentflow.entity.PromptTemplateVersionEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.PromptExperimentMapper;
import com.openagentflow.mapper.PromptExperimentVariantMapper;
import com.openagentflow.mapper.PromptTemplateMapper;
import com.openagentflow.mapper.PromptTemplateVersionMapper;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Prompt A/B 实验生命周期与自动选优服务。 */
@Service
public class PromptExperimentService {

    /** 实验 Mapper。 */ private final PromptExperimentMapper experimentMapper;
    /** 实验变体 Mapper。 */ private final PromptExperimentVariantMapper variantMapper;
    /** Prompt 模板 Mapper。 */ private final PromptTemplateMapper templateMapper;
    /** Prompt 版本 Mapper。 */ private final PromptTemplateVersionMapper versionMapper;

    public PromptExperimentService(PromptExperimentMapper experimentMapper,
                                   PromptExperimentVariantMapper variantMapper,
                                   PromptTemplateMapper templateMapper,
                                   PromptTemplateVersionMapper versionMapper) {
        this.experimentMapper = experimentMapper;
        this.variantMapper = variantMapper;
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
    }

    /** 查询模板下的全部实验。 */
    public List<PromptRuntimeDtos.ExperimentSummary> list(String templateId) {
        requireTemplate(templateId);
        return experimentMapper.selectList(new LambdaQueryWrapper<PromptExperimentEntity>()
                        .eq(PromptExperimentEntity::getPromptTemplateId, templateId)
                        .orderByDesc(PromptExperimentEntity::getCreatedAt))
                .stream().map(this::toSummary).toList();
    }

    /** 创建 Prompt 实验草稿。 */
    @Transactional
    public PromptRuntimeDtos.ExperimentSummary create(String templateId, PromptRuntimeDtos.ExperimentRequest request) {
        PromptTemplateEntity template = requireTemplate(templateId);
        validateRequest(request);
        PromptExperimentEntity entity = new PromptExperimentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setWorkspaceId(firstText(WorkspaceContextHolder.current(), template.getWorkspaceId()));
        entity.setExperimentCode("PE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        entity.setExperimentName(request.experimentName.trim());
        entity.setPromptTemplateId(templateId);
        entity.setAgentId(request.agentId);
        entity.setDatasetId(request.datasetId);
        entity.setTrafficPolicy("{\"strategy\":\"stable_hash\"}");
        entity.setMetricKey(firstText(request.metricKey, "quality_score"));
        entity.setMinSampleSize(request.minSampleSize == null ? 30 : Math.max(1, request.minSampleSize));
        entity.setConfidenceThreshold(BigDecimal.valueOf(0.95D));
        entity.setAutoWinnerEnabled(Boolean.TRUE.equals(request.autoWinnerEnabled));
        entity.setStatus("draft");
        entity.setOwnerUserId(currentUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        experimentMapper.insert(entity);
        saveVariants(entity.getId(), templateId, request.variants);
        return toSummary(entity);
    }

    /** 更新尚未运行的 Prompt 实验。 */
    @Transactional
    public PromptRuntimeDtos.ExperimentSummary update(String templateId,
                                                       String experimentId,
                                                       PromptRuntimeDtos.ExperimentRequest request) {
        PromptExperimentEntity entity = requireExperiment(templateId, experimentId);
        if ("running".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("PROMPT_EXPERIMENT_RUNNING", "运行中的实验不能修改，请先停止实验");
        }
        validateRequest(request);
        entity.setExperimentName(request.experimentName.trim());
        entity.setAgentId(request.agentId);
        entity.setDatasetId(request.datasetId);
        entity.setMetricKey(firstText(request.metricKey, "quality_score"));
        entity.setMinSampleSize(request.minSampleSize == null ? 30 : Math.max(1, request.minSampleSize));
        entity.setAutoWinnerEnabled(Boolean.TRUE.equals(request.autoWinnerEnabled));
        entity.setWinnerVariantId(null);
        entity.setStatus("draft");
        entity.setUpdatedAt(LocalDateTime.now());
        experimentMapper.updateById(entity);
        variantMapper.delete(new LambdaQueryWrapper<PromptExperimentVariantEntity>()
                .eq(PromptExperimentVariantEntity::getExperimentId, experimentId));
        saveVariants(experimentId, templateId, request.variants);
        return toSummary(entity);
    }

    /** 启动实验，同一模板和 Agent 同时只允许一个活动实验。 */
    @Transactional
    public PromptRuntimeDtos.ExperimentSummary start(String templateId, String experimentId) {
        PromptExperimentEntity entity = requireExperiment(templateId, experimentId);
        List<PromptExperimentEntity> running = experimentMapper.selectList(new LambdaQueryWrapper<PromptExperimentEntity>()
                .eq(PromptExperimentEntity::getPromptTemplateId, templateId)
                .eq(PromptExperimentEntity::getStatus, "running"));
        for (PromptExperimentEntity item : running) {
            if (!item.getId().equals(experimentId) && sameAgent(item.getAgentId(), entity.getAgentId())) {
                item.setStatus("completed");
                item.setEndedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                experimentMapper.updateById(item);
            }
        }
        entity.setStatus("running");
        entity.setStartedAt(LocalDateTime.now());
        entity.setEndedAt(null);
        entity.setUpdatedAt(LocalDateTime.now());
        experimentMapper.updateById(entity);
        return toSummary(entity);
    }

    /** 停止活动实验。 */
    @Transactional
    public PromptRuntimeDtos.ExperimentSummary stop(String templateId, String experimentId) {
        PromptExperimentEntity entity = requireExperiment(templateId, experimentId);
        entity.setStatus("completed");
        entity.setEndedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        experimentMapper.updateById(entity);
        return toSummary(entity);
    }

    /** 手动指定实验胜出变体。 */
    @Transactional
    public PromptRuntimeDtos.ExperimentSummary chooseWinner(String templateId, String experimentId, String variantId) {
        PromptExperimentEntity entity = requireExperiment(templateId, experimentId);
        PromptExperimentVariantEntity variant = variantMapper.selectById(variantId);
        if (variant == null || !experimentId.equals(variant.getExperimentId())) {
            throw new BusinessException("PROMPT_VARIANT_NOT_FOUND", "实验变体不存在");
        }
        entity.setWinnerVariantId(variantId);
        entity.setStatus("completed");
        entity.setEndedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        experimentMapper.updateById(entity);
        return toSummary(entity);
    }

    /** 按当前聚合指标尝试自动选择胜出变体。 */
    @Transactional
    public PromptRuntimeDtos.ExperimentSummary autoChooseWinner(String templateId, String experimentId) {
        PromptExperimentEntity entity = requireExperiment(templateId, experimentId);
        List<PromptExperimentVariantEntity> variants = variants(experimentId);
        String winnerId = PromptExperimentPolicy.selectWinner(variants.stream()
                .map(item -> new PromptExperimentPolicy.Candidate(item.getId(), value(item.getSampleCount()),
                        decimal(item.getAvgQualityScore()), successRate(item)))
                .toList(), entity.getMinSampleSize() == null ? 30 : entity.getMinSampleSize());
        if (!StringUtils.hasText(winnerId)) {
            throw new BusinessException("PROMPT_EXPERIMENT_SAMPLE_INSUFFICIENT", "所有实验变体达到最小样本量后才能自动选优");
        }
        return chooseWinner(templateId, experimentId, winnerId);
    }

    /** 删除已停止的实验。 */
    @Transactional
    public void delete(String templateId, String experimentId) {
        PromptExperimentEntity entity = requireExperiment(templateId, experimentId);
        if ("running".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException("PROMPT_EXPERIMENT_RUNNING", "运行中的实验不能删除");
        }
        variantMapper.delete(new LambdaQueryWrapper<PromptExperimentVariantEntity>()
                .eq(PromptExperimentVariantEntity::getExperimentId, experimentId));
        experimentMapper.deleteById(entity.getId());
    }

    /** 校验并保存实验变体，同时固化版本内容快照。 */
    private void saveVariants(String experimentId, String templateId, List<PromptRuntimeDtos.VariantRequest> requests) {
        for (PromptRuntimeDtos.VariantRequest request : requests) {
            PromptTemplateVersionEntity version = null;
            if (StringUtils.hasText(request.promptVersionId)) {
                version = versionMapper.selectById(request.promptVersionId);
                if (version == null || !templateId.equals(version.getTemplateId())) {
                    throw new BusinessException("PROMPT_VERSION_NOT_FOUND", "实验变体关联的 Prompt 版本不存在");
                }
            }
            String content = firstText(request.promptContent, version == null ? null : version.getContent());
            if (!StringUtils.hasText(content)) {
                throw new BusinessException("PROMPT_VARIANT_CONTENT_REQUIRED", "实验变体必须选择版本或填写 Prompt 内容");
            }
            PromptExperimentVariantEntity entity = new PromptExperimentVariantEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setExperimentId(experimentId);
            entity.setVariantCode(request.variantCode.trim());
            entity.setPromptVersionId(request.promptVersionId);
            entity.setPromptContent(content);
            entity.setModelParams(firstText(request.modelParams, "{}"));
            entity.setTrafficWeight(request.trafficWeight);
            entity.setMetricsSnapshot("{}");
            entity.setSampleCount(0L);
            entity.setSuccessCount(0L);
            entity.setFailureCount(0L);
            entity.setAvgQualityScore(BigDecimal.ZERO);
            entity.setAvgLatencyMs(BigDecimal.ZERO);
            entity.setTotalTokens(0L);
            entity.setTotalCost(BigDecimal.ZERO);
            entity.setCreatedAt(LocalDateTime.now());
            variantMapper.insert(entity);
        }
    }

    /** 校验实验输入与总权重。 */
    private void validateRequest(PromptRuntimeDtos.ExperimentRequest request) {
        if (request == null || !StringUtils.hasText(request.experimentName)) {
            throw new BusinessException("PROMPT_EXPERIMENT_NAME_REQUIRED", "实验名称不能为空");
        }
        if (request.variants == null || request.variants.size() < 2) {
            throw new BusinessException("PROMPT_EXPERIMENT_VARIANTS_REQUIRED", "Prompt 实验至少需要两个变体");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PromptRuntimeDtos.VariantRequest variant : request.variants) {
            if (variant == null || !StringUtils.hasText(variant.variantCode)
                    || variant.trafficWeight == null || variant.trafficWeight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("PROMPT_VARIANT_INVALID", "变体编码和正数流量权重不能为空");
            }
            total = total.add(variant.trafficWeight);
        }
        if (total.subtract(BigDecimal.valueOf(100D)).abs().compareTo(BigDecimal.valueOf(0.0001D)) > 0) {
            throw new BusinessException("PROMPT_VARIANT_WEIGHT_INVALID", "实验变体流量权重之和必须等于 100");
        }
    }

    /** 转换实验摘要。 */
    private PromptRuntimeDtos.ExperimentSummary toSummary(PromptExperimentEntity entity) {
        PromptRuntimeDtos.ExperimentSummary summary = new PromptRuntimeDtos.ExperimentSummary();
        summary.id = entity.getId();
        summary.experimentCode = entity.getExperimentCode();
        summary.experimentName = entity.getExperimentName();
        summary.promptTemplateId = entity.getPromptTemplateId();
        summary.agentId = entity.getAgentId();
        summary.status = entity.getStatus();
        summary.metricKey = entity.getMetricKey();
        summary.winnerVariantId = entity.getWinnerVariantId();
        summary.createdAt = entity.getCreatedAt();
        summary.variants = variants(entity.getId()).stream().map(this::toVariantSummary).toList();
        return summary;
    }

    /** 转换实验变体摘要。 */
    private PromptRuntimeDtos.VariantSummary toVariantSummary(PromptExperimentVariantEntity entity) {
        PromptRuntimeDtos.VariantSummary summary = new PromptRuntimeDtos.VariantSummary();
        summary.id = entity.getId();
        summary.variantCode = entity.getVariantCode();
        summary.promptVersionId = entity.getPromptVersionId();
        summary.trafficWeight = entity.getTrafficWeight();
        summary.sampleCount = value(entity.getSampleCount());
        summary.successRate = successRate(entity);
        summary.avgQualityScore = decimal(entity.getAvgQualityScore());
        summary.avgLatencyMs = decimal(entity.getAvgLatencyMs());
        summary.totalTokens = value(entity.getTotalTokens());
        summary.totalCost = entity.getTotalCost() == null ? BigDecimal.ZERO : entity.getTotalCost();
        return summary;
    }

    /** 查询实验变体。 */
    private List<PromptExperimentVariantEntity> variants(String experimentId) {
        return variantMapper.selectList(new LambdaQueryWrapper<PromptExperimentVariantEntity>()
                .eq(PromptExperimentVariantEntity::getExperimentId, experimentId)
                .orderByAsc(PromptExperimentVariantEntity::getVariantCode));
    }

    /** 查询并校验模板。 */
    private PromptTemplateEntity requireTemplate(String templateId) {
        PromptTemplateEntity entity = templateMapper.selectById(templateId);
        if (entity == null) {
            throw new BusinessException("PROMPT_TEMPLATE_NOT_FOUND", "Prompt 模板不存在");
        }
        return entity;
    }

    /** 查询并校验实验归属。 */
    private PromptExperimentEntity requireExperiment(String templateId, String experimentId) {
        PromptExperimentEntity entity = experimentMapper.selectById(experimentId);
        if (entity == null || !templateId.equals(entity.getPromptTemplateId())) {
            throw new BusinessException("PROMPT_EXPERIMENT_NOT_FOUND", "Prompt 实验不存在");
        }
        return entity;
    }

    /** 计算变体成功率。 */
    private double successRate(PromptExperimentVariantEntity entity) {
        long samples = value(entity.getSampleCount());
        return samples == 0 ? 0D : BigDecimal.valueOf(value(entity.getSuccessCount()) * 100D / samples)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** 获取当前登录用户编号。 */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthUserDetails details
                ? details.getUserId() : null;
    }

    /** 比较可空 Agent 编号。 */
    private boolean sameAgent(String left, String right) {
        return String.valueOf(left).equals(String.valueOf(right));
    }

    /** 获取第一个非空文本。 */
    private String firstText(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
    }

    /** 将可空长整数转换为零值安全结果。 */
    private long value(Long value) {
        return value == null ? 0L : value;
    }

    /** 将可空小数转换为双精度结果。 */
    private double decimal(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }
}
