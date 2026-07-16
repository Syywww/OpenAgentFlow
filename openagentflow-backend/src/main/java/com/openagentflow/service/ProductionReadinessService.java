package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 容量基线与灾备目标管理服务。 */
@Service
public class ProductionReadinessService {

    /** 数据库访问工具。 */
    private final JdbcTemplate jdbcTemplate;
    /** JSON 工具。 */
    private final ObjectMapper objectMapper;

    public ProductionReadinessService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** 保存 k6 或 Gatling 产出的容量基线。 */
    public Map<String, Object> saveCapacityBaseline(Map<String, Object> body) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO capacity_baseline
                  (id,scenario_code,environment_code,concurrency_level,request_rate,p50_latency_ms,p95_latency_ms,
                   p99_latency_ms,error_rate,saturation_json,dataset_scale_json,passed,measured_at,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON),?,NOW(3),NOW(3))
                """, id, text(body, "scenarioCode"), text(body, "environmentCode"), integer(body, "concurrencyLevel"),
                body.get("requestRate"), body.get("p50LatencyMs"), body.get("p95LatencyMs"), body.get("p99LatencyMs"),
                body.get("errorRate"), json(body.get("saturation")), json(body.get("datasetScale")),
                Boolean.TRUE.equals(body.get("passed")));
        return jdbcTemplate.queryForMap("SELECT * FROM capacity_baseline WHERE id=?", id);
    }

    /** 查询最近容量基线，支持不同并发档位横向比较。 */
    public List<Map<String, Object>> capacityBaselines() {
        return jdbcTemplate.queryForList("SELECT * FROM capacity_baseline ORDER BY measured_at DESC LIMIT 100");
    }

    /** 查询 MySQL、Redis、Kafka、对象存储和 Milvus 的 RPO/RTO 目标。 */
    public List<Map<String, Object>> disasterRecoveryTargets() {
        return jdbcTemplate.queryForList("SELECT * FROM disaster_recovery_target WHERE enabled=1 ORDER BY component_code");
    }

    private String text(Map<String, Object> body, String key) {
        String value = body.get(key) == null ? "" : String.valueOf(body.get(key));
        if (value.isBlank()) throw new BusinessException("CAPACITY_FIELD_REQUIRED", key + " 不能为空");
        return value;
    }

    private int integer(Map<String, Object> body, String key) {
        if (body.get(key) instanceof Number number) return number.intValue();
        throw new BusinessException("CAPACITY_FIELD_REQUIRED", key + " 必须为整数");
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value == null ? Map.of() : value); }
        catch (Exception exception) { throw new BusinessException("JSON_SERIALIZE_FAILED", "容量数据序列化失败"); }
    }
}
