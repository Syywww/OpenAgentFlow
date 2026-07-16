USE openagentflow;

-- P73：PromptOps 生产化，覆盖版本绑定、变量 Schema、环境晋级、灰度、实验和运行指标。
ALTER TABLE agent
  ADD COLUMN system_prompt_version_id char(36) NULL COMMENT '锁定的System Prompt版本ID' AFTER system_prompt_template_id,
  ADD COLUMN prompt_binding_mode varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT 'Prompt绑定模式：MANUAL手工、LOCKED锁定版本、FOLLOW_STABLE跟随稳定版' AFTER system_prompt_version_id,
  ADD COLUMN prompt_variables json NOT NULL DEFAULT (JSON_OBJECT()) COMMENT 'Agent级Prompt变量值JSON' AFTER prompt_binding_mode;

ALTER TABLE prompt_template
  ADD COLUMN variable_schema json NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '强类型变量Schema JSON数组' AFTER variables,
  ADD COLUMN stable_version_id char(36) NULL COMMENT '当前稳定版本ID' AFTER variable_schema,
  ADD COLUMN current_environment varchar(32) NOT NULL DEFAULT 'development' COMMENT '当前最高晋级环境：development、testing、production' AFTER stable_version_id,
  ADD COLUMN risk_level varchar(32) NOT NULL DEFAULT 'low' COMMENT 'Prompt风险等级：low、medium、high' AFTER current_environment;

ALTER TABLE prompt_template_version
  ADD COLUMN content_hash char(64) NULL COMMENT 'Prompt内容SHA-256哈希' AFTER variables,
  ADD COLUMN validation_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '校验状态：pending、passed、blocked' AFTER content_hash,
  ADD COLUMN validation_result json NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '注入、敏感信息、变量和回归校验结果JSON' AFTER validation_status,
  ADD COLUMN quality_score decimal(8,4) NULL COMMENT '关联评测质量得分' AFTER validation_result,
  ADD COLUMN environment varchar(32) NOT NULL DEFAULT 'development' COMMENT '版本当前环境' AFTER quality_score,
  ADD COLUMN published_at datetime(3) NULL COMMENT '版本发布时间' AFTER environment;

ALTER TABLE prompt_experiment
  ADD COLUMN workspace_id char(36) NULL COMMENT '工作空间ID' AFTER id,
  ADD COLUMN metric_key varchar(80) NOT NULL DEFAULT 'quality_score' COMMENT '主要胜出指标' AFTER traffic_policy,
  ADD COLUMN min_sample_size int NOT NULL DEFAULT 30 COMMENT '自动选优最小样本数' AFTER metric_key,
  ADD COLUMN confidence_threshold decimal(8,4) NOT NULL DEFAULT 0.9500 COMMENT '自动选优置信阈值' AFTER min_sample_size,
  ADD COLUMN auto_winner_enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用自动选优' AFTER confidence_threshold,
  ADD COLUMN winner_variant_id char(36) NULL COMMENT '胜出变体ID' AFTER auto_winner_enabled;

ALTER TABLE prompt_experiment_variant
  ADD COLUMN prompt_version_id char(36) NULL COMMENT '关联Prompt版本ID' AFTER variant_code,
  ADD COLUMN sample_count bigint NOT NULL DEFAULT 0 COMMENT '累计样本数' AFTER metrics_snapshot,
  ADD COLUMN success_count bigint NOT NULL DEFAULT 0 COMMENT '累计成功数' AFTER sample_count,
  ADD COLUMN failure_count bigint NOT NULL DEFAULT 0 COMMENT '累计失败数' AFTER success_count,
  ADD COLUMN avg_quality_score decimal(10,4) NOT NULL DEFAULT 0 COMMENT '平均质量得分' AFTER failure_count,
  ADD COLUMN avg_latency_ms decimal(14,4) NOT NULL DEFAULT 0 COMMENT '平均耗时毫秒' AFTER avg_quality_score,
  ADD COLUMN total_tokens bigint NOT NULL DEFAULT 0 COMMENT '累计Token数量' AFTER avg_latency_ms,
  ADD COLUMN total_cost decimal(18,6) NOT NULL DEFAULT 0 COMMENT '累计成本' AFTER total_tokens;

ALTER TABLE runtime_llm_call
  ADD COLUMN prompt_template_id char(36) NULL COMMENT '本次调用实际Prompt模板ID' AFTER route_decision,
  ADD COLUMN prompt_version_id char(36) NULL COMMENT '本次调用实际Prompt版本ID' AFTER prompt_template_id,
  ADD COLUMN prompt_content_hash char(64) NULL COMMENT '最终Prompt内容哈希' AFTER prompt_version_id,
  ADD COLUMN prompt_layers json NULL COMMENT 'Prompt分层装配摘要JSON' AFTER prompt_content_hash,
  ADD COLUMN prompt_variable_sources json NULL COMMENT 'Prompt变量来源JSON，不保存敏感明文' AFTER prompt_layers;

