package com.openagentflow.domain.prompt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PromptOps 运行时、实验和治理 DTO 集合。
 */
public final class PromptRuntimeDtos {

    /** 工具类不允许实例化。 */
    private PromptRuntimeDtos() {
    }

    /** Prompt 装配层。 */
    public static class PromptLayer {
        /** 装配层编码。 */
        public String layerCode;
        /** 装配层名称。 */
        public String layerName;
        /** 装配层内容。 */
        public String content;
        /** 装配顺序，数值越小越靠前。 */
        public Integer orderNo;

        public PromptLayer() {
        }

        public PromptLayer(String layerCode, String layerName, String content, Integer orderNo) {
            this.layerCode = layerCode;
            this.layerName = layerName;
            this.content = content;
            this.orderNo = orderNo;
        }
    }

    /** Prompt 变量定义。 */
    public static class VariableDefinition {
        /** 变量名称。 */
        public String name;
        /** 数据类型：string、number、integer、boolean、object、array。 */
        public String type;
        /** 是否必填。 */
        public Boolean required;
        /** 默认值。 */
        public Object defaultValue;
        /** 枚举可选值。 */
        public List<Object> enumValues = new ArrayList<>();
        /** 是否为敏感变量。 */
        public Boolean sensitive;
        /** 变量说明。 */
        public String description;
    }

    /** Prompt 编译请求。 */
    public static class CompileRequest {
        /** 模板 ID。 */
        public String templateId;
        /** 指定版本 ID。 */
        public String versionId;
        /** 绑定模式：LOCKED、FOLLOW_STABLE、MANUAL。 */
        public String bindingMode;
        /** 手工模板内容。 */
        public String content;
        /** 变量定义 JSON。 */
        public String variableSchema;
        /** 运行时变量。 */
        public Map<String, Object> variables = new LinkedHashMap<>();
        /** Prompt 装配层。 */
        public List<PromptLayer> layers = new ArrayList<>();
        /** 是否严格校验必填变量。 */
        public Boolean strict;
        /** 资源类型。 */
        public String resourceType;
        /** 资源 ID。 */
        public String resourceId;
        /** 运行 ID。 */
        public String runId;
        /** Agent ID。 */
        public String agentId;
        /** 用户或会话分桶键。 */
        public String routingKey;
    }

    /** Prompt 编译结果。 */
    public static class CompileResult {
        /** 模板 ID。 */
        public String templateId;
        /** 实际使用的版本 ID。 */
        public String versionId;
        /** 实际使用的版本号。 */
        public String versionNo;
        /** 绑定模式。 */
        public String bindingMode;
        /** 最终渲染后的 Prompt。 */
        public String renderedPrompt;
        /** 缺失的必填变量。 */
        public List<String> missingVariables = new ArrayList<>();
        /** 变量来源，不保存变量明文。 */
        public Map<String, String> variableSources = new LinkedHashMap<>();
        /** 敏感变量名称。 */
        public List<String> sensitiveVariableNames = new ArrayList<>();
        /** 实际装配层。 */
        public List<PromptLayer> layers = new ArrayList<>();
        /** 估算 Token 数量。 */
        public Integer estimatedTokens;
        /** 最终内容 SHA-256。 */
        public String contentHash;
        /** 编译警告。 */
        public List<String> warnings = new ArrayList<>();
        /** 命中的实验 ID。 */
        public String experimentId;
        /** 命中的实验变体 ID。 */
        public String variantId;
        /** 命中的实验变体编码。 */
        public String variantCode;
    }

    /** Prompt 预览请求。 */
    public static class PreviewRequest extends CompileRequest {
    }

    /** Prompt 版本差异。 */
    public static class VersionDiff {
        /** 来源版本 ID。 */
        public String fromVersionId;
        /** 目标版本 ID。 */
        public String toVersionId;
        /** 新增行。 */
        public List<String> addedLines = new ArrayList<>();
        /** 删除行。 */
        public List<String> removedLines = new ArrayList<>();
        /** 是否发生变量 Schema 变化。 */
        public Boolean variableSchemaChanged;
    }

