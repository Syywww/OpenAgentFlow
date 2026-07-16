package com.openagentflow.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** 深分页使用的HMAC签名游标编解码器。 */
public class SignedCursorCodec {

    /** 游标签名密钥。 */
    private final byte[] secret;

    public SignedCursorCodec(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("游标签名密钥长度不能少于32个字符");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** 编码排序值和主键并追加不可伪造签名。 */
    public String encode(String sortValue, String id) {
        String payload = safe(sortValue) + "\n" + safe(id);
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(body);
    }

    /** 校验签名并解析游标。 */
    public Cursor decode(String cursor) {
        try {
            String[] parts = cursor == null ? new String[0] : cursor.split("\\.", 2);
            if (parts.length != 2 || !MessageDigest.isEqual(
                    sign(parts[0]).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("分页游标签名无效");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split("\\n", 2);
            if (values.length != 2) throw new IllegalArgumentException("分页游标格式无效");
            return new Cursor(values[0], values[1]);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("分页游标解析失败", exception);
        }
    }

    /** 计算HMAC-SHA256签名。 */
    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("分页游标签名失败", exception);
        }
    }

    private String safe(String value) { return value == null ? "" : value; }

    /**
     * 游标内容。
     *
     * @param sortValue 排序字段值
     * @param id 主键ID
     */
    public record Cursor(String sortValue, String id) { }
}
