package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.prompt.PromptRuntimeDtos;
import com.openagentflow.entity.AgentEntity;
import com.openagentflow.entity.PromptBindingEntity;
import com.openagentflow.entity.PromptEnvironmentReleaseEntity;
import com.openagentflow.entity.PromptExperimentEntity;
import com.openagentflow.entity.PromptExperimentVariantEntity;
import com.openagentflow.entity.PromptRuntimeMetricEntity;
import com.openagentflow.entity.PromptTemplateEntity;
import com.openagentflow.entity.PromptTemplateVersionEntity;
import com.openagentflow.entity.RuntimeLlmCallEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.PromptBindingMapper;
import com.openagentflow.mapper.PromptEnvironmentReleaseMapper;
import com.openagentflow.mapper.PromptExperimentMapper;
import com.openagentflow.mapper.PromptExperimentVariantMapper;
import com.openagentflow.mapper.PromptRuntimeMetricMapper;
import com.openagentflow.mapper.PromptTemplateMapper;
import com.openagentflow.mapper.PromptTemplateVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 统一 Prompt Runtime 服务。
 *
 * <p>所有 Agent、工作流、RAG、工具和评测入口通过该服务解析实际版本、实验变体、变量和最终 Prompt。</p>
 */
@Service
public class PromptRuntimeService {

    /** Prompt模板Mapper。 */ private final PromptTemplateMapper templateMapper;
    /** Prompt版本Mapper。 */ private final PromptTemplateVersionMapper versionMapper;
    /** Prompt绑定Mapper。 */ private final PromptBindingMapper bindingMapper;
    /** Prompt环境发布Mapper。 */ private final PromptEnvironmentReleaseMapper releaseMapper;
    /** Prompt实验Mapper。 */ private final PromptExperimentMapper experimentMapper;
    /** Prompt实验变体Mapper。 */ private final PromptExperimentVariantMapper variantMapper;
    /** Prompt运行指标Mapper。 */ private final PromptRuntimeMetricMapper metricMapper;
    /** Prompt编译器。 */ private final PromptCompiler promptCompiler;
    /** JSON序列化工具。 */ private final ObjectMapper objectMapper;

    public PromptRuntimeService(PromptTemplateMapper templateMapper,
                                PromptTemplateVersionMapper versionMapper,
                                PromptBindingMapper bindingMapper,
                                PromptEnvironmentReleaseMapper releaseMapper,
                                PromptExperimentMapper experimentMapper,
                                PromptExperimentVariantMapper variantMapper,
                                PromptRuntimeMetricMapper metricMapper,
                                PromptCompiler promptCompiler,
                                ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.bindingMapper = bindingMapper;
        this.releaseMapper = releaseMapper;
        this.experimentMapper = experimentMapper;
        this.variantMapper = variantMapper;
        this.metricMapper = metricMapper;
        this.promptCompiler = promptCompiler;
        this.objectMapper = objectMapper;
    }

    /**
     * 编译Agent的System Prompt。
     *
     * @param agent Agent实体
     * @param userInput 用户输入
     * @param routingKey 会话或用户分桶键
     * @return 编译结果
     */
    public PromptRuntimeDtos.CompileResult compileForAgent(AgentEntity agent, String userInput, String routingKey) {
        PromptRuntimeDtos.CompileRequest request = new PromptRuntimeDtos.CompileRequest();
        request.agentId = agent == null ? null : agent.getId();
        request.resourceType = "agent";
        request.resourceId = agent == null ? null : agent.getId();
        request.templateId = agent == null ? null : agent.getSystemPromptTemplateId();
        request.versionId = agent == null ? null : agent.getSystemPromptVersionId();
        request.bindingMode = agent == null ? "MANUAL" : agent.getPromptBindingMode();
        request.content = agent != null && StringUtils.hasText(agent.getSystemPrompt())
                ? agent.getSystemPrompt()
                : "你是 OpenAgentFlow-Java 的 AI 助手，请用清晰、准确的中文回答用户问题。";
        request.variables = parseMap(agent == null ? null : agent.getPromptVariables());
        request.variables.put("user_input", userInput == null ? "" : userInput);
        request.variables.put("input", userInput == null ? "" : userInput);
        request.variables.put("question", userInput == null ? "" : userInput);
        request.routingKey = StringUtils.hasText(routingKey) ? routingKey : userInput;
        request.strict = true;
        return compile(request);
    }

