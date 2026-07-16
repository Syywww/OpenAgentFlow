package com.openagentflow.service;

import com.openagentflow.security.SensitiveDataSanitizer;
import com.openagentflow.security.TenantIsolationPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** P67-P72生产治理核心规则单元测试。 */
class ProductionGovernanceRulesTests {

    /** 关键AI指标低于黄金基线容忍值时必须阻断发布。 */
    @Test
    void evaluationRegressionShouldBlockRelease() {
        Map<String, Double> baseline = Map.of("rag_recall_at_k", 0.90D, "tool_false_call_rate", 0.02D);
        Map<String, Double> candidate = Map.of("rag_recall_at_k", 0.80D, "tool_false_call_rate", 0.08D);
        EvaluationRegressionPolicy.Result result = EvaluationRegressionPolicy.compare(baseline, candidate, 0.05D);
        assertFalse(result.passed());
        assertEquals(2, result.regressions().size());
    }

    /** 密钥、Bearer令牌、手机号和身份证不能进入日志或Trace明文。 */
    @Test
    void sensitiveDataShouldBeRedactedRecursively() {
        SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();
        String text = sanitizer.sanitize("key=ark-1234567890abcdefghijkl Bearer abcdefghijklmnopqrstuvwxyz 手机13800138000");
        assertFalse(text.contains("ark-123"));
        assertFalse(text.contains("abcdefghijklmnop"));
        assertFalse(text.contains("13800138000"));
        assertTrue(text.contains("***"));
    }

    /** 深分页游标必须带签名且被篡改后拒绝解析。 */
    @Test
    void signedCursorShouldRejectTampering() {
        SignedCursorCodec codec = new SignedCursorCodec("cursor-test-secret-at-least-32-bytes");
        String cursor = codec.encode("2026-07-13T10:00:00", "id-100");
        assertEquals("id-100", codec.decode(cursor).id());
        assertThrows(IllegalArgumentException.class,
                () -> codec.decode(cursor.substring(0, cursor.length() - 1) + "x"));
    }

    /** 核心租户表必须由MyBatis拦截器追加workspace_id条件。 */
    @Test
    void coreTenantTablesShouldBeGoverned() {
        assertTrue(TenantIsolationPolicy.requiresTenantCondition("agent"));
        assertTrue(TenantIsolationPolicy.requiresTenantCondition("runtime_run"));
        assertFalse(TenantIsolationPolicy.requiresTenantCondition("iam_user"));
        assertFalse(TenantIsolationPolicy.requiresTenantCondition("sys_config"));
    }
}
