-- P15：模型网关与模型治理增强。
-- 目标：补齐模型路由策略、候选模型、网关决策日志字段和默认策略种子数据。

SELECT IF(COUNT(*) > 0,
          'SELECT 1',
          'ALTER TABLE runtime_llm_call ADD COLUMN route_policy_id char(36) NULL COMMENT ''模型路由策略ID'' AFTER model_id')
INTO @ddl
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'runtime_llm_call'
  AND column_name = 'route_policy_id';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(COUNT(*) > 0,
          'SELECT 1',
          'ALTER TABLE runtime_llm_call ADD COLUMN gateway_scene_type varchar(80) NULL COMMENT ''模型网关场景类型'' AFTER route_policy_id')
INTO @ddl
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'runtime_llm_call'
  AND column_name = 'gateway_scene_type';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(COUNT(*) > 0,
          'SELECT 1',
          'ALTER TABLE runtime_llm_call ADD COLUMN route_decision json NULL COMMENT ''模型网关路由决策快照'' AFTER gateway_scene_type')
INTO @ddl
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'runtime_llm_call'
  AND column_name = 'route_decision';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(COUNT(*) > 0,
          'SELECT 1',
          'ALTER TABLE runtime_llm_call ADD COLUMN fallback_used tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否使用模型回退'' AFTER route_decision')
INTO @ddl
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'runtime_llm_call'
  AND column_name = 'fallback_used';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE runtime_llm_call COMMENT='运行时大模型调用日志表';
ALTER TABLE runtime_llm_call MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE runtime_llm_call MODIFY COLUMN run_id char(36) COMMENT '运行ID';
ALTER TABLE runtime_llm_call MODIFY COLUMN step_id char(36) COMMENT 'Trace步骤ID';
ALTER TABLE runtime_llm_call MODIFY COLUMN provider_id char(36) COMMENT '模型服务商ID';
ALTER TABLE runtime_llm_call MODIFY COLUMN model_id char(36) COMMENT '模型ID';
ALTER TABLE runtime_llm_call MODIFY COLUMN route_policy_id char(36) NULL COMMENT '模型路由策略ID';
ALTER TABLE runtime_llm_call MODIFY COLUMN gateway_scene_type varchar(80) NULL COMMENT '模型网关场景类型';
ALTER TABLE runtime_llm_call MODIFY COLUMN route_decision json NULL COMMENT '模型网关路由决策快照';
ALTER TABLE runtime_llm_call MODIFY COLUMN fallback_used tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否使用模型回退';
ALTER TABLE runtime_llm_call MODIFY COLUMN request_messages json NOT NULL COMMENT '请求消息JSON';
ALTER TABLE runtime_llm_call MODIFY COLUMN response_message json COMMENT '响应消息JSON';
ALTER TABLE runtime_llm_call MODIFY COLUMN stream tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否流式输出';
ALTER TABLE runtime_llm_call MODIFY COLUMN prompt_tokens int NOT NULL DEFAULT 0 COMMENT '提示词Token数';
ALTER TABLE runtime_llm_call MODIFY COLUMN completion_tokens int NOT NULL DEFAULT 0 COMMENT '完成Token数';
ALTER TABLE runtime_llm_call MODIFY COLUMN total_tokens int NOT NULL DEFAULT 0 COMMENT '总Token数';
ALTER TABLE runtime_llm_call MODIFY COLUMN cost_amount decimal(14,6) NOT NULL DEFAULT 0 COMMENT '模型调用成本金额';
ALTER TABLE runtime_llm_call MODIFY COLUMN latency_ms int COMMENT '调用耗时毫秒';
ALTER TABLE runtime_llm_call MODIFY COLUMN success tinyint(1) NOT NULL COMMENT '是否调用成功';
ALTER TABLE runtime_llm_call MODIFY COLUMN error_message text COMMENT '错误信息';
ALTER TABLE runtime_llm_call MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';

