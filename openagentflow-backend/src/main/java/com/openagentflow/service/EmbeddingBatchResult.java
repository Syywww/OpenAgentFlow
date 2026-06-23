package com.openagentflow.service;

import java.util.List;

/**
 * Embedding 批量调用结果。
 */
public class EmbeddingBatchResult {

    /** 向量列表，顺序与输入文本一致。 */
    private List<List<Double>> vectors;

    /** 模型主键 ID。 */
    private String modelId;

    /** 模型编码，豆包方舟场景通常是接入点 ID。 */
    private String modelCode;

    /** 模型展示名称。 */
    private String modelName;

    /** Embedding 接口类型，例如 openai 或 multimodal。 */
    private String embeddingApi;

    /** 是否使用本地兜底向量。 */
    private Boolean fallbackUsed;

    /** 真实模型调用失败时的错误信息。 */
    private String errorMessage;

    /** 返回向量维度。 */
    private Integer dimension;

    public List<List<Double>> getVectors() {
        return vectors;
    }

    public void setVectors(List<List<Double>> vectors) {
        this.vectors = vectors;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getEmbeddingApi() {
        return embeddingApi;
    }

    public void setEmbeddingApi(String embeddingApi) {
        this.embeddingApi = embeddingApi;
    }

    public Boolean getFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(Boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }
}
