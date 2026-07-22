package com.openagentflow.security;

import java.util.Collection;
import java.util.Set;

/**
 * 平台级管理员权限策略。
 *
 * <p>模块管理权限只在当前授权范围内生效，不能隐式升级为跨工作空间的平台管理员。</p>
 */
public final class PlatformAuthorityPolicy {

    /** 允许跨工作空间执行平台管理的角色。 */
    private static final Set<String> PLATFORM_MANAGER_AUTHORITIES = Set.of(
            "ROLE_super_admin",
            "ROLE_admin"
    );

    private PlatformAuthorityPolicy() {
    }

    /**
     * 判断权限集合是否包含平台管理员角色。
     *
     * @param authorities 权限编码集合
     * @return 是否为平台管理员
     */
    public static boolean isPlatformManager(Collection<String> authorities) {
        return authorities != null && authorities.stream().anyMatch(PLATFORM_MANAGER_AUTHORITIES::contains);
    }
}