ALTER TABLE model_route_policy COMMENT='模型路由策略表';
ALTER TABLE model_route_policy MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE model_route_policy MODIFY COLUMN policy_code varchar(120) NOT NULL COMMENT '策略编码';
ALTER TABLE model_route_policy MODIFY COLUMN policy_name varchar(160) NOT NULL COMMENT '策略名称';
ALTER TABLE model_route_policy MODIFY COLUMN scene_type varchar(80) NOT NULL COMMENT '适用场景类型';
ALTER TABLE model_route_policy MODIFY COLUMN match_rule json NOT NULL COMMENT '匹配规则JSON';
ALTER TABLE model_route_policy MODIFY COLUMN fallback_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用失败回退';
ALTER TABLE model_route_policy MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '状态';
ALTER TABLE model_route_policy MODIFY COLUMN created_by char(36) COMMENT '创建人ID';
ALTER TABLE model_route_policy MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';
ALTER TABLE model_route_policy MODIFY COLUMN updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间';

ALTER TABLE model_route_candidate COMMENT='模型路由候选表';
ALTER TABLE model_route_candidate MODIFY COLUMN id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID';
ALTER TABLE model_route_candidate MODIFY COLUMN policy_id char(36) NOT NULL COMMENT '策略ID';
ALTER TABLE model_route_candidate MODIFY COLUMN model_id char(36) NOT NULL COMMENT '模型ID';
ALTER TABLE model_route_candidate MODIFY COLUMN priority int NOT NULL DEFAULT 0 COMMENT '候选优先级，数字越小越优先';
ALTER TABLE model_route_candidate MODIFY COLUMN weight decimal(6,4) NOT NULL DEFAULT 1 COMMENT '候选权重';
ALTER TABLE model_route_candidate MODIFY COLUMN max_latency_ms int COMMENT '最大允许平均耗时毫秒';
ALTER TABLE model_route_candidate MODIFY COLUMN max_cost_per_1k decimal(12,8) COMMENT '最大允许每千Token成本';
ALTER TABLE model_route_candidate MODIFY COLUMN enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用';
ALTER TABLE model_route_candidate MODIFY COLUMN created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间';

SELECT IF(COUNT(*) > 0,
          'SELECT 1',
          'CREATE INDEX idx_runtime_llm_call_route_policy ON runtime_llm_call(route_policy_id, created_at)')
INTO @ddl
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'runtime_llm_call'
  AND index_name = 'idx_runtime_llm_call_route_policy';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(COUNT(*) > 0,
          'SELECT 1',
          'CREATE INDEX idx_runtime_llm_call_gateway_scene ON runtime_llm_call(gateway_scene_type, created_at)')
INTO @ddl
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'runtime_llm_call'
  AND index_name = 'idx_runtime_llm_call_gateway_scene';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(COUNT(*) > 0,
          'SELECT 1',
          'CREATE INDEX idx_model_route_candidate_policy_priority ON model_route_candidate(policy_id, enabled, priority)')
INTO @ddl
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'model_route_candidate'
  AND index_name = 'idx_model_route_candidate_policy_priority';
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO model_route_policy
  (id, policy_code, policy_name, scene_type, match_rule, fallback_enabled, status)
VALUES
  ('8f62b5b8-9775-43c0-a89d-42c5d10f1501',
   'default-agent-chat',
   '默认 Agent 对话模型路由',
   'AGENT_CHAT',
   JSON_OBJECT('scope', 'GLOBAL', 'description', '未显式指定模型时使用的默认聊天路由策略'),
   1,
   'enabled');

INSERT IGNORE INTO model_route_candidate
  (id, policy_id, model_id, priority, weight, enabled)
SELECT UUID(),
       '8f62b5b8-9775-43c0-a89d-42c5d10f1501',
       m.id,
       ROW_NUMBER() OVER (ORDER BY m.is_default DESC, m.created_at DESC),
       1,
       1
FROM model_config m
JOIN model_provider p ON p.id = m.provider_id
WHERE m.model_type = 'chat'
  AND m.status = 'enabled'
  AND p.status = 'enabled'
  AND NOT EXISTS (
      SELECT 1
      FROM model_route_candidate c
      WHERE c.policy_id = '8f62b5b8-9775-43c0-a89d-42c5d10f1501'
        AND c.model_id = m.id
  );