    /**
     * 编译任意资源的Prompt。
     *
     * @param request 编译请求
     * @return 编译结果
     */
    public PromptRuntimeDtos.CompileResult compile(PromptRuntimeDtos.CompileRequest request) {
        if (request == null) {
            throw new BusinessException("PROMPT_COMPILE_REQUEST_REQUIRED", "Prompt编译请求不能为空");
        }
        Resolution resolution = resolve(request);
        Map<String, Object> variables = new LinkedHashMap<>(resolution.bindingVariables());
        if (request.variables != null) {
            variables.putAll(request.variables);
        }
        PromptRuntimeDtos.CompileResult result = promptCompiler.compile(
                resolution.content(), resolution.variableSchema(), variables, request.layers,
                Boolean.TRUE.equals(request.strict)
        );
        result.templateId = resolution.templateId();
        result.versionId = resolution.versionId();
        result.versionNo = resolution.versionNo();
        result.bindingMode = resolution.bindingMode();
        result.experimentId = resolution.experimentId();
        result.variantId = resolution.variantId();
        result.variantCode = resolution.variantCode();
        return result;
    }

    /**
     * 根据最终模型消息刷新Trace中的Prompt快照，确保RAG、Memory和工具系统消息也可解释。
     *
     * @param call LLM调用实体
     * @param compiled 初始编译结果
     * @param messages 最终模型消息
     */
    public void enrichLlmCall(RuntimeLlmCallEntity call,
                              PromptRuntimeDtos.CompileResult compiled,
                              List<ChatMessage> messages) {
        if (call == null || compiled == null) {
            return;
        }
        List<PromptRuntimeDtos.PromptLayer> finalLayers = new ArrayList<>();
        int order = 10;
        if (messages != null) {
            for (ChatMessage message : messages) {
                if (message != null && "system".equalsIgnoreCase(message.getRole()) && StringUtils.hasText(message.getContent())) {
                    finalLayers.add(new PromptRuntimeDtos.PromptLayer(
                            finalLayers.isEmpty() ? "system" : "context_" + finalLayers.size(),
                            finalLayers.isEmpty() ? "System Prompt" : inferLayerName(message.getContent()),
                            message.getContent(), order
                    ));
                    order += 10;
                }
            }
        }
        PromptRuntimeDtos.CompileResult finalResult = promptCompiler.compile("", "[]", Map.of(), finalLayers, false);
        call.setPromptTemplateId(compiled.templateId);
        call.setPromptVersionId(compiled.versionId);
        call.setPromptContentHash(finalResult.contentHash);
        call.setPromptLayers(toJson(finalLayers));
        call.setPromptVariableSources(toJson(compiled.variableSources));
    }

    /**
     * 保存Prompt运行指标，并增量更新实验变体统计。
     */
    public void recordMetric(String workspaceId,
                             String runId,
                             String agentId,
                             PromptRuntimeDtos.CompileResult compiled,
                             boolean success,
                             int latencyMs,
                             int tokenCount,
                             BigDecimal costAmount) {
        if (compiled == null || !StringUtils.hasText(compiled.templateId)) {
            return;
        }
        try {
            PromptRuntimeMetricEntity metric = new PromptRuntimeMetricEntity();
            metric.setId(UUID.randomUUID().toString());
            metric.setWorkspaceId(workspaceId);
            metric.setTemplateId(compiled.templateId);
            metric.setVersionId(compiled.versionId);
            metric.setExperimentId(compiled.experimentId);
            metric.setVariantId(compiled.variantId);
            metric.setRunId(runId);
            metric.setAgentId(agentId);
            metric.setSuccess(success);
            metric.setLatencyMs(Math.max(0, latencyMs));
            metric.setTokenCount(Math.max(0, tokenCount));
            metric.setCostAmount(costAmount == null ? BigDecimal.ZERO : costAmount);
            metric.setCreatedAt(LocalDateTime.now());
            metricMapper.insert(metric);
            if (StringUtils.hasText(compiled.variantId)) {
                updateVariantMetric(compiled.variantId, success, latencyMs, tokenCount, costAmount);
            }
        } catch (Exception ignored) {
            // 指标写入失败不能阻断模型主链路，运营页可通过指标缺口发现异常。
        }
    }

