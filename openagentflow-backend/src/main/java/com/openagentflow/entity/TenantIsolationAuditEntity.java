package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 跨存储租户隔离审计问题实体。 */
@TableName("tenant_isolation_audit")
public class TenantIsolationAuditEntity {
    /** 主键ID。 */ @TableId("id") private String id;
    /** 工作空间ID。 */ @TableField("workspace_id") private String workspaceId;
    /** 审计范围。 */ @TableField("audit_scope") private String auditScope;
    /** 资源类型。 */ @TableField("resource_type") private String resourceType;
    /** 资源ID。 */ @TableField("resource_id") private String resourceId;
    /** 隔离问题类型。 */ @TableField("issue_type") private String issueType;
    /** 严重级别。 */ @TableField("severity") private String severity;
    /** 问题证据JSON。 */ @TableField("evidence_json") private String evidenceJson;
    /** 处理状态。 */ @TableField("status") private String status;
    /** 发现时间。 */ @TableField("detected_at") private LocalDateTime detectedAt;
    /** 解决时间。 */ @TableField("resolved_at") private LocalDateTime resolvedAt;
    /** 创建时间。 */ @TableField("created_at") private LocalDateTime createdAt;
    /** 更新时间。 */ @TableField("updated_at") private LocalDateTime updatedAt;
    public String getId(){return id;} public void setId(String value){id=value;}
    public String getWorkspaceId(){return workspaceId;} public void setWorkspaceId(String value){workspaceId=value;}
    public String getAuditScope(){return auditScope;} public void setAuditScope(String value){auditScope=value;}
    public String getResourceType(){return resourceType;} public void setResourceType(String value){resourceType=value;}
    public String getResourceId(){return resourceId;} public void setResourceId(String value){resourceId=value;}
    public String getIssueType(){return issueType;} public void setIssueType(String value){issueType=value;}
    public String getSeverity(){return severity;} public void setSeverity(String value){severity=value;}
    public String getEvidenceJson(){return evidenceJson;} public void setEvidenceJson(String value){evidenceJson=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public LocalDateTime getDetectedAt(){return detectedAt;} public void setDetectedAt(LocalDateTime value){detectedAt=value;}
    public LocalDateTime getResolvedAt(){return resolvedAt;} public void setResolvedAt(LocalDateTime value){resolvedAt=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
}
