package com.openagentflow.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.config.OpenAgentFlowProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 令牌服务。
 *
 * <p>这里使用 HS256 自行签名，减少额外 JWT 依赖，同时保持标准 JWT 三段式结构。</p>
 */
@Service
public class JwtTokenService {

    /** JSON 序列化工具。 */
    private final ObjectMapper objectMapper;

    /** OpenAgentFlow 自定义配置。 */
    private final OpenAgentFlowProperties properties;

    public JwtTokenService(ObjectMapper objectMapper, OpenAgentFlowProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 为认证用户创建 JWT。
     *
     * @param userDetails 当前认证用户
     * @return JWT 字符串
     */
    public String createToken(AuthUserDetails userDetails) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(properties.getSecurity().getJwtExpireMinutes() * 60);
            String tokenId = UUID.randomUUID().toString();

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("jti", tokenId);
            payload.put("sub", userDetails.getUserId());
            payload.put("username", userDetails.getUsername());
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", expiresAt.getEpochSecond());

            // JWT 前两段分别是 header 和 payload 的 Base64Url 编码。
            String encodedHeader = base64Url(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
            String content = encodedHeader + "." + encodedPayload;
            String signature = sign(content);
            return content + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("创建 JWT 失败", exception);
        }
    }

    /**
     * 解析并校验 JWT。
     *
     * @param token JWT 字符串
     * @return JWT 载荷
     */
    public JwtTokenPayload parseAndValidate(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("JWT 不能为空");
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT 格式不正确");
            }

            // 重新计算签名并做常量时间比较，避免令牌被篡改。
            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("JWT 签名不正确");
            }

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            Instant expiresAt = Instant.ofEpochSecond(payload.get("exp").asLong());
            if (Instant.now().isAfter(expiresAt)) {
                throw new IllegalArgumentException("JWT 已过期");
            }

            JwtTokenPayload result = new JwtTokenPayload();
            result.setTokenId(payload.get("jti").asText());
            result.setUserId(payload.get("sub").asText());
            result.setUsername(payload.get("username").asText());
            result.setExpiresAt(expiresAt);
            return result;
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT 校验失败", exception);
        }
    }

    /**
     * 生成 HMAC-SHA256 签名。
     *
     * @param content 待签名内容
     * @return Base64Url 签名
     */
    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(
                properties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(key);
        return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Base64Url 编码。
     *
     * @param bytes 原始字节
     * @return 不带等号填充的 Base64Url 字符串
     */
    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 常量时间字符串比较。
     *
     * @param left 左侧字符串
     * @param right 右侧字符串
     * @return 是否相等
     */
    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            // 使用异或累积比较结果，避免提前返回暴露长度外的比较信息。
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }
}
