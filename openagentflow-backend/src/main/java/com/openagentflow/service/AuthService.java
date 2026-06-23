package com.openagentflow.service;

import com.openagentflow.domain.auth.CaptchaResponse;
import com.openagentflow.domain.auth.CurrentUser;
import com.openagentflow.domain.auth.LoginRequest;
import com.openagentflow.domain.auth.LoginResponse;
import com.openagentflow.entity.IamUserEntity;
import com.openagentflow.mapper.IamPermissionMapper;
import com.openagentflow.mapper.IamRoleMapper;
import com.openagentflow.mapper.IamUserMapper;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.JwtTokenPayload;
import com.openagentflow.security.JwtTokenService;
import com.openagentflow.security.RedisCaptchaService;
import com.openagentflow.security.RedisTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 登录认证服务。
 */
@Service
public class AuthService {

    /** 认证管理器。 */
    private final AuthenticationManager authenticationManager;

    /** JWT 服务。 */
    private final JwtTokenService jwtTokenService;

    /** Redis token 状态服务。 */
    private final RedisTokenService redisTokenService;

    /** Redis 图形验证码服务。 */
    private final RedisCaptchaService redisCaptchaService;

    /** 用户 Mapper。 */
    private final IamUserMapper iamUserMapper;

    /** 角色 Mapper。 */
    private final IamRoleMapper iamRoleMapper;

    /** 权限 Mapper。 */
    private final IamPermissionMapper iamPermissionMapper;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenService jwtTokenService,
                       RedisTokenService redisTokenService,
                       RedisCaptchaService redisCaptchaService,
                       IamUserMapper iamUserMapper,
                       IamRoleMapper iamRoleMapper,
                       IamPermissionMapper iamPermissionMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.redisTokenService = redisTokenService;
        this.redisCaptchaService = redisCaptchaService;
        this.iamUserMapper = iamUserMapper;
        this.iamRoleMapper = iamRoleMapper;
        this.iamPermissionMapper = iamPermissionMapper;
    }

    /**
     * 生成登录图形验证码。
     *
     * @return 图形验证码响应
     */
    public CaptchaResponse captcha() {
        return redisCaptchaService.createCaptcha();
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    public LoginResponse login(LoginRequest request) {
        // 先校验 Redis 中的一次性图形验证码，再进入用户名密码认证。
        redisCaptchaService.validateCaptcha(request.getCaptchaKey(), request.getCaptcha());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        AuthUserDetails userDetails = (AuthUserDetails) authentication.getPrincipal();

        String token = jwtTokenService.createToken(userDetails);
        JwtTokenPayload payload = jwtTokenService.parseAndValidate(token);
        // 登录成功后把 tokenId 写入 Redis，后续请求必须同时通过 JWT 和 Redis 校验。
        redisTokenService.saveToken(payload.getTokenId(), payload.getUserId());

        IamUserEntity user = userDetails.getUser();
        user.setLastLoginAt(LocalDateTime.now());
        // 登录成功后更新最后登录时间，方便安全审计和后台展示。
        iamUserMapper.updateById(user);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresAt(LocalDateTime.ofInstant(payload.getExpiresAt(), ZoneId.systemDefault()));
        response.setCurrentUser(buildCurrentUser(user));
        return response;
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前用户
     */
    public CurrentUser currentUser() {
        AuthUserDetails userDetails = (AuthUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return buildCurrentUser(userDetails.getUser());
    }

    /**
     * 退出登录。
     *
     * @param token 当前请求 JWT
     */
    public void logout(String token) {
        JwtTokenPayload payload = jwtTokenService.parseAndValidate(token);
        // 删除 Redis 中的 token 状态，JWT 即使未过期也会立即失效。
        redisTokenService.revokeToken(payload.getTokenId());
        SecurityContextHolder.clearContext();
    }

    /**
     * 构建当前用户视图对象。
     *
     * @param user 用户实体
     * @return 当前用户对象
     */
    private CurrentUser buildCurrentUser(IamUserEntity user) {
        List<String> roles = iamRoleMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = iamPermissionMapper.selectPermissionCodesByUserId(user.getId());

        CurrentUser currentUser = new CurrentUser();
        currentUser.setId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setDisplayName(user.getDisplayName());
        currentUser.setEmail(user.getEmail());
        currentUser.setAvatarUrl(user.getAvatarUrl());
        currentUser.setRoles(roles);
        currentUser.setPermissions(permissions);
        return currentUser;
    }
}