    /** 解析模板、版本、环境灰度和实验变体。 */
    private Resolution resolve(PromptRuntimeDtos.CompileRequest request) {
        PromptBindingEntity binding = findBinding(request.resourceType, request.resourceId);
        String mode = StringUtils.hasText(request.bindingMode)
                ? normalizeMode(request.bindingMode)
                : normalizeMode(binding == null ? null : binding.getBindingMode());
        String templateId = firstText(request.templateId, binding == null ? null : binding.getTemplateId());
        if (!StringUtils.hasText(templateId) || "MANUAL".equals(mode)) {
            return new Resolution(templateId, null, null, "MANUAL", request.content, request.variableSchema,
                    bindingVariables(binding), null, null, null);
        }
        PromptTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("PROMPT_TEMPLATE_NOT_FOUND", "Prompt模板不存在");
        }
        String requestedVersionId = firstText(request.versionId, binding == null ? null : binding.getVersionId());
        PromptTemplateVersionEntity version = resolveVersion(template, requestedVersionId, mode, request.routingKey);
        VariantResolution variant = resolveExperimentVariant(templateId, request.agentId, request.routingKey);
        if (variant != null) {
            if (StringUtils.hasText(variant.variant().getPromptVersionId())) {
                PromptTemplateVersionEntity variantVersion = versionMapper.selectById(variant.variant().getPromptVersionId());
                if (variantVersion != null && templateId.equals(variantVersion.getTemplateId())) {
                    version = variantVersion;
                }
            }
        }
        String content = variant != null && StringUtils.hasText(variant.variant().getPromptContent())
                ? variant.variant().getPromptContent()
                : version == null ? template.getContent() : version.getContent();
        String schema = version != null && StringUtils.hasText(version.getVariableSchema())
                ? version.getVariableSchema()
                : StringUtils.hasText(template.getVariableSchema()) ? template.getVariableSchema()
                : StringUtils.hasText(template.getVariables()) ? template.getVariables()
                : version == null ? "[]" : version.getVariables();
        return new Resolution(templateId,
                version == null ? null : version.getId(),
                version == null ? null : version.getVersionNo(),
                mode, content, schema, bindingVariables(binding),
                variant == null ? null : variant.experiment().getId(),
                variant == null ? null : variant.variant().getId(),
                variant == null ? null : variant.variant().getVariantCode());
    }

    /** 根据绑定模式和环境灰度解析实际版本。 */
    private PromptTemplateVersionEntity resolveVersion(PromptTemplateEntity template,
                                                        String requestedVersionId,
                                                        String mode,
                                                        String routingKey) {
        if ("LOCKED".equals(mode)) {
            if (!StringUtils.hasText(requestedVersionId)) {
                throw new BusinessException("PROMPT_VERSION_REQUIRED", "锁定版本模式必须选择Prompt版本");
            }
            return requireVersion(template.getId(), requestedVersionId);
        }
        PromptTemplateVersionEntity stable = StringUtils.hasText(template.getStableVersionId())
                ? versionMapper.selectById(template.getStableVersionId()) : latestVersion(template.getId());
        PromptEnvironmentReleaseEntity release = releaseMapper.selectOne(new LambdaQueryWrapper<PromptEnvironmentReleaseEntity>()
                .eq(PromptEnvironmentReleaseEntity::getTemplateId, template.getId())
                .eq(PromptEnvironmentReleaseEntity::getEnvironment, "production")
                .eq(PromptEnvironmentReleaseEntity::getStatus, "active")
                .orderByDesc(PromptEnvironmentReleaseEntity::getPromotedAt)
                .last("limit 1"));
        if (release != null && release.getGrayPercent() != null
                && inBucket(routingKey, release.getGrayPercent())) {
            PromptTemplateVersionEntity candidate = versionMapper.selectById(release.getVersionId());
            if (candidate != null) {
                return candidate;
            }
        }
        return stable;
    }

    /** 选择当前Agent命中的活动实验变体。 */
    private VariantResolution resolveExperimentVariant(String templateId, String agentId, String routingKey) {
        List<PromptExperimentEntity> experiments = experimentMapper.selectList(new LambdaQueryWrapper<PromptExperimentEntity>()
                .eq(PromptExperimentEntity::getPromptTemplateId, templateId)
                .eq(PromptExperimentEntity::getStatus, "running")
                .orderByDesc(PromptExperimentEntity::getStartedAt));
        PromptExperimentEntity experiment = experiments.stream()
                .filter(item -> !StringUtils.hasText(item.getAgentId()) || item.getAgentId().equals(agentId))
                .findFirst().orElse(null);
        if (experiment == null) {
            return null;
        }
        List<PromptExperimentVariantEntity> variants = variantMapper.selectList(new LambdaQueryWrapper<PromptExperimentVariantEntity>()
                        .eq(PromptExperimentVariantEntity::getExperimentId, experiment.getId()))
                .stream().sorted(Comparator.comparing(PromptExperimentVariantEntity::getVariantCode)).toList();
        if (variants.isEmpty()) {
            return null;
        }
        String variantId = PromptExperimentPolicy.selectVariant(firstText(routingKey, templateId), variants.stream()
                .map(item -> new PromptExperimentPolicy.WeightedVariant(item.getId(),
                        item.getTrafficWeight() == null ? 0D : item.getTrafficWeight().doubleValue()))
                .toList());
        return variants.stream()
                .filter(item -> item.getId().equals(variantId))
                .findFirst()
                .map(item -> new VariantResolution(experiment, item))
                .orElse(null);
    }

    /** 查询资源绑定。 */
    private PromptBindingEntity findBinding(String resourceType, String resourceId) {
        if (!StringUtils.hasText(resourceType) || !StringUtils.hasText(resourceId)) {
            return null;
        }
        return bindingMapper.selectOne(new LambdaQueryWrapper<PromptBindingEntity>()
                .eq(PromptBindingEntity::getResourceType, resourceType)
                .eq(PromptBindingEntity::getResourceId, resourceId)
                .eq(PromptBindingEntity::getEnabled, true)
                .orderByDesc(PromptBindingEntity::getUpdatedAt)
                .last("limit 1"));
    }

    /** 读取绑定变量。 */
    private Map<String, Object> bindingVariables(PromptBindingEntity binding) {
        return parseMap(binding == null ? null : binding.getVariableValues());
    }

    /** 读取JSON对象。 */
    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception exception) {
            throw new BusinessException("PROMPT_VARIABLE_VALUES_INVALID", "Prompt变量值必须是合法JSON对象");
        }
    }

    /** 校验版本归属。 */
    private PromptTemplateVersionEntity requireVersion(String templateId, String versionId) {
        PromptTemplateVersionEntity version = versionMapper.selectById(versionId);
        if (version == null || !templateId.equals(version.getTemplateId())) {
            throw new BusinessException("PROMPT_VERSION_NOT_FOUND", "Prompt版本不存在或不属于当前模板");
        }
        return version;
    }

    /** 查询最新版本。 */
    private PromptTemplateVersionEntity latestVersion(String templateId) {
        return versionMapper.selectOne(new LambdaQueryWrapper<PromptTemplateVersionEntity>()
                .eq(PromptTemplateVersionEntity::getTemplateId, templateId)
                .orderByDesc(PromptTemplateVersionEntity::getCreatedAt)
                .last("limit 1"));
    }

    /** 判断路由键是否进入灰度桶。 */
    private boolean inBucket(String routingKey, int percent) {
        int normalized = Math.max(0, Math.min(100, percent));
        int bucket = Math.floorMod(String.valueOf(routingKey).hashCode(), 100);
        return bucket < normalized;
    }

    /** 增量更新实验变体指标。 */
    private void updateVariantMetric(String variantId, boolean success, int latencyMs, int tokenCount, BigDecimal cost) {
        PromptExperimentVariantEntity variant = variantMapper.selectById(variantId);
        if (variant == null) {
            return;
        }
        long oldSamples = variant.getSampleCount() == null ? 0L : variant.getSampleCount();
        long newSamples = oldSamples + 1;
        BigDecimal oldLatency = variant.getAvgLatencyMs() == null ? BigDecimal.ZERO : variant.getAvgLatencyMs();
        variant.setSampleCount(newSamples);
        variant.setSuccessCount((variant.getSuccessCount() == null ? 0L : variant.getSuccessCount()) + (success ? 1 : 0));
        variant.setFailureCount((variant.getFailureCount() == null ? 0L : variant.getFailureCount()) + (success ? 0 : 1));
        variant.setAvgLatencyMs(oldLatency.multiply(BigDecimal.valueOf(oldSamples))
                .add(BigDecimal.valueOf(Math.max(0, latencyMs))).divide(BigDecimal.valueOf(newSamples), 4, java.math.RoundingMode.HALF_UP));
        variant.setTotalTokens((variant.getTotalTokens() == null ? 0L : variant.getTotalTokens()) + Math.max(0, tokenCount));
        variant.setTotalCost((variant.getTotalCost() == null ? BigDecimal.ZERO : variant.getTotalCost()).add(cost == null ? BigDecimal.ZERO : cost));
        variantMapper.updateById(variant);
    }

    /** 推断系统消息所属装配层。 */
    private String inferLayerName(String content) {
        String text = content == null ? "" : content;
        if (text.contains("知识库") || text.contains("引用来源")) return "RAG证据";
        if (text.contains("记忆") || text.contains("Memory")) return "Memory上下文";
        if (text.contains("工具") || text.contains("tool")) return "工具约束";
        if (text.contains("安全") || text.contains("不得")) return "安全约束";
        return "运行上下文";
    }

    /** 统一绑定模式。 */
    private String normalizeMode(String mode) {
        String normalized = StringUtils.hasText(mode) ? mode.trim().toUpperCase(Locale.ROOT) : "MANUAL";
        return List.of("MANUAL", "LOCKED", "FOLLOW_STABLE").contains(normalized) ? normalized : "MANUAL";
    }

    /** 返回首个非空文本。 */
    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    /** JSON序列化。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    /** Prompt版本解析结果。 */
    private record Resolution(String templateId, String versionId, String versionNo, String bindingMode,
                              String content, String variableSchema, Map<String, Object> bindingVariables,
                              String experimentId, String variantId, String variantCode) {
    }

    /** 实验变体解析结果。 */
    private record VariantResolution(PromptExperimentEntity experiment, PromptExperimentVariantEntity variant) {
    }
}
