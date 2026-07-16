package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提示词模板版本表。
 * <p>对应数据库表：prompt_template_version。</p>
 */
@TableName("prompt_template_version")
public class PromptTemplateVersionEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 模板ID。 */
    @TableField("template_id")
    private String templateId;

    /** 版本序号。 */
    @TableField("version_no")
    private String versionNo;

    /** 内容。 */
    @TableField("content")
    private String content;

    /** 字段说明：VARIABLES。 */
    @TableField("variables")
    private String variables;

    /** 该版本固化的强类型变量Schema JSON数组。 */
    @TableField("variable_schema")
    private String variableSchema;

    /** Prompt内容SHA-256哈希。 */ @TableField("content_hash") private String contentHash;
    /** 校验状态。 */ @TableField("validation_status") private String validationStatus;
    /** 校验结果JSON。 */ @TableField("validation_result") private String validationResult;
    /** 关联评测质量得分。 */ @TableField("quality_score") private BigDecimal qualityScore;
    /** 版本当前环境。 */ @TableField("environment") private String environment;
    /** 版本发布时间。 */ @TableField("published_at") private LocalDateTime publishedAt;

    /** 变更NOTE。 */
    @TableField("change_note")
    private String changeNote;

    /** 创建人ID。 */
    @TableField("created_by")
    private String createdBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getVariableSchema() { return variableSchema; }
    public void setVariableSchema(String variableSchema) { this.variableSchema = variableSchema; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getValidationResult() { return validationResult; }
    public void setValidationResult(String validationResult) { this.validationResult = validationResult; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
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
}
