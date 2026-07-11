package com.openagentflow.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 平台敏感配置AES-256-GCM加解密服务。
 *
 * <p>生产环境应由KMS或Secret注入主密钥；开发环境未配置时兼容读取历史明文。</p>
 */
@Service
public class SecretCryptoService {

    /** 密文版本前缀。 */
    private static final String PREFIX = "enc:v1:";

    /** 安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 主密钥摘要。 */
    private final byte[] key;

    public SecretCryptoService(@Value("${openagentflow.security.secret-encryption-key:}") String masterKey) {
        this.key = StringUtils.hasText(masterKey) ? sha256(masterKey) : null;
    }

    /** 加密敏感文本。 */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText) || key == null || plainText.startsWith(PREFIX)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("敏感配置加密失败", exception);
        }
    }

    /** 解密敏感文本并兼容历史明文。 */
    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText) || !cipherText.startsWith(PREFIX)) {
            return cipherText == null ? "" : cipherText;
        }
        if (key == null) {
            throw new IllegalStateException("检测到加密密文，但未配置OAF_SECRET_ENCRYPTION_KEY");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("敏感配置解密失败", exception);
        }
    }

    private byte[] sha256(String text) {
        try { return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)); }
        catch (Exception exception) { throw new IllegalStateException("主密钥初始化失败", exception); }
    }
}
