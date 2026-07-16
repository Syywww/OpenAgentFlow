package com.openagentflow.domain.template;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 企业解决方案模板广场 DTO 集合。 */
public final class TemplateDtos {

    private TemplateDtos() {
    }

    /** 模板广场运营概览。 */
    public static class Overview {
        /** 公开上架模板数。 */ public long publishedCount;
        /** 当前用户收藏数。 */ public long favoriteCount;
        /** 当前工作空间成功安装数。 */ public long installedCount;
        /** 待人工审核版本数。 */ public long pendingReviewCount;
        /** 待处理举报数。 */ public long pendingReportCount;
        /** 运行中的安装任务数。 */ public long runningInstallCount;
    }

    /** 模板广场列表摘要。 */
    public static class TemplateSummary {
        /** 模板ID。 */ public String id;
        /** 模板编码。 */ public String templateCode;
        /** 模板名称。 */ public String templateName;
        /** 模板类型。 */ public String templateType;
        /** 可见范围。 */ public String visibility;
        /** 分类。 */ public String category;
        /** 描述。 */ public String description;
        /** 图标名称。 */ public String icon;
        /** 封面地址。 */ public String coverUrl;
        /** 标签。 */ public List<String> tags = new ArrayList<>();
        /** 作者用户ID。 */ public String authorUserId;
        /** 作者名称。 */ public String authorName;
        /** 当前语义化版本。 */ public String currentVersion;
        /** 当前版本ID。 */ public String currentVersionId;
        /** 模板状态。 */ public String status;
        /** 审核状态。 */ public String reviewStatus;
        /** 是否推荐。 */ public boolean recommended;
        /** 是否已收藏。 */ public boolean favorite;
        /** 安装次数。 */ public long installCount;
        /** 平均评分。 */ public BigDecimal averageRating = BigDecimal.ZERO;
        /** 评分人数。 */ public long ratingCount;
        /** 收藏人数。 */ public long favoriteCount;
        /** 趋势分。 */ public BigDecimal trendScore = BigDecimal.ZERO;
        /** 依赖资源数量。 */ public Map<String, Integer> resourceCounts = new LinkedHashMap<>();
        /** 发布时间。 */ public LocalDateTime publishedAt;
        /** 更新时间。 */ public LocalDateTime updatedAt;
    }

    /** 模板详情。 */
    public static class TemplateDetail extends TemplateSummary {
        /** 工作空间ID。 */ public String workspaceId;
        /** 许可证。 */ public String licenseCode;
        /** 兼容性声明。 */ public String compatibility;
        /** 依赖清单。 */ public Map<String, Object> dependencyManifest = new LinkedHashMap<>();
        /** 版本列表。 */ public List<VersionSummary> versions = new ArrayList<>();
        /** 当前版本资源。 */ public List<ResourceSummary> resources = new ArrayList<>();
        /** 评论列表。 */ public List<CommentSummary> comments = new ArrayList<>();
        /** 是否允许评价。 */ public boolean canReview;
        /** 是否允许管理。 */ public boolean canManage;
    }

    /** 模板作者主页摘要。 */
    public static class AuthorProfile {
        /** 作者用户ID。 */ public String userId;
        /** 作者名称。 */ public String authorName;
        /** 作者头像地址。 */ public String avatarUrl;
        /** 作者公开模板数量。 */ public long publishedTemplateCount;
        /** 模板累计安装数量。 */ public long totalInstallCount;
        /** 模板累计收藏数量。 */ public long totalFavoriteCount;
        /** 模板综合平均评分。 */ public BigDecimal averageRating = BigDecimal.ZERO;
        /** 作者公开模板。 */ public List<TemplateSummary> templates = new ArrayList<>();
    }

