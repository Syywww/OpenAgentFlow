package com.openagentflow.security;

import com.openagentflow.entity.IamUserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 当前认证用户。
 */
public class AuthUserDetails implements UserDetails {

    /** 用户数据库实体。 */
    private final IamUserEntity user;

    /** 用户权限集合。 */
    private final List<GrantedAuthority> authorities;

    public AuthUserDetails(IamUserEntity user, List<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public IamUserEntity getUser() {
        return user;
    }

    public String getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "enabled".equalsIgnoreCase(user.getStatus()) && user.getDeletedAt() == null;
    }
}
