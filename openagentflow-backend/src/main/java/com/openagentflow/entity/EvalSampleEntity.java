package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 评测样本表。
 * <p>对应数据库表：eval_sample。</p>
 */
@TableName("eval_sample")
public class EvalSampleEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 数据集ID。 */
    @TableField("dataset_id")
    private String datasetId;

    /** 样本序号。 */
    @TableField("sample_no")
    private Integer sampleNo;

    /** 字段说明：QUESTION。 */
    @TableField("question")
    private String question;

    /** 字段说明：EXPECTEDANSWER。 */
    @TableField("expected_answer")
    private String expectedAnswer;

    /** REFERENCE上下文。 */
    @TableField("reference_context")
    private String referenceContext;

    /** 字段说明：SCORINGPOINTS。 */
    @TableField("scoring_points")
    private String scoringPoints;

    /** 元数据JSON。 */
    @TableField("metadata")
    private String metadata;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Integer getSampleNo() {
        return sampleNo;
    }

    public void setSampleNo(Integer sampleNo) {
        this.sampleNo = sampleNo;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    public void setExpectedAnswer(String expectedAnswer) {
        this.expectedAnswer = expectedAnswer;
    }

    public String getReferenceContext() {
        return referenceContext;
    }

    public void setReferenceContext(String referenceContext) {
        this.referenceContext = referenceContext;
    }

    public String getScoringPoints() {
        return scoringPoints;
    }

    public void setScoringPoints(String scoringPoints) {
        this.scoringPoints = scoringPoints;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