    /** 模板版本摘要。 */
    public static class VersionSummary {
        /** 版本ID。 */ public String id;
        /** 模板ID。 */ public String templateId;
        /** 语义化版本号。 */ public String versionNo;
        /** 版本名称。 */ public String versionName;
        /** 更新说明。 */ public String changeLog;
        /** 兼容性声明。 */ public String compatibilityStatement;
        /** 是否破坏性升级。 */ public boolean breakingChange;
        /** 版本状态。 */ public String status;
        /** 包哈希。 */ public String packageHash;
        /** 包大小。 */ public long packageSize;
        /** 自动安全检查。 */ public Map<String, Object> securityScanResult = new LinkedHashMap<>();
        /** 最小运行检查。 */ public Map<String, Object> runtimeCheckResult = new LinkedHashMap<>();
        /** 提交时间。 */ public LocalDateTime submittedAt;
        /** 发布时间。 */ public LocalDateTime publishedAt;
        /** 创建时间。 */ public LocalDateTime createdAt;
    }

    /** 模板资源摘要。 */
    public static class ResourceSummary {
        /** 模板资源ID。 */ public String id;
        /** 资源类型。 */ public String resourceType;
        /** 来源资源ID。 */ public String sourceResourceId;
        /** 资源编码。 */ public String resourceCode;
        /** 资源名称。 */ public String resourceName;
        /** 内容哈希。 */ public String contentHash;
        /** 是否必需。 */ public boolean required;
        /** 依赖资源ID。 */ public List<String> dependencyIds = new ArrayList<>();
        /** 安装顺序。 */ public int sortOrder;
    }

    /** 模板基础信息保存请求。 */
    public static class TemplateRequest {
        /** 模板编码。 */ public String templateCode;
        /** 模板名称。 */ public String templateName;
        /** 模板类型。 */ public String templateType;
        /** 工作空间或公开可见范围。 */ public String visibility;
        /** 工作空间ID。 */ public String workspaceId;
        /** 分类。 */ public String category;
        /** 描述。 */ public String description;
        /** 图标名称。 */ public String icon;
        /** 封面地址。 */ public String coverUrl;
        /** 标签。 */ public List<String> tags = new ArrayList<>();
        /** 许可证编码。 */ public String licenseCode;
        /** 兼容性说明。 */ public String compatibility;
    }

    /** 依赖分析与版本发布请求。 */
    public static class PublishRequest {
        /** 语义化版本号。 */ public String versionNo;
        /** 版本名称。 */ public String versionName;
        /** 更新说明。 */ public String changeLog;
        /** 兼容性声明。 */ public String compatibilityStatement;
        /** 是否破坏性升级。 */ public boolean breakingChange;
        /** 入口Agent ID列表。 */ public List<String> entryAgentIds = new ArrayList<>();
        /** 入口Agent团队ID列表。 */ public List<String> entryTeamIds = new ArrayList<>();
        /** 发布者手动追加资源。 */ public List<ResourceReference> includeResources = new ArrayList<>();
        /** 发布者手动排除资源。 */ public List<ResourceReference> excludeResources = new ArrayList<>();
        /** 是否提交公开审核。 */ public boolean submitForPublicReview;
    }

    /** 资源引用。 */
    public static class ResourceReference {
        /** 资源类型。 */ public String resourceType;
        /** 资源ID。 */ public String resourceId;
        /** 资源名称。 */ public String resourceName;
        /** 是否必需。 */ public boolean required = true;
    }

    /** 人工审核请求。 */
    public static class ReviewRequest {
        /** 审核动作：approve、reject、changes_required。 */ public String action;
        /** 审核意见。 */ public String comment;
        /** 风险等级。 */ public String riskLevel;
    }

    /** 评分与评论请求。 */
    public static class RatingRequest {
        /** 1到5分。 */ public int rating;
        /** 用户评论原文。 */ public String comment;
    }

    /** 评论回复请求。 */
    public static class ReplyRequest {
        /** 回复内容。 */ public String content;
    }

    /** 模板评论摘要。 */
    public static class CommentSummary {
        /** 评论ID。 */ public String id;
        /** 用户ID。 */ public String userId;
        /** 用户名称。 */ public String userName;
        /** 父评论ID。 */ public String parentCommentId;
        /** 评论内容。 */ public String content;
        /** 是否作者回复。 */ public boolean authorReply;
        /** 是否管理员回复。 */ public boolean adminReply;
        /** 用户评分。 */ public Integer rating;
        /** 创建时间。 */ public LocalDateTime createdAt;
    }

