package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 工具确认请求表。
 * <p>对应数据库表：tool_confirm_request。</p>
 */
@TableName("tool_confirm_request")
public class ToolConfirmRequestEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 工作空间ID。 */
    @TableField("workspace_id")
    private String workspaceId;

    /** 工具ID。 */
    @TableField("tool_id")
    private String toolId;

    /** REQUESTER用户ID。 */
    @TableField("requester_user_id")
    private String requesterUserId;

    /** 字段说明：AgentID。 */
    @TableField("agent_id")
    private String agentId;

    /** 运行ID。 */
    @TableField("run_id")
    private String runId;

    /** 请求载荷。 */
    @TableField("request_payload")
    private String requestPayload;

    /** 字段说明：REASON。 */
    @TableField("reason")
    private String reason;

    /** 状态。 */
    @TableField("status")
    private String status;

    /** 一次性执行令牌哈希。 */
    @TableField("approval_token_hash")
    private String approvalTokenHash;

    /** 一次性执行令牌失效时间。 */
    @TableField("approval_token_expires_at")
    private LocalDateTime approvalTokenExpiresAt;

    /** 一次性执行令牌使用时间。 */
    @TableField("approval_token_used_at")
    private LocalDateTime approvalTokenUsedAt;

    /** CONFIRMED人。 */
    @TableField("confirmed_by")
    private String confirmedBy;

    /** CONFIRMED时间。 */
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    /** 过期时间。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkspaceId() { return workspaceId; }

    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }

    public String getToolId() {
        return toolId;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public String getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(String requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovalTokenHash() { return approvalTokenHash; }

    public void setApprovalTokenHash(String approvalTokenHash) { this.approvalTokenHash = approvalTokenHash; }

    public LocalDateTime getApprovalTokenExpiresAt() { return approvalTokenExpiresAt; }

    public void setApprovalTokenExpiresAt(LocalDateTime approvalTokenExpiresAt) { this.approvalTokenExpiresAt = approvalTokenExpiresAt; }

    public LocalDateTime getApprovalTokenUsedAt() { return approvalTokenUsedAt; }

    public void setApprovalTokenUsedAt(LocalDateTime approvalTokenUsedAt) { this.approvalTokenUsedAt = approvalTokenUsedAt; }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
