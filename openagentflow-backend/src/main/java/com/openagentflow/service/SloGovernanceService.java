package com.openagentflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 平台SLO计算、违规归集与恢复服务。 */
@Service
public class SloGovernanceService {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    public SloGovernanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 按策略窗口计算真实业务数据并维护违规状态。 */
    @Scheduled(fixedDelayString = "${openagentflow.slo.evaluate-ms:60000}")
    public void evaluate() {
        List<Map<String, Object>> policies = jdbcTemplate.queryForList("SELECT * FROM platform_slo_policy WHERE enabled=1");
        for (Map<String, Object> policy : policies) {
            double observed = observed(String.valueOf(policy.get("policy_code")), ((Number) policy.get("window_minutes")).intValue());
            double threshold = ((Number) policy.get("threshold_value")).doubleValue();
            boolean passed = compare(observed, threshold, String.valueOf(policy.get("comparator")));
            String policyId = String.valueOf(policy.get("id"));
            if (passed) {
                jdbcTemplate.update("UPDATE platform_slo_violation SET status='resolved',resolved_at=NOW(3),last_seen_at=NOW(3) WHERE policy_id=? AND status IN ('open','acknowledged')", policyId);
            } else {
                Long open = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM platform_slo_violation WHERE policy_id=? AND status='open'", Long.class, policyId);
                if (open != null && open > 0) {
                    jdbcTemplate.update("UPDATE platform_slo_violation SET observed_value=?,last_seen_at=NOW(3) WHERE policy_id=? AND status='open'", observed, policyId);
                } else {
                    jdbcTemplate.update("""
                            INSERT INTO platform_slo_violation
                              (id,policy_id,workspace_id,observed_value,threshold_value,status,detail_json,first_seen_at,last_seen_at,created_at)
                            VALUES (?,?,? ,?,?,'open',JSON_OBJECT('policyCode',?,'comparator',?),NOW(3),NOW(3),NOW(3))
                            """, UUID.randomUUID().toString(), policyId, policy.get("workspace_id"), observed, threshold,
                            policy.get("policy_code"), policy.get("comparator"));
                }
            }
        }
    }

    /** 返回SLO策略及当前违规。 */
    public Map<String, Object> overview() {
        return Map.of(
                "policies", jdbcTemplate.queryForList("SELECT * FROM platform_slo_policy ORDER BY severity,policy_code"),
                "violations", jdbcTemplate.queryForList("SELECT v.*,p.policy_name,p.metric_name FROM platform_slo_violation v JOIN platform_slo_policy p ON p.id=v.policy_id WHERE v.status<>'resolved' ORDER BY v.last_seen_at DESC LIMIT 100"));
    }

    private double observed(String code, int windowMinutes) {
        return switch (code) {
            case "runtime_success_rate" -> ratio("SELECT SUM(status IN ('SUCCESS','success')) ok,COUNT(1) total FROM runtime_run WHERE created_at>=DATE_SUB(NOW(),INTERVAL ? MINUTE)", windowMinutes);
            case "document_success_rate" -> ratio("SELECT SUM(status='success') ok,COUNT(1) total FROM async_task WHERE task_type='DOCUMENT_PIPELINE_FINALIZE' AND created_at>=DATE_SUB(NOW(),INTERVAL ? MINUTE)", windowMinutes);
            case "outbox_oldest_age" -> number("SELECT COALESCE(TIMESTAMPDIFF(SECOND,MIN(created_at),NOW()),0) FROM async_task_outbox WHERE status IN ('pending','failed','sending')");
            case "first_token_p95" -> number("SELECT COALESCE(MAX(latency_ms),0) FROM runtime_llm_call WHERE created_at>=DATE_SUB(NOW(),INTERVAL " + Math.max(1, windowMinutes) + " MINUTE)");
            default -> 0D;
        };
    }

    private double ratio(String sql, int minutes) {
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, Math.max(1, minutes));
        double total = row.get("total") instanceof Number n ? n.doubleValue() : 0D;
        double ok = row.get("ok") instanceof Number n ? n.doubleValue() : 0D;
        return total <= 0 ? 1D : ok / total;
    }

    private double number(String sql) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class);
        return value == null ? 0D : value.doubleValue();
    }

    private boolean compare(double observed, double threshold, String comparator) {
        return switch (comparator) {
            case "gt" -> observed > threshold;
            case "gte" -> observed >= threshold;
            case "lt" -> observed < threshold;
            default -> observed <= threshold;
        };
    }
}
