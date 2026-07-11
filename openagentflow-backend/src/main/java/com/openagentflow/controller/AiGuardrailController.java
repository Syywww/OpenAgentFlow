package com.openagentflow.controller;

import com.openagentflow.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** AI安全护栏策略与事件查询接口。 */
@RestController
@RequestMapping("/governance/ai-guardrails")
public class AiGuardrailController {

    /** JDBC工具。 */
    private final JdbcTemplate jdbcTemplate;

    public AiGuardrailController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 查询启用的AI护栏策略。 */
    @GetMapping("/policies")
    public ApiResponse<List<Map<String, Object>>> policies() {
        return ApiResponse.ok(jdbcTemplate.queryForList("SELECT * FROM ai_guardrail_policy ORDER BY priority,created_at"));
    }

    /** 查询最近护栏命中事件。 */
    @GetMapping("/events")
    public ApiResponse<List<Map<String, Object>>> events() {
        return ApiResponse.ok(jdbcTemplate.queryForList("SELECT * FROM ai_guardrail_event ORDER BY created_at DESC LIMIT 200"));
    }
}
