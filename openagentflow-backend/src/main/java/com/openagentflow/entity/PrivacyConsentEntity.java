package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** PII数据处理同意实体。 */
@TableName("privacy_consent")
public class PrivacyConsentEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 数据主体用户ID。 */ @TableField("user_id") private String userId;
    /** 处理目的编码。 */ @TableField("purpose_code") private String purposeCode;
    /** 条款版本。 */ @TableField("consent_version") private String consentVersion;
    /** 同意状态。 */ @TableField("status") private String status;
    /** 同意时间。 */ @TableField("granted_at") private LocalDateTime grantedAt;
    /** 撤回时间。 */ @TableField("withdrawn_at") private LocalDateTime withdrawnAt;
    /** 失效时间。 */ @TableField("expires_at") private LocalDateTime expiresAt;
    /** 同意证据JSON。 */ @TableField("evidence_json") private String evidenceJson;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getWorkspaceId(){return workspaceId;} public void setWorkspaceId(String value){workspaceId=value;}
    public String getUserId(){return userId;} public void setUserId(String value){userId=value;}
    public String getPurposeCode(){return purposeCode;} public void setPurposeCode(String value){purposeCode=value;}
    public String getConsentVersion(){return consentVersion;} public void setConsentVersion(String value){consentVersion=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public LocalDateTime getGrantedAt(){return grantedAt;} public void setGrantedAt(LocalDateTime value){grantedAt=value;}
    public LocalDateTime getWithdrawnAt(){return withdrawnAt;} public void setWithdrawnAt(LocalDateTime value){withdrawnAt=value;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
    public String getEvidenceJson(){return evidenceJson;} public void setEvidenceJson(String value){evidenceJson=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
