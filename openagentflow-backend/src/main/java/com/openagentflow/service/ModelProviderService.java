package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.chat.ChatMessage;
import com.openagentflow.domain.chat.ChatRunContext;
import com.openagentflow.domain.chat.LlmCallResult;
import com.openagentflow.domain.model.ModelConfigRequest;
import com.openagentflow.domain.model.ModelConfigSummary;
import com.openagentflow.domain.model.ModelConnectivityResult;
import com.openagentflow.domain.model.ModelProviderRequest;
import com.openagentflow.domain.model.ModelProviderSummary;
import com.openagentflow.entity.ModelApiKeyEntity;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelConnectivityTestEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.ModelApiKeyMapper;
import com.openagentflow.mapper.ModelConfigMapper;
import com.openagentflow.mapper.ModelConnectivityTestMapper;
import com.openagentflow.mapper.ModelProviderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 模型服务商应用服务。
 */
@Service
public class ModelProviderService {

    /** 模型服务商 Mapper。 */
    private final ModelProviderMapper modelProviderMapper;

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** 模型 API Key Mapper。 */
    private final ModelApiKeyMapper modelApiKeyMapper;

    /** 模型连通性测试 Mapper。 */
    private final ModelConnectivityTestMapper modelConnectivityTestMapper;

    /** OpenAI-compatible 调用客户端。 */
    private final OpenAiCompatibleClient openAiCompatibleClient;

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    public ModelProviderService(ModelProviderMapper modelProviderMapper,
                                ModelConfigMapper modelConfigMapper,
                                ModelApiKeyMapper modelApiKeyMapper,
                                ModelConnectivityTestMapper modelConnectivityTestMapper,
                                OpenAiCompatibleClient openAiCompatibleClient,
                                ObjectMapper objectMapper) {
        this.modelProviderMapper = modelProviderMapper;
        this.modelConfigMapper = modelConfigMapper;
        this.modelApiKeyMapper = modelApiKeyMapper;
        this.modelConnectivityTestMapper = modelConnectivityTestMapper;
        this.openAiCompatibleClient = openAiCompatibleClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询模型服务商列表。
     *
     * @return 模型服务商摘要列表
     */
    public List<ModelProviderSummary> listProviders() {
        List<ModelProviderEntity> providers = modelProviderMapper.selectList(new LambdaQueryWrapper<ModelProviderEntity>()
                .orderByAsc(ModelProviderEntity::getSortOrder)
                .orderByDesc(ModelProviderEntity::getCreatedAt));
        List<ModelConfigEntity> models = modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                .orderByAsc(ModelConfigEntity::getModelType)
                .orderByDesc(ModelConfigEntity::getIsDefault)
                .orderByDesc(ModelConfigEntity::getCreatedAt));
        Map<String, List<ModelConfigSummary>> modelMap = models.stream()
                .map(model -> toModelSummary(model, findProviderName(providers, model.getProviderId())))
                .collect(Collectors.groupingBy(ModelConfigSummary::getProviderId));

        return providers.stream()
                .map(provider -> {
                    ModelProviderSummary summary = toProviderSummary(provider);
                    summary.setKeyMask(findEnabledKey(provider.getId()).map(ModelApiKeyEntity::getKeyMask).orElse(""));
                    summary.setModels(modelMap.getOrDefault(provider.getId(), List.of()));
                    return summary;
                })
                .toList();
    }