    /** Prompt 影响资源。 */
    public static class ImpactItem {
        /** 资源类型。 */
        public String resourceType;
        /** 资源 ID。 */
        public String resourceId;
        /** 资源名称。 */
        public String resourceName;
        /** 绑定模式。 */
        public String bindingMode;
        /** 当前版本 ID。 */
        public String versionId;
    }

    /** 环境晋级请求。 */
    public static class PromotionRequest {
        /** 版本 ID。 */
        public String versionId;
        /** 目标环境：development、testing、production。 */
        public String environment;
        /** 灰度比例，范围 0-100。 */
        public Integer grayPercent;
        /** 晋级说明。 */
        public String releaseNote;
    }

    /** 环境发布摘要。 */
    public static class EnvironmentRelease {
        /** 发布 ID。 */
        public String id;
        /** 模板 ID。 */
        public String templateId;
        /** 版本 ID。 */
        public String versionId;
        /** 环境。 */
        public String environment;
        /** 发布状态。 */
        public String status;
        /** 灰度比例。 */
        public Integer grayPercent;
        /** 晋级人。 */
        public String promotedBy;
        /** 晋级时间。 */
        public LocalDateTime promotedAt;
    }

    /** Prompt 实验保存请求。 */
    public static class ExperimentRequest {
        /** 实验名称。 */
        public String experimentName;
        /** Prompt 模板 ID。 */
        public String promptTemplateId;
        /** Agent ID。 */
        public String agentId;
        /** 评测集 ID。 */
        public String datasetId;
        /** 主要指标。 */
        public String metricKey;
        /** 最小样本数。 */
        public Integer minSampleSize;
        /** 自动选优是否启用。 */
        public Boolean autoWinnerEnabled;
        /** 实验变体。 */
        public List<VariantRequest> variants = new ArrayList<>();
    }

    /** Prompt 实验变体请求。 */
    public static class VariantRequest {
        /** 变体编码。 */
        public String variantCode;
        /** Prompt 版本 ID。 */
        public String promptVersionId;
        /** 覆盖 Prompt 内容。 */
        public String promptContent;
        /** 模型参数 JSON。 */
        public String modelParams;
        /** 流量权重，范围 0-100。 */
        public BigDecimal trafficWeight;
    }

    /** Prompt 实验摘要。 */
    public static class ExperimentSummary {
        /** 实验 ID。 */
        public String id;
        /** 实验编码。 */
        public String experimentCode;
        /** 实验名称。 */
        public String experimentName;
        /** 模板 ID。 */
        public String promptTemplateId;
        /** Agent ID。 */
        public String agentId;
        /** 状态。 */
        public String status;
        /** 主要指标。 */
        public String metricKey;
        /** 胜出变体 ID。 */
        public String winnerVariantId;
        /** 变体列表。 */
        public List<VariantSummary> variants = new ArrayList<>();
        /** 创建时间。 */
        public LocalDateTime createdAt;
    }

    /** Prompt 实验变体摘要。 */
    public static class VariantSummary {
        /** 变体 ID。 */
        public String id;
        /** 变体编码。 */
        public String variantCode;
        /** Prompt 版本 ID。 */
        public String promptVersionId;
        /** 流量权重。 */
        public BigDecimal trafficWeight;
        /** 样本数。 */
        public Long sampleCount;
        /** 成功率。 */
        public Double successRate;
        /** 平均质量得分。 */
        public Double avgQualityScore;
        /** 平均耗时。 */
        public Double avgLatencyMs;
        /** Token 总量。 */
        public Long totalTokens;
        /** 成本总额。 */
        public BigDecimal totalCost;
    }

    /** Prompt 版本运行指标。 */
    public static class VersionMetric {
        /** 版本 ID。 */
        public String versionId;
        /** 版本号。 */
        public String versionNo;
        /** 调用次数。 */
        public Long callCount;
        /** 成功率。 */
        public Double successRate;
        /** 平均质量得分。 */
        public Double avgQualityScore;
        /** 平均耗时。 */
        public Double avgLatencyMs;
        /** Token 总量。 */
        public Long totalTokens;
        /** 成本总额。 */
        public BigDecimal totalCost;
    }
}
