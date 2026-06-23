package com.openagentflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.entity.ModelConfigEntity;
import com.openagentflow.entity.ModelProviderEntity;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.mapper.ModelConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedding 向量化服务。
 */
@Service
public class EmbeddingService {

    /** 模型配置 Mapper。 */
    private final ModelConfigMapper modelConfigMapper;

    /** 模型服务商服务。 */
    private final ModelProviderService modelProviderService;

    /** OpenAI-compatible 客户端。 */
    private final OpenAiCompatibleClient openAiCompatibleClient;

    /** JSON 工具，用于读取模型默认参数。 */
    private final ObjectMapper objectMapper;

    public EmbeddingService(ModelConfigMapper modelConfigMapper,
                            ModelProviderService modelProviderService,
                            OpenAiCompatibleClient openAiCompatibleClient,
                            ObjectMapper objectMapper) {
        this.modelConfigMapper = modelConfigMapper;
        this.modelProviderService = modelProviderService;
        this.openAiCompatibleClient = openAiCompatibleClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析指定或默认 Embedding 模型。
     *
     * @param preferredModelId 指定模型 ID
     * @return Embedding 模型
     */
    public ModelConfigEntity resolveEmbeddingModel(String preferredModelId) {
        if (StringUtils.hasText(preferredModelId)) {
            return modelProviderService.requireModel(preferredModelId);
        }
        return modelConfigMapper.selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getModelType, "embedding")
                        .eq(ModelConfigEntity::getStatus, "enabled")
                        .orderByDesc(ModelConfigEntity::getIsDefault)
                        .orderByDesc(ModelConfigEntity::getCreatedAt))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("EMBEDDING_MODEL_NOT_FOUND", "请先配置可用的 Embedding 模型"));
    }

    /**
     * 批量生成文本向量。
     *
     * @param model Embedding 模型
     * @param texts 待向量化文本
     * @return 向量列表
     */
    public List<List<Double>> embed(ModelConfigEntity model, List<String> texts) {
        return embedWithTrace(model, texts).getVectors();
    }

    /**
     * 批量生成文本向量，并返回真实调用或兜底调用的轨迹信息。
     *
     * @param model Embedding 模型
     * @param texts 待向量化文本
     * @return Embedding 调用结果
     */
    public EmbeddingBatchResult embedWithTrace(ModelConfigEntity model, List<String> texts) {
        EmbeddingBatchResult result = new EmbeddingBatchResult();
        result.setModelId(model.getId());
        result.setModelCode(model.getModelCode());
        result.setModelName(model.getModelName());
        result.setEmbeddingApi(resolveEmbeddingApi(model));
        ModelProviderEntity provider = modelProviderService.requireProviderByModel(model);
        String apiKey = modelProviderService.findApiKeyValue(provider.getId());
        try {
            List<List<Double>> vectors = openAiCompatibleClient.embeddings(provider, model, apiKey, texts);
            result.setVectors(vectors);
            result.setFallbackUsed(false);
            result.setDimension(firstDimension(vectors));
            return result;
        } catch (Exception exception) {
            // 开发阶段保留本地兜底向量，同时把失败原因返回给调用方用于日志展示。
            List<List<Double>> fallbackVectors = texts.stream().map(this::localFallbackEmbedding).toList();
            result.setVectors(fallbackVectors);
            result.setFallbackUsed(true);
            result.setErrorMessage(exception.getMessage());
            result.setDimension(firstDimension(fallbackVectors));
            return result;
        }
    }

    /**
     * 从模型默认参数读取 Embedding 接口类型。
     *
     * @param model Embedding 模型
     * @return 接口类型
     */
    private String resolveEmbeddingApi(ModelConfigEntity model) {
        if (!StringUtils.hasText(model.getDefaultParams())) {
            return "openai";
        }
        try {
            JsonNode params = objectMapper.readTree(model.getDefaultParams());
            String api = params.path("embeddingApi").asText("");
            return StringUtils.hasText(api) ? api : "openai";
        } catch (Exception exception) {
            return "openai";
        }
    }

    /**
     * 获取第一条向量的维度。
     *
     * @param vectors 向量列表
     * @return 向量维度
     */
    private int firstDimension(List<List<Double>> vectors) {
        if (vectors == null || vectors.isEmpty() || vectors.getFirst() == null) {
            return 0;
        }
        return vectors.getFirst().size();
    }

    /**
     * 生成确定性的本地兜底向量。
     *
     * @param text 文本内容
     * @return 归一化后的向量
     */
    private List<Double> localFallbackEmbedding(String text) {
        int dimension = 256;
        double[] values = new double[dimension];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
            for (int offset = 0; offset < bytes.length; offset += 32) {
                digest.update(bytes, offset, Math.min(32, bytes.length - offset));
                byte[] hash = digest.digest();
                for (int index = 0; index < hash.length; index++) {
                    int target = (offset + index) % dimension;
                    values[target] += (hash[index] & 0xFF) / 255.0D;
                }
            }
        } catch (Exception ignored) {
            values[0] = 1D;
        }
        double norm = 0D;
        for (double value : values) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> vector = new ArrayList<>(dimension);
        for (double value : values) {
            vector.add(norm == 0D ? 0D : value / norm);
        }
        return vector;
    }
}
