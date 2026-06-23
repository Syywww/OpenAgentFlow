package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提示词实验变体表。
 * <p>对应数据库表：prompt_experiment_variant。</p>
 */
@TableName("prompt_experiment_variant")
public class PromptExperimentVariantEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 实验ID。 */
    @TableField("experiment_id")
    private String experimentId;

    /** 变体编码。 */
    @TableField("variant_code")
    private String variantCode;

    /** 提示词内容。 */
    @TableField("prompt_content")
    private String promptContent;

    /** 模型参数。 */
    @TableField("model_params")
    private String modelParams;

    /** 字段说明：TRAFFICWEIGHT。 */
    @TableField("traffic_weight")
    private BigDecimal trafficWeight;

    /** 字段说明：METRICSSNAPSHOT。 */
    @TableField("metrics_snapshot")
    private String metricsSnapshot;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getVariantCode() {
        return variantCode;
    }

    public void setVariantCode(String variantCode) {
        this.variantCode = variantCode;
    }

    public String getPromptContent() {
        return promptContent;
    }

    public void setPromptContent(String promptContent) {
        this.promptContent = promptContent;
    }

    public String getModelParams() {
        return modelParams;
    }

    public void setModelParams(String modelParams) {
        this.modelParams = modelParams;
    }

    public BigDecimal getTrafficWeight() {
        return trafficWeight;
    }

    public void setTrafficWeight(BigDecimal trafficWeight) {
        this.trafficWeight = trafficWeight;
    }

    public String getMetricsSnapshot() {
        return metricsSnapshot;
    }

    public void setMetricsSnapshot(String metricsSnapshot) {
        this.metricsSnapshot = metricsSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
