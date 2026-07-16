package com.openagentflow.domain.prompt;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * Prompt 模板中心 DTO 集合。
 */
public final class PromptDtos {

    private PromptDtos() {
    }

    /**
     * Prompt 模板中心概览指标。
     */
    public static class Overview {
        /** 模板总数。 */
        public Long templateCount;

        /** 已发布模板数。 */
        public Long publishedCount;

        /** 草稿模板数。 */
        public Long draftCount;

        /** 模板版本总数。 */
        public Long versionCount;

        /** 运行中实验数。 */
        public Long runningExperimentCount;

        /** 生产环境发布数。 */
        public Long productionReleaseCount;

        /** 启用资源绑定数。 */
        public Long activeBindingCount;
    }

    /**
     * Prompt 模板摘要。
     */
    public static class TemplateSummary {
        /** 模板ID。 */
        public String id;

        /** 模板编码。 */
        public String templateCode;

        /** 模板名称。 */
        public String templateName;

        /** Prompt 类型。 */
        public String promptType;

        /** Prompt 类型中文名称。 */
        public String promptTypeLabel;

        /** 模板内容。 */
        public String content;

        /** 变量定义JSON。 */
        public String variables;

        /** 强类型变量Schema JSON。 */
        public String variableSchema;

        /** 当前稳定版本ID。 */
        public String stableVersionId;

        /** 当前最高晋级环境。 */
        public String currentEnvironment;

        /** Prompt风险等级。 */
        public String riskLevel;

        /** 变量名称列表。 */
        public List<String> variableNames;

        /** 模板描述。 */
        public String description;

        /** 模板状态。 */
        public String status;

        /** 模板状态中文名称。 */
        public String statusLabel;

        /** 版本数量。 */
        public Long versionCount;

        /** 最近版本号。 */
        public String latestVersionNo;

        /** 绑定资源数量。 */
        public Long bindingCount;

        /** 所有者用户ID。 */
        public String ownerUserId;

        /** 创建时间。 */
        public LocalDateTime createdAt;

        /** 更新时间。 */
        public LocalDateTime updatedAt;
    }

    /**
     * Prompt 模板详情。
     */
    public static class TemplateDetail extends TemplateSummary {
        /** 历史版本列表。 */
        public List<VersionSummary> versions;
    }

    /**
     * Prompt 模板版本摘要。
     */
    public static class VersionSummary {
        /** 版本ID。 */
        public String id;

        /** 模板ID。 */
        public String templateId;

        /** 版本号。 */
        public String versionNo;

        /** 版本内容。 */
        public String content;

        /** 变量定义JSON。 */
        public String variables;

        /** 该版本固化的强类型变量Schema JSON。 */
        public String variableSchema;

        /** 内容SHA-256。 */
        public String contentHash;

        /** 校验状态。 */
        public String validationStatus;

        /** 校验结果JSON。 */
        public String validationResult;

        /** 质量得分。 */
        public BigDecimal qualityScore;

        /** 当前环境。 */
        public String environment;

        /** 发布时间。 */
        public LocalDateTime publishedAt;

        /** 变量名称列表。 */
        public List<String> variableNames;

        /** 变更说明。 */
        public String changeNote;

        /** 创建人ID。 */
        public String createdBy;

        /** 创建时间。 */
        public LocalDateTime createdAt;
    }

    /**
     * Prompt 模板保存请求。
     */
    public static class TemplateRequest {
        /** 模板编码，为空时后端自动生成。 */
        public String templateCode;

        /** 模板名称。 */
        public String templateName;

        /** Prompt 类型。 */
        public String promptType;

        /** 模板内容。 */
        public String content;

        /** 变量定义JSON，为空时后端会从 {{变量名}} 中自动解析。 */
        public String variables;

        /** 强类型变量Schema JSON，为空时兼容使用variables。 */
        public String variableSchema;

        /** 模板描述。 */
        public String description;

        /** 模板状态。 */
        public String status;

        /** Prompt风险等级。 */
        public String riskLevel;
    }

    /**
     * Prompt 模板发布请求。
     */
    public static class PublishRequest {
        /** 指定版本号，为空时后端按 vN 自动生成。 */
        public String versionNo;

        /** 发布说明。 */
        public String changeNote;
    }

    /**
     * Prompt 模板复制请求。
     */
    public static class CopyRequest {
        /** 复制后的模板名称。 */
        public String templateName;

        /** 复制后的模板编码。 */
        public String templateCode;
    }
}
