package com.openagentflow.service;

import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** 高风险工具一次性执行令牌服务。 */
@Service
public class ToolApprovalTokenService {

    /** 安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();
    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;

    public ToolApprovalTokenService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 审批通过后签发十分钟有效的一次性令牌，数据库只保存哈希。 */
    @Transactional
    public String issue(String confirmationId) {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        int updated = jdbcTemplate.update("""
                UPDATE tool_confirm_request SET approval_token_hash=?,approval_token_expires_at=DATE_ADD(NOW(3),INTERVAL 10 MINUTE),
                  approval_token_used_at=NULL WHERE id=? AND status='approved'
                """, hash(token), confirmationId);
        if (updated != 1) throw new BusinessException("TOOL_APPROVAL_INVALID", "确认请求尚未审批通过");
        return token;
    }

    /** 原子核销令牌，确保高风险动作最多执行一次。 */
    @Transactional
    public void consume(String confirmationId, String token) {
        if (token == null || token.isBlank()) throw new BusinessException("TOOL_APPROVAL_TOKEN_REQUIRED", "缺少高风险工具执行令牌");
        int updated = jdbcTemplate.update("""
                UPDATE tool_confirm_request SET approval_token_used_at=NOW(3),status='executing'
                WHERE id=? AND status='approved' AND approval_token_hash=? AND approval_token_used_at IS NULL
                  AND approval_token_expires_at>NOW(3)
                """, confirmationId, hash(token));
        if (updated != 1) throw new BusinessException("TOOL_APPROVAL_TOKEN_INVALID", "执行令牌无效、已过期或已使用");
    }

    /** 计算不可逆 SHA-256 哈希。 */
    private String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("执行令牌哈希失败", exception);
        }
    }
}
