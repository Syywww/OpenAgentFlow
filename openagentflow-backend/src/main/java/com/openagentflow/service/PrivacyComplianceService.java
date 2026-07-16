package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.exception.BusinessException;
import com.openagentflow.security.AuthUserDetails;
import com.openagentflow.security.WorkspaceContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** PII 同意、导出、遗忘和处理留痕服务。 */
@Service
public class PrivacyComplianceService {

    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    public PrivacyComplianceService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** 授予或更新指定处理目的的隐私同意。 */
    @Transactional
    public Map<String, Object> grantConsent(String purposeCode, String version, Map<String, Object> evidence) {
        String workspaceId = requireWorkspace();
        String userId = requireUser();
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO privacy_consent
                  (id,workspace_id,user_id,purpose_code,consent_version,status,granted_at,evidence_json,created_at,updated_at)
                VALUES (?,?,?,?,?,'granted',NOW(3),CAST(? AS JSON),NOW(3),NOW(3))
                ON DUPLICATE KEY UPDATE status='granted',granted_at=NOW(3),withdrawn_at=NULL,
                  evidence_json=VALUES(evidence_json),updated_at=NOW(3)
                """, id, workspaceId, userId, purposeCode, version, json(evidence));
        return jdbcTemplate.queryForMap("""
                SELECT * FROM privacy_consent WHERE workspace_id=? AND user_id=? AND purpose_code=? AND consent_version=?
                """, workspaceId, userId, purposeCode, version);
    }

    /** 撤回隐私同意，后续模型与记忆写入可据此阻断。 */
    public void withdrawConsent(String id) {
        int updated = jdbcTemplate.update("""
                UPDATE privacy_consent SET status='withdrawn',withdrawn_at=NOW(3),updated_at=NOW(3)
                WHERE id=? AND workspace_id=? AND user_id=? AND status='granted'
                """, id, requireWorkspace(), requireUser());
        if (updated != 1) throw new BusinessException("CONSENT_NOT_FOUND", "可撤回的隐私同意不存在");
    }

    /** 创建数据导出、遗忘、限制处理或更正申请。 */
    public Map<String, Object> createSubjectRequest(String requestType, Map<String, Object> scope) {
        if (!List.of("export", "forget", "restrict", "correct").contains(requestType)) {
            throw new BusinessException("PII_REQUEST_TYPE_INVALID", "不支持的数据主体请求类型");
        }
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO pii_data_subject_request
                  (id,workspace_id,requester_user_id,request_type,status,scope_json,created_at,updated_at)
                VALUES (?,?,?,?,'pending',CAST(? AS JSON),NOW(3),NOW(3))
                """, id, requireWorkspace(), requireUser(), requestType, json(scope));
        return jdbcTemplate.queryForMap("SELECT * FROM pii_data_subject_request WHERE id=?", id);
    }

    /** 查询当前用户的数据主体申请。 */
    public List<Map<String, Object>> subjectRequests() {
        return jdbcTemplate.queryForList("""
                SELECT * FROM pii_data_subject_request WHERE workspace_id=? AND requester_user_id=?
                ORDER BY created_at DESC LIMIT 100
                """, requireWorkspace(), requireUser());
    }

    private String requireWorkspace() {
        String value = WorkspaceContextHolder.current();
        if (value == null || value.isBlank()) throw new BusinessException("WORKSPACE_REQUIRED", "请选择工作空间");
        return value;
    }

    private String requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails user) return user.getUserId();
        throw new BusinessException("UNAUTHORIZED", "请先登录");
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value == null ? Map.of() : value); }
        catch (Exception exception) { throw new BusinessException("JSON_SERIALIZE_FAILED", "合规请求序列化失败"); }
    }
}
