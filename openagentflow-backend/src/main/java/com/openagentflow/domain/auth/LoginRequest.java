package com.openagentflow.domain.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求对象。
 */
public class LoginRequest {

    /** 用户名。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码。 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 图形验证码唯一标识，用于从 Redis 中读取验证码答案。 */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 用户输入的图形验证码。 */
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    /** 是否记住登录状态。 */
    private Boolean rememberMe;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