    /**
     * 查询可用于聊天的模型列表。
     *
     * @return 聊天模型摘要列表
     */
    public List<ModelConfigSummary> listChatModels() {
        List<ModelProviderEntity> providers = modelProviderMapper.selectList(new LambdaQueryWrapper<ModelProviderEntity>()
                .eq(ModelProviderEntity::getStatus, "enabled"));
        Map<String, ModelProviderEntity> providerMap = providers.stream()
                .collect(Collectors.toMap(ModelProviderEntity::getId, provider -> provider));
        return modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getModelType, "chat")
                        .eq(ModelConfigEntity::getStatus, "enabled")
                        .orderByDesc(ModelConfigEntity::getIsDefault)
                        .orderByDesc(ModelConfigEntity::getCreatedAt))
                .stream()
                .filter(model -> providerMap.containsKey(model.getProviderId()))
                .map(model -> toModelSummary(model, providerMap.get(model.getProviderId()).getProviderName()))
                .toList();
    }

    /**
     * 创建模型服务商。
     *
     * @param request 保存请求
     * @return 模型服务商摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderSummary createProvider(ModelProviderRequest request) {
        ModelProviderEntity entity = new ModelProviderEntity();
        entity.setId(newId());
        fillProvider(entity, request);
        entity.setHealthStatus("unknown");
        modelProviderMapper.insert(entity);
        saveOrReplaceApiKey(entity.getId(), request.getApiKey());
        saveModels(entity.getId(), request.getModels());
        return getProvider(entity.getId());
    }

    /**
     * 更新模型服务商。
     *
     * @param id 模型服务商 ID
     * @param request 保存请求
     * @return 模型服务商摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderSummary updateProvider(String id, ModelProviderRequest request) {
        ModelProviderEntity entity = requireProvider(id);
        fillProvider(entity, request);
        modelProviderMapper.updateById(entity);
        if (StringUtils.hasText(request.getApiKey())) {
            saveOrReplaceApiKey(entity.getId(), request.getApiKey());
        }
        if (request.getModels() != null) {
            saveModels(entity.getId(), request.getModels());
        }
        return getProvider(entity.getId());
    }

    /**
     * 删除模型服务商。
     *
     * @param id 模型服务商 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(String id) {
        requireProvider(id);
        // 外键会级联删除模型和密钥，业务侧只需要删除服务商主记录。
        modelProviderMapper.deleteById(id);
    }

    /**
     * 查询模型服务商详情。
     *
     * @param id 模型服务商 ID
     * @return 模型服务商摘要
     */
    public ModelProviderSummary getProvider(String id) {
        ModelProviderEntity provider = requireProvider(id);
        ModelProviderSummary summary = toProviderSummary(provider);
        summary.setKeyMask(findEnabledKey(provider.getId()).map(ModelApiKeyEntity::getKeyMask).orElse(""));
        summary.setModels(modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getProviderId, provider.getId())
                        .orderByAsc(ModelConfigEntity::getModelType)
                        .orderByDesc(ModelConfigEntity::getIsDefault)
                        .orderByDesc(ModelConfigEntity::getCreatedAt))
                .stream()
                .map(model -> toModelSummary(model, provider.getProviderName()))
                .toList());
        return summary;
    }

    /**
     * 执行模型连通性测试。
     *
     * @param providerId 模型服务商 ID
     * @return 连通性测试结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelConnectivityResult testProvider(String providerId) {
        ModelProviderEntity provider = requireProvider(providerId);
        ModelConfigEntity model = modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getProviderId, providerId)
                        .eq(ModelConfigEntity::getModelType, "chat")
                        .eq(ModelConfigEntity::getStatus, "enabled"))
                .stream()
                .max(Comparator.comparing(modelConfig -> Boolean.TRUE.equals(modelConfig.getIsDefault())))
                .orElseThrow(() -> new BusinessException("MODEL_NOT_FOUND", "请先配置可用的 Chat 模型"));
        String apiKey = findEnabledKey(providerId).map(ModelApiKeyEntity::getKeyCipher).orElse("");

        ChatRunContext context = new ChatRunContext();
        context.setProvider(provider);
        context.setModel(model);
        context.setApiKey(apiKey);
        context.setMessages(List.of(
                new ChatMessage("system", "你是模型连通性测试助手。"),
                new ChatMessage("user", "请用一句中文短句回答：OpenAgentFlow 模型连通性测试成功。")
        ));

        LocalDateTime startedAt = LocalDateTime.now();
        ModelConnectivityResult result = new ModelConnectivityResult();
        ModelConnectivityTestEntity testEntity = new ModelConnectivityTestEntity();
        testEntity.setId(newId());
        testEntity.setProviderId(provider.getId());
        testEntity.setModelId(model.getId());
        testEntity.setTestType("chat");
        testEntity.setCreatedAt(startedAt);
        try {
            LlmCallResult callResult = openAiCompatibleClient.complete(context, 0.1, 64);
            result.setSuccess(true);
            result.setHealthStatus("healthy");
            result.setLatencyMs(callResult.getLatencyMs());
            result.setResponseText(callResult.getContent());
            testEntity.setSuccess(true);
            testEntity.setLatencyMs(callResult.getLatencyMs());
            testEntity.setRequestPayload(toJson(Map.of("model", model.getModelCode(), "type", "chat")));
            testEntity.setResponsePayload(toJson(Map.of("content", callResult.getContent())));
            provider.setHealthStatus("healthy");
        } catch (Exception exception) {
            result.setSuccess(false);
            result.setHealthStatus("unhealthy");
            result.setLatencyMs((int) java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis());
            result.setErrorMessage(exception.getMessage());
            testEntity.setSuccess(false);
            testEntity.setLatencyMs(result.getLatencyMs());
            testEntity.setRequestPayload(toJson(Map.of("model", model.getModelCode(), "type", "chat")));
            testEntity.setErrorMessage(exception.getMessage());
            provider.setHealthStatus("unhealthy");
        }
        modelConnectivityTestMapper.insert(testEntity);
        modelProviderMapper.updateById(provider);
        return result;
    }

    /**
     * 根据 ID 查询模型实体。
     *
     * @param modelId 模型 ID
     * @return 模型实体
     */
    public ModelConfigEntity requireModel(String modelId) {
        ModelConfigEntity model = modelConfigMapper.selectById(modelId);
        if (model == null) {
            throw new BusinessException("MODEL_NOT_FOUND", "模型不存在");
        }
        return model;
    }

    /**
     * 根据模型查询服务商实体。
     *
     * @param model 模型实体
     * @return 服务商实体
     */
    public ModelProviderEntity requireProviderByModel(ModelConfigEntity model) {
        return requireProvider(model.getProviderId());
    }

    /**
     * 查询服务商可用密钥。
     *
     * @param providerId 服务商 ID
     * @return API Key 明文
     */
    public String findApiKeyValue(String providerId) {
        return findEnabledKey(providerId).map(ModelApiKeyEntity::getKeyCipher).orElse("");
    }

    /**
     * 填充模型服务商实体。
     *
     * @param entity 服务商实体
     * @param request 保存请求
     */
    private void fillProvider(ModelProviderEntity entity, ModelProviderRequest request) {
        entity.setProviderCode(request.getProviderCode());
        entity.setProviderName(request.getProviderName());
        entity.setProviderType(request.getProviderType());
        entity.setBaseUrl(request.getBaseUrl());
        entity.setAuthType(StringUtils.hasText(request.getAuthType()) ? request.getAuthType() : "api_key");
        entity.setDefaultHeaders(StringUtils.hasText(request.getDefaultHeaders()) ? request.getDefaultHeaders() : "{}");
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    /**
     * 保存或更新模型配置列表。
     *
     * @param providerId 服务商 ID
     * @param requests 模型配置请求
     */
    private void saveModels(String providerId, List<ModelConfigRequest> requests) {
        if (requests == null) {
            return;
        }
        for (ModelConfigRequest request : requests) {
            ModelConfigEntity entity = StringUtils.hasText(request.getId())
                    ? modelConfigMapper.selectById(request.getId())
                    : null;
            if (entity == null) {
                entity = new ModelConfigEntity();
                entity.setId(newId());
                entity.setProviderId(providerId);
            }
            entity.setModelCode(request.getModelCode());
            entity.setModelName(request.getModelName());
            entity.setModelType(StringUtils.hasText(request.getModelType()) ? request.getModelType() : "chat");
            entity.setContextWindow(request.getContextWindow());
            entity.setMaxOutputTokens(request.getMaxOutputTokens());
            // 模型价格由成本中心直接使用，保存时允许为空；为空表示该模型暂不估算费用。
            entity.setInputPricePer1k(request.getInputPricePer1k());
            entity.setOutputPricePer1k(request.getOutputPricePer1k());
            entity.setSupportStream(Boolean.TRUE.equals(request.getSupportStream()));
            entity.setSupportFunctionCalling(Boolean.TRUE.equals(request.getSupportFunctionCalling()));
            entity.setSupportVision(Boolean.TRUE.equals(request.getSupportVision()));
            entity.setDefaultParams(StringUtils.hasText(request.getDefaultParams()) ? request.getDefaultParams() : "{}");
            entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "enabled");
            entity.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
            if (modelConfigMapper.selectById(entity.getId()) == null) {
                modelConfigMapper.insert(entity);
            } else {
                modelConfigMapper.updateById(entity);
            }
        }
    }

    /**
     * 保存或替换服务商 API Key。
     *
     * @param providerId 服务商 ID
     * @param apiKey API Key 明文
     */
    private void saveOrReplaceApiKey(String providerId, String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return;
        }
        List<ModelApiKeyEntity> oldKeys = modelApiKeyMapper.selectList(new LambdaQueryWrapper<ModelApiKeyEntity>()
                .eq(ModelApiKeyEntity::getProviderId, providerId));
        oldKeys.forEach(old -> {
            old.setStatus("disabled");
            modelApiKeyMapper.updateById(old);
        });

        ModelApiKeyEntity entity = new ModelApiKeyEntity();
        entity.setId(newId());
        entity.setProviderId(providerId);
        entity.setKeyName("default");
        entity.setKeyCipher(apiKey);
        entity.setKeyMask(maskApiKey(apiKey));
        entity.setStatus("enabled");
        entity.setQuotaUsed(0L);
        modelApiKeyMapper.insert(entity);
    }

    /**
     * 查询可用密钥。
     *
     * @param providerId 服务商 ID
     * @return API Key 实体
     */
    private java.util.Optional<ModelApiKeyEntity> findEnabledKey(String providerId) {
        return modelApiKeyMapper.selectList(new LambdaQueryWrapper<ModelApiKeyEntity>()
                        .eq(ModelApiKeyEntity::getProviderId, providerId)
                        .eq(ModelApiKeyEntity::getStatus, "enabled")
                        .orderByDesc(ModelApiKeyEntity::getCreatedAt))
                .stream()
                .findFirst();
    }

    /**
     * 查询服务商实体，缺失时抛出业务异常。
     *
     * @param id 服务商 ID
     * @return 服务商实体
     */
    private ModelProviderEntity requireProvider(String id) {
        ModelProviderEntity entity = modelProviderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("MODEL_PROVIDER_NOT_FOUND", "模型服务商不存在");
        }
        return entity;
    }

    /**
     * 转换服务商摘要。
     *
     * @param entity 服务商实体
     * @return 服务商摘要
     */
    private ModelProviderSummary toProviderSummary(ModelProviderEntity entity) {
        ModelProviderSummary item = new ModelProviderSummary();
        item.setId(entity.getId());
        item.setProviderCode(entity.getProviderCode());
        item.setProviderName(entity.getProviderName());
        item.setProviderType(entity.getProviderType());
        item.setBaseUrl(entity.getBaseUrl());
        item.setAuthType(entity.getAuthType());
        item.setStatus(entity.getStatus());
        item.setHealthStatus(entity.getHealthStatus());
        return item;
    }

    /**
     * 转换模型摘要。
     *
     * @param entity 模型实体
     * @param providerName 服务商名称
     * @return 模型摘要
     */
    private ModelConfigSummary toModelSummary(ModelConfigEntity entity, String providerName) {
        ModelConfigSummary item = new ModelConfigSummary();
        item.setId(entity.getId());
        item.setProviderId(entity.getProviderId());
        item.setProviderName(providerName);
        item.setModelCode(entity.getModelCode());
        item.setModelName(entity.getModelName());
        item.setModelType(entity.getModelType());
        item.setContextWindow(entity.getContextWindow());
        item.setMaxOutputTokens(entity.getMaxOutputTokens());
        item.setInputPricePer1k(entity.getInputPricePer1k());
        item.setOutputPricePer1k(entity.getOutputPricePer1k());
        item.setSupportStream(entity.getSupportStream());
        item.setSupportFunctionCalling(entity.getSupportFunctionCalling());
        item.setSupportVision(entity.getSupportVision());
        item.setStatus(entity.getStatus());
        item.setIsDefault(entity.getIsDefault());
        return item;
    }

    /**
     * 根据服务商 ID 查找服务商名称。
     *
     * @param providers 服务商列表
     * @param providerId 服务商 ID
     * @return 服务商名称
     */
    private String findProviderName(List<ModelProviderEntity> providers, String providerId) {
        return providers.stream()
                .filter(provider -> provider.getId().equals(providerId))
                .findFirst()
                .map(ModelProviderEntity::getProviderName)
                .orElse("");
    }

    /**
     * 生成 UUID 主键。
     *
     * @return UUID 字符串
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 对 API Key 做脱敏。
     *
     * @param apiKey API Key 明文
     * @return 脱敏文本
     */
    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        if (apiKey.length() <= 10) {
            return apiKey.charAt(0) + "****" + apiKey.charAt(apiKey.length() - 1);
        }
        return apiKey.substring(0, 6) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 转换 JSON 字符串。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
