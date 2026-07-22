package com.openagentflow.security;

import java.time.Instant;

/**
 * 资源 ACL 生命周期策略。
 */
public final class ResourceAclPolicy {

    private ResourceAclPolicy() {
    }

    /**
     * 判断授权是否处于有效状态。
     *
     * @param status 授权状态
     * @param expiresAt 到期时间
     * @param now 当前时间
     * @return 是否有效
     */
    public static boolean isActive(String status, Instant expiresAt, Instant now) {
        if (!"enabled".equalsIgnoreCase(status) || now == null) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(now);
    }
}
