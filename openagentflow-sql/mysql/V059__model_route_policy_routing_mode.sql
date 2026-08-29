USE openagentflow;

-- 模型网关成本优化路由（Phase 11）。
-- 为 model_route_policy 增加路由模式：weighted（默认，按权重分发，现状）/
-- cost_first（在最低优先级放量池内确定性选估算单次成本最低的模型）。
-- NOT NULL DEFAULT 'weighted'，存量行与新行均自动落默认值，向后零变化。
ALTER TABLE model_route_policy
  ADD COLUMN routing_mode varchar(16) NOT NULL DEFAULT 'weighted'
  COMMENT '路由模式: weighted按权重分发 / cost_first按估算成本优选' AFTER breaker_timeout_seconds;