CREATE TABLE prompt_binding (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型：agent、workflow、rag、tool、evaluation',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  prompt_role varchar(64) NOT NULL DEFAULT 'system' COMMENT 'Prompt角色：system、user、rag、tool、evaluation、workflow',
  template_id char(36) NOT NULL COMMENT 'Prompt模板ID',
  version_id char(36) NULL COMMENT '锁定版本ID，跟随稳定版时为空',
  binding_mode varchar(32) NOT NULL DEFAULT 'LOCKED' COMMENT '绑定模式：LOCKED、FOLLOW_STABLE',
  variable_values json NOT NULL COMMENT '资源级变量值JSON',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by char(36) NULL COMMENT '创建人用户ID',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_prompt_binding_resource_role (resource_type, resource_id, prompt_role),
  KEY idx_prompt_binding_workspace (workspace_id, resource_type, enabled),
  KEY idx_prompt_binding_template (template_id, version_id)
) ENGINE=InnoDB COMMENT='Prompt资源版本绑定表';

CREATE TABLE prompt_environment_release (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  template_id char(36) NOT NULL COMMENT 'Prompt模板ID',
  version_id char(36) NOT NULL COMMENT 'Prompt版本ID',
  environment varchar(32) NOT NULL COMMENT '目标环境：development、testing、production',
  status varchar(32) NOT NULL DEFAULT 'active' COMMENT '发布状态：active、superseded、rolled_back',
  gray_percent int NOT NULL DEFAULT 100 COMMENT '灰度比例0到100',
  release_note varchar(500) NULL COMMENT '环境晋级说明',
  promoted_by char(36) NULL COMMENT '晋级人用户ID',
  promoted_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '晋级时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_prompt_environment_version (template_id, version_id, environment),
  KEY idx_prompt_release_active (workspace_id, template_id, environment, status)
) ENGINE=InnoDB COMMENT='Prompt多环境晋级与灰度发布表';

CREATE TABLE prompt_runtime_metric (
  id char(36) NOT NULL DEFAULT (UUID()) COMMENT '主键ID',
  workspace_id char(36) NULL COMMENT '工作空间ID',
  template_id char(36) NULL COMMENT 'Prompt模板ID',
  version_id char(36) NULL COMMENT 'Prompt版本ID',
  experiment_id char(36) NULL COMMENT 'Prompt实验ID',
  variant_id char(36) NULL COMMENT 'Prompt实验变体ID',
  run_id char(36) NULL COMMENT '运行ID',
  agent_id char(36) NULL COMMENT 'Agent ID',
  success tinyint(1) NOT NULL DEFAULT 1 COMMENT '调用是否成功',
  quality_score decimal(10,4) NULL COMMENT '质量得分',
  latency_ms int NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
  token_count int NOT NULL DEFAULT 0 COMMENT 'Token数量',
  cost_amount decimal(18,6) NOT NULL DEFAULT 0 COMMENT '成本金额',
  trusted_answer_passed tinyint(1) NULL COMMENT '可信回答是否通过',
  tool_success tinyint(1) NULL COMMENT '工具调用是否成功',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_prompt_metric_run_version (run_id, version_id),
  KEY idx_prompt_metric_version_time (version_id, created_at),
  KEY idx_prompt_metric_experiment (experiment_id, variant_id, created_at)
) ENGINE=InnoDB COMMENT='Prompt版本与实验运行指标表';

-- 将旧版 variables 迁移为强类型 Schema，并为已有版本计算稳定关系。
UPDATE prompt_template
SET variable_schema = variables
WHERE JSON_LENGTH(variable_schema) = 0;

UPDATE prompt_template p
SET stable_version_id = (
  SELECT v.id FROM prompt_template_version v
  WHERE v.template_id = p.id
  ORDER BY v.created_at DESC, v.id DESC LIMIT 1
)
WHERE p.status = 'published' AND p.stable_version_id IS NULL;

UPDATE prompt_template_version v
JOIN prompt_template p ON p.id = v.template_id
SET v.environment = IF(p.status = 'published', 'production', 'development'),
    v.validation_status = 'passed',
    v.validation_result = JSON_OBJECT('source', 'V046兼容迁移'),
    v.published_at = COALESCE(v.published_at, v.created_at);

UPDATE agent a
JOIN prompt_template p ON p.id = a.system_prompt_template_id
SET a.system_prompt_version_id = p.stable_version_id,
    a.prompt_binding_mode = IF(p.stable_version_id IS NULL, 'MANUAL', 'LOCKED')
WHERE a.system_prompt_template_id IS NOT NULL;

INSERT INTO prompt_binding (
  id, workspace_id, resource_type, resource_id, prompt_role, template_id, version_id,
  binding_mode, variable_values, enabled, created_by
)
SELECT UUID(), a.workspace_id, 'agent', a.id, 'system', a.system_prompt_template_id,
       a.system_prompt_version_id, a.prompt_binding_mode, a.prompt_variables, 1, a.created_by
FROM agent a
WHERE a.system_prompt_template_id IS NOT NULL
  AND a.workspace_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  template_id = VALUES(template_id),
  version_id = VALUES(version_id),
  binding_mode = VALUES(binding_mode),
  variable_values = VALUES(variable_values),
  enabled = 1,
  updated_at = CURRENT_TIMESTAMP(3);

INSERT IGNORE INTO iam_permission (
  permission_code, permission_name, permission_type, route_path, api_method, api_path, sort_order
) VALUES
  ('prompt:experiment', 'Prompt实验管理', 'api', '/prompts', 'ALL', '/api/prompt-templates/*/experiments/**', 106),
  ('prompt:release', 'Prompt环境发布', 'api', '/prompts', 'ALL', '/api/prompt-templates/*/releases/**', 107);

INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM iam_role r JOIN iam_permission p
  ON p.permission_code IN ('prompt:experiment', 'prompt:release')
WHERE r.role_code IN ('super_admin', 'admin', 'developer');
