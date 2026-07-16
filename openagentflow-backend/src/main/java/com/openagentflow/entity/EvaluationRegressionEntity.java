package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** AI资源黄金基线回归比较实体。 */
@TableName("evaluation_regression")
public class EvaluationRegressionEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 黄金基线ID。 */ @TableField("baseline_id") private String baselineId;
    /** 候选评测任务ID。 */ @TableField("candidate_task_id") private String candidateTaskId;
    /** 资源类型。 */ @TableField("resource_type") private String resourceType;
    /** 资源ID。 */ @TableField("resource_id") private String resourceId;
    /** 目标版本。 */ @TableField("target_version") private String targetVersion;
    /** 比较状态。 */ @TableField("status") private String status;
    /** 基线指标JSON。 */ @TableField("baseline_metrics") private String baselineMetrics;
    /** 候选指标JSON。 */ @TableField("candidate_metrics") private String candidateMetrics;
    /** 退化明细JSON。 */ @TableField("regression_detail") private String regressionDetail;
    /** 创建用户ID。 */ @TableField("created_by") private String createdBy;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getWorkspaceId(){return workspaceId;} public void setWorkspaceId(String value){workspaceId=value;}
    public String getBaselineId(){return baselineId;} public void setBaselineId(String value){baselineId=value;}
    public String getCandidateTaskId(){return candidateTaskId;} public void setCandidateTaskId(String value){candidateTaskId=value;}
    public String getResourceType(){return resourceType;} public void setResourceType(String value){resourceType=value;}
    public String getResourceId(){return resourceId;} public void setResourceId(String value){resourceId=value;}
    public String getTargetVersion(){return targetVersion;} public void setTargetVersion(String value){targetVersion=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public String getBaselineMetrics(){return baselineMetrics;} public void setBaselineMetrics(String value){baselineMetrics=value;}
    public String getCandidateMetrics(){return candidateMetrics;} public void setCandidateMetrics(String value){candidateMetrics=value;}
    public String getRegressionDetail(){return regressionDetail;} public void setRegressionDetail(String value){regressionDetail=value;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String value){createdBy=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
}
