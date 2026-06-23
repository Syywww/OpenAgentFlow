package com.openagentflow.domain.auth;

import java.time.LocalDateTime;

/**
 * 登录响应对象。
 */
public class LoginResponse {

    /** JWT 访问令牌。 */
    private String accessToken;

    /** 令牌类型。 */
    private String tokenType;

    /** 令牌过期时间。 */
    private LocalDateTime expiresAt;

    /** 当前登录用户。 */
    private CurrentUser currentUser;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public CurrentUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }
}
