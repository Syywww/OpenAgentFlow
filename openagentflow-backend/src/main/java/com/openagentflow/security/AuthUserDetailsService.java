package com.openagentflow.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openagentflow.entity.IamUserEntity;
import com.openagentflow.mapper.IamPermissionMapper;
import com.openagentflow.mapper.IamRoleMapper;
import com.openagentflow.mapper.IamUserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 用户加载服务。
 */
@Service
public class AuthUserDetailsService implements UserDetailsService {

    /** 用户 Mapper。 */
    private final IamUserMapper iamUserMapper;

    /** 角色 Mapper。 */
    private final IamRoleMapper iamRoleMapper;

    /** 权限 Mapper。 */
    private final IamPermissionMapper iamPermissionMapper;

    public AuthUserDetailsService(IamUserMapper iamUserMapper,
                                  IamRoleMapper iamRoleMapper,
                                  IamPermissionMapper iamPermissionMapper) {
        this.iamUserMapper = iamUserMapper;
        this.iamRoleMapper = iamRoleMapper;
        this.iamPermissionMapper = iamPermissionMapper;
    }

    /**
     * 根据用户名加载认证用户。
     *
     * @param username 用户名
     * @return Spring Security 用户详情
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        IamUserEntity user = iamUserMapper.selectOne(new LambdaQueryWrapper<IamUserEntity>()
                .eq(IamUserEntity::getUsername, username)
                .isNull(IamUserEntity::getDeletedAt)
                .last("limit 1"));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        return buildUserDetails(user);
    }

    /**
     * 根据用户ID加载异步任务创建人，用于 Kafka Worker 恢复资源权限上下文。
     *
     * @param userId 用户ID
     * @return 认证用户详情
     */
    public AuthUserDetails loadUserById(String userId) {
        IamUserEntity user = iamUserMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new UsernameNotFoundException("异步任务创建用户不存在");
        }
        return buildUserDetails(user);
    }

    /**
     * 为用户加载角色和权限集合。
     */
    private AuthUserDetails buildUserDetails(IamUserEntity user) {
        // 角色统一加 ROLE_ 前缀，权限编码保持原值，兼容 Spring Security 的角色判断习惯。
        List<GrantedAuthority> authorities = new ArrayList<>();
        iamRoleMapper.selectRoleCodesByUserId(user.getId())
                .forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        iamPermissionMapper.selectPermissionCodesByUserId(user.getId())
                .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return new AuthUserDetails(user, authorities);
    }
}
