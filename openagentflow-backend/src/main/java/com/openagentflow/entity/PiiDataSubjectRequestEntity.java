package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** PII数据主体权利请求实体。 */
@TableName("pii_data_subject_request")
public class PiiDataSubjectRequestEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 申请用户ID。 */ @TableField("requester_user_id") private String requesterUserId;
    /** 请求类型。 */ @TableField("request_type") private String requestType;
    /** 处理状态。 */ @TableField("status") private String status;
    /** 数据范围JSON。 */ @TableField("scope_json") private String scopeJson;
    /** 导出结果地址。 */ @TableField("result_uri") private String resultUri;
    /** 错误信息。 */ @TableField("error_message") private String errorMessage;
    /** 审批用户ID。 */ @TableField("approved_by") private String approvedBy;
    /** 审批时间。 */ @TableField("approved_at") private LocalDateTime approvedAt;
    /** 完成时间。 */ @TableField("completed_at") private LocalDateTime completedAt;
    /** 制品失效时间。 */ @TableField("expires_at") private LocalDateTime expiresAt;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getWorkspaceId(){return workspaceId;} public void setWorkspaceId(String value){workspaceId=value;}
    public String getRequesterUserId(){return requesterUserId;} public void setRequesterUserId(String value){requesterUserId=value;}
    public String getRequestType(){return requestType;} public void setRequestType(String value){requestType=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public String getScopeJson(){return scopeJson;} public void setScopeJson(String value){scopeJson=value;}
    public String getResultUri(){return resultUri;} public void setResultUri(String value){resultUri=value;}
    public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String value){errorMessage=value;}
    public String getApprovedBy(){return approvedBy;} public void setApprovedBy(String value){approvedBy=value;}
    public LocalDateTime getApprovedAt(){return approvedAt;} public void setApprovedAt(LocalDateTime value){approvedAt=value;}
    public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime value){completedAt=value;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
