package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评测得分表。
 * <p>对应数据库表：eval_score。</p>
 */
@TableName("eval_score")
public class EvalScoreEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 任务运行ID。 */
    @TableField("task_run_id")
    private String taskRunId;

    /** 距离度量ID。 */
    @TableField("metric_id")
    private String metricId;

    /** 得分。 */
    @TableField("score")
    private BigDecimal score;

    /** 字段说明：PASSED。 */
    @TableField("passed")
    private Boolean passed;

    /** JUDGE类型。 */
    @TableField("judge_type")
    private String judgeType;

    /** 字段说明：JUDGEDETAIL。 */
    @TableField("judge_detail")
    private String judgeDetail;

    /** JUDGED人。 */
    @TableField("judged_by")
    private String judgedBy;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskRunId() {
        return taskRunId;
    }

    public void setTaskRunId(String taskRunId) {
        this.taskRunId = taskRunId;
    }

    public String getMetricId() {
        return metricId;
    }

    public void setMetricId(String metricId) {
        this.metricId = metricId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getJudgeType() {
        return judgeType;
    }

    public void setJudgeType(String judgeType) {
        this.judgeType = judgeType;
    }

    public String getJudgeDetail() {
        return judgeDetail;
    }

    public void setJudgeDetail(String judgeDetail) {
        this.judgeDetail = judgeDetail;
    }

    public String getJudgedBy() {
        return judgedBy;
    }

    public void setJudgedBy(String judgedBy) {
        this.judgedBy = judgedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
