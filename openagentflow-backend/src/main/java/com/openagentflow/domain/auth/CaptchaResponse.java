package com.openagentflow.domain.auth;

/**
 * 图形验证码响应对象。
 */
public class CaptchaResponse {

    /** 验证码唯一标识，登录时需要原样提交。 */
    private String captchaKey;

    /** Base64 图片数据，前端可直接赋值给 img 的 src。 */
    private String imageBase64;

    /** 验证码过期秒数。 */
    private Long expireSeconds;

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public Long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(Long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
