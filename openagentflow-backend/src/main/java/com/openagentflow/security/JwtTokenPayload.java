package com.openagentflow.security;

import java.time.Instant;

/**
 * JWT 载荷对象。
 */
public class JwtTokenPayload {

    /** 令牌唯一ID。 */
    private String tokenId;

    /** 用户ID。 */
    private String userId;

    /** 用户名。 */
    private String username;

    /** 过期时间。 */
    private Instant expiresAt;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
