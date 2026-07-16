package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** AI资源黄金评测基线实体。 */
@TableName("evaluation_baseline")
public class EvaluationBaselineEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 资源类型。 */ @TableField("resource_type") private String resourceType;
    /** 资源ID。 */ @TableField("resource_id") private String resourceId;
    /** 资源版本。 */ @TableField("resource_version") private String resourceVersion;
    /** 来源评测任务ID。 */ @TableField("eval_task_id") private String evalTaskId;
    /** 基线名称。 */ @TableField("baseline_name") private String baselineName;
    /** 指标值JSON。 */ @TableField("metric_values") private String metricValues;
    /** 综合得分。 */ @TableField("overall_score") private BigDecimal overallScore;
    /** 样本数量。 */ @TableField("sample_count") private Integer sampleCount;
    /** 是否生效。 */ @TableField("active") private Boolean active;
    /** 创建用户ID。 */ @TableField("created_by") private String createdBy;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getWorkspaceId(){return workspaceId;} public void setWorkspaceId(String value){workspaceId=value;}
    public String getResourceType(){return resourceType;} public void setResourceType(String value){resourceType=value;}
    public String getResourceId(){return resourceId;} public void setResourceId(String value){resourceId=value;}
    public String getResourceVersion(){return resourceVersion;} public void setResourceVersion(String value){resourceVersion=value;}
    public String getEvalTaskId(){return evalTaskId;} public void setEvalTaskId(String value){evalTaskId=value;}
    public String getBaselineName(){return baselineName;} public void setBaselineName(String value){baselineName=value;}
    public String getMetricValues(){return metricValues;} public void setMetricValues(String value){metricValues=value;}
    public BigDecimal getOverallScore(){return overallScore;} public void setOverallScore(BigDecimal value){overallScore=value;}
    public Integer getSampleCount(){return sampleCount;} public void setSampleCount(Integer value){sampleCount=value;}
    public Boolean getActive(){return active;} public void setActive(Boolean value){active=value;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String value){createdBy=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
