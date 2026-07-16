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

    /** 关联Prompt版本ID。 */
    @TableField("prompt_version_id")
    private String promptVersionId;

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

    /** 累计样本数。 */ @TableField("sample_count") private Long sampleCount;
    /** 累计成功数。 */ @TableField("success_count") private Long successCount;
    /** 累计失败数。 */ @TableField("failure_count") private Long failureCount;
    /** 平均质量得分。 */ @TableField("avg_quality_score") private BigDecimal avgQualityScore;
    /** 平均耗时毫秒。 */ @TableField("avg_latency_ms") private BigDecimal avgLatencyMs;
    /** 累计Token数量。 */ @TableField("total_tokens") private Long totalTokens;
    /** 累计成本。 */ @TableField("total_cost") private BigDecimal totalCost;

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

    public String getPromptVersionId() { return promptVersionId; }
    public void setPromptVersionId(String promptVersionId) { this.promptVersionId = promptVersionId; }

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

    public Long getSampleCount() { return sampleCount; }
    public void setSampleCount(Long sampleCount) { this.sampleCount = sampleCount; }
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    public BigDecimal getAvgQualityScore() { return avgQualityScore; }
    public void setAvgQualityScore(BigDecimal avgQualityScore) { this.avgQualityScore = avgQualityScore; }
    public BigDecimal getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(BigDecimal avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
