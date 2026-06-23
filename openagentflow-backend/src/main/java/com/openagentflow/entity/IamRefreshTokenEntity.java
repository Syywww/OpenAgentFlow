package com.openagentflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 权限刷新令牌表。
 * <p>对应数据库表：iam_refresh_token。</p>
 */
@TableName("iam_refresh_token")
public class IamRefreshTokenEntity {

    /** 主键ID。 */
    @TableId(value = "id")
    private String id;

    /** 用户ID。 */
    @TableField("user_id")
    private String userId;

    /** 令牌哈希。 */
    @TableField("token_hash")
    private String tokenHash;

    /** 用户Agent。 */
    @TableField("user_agent")
    private String userAgent;

    /** 客户端IP。 */
    @TableField("client_ip")
    private String clientIp;

    /** 撤销。 */
    @TableField("revoked")
    private Boolean revoked;

    /** 签发时间。 */
    @TableField("issued_at")
    private LocalDateTime issuedAt;

    /** 过期时间。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /** 撤销时间。 */
    @TableField("revoked_at")
    private LocalDateTime revokedAt;

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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
