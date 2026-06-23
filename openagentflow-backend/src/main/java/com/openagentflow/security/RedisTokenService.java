package com.openagentflow.security;

import com.openagentflow.config.OpenAgentFlowProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 令牌状态服务。
 *
 * <p>JWT 本身无状态，Redis 用于保存令牌是否仍然有效，从而支持退出登录和服务端主动失效。</p>
 */
@Service
public class RedisTokenService {

    /** Redis 字符串模板。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** OpenAgentFlow 自定义配置。 */
    private final OpenAgentFlowProperties properties;

    public RedisTokenService(StringRedisTemplate stringRedisTemplate, OpenAgentFlowProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    /**
     * 保存登录令牌。
     *
     * @param tokenId 令牌ID
     * @param userId 用户ID
     */
    public void saveToken(String tokenId, String userId) {
        String key = tokenKey(tokenId);
        Duration ttl = Duration.ofMinutes(properties.getSecurity().getJwtExpireMinutes());
        // Redis 中只保存 tokenId 与 userId 的映射，不保存完整 JWT，降低泄露风险。
        stringRedisTemplate.opsForValue().set(key, userId, ttl);
    }

    /**
     * 判断令牌是否有效。
     *
     * @param tokenId 令牌ID
     * @param userId 用户ID
     * @return 是否有效
     */
    public boolean isTokenValid(String tokenId, String userId) {
        String storedUserId = stringRedisTemplate.opsForValue().get(tokenKey(tokenId));
        return userId.equals(storedUserId);
    }

    /**
     * 删除登录令牌。
     *
     * @param tokenId 令牌ID
     */
    public void revokeToken(String tokenId) {
        stringRedisTemplate.delete(tokenKey(tokenId));
    }

    /**
     * 生成 Redis key。
     *
     * @param tokenId 令牌ID
     * @return Redis key
     */
    private String tokenKey(String tokenId) {
        return "oaf:auth:token:" + tokenId;
    }
}
