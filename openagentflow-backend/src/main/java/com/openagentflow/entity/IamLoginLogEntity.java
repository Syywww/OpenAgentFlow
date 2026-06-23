package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 权限登录日志表。
 * <p>对应数据库表：iam_login_log。</p>
 */
@TableName("iam_login_log")
public class IamLoginLogEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 用户ID。 */
    @TableField("user_id")
    private String userId;

    /** 用户名。 */
    @TableField("username")
    private String username;

    /** 登录类型。 */
    @TableField("login_type")
    private String loginType;

    /** 成功。 */
    @TableField("success")
    private Boolean success;

    /** 失败REASON。 */
    @TableField("failure_reason")
    private String failureReason;

    /** 客户端IP。 */
    @TableField("client_ip")
    private String clientIp;

    /** 用户Agent。 */
    @TableField("user_agent")
    private String userAgent;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
