package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import com.openagentflow.domain.auth.CaptchaResponse;
import com.openagentflow.domain.auth.CurrentUser;
import com.openagentflow.domain.auth.LoginRequest;
import com.openagentflow.domain.auth.LoginResponse;
import com.openagentflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证接口。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** 登录认证服务。 */
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 获取登录图形验证码。
     *
     * @return 图形验证码响应
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        // 验证码答案会写入 Redis，前端只拿到验证码标识和图片。
        return ApiResponse.ok(authService.captcha());
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 登录前会先校验 Redis 图形验证码，成功后再生成 JWT 并写入 Redis token 状态。
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 查询当前登录用户。
     *
     * @return 当前登录用户
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUser> me() {
        // 当前用户来自 Spring Security 上下文，已通过 JWT 和 Redis 双重校验。
        return ApiResponse.ok(authService.currentUser());
    }

    /**
     * 退出登录。
     *
     * @param authorization Authorization 请求头
     * @return 空响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
        }
        return ApiResponse.ok(null);
    }
}
