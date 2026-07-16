package com.openagentflow.security;

import java.util.Locale;
import java.util.Set;

/** MyBatis强制租户条件表策略。 */
public final class TenantIsolationPolicy {

    /** 已具备直接workspace_id字段且由请求层访问的核心业务表。 */
    private static final Set<String> GOVERNED_TABLES = Set.of(
            "agent", "agent_memory", "async_task", "knowledge_base", "mcp_server",
            "release_gate_execution", "risk_governance_event", "runtime_run",
            "tenant_resource_quota", "tenant_resource_reservation", "tool_definition",
            "workflow_definition", "ai_guardrail_event", "memory_feedback",
            "memory_governance_issue", "memory_policy", "memory_access_metric",
            "eval_task", "prompt_template", "tool_confirm_request",
            "data_consistency_issue", "data_lifecycle_job", "knowledge_index_version",
            "platform_security_event", "platform_slo_violation", "evaluation_baseline",
            "evaluation_regression", "file_security_scan", "privacy_consent",
            "pii_data_subject_request", "tenant_isolation_audit");

    private TenantIsolationPolicy() { }

    /** 判断指定表是否必须由SQL拦截器追加工作空间条件。 */
    public static boolean requiresTenantCondition(String tableName) {
        return tableName != null && GOVERNED_TABLES.contains(tableName.toLowerCase(Locale.ROOT));
    }
}