    /** 举报请求。 */
    public static class ReportRequest {
        /** 举报类型。 */ public String reportType;
        /** 举报原因。 */ public String reason;
        /** 证据对象地址。 */ public List<String> evidence = new ArrayList<>();
    }

    /** 模板举报队列摘要。 */
    public static class ReportSummary {
        /** 举报ID。 */ public String id;
        /** 模板ID。 */ public String templateId;
        /** 模板名称。 */ public String templateName;
        /** 举报用户ID。 */ public String reporterUserId;
        /** 举报用户名。 */ public String reporterName;
        /** 举报类型。 */ public String reportType;
        /** 举报原因。 */ public String reason;
        /** 证据对象地址。 */ public List<String> evidence = new ArrayList<>();
        /** 处理状态。 */ public String status;
        /** 处理结论。 */ public String resolution;
        /** 处理人ID。 */ public String handledBy;
        /** 提交时间。 */ public LocalDateTime createdAt;
        /** 处理时间。 */ public LocalDateTime handledAt;
    }

    /** 模板举报处置请求。 */
    public static class ReportResolutionRequest {
        /** 处理动作：resolved或rejected。 */ public String action;
        /** 处理结论。 */ public String resolution;
        /** 是否同步下架模板。 */ public boolean offlineTemplate;
    }

    /** 安装向导请求。 */
    public static class InstallRequest {
        /** 模板版本ID，为空时安装当前版本。 */ public String templateVersionId;
        /** 目标工作空间ID。 */ public String workspaceId;
        /** 资源名称前缀。 */ public String namePrefix;
        /** 来源模型到目标模型替代映射。 */ public Map<String, String> modelMapping = new LinkedHashMap<>();
        /** 目标Embedding模型ID。 */ public String embeddingModelId;
        /** 外部工具凭证是否已补齐。 */ public boolean credentialsReady;
        /** 请求幂等键。 */ public String idempotencyKey;
    }

    /** 安装实例摘要。 */
    public static class InstallSummary {
        /** 安装ID。 */ public String id;
        /** 模板ID。 */ public String templateId;
        /** 模板名称。 */ public String templateName;
        /** 工作空间ID。 */ public String workspaceId;
        /** 模板版本ID。 */ public String templateVersionId;
        /** 已安装版本号。 */ public String versionNo;
        /** 异步任务ID。 */ public String installTaskId;
        /** 安装状态。 */ public String installStatus;
        /** 进度百分比。 */ public int progressPercent;
        /** 当前阶段。 */ public String currentStage;
        /** 当前说明。 */ public String currentMessage;
        /** 首个目标Agent ID。 */ public String targetAgentId;
        /** 是否存在升级。 */ public boolean upgradeAvailable;
        /** 最新可升级版本号。 */ public String latestVersionNo;
        /** 错误原因。 */ public String errorMessage;
        /** 创建时间。 */ public LocalDateTime createdAt;
        /** 完成时间。 */ public LocalDateTime completedAt;
    }

    /** 三方升级冲突摘要。 */
    public static class UpgradeConflict {
        /** 冲突ID。 */ public String id;
        /** 资源类型。 */ public String resourceType;
        /** 本地资源ID。 */ public String targetResourceId;
        /** 资源名称。 */ public String resourceName;
        /** 合并判定。 */ public String mergeDecision;
        /** 用户选择。 */ public String userChoice;
        /** 旧模板哈希。 */ public String oldHash;
        /** 本地哈希。 */ public String localHash;
        /** 新模板哈希。 */ public String newHash;
        /** 三方差异详情。 */ public Map<String, Object> detail = new LinkedHashMap<>();
    }

    /** 升级请求。 */
    public static class UpgradeRequest {
        /** 目标版本ID。 */ public String targetVersionId;
        /** 冲突ID到use_new或keep_local的选择。 */ public Map<String, String> conflictChoices = new LinkedHashMap<>();
    }

    /** 卸载请求。 */
    public static class UninstallRequest {
        /** 是否删除未修改的模板资源。 */ public boolean deleteUnmodifiedResources;
    }
}
