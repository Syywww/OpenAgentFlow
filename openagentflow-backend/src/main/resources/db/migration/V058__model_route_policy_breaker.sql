USE openagentflow;

-- 模型网关熔断器配置（Phase 10）。
-- 为 model_route_policy 增加熔断参数：模型连续失败达到 breaker_failure_threshold 次 → 熔断（OPEN），
-- 持续 breaker_timeout_seconds 秒后进入半开探测（HALF_OPEN）。字段可空，空值由后端 DEFAULT_BREAKER_* 常量兜底（默认 5 / 60）。
ALTER TABLE model_route_policy
  ADD COLUMN breaker_failure_threshold INT NULL DEFAULT 5 COMMENT '熔断连续失败次数阈值' AFTER fallback_enabled,
  ADD COLUMN breaker_timeout_seconds INT NULL DEFAULT 60 COMMENT '熔断持续时间(秒)' AFTER breaker_failure_threshold;
