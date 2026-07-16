package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Agent模板表。
 * <p>对应数据库表：agent_template。</p>
 */
@TableName("agent_template")
public class AgentTemplateEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 私有模板所属工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 模板类型。 */ @TableField("template_type") private String templateType;
    /** 可见范围。 */ @TableField("visibility") private String visibility;
    /** 当前版本ID。 */ @TableField("current_version_id") private String currentVersionId;
    /** 审核状态。 */ @TableField("review_status") private String reviewStatus;
    /** 作者用户ID。 */ @TableField("author_user_id") private String authorUserId;
    /** 作者展示名称。 */ @TableField("author_name") private String authorName;
    /** 许可证编码。 */ @TableField("license_code") private String licenseCode;
    /** 兼容性声明。 */ @TableField("compatibility") private String compatibility;

    /** 模板编码。 */
    @TableField("template_code")
    private String templateCode;

    /** 模板名称。 */
    @TableField("template_name")
    private String templateName;

    /** 字段说明：CATEGORY。 */
    @TableField("category")
    private String category;

    /** 描述。 */
    @TableField("description")
    private String description;

    /** 字段说明：ICON。 */
    @TableField("icon")
    private String icon;

    /** 模板封面地址。 */ @TableField("cover_url") private String coverUrl;

    /** 字段说明：TAGS。 */
    @TableField("tags")
    private String tags;

    /** 字段说明：AgentSNAPSHOT。 */
    @TableField("agent_snapshot")
    private String agentSnapshot;

    /** 提示词SNAPSHOT。 */
    @TableField("prompt_snapshot")
    private String promptSnapshot;

    /** 工具SNAPSHOT。 */
    @TableField("tool_snapshot")
    private String toolSnapshot;

    /** 知识SNAPSHOT。 */
    @TableField("knowledge_snapshot")
    private String knowledgeSnapshot;

    /** 依赖清单JSON。 */ @TableField("dependency_manifest") private String dependencyManifest;
    /** 模板包存储桶。 */ @TableField("package_bucket") private String packageBucket;
    /** 模板包对象键。 */ @TableField("package_key") private String packageKey;
    /** 模板包哈希。 */ @TableField("package_hash") private String packageHash;
    /** 模板包大小。 */ @TableField("package_size") private Long packageSize;

    /** 字段说明：RECOMMENDED。 */
    @TableField("recommended")
    private Boolean recommended;

    /** INSTALL数量。 */
    @TableField("install_count")
    private Long installCount;

    /** 平均评分。 */ @TableField("average_rating") private java.math.BigDecimal averageRating;
    /** 评分人数。 */ @TableField("rating_count") private Long ratingCount;
    /** 收藏人数。 */ @TableField("favorite_count") private Long favoriteCount;
    /** 趋势热度分。 */ @TableField("trend_score") private java.math.BigDecimal trendScore;
    /** 举报次数。 */ @TableField("report_count") private Long reportCount;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 首次公开时间。 */ @TableField("published_at") private LocalDateTime publishedAt;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 软删除时间。 */ @TableField("deleted_at") private LocalDateTime deletedAt;
    /** 乐观锁版本号。 */ @TableField("version") private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getAgentSnapshot() {
        return agentSnapshot;
    }

    public void setAgentSnapshot(String agentSnapshot) {
        this.agentSnapshot = agentSnapshot;
    }

    public String getPromptSnapshot() {
        return promptSnapshot;
    }

    public void setPromptSnapshot(String promptSnapshot) {
        this.promptSnapshot = promptSnapshot;
    }

    public String getToolSnapshot() {
        return toolSnapshot;
    }

    public void setToolSnapshot(String toolSnapshot) {
        this.toolSnapshot = toolSnapshot;
    }

    public String getKnowledgeSnapshot() {
        return knowledgeSnapshot;
    }

    public void setKnowledgeSnapshot(String knowledgeSnapshot) {
        this.knowledgeSnapshot = knowledgeSnapshot;
    }

    public Boolean getRecommended() {
        return recommended;
    }

    public void setRecommended(Boolean recommended) {
        this.recommended = recommended;
    }

    public Long getInstallCount() {
        return installCount;
    }

    public void setInstallCount(Long installCount) {
        this.installCount = installCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
