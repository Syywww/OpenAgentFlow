-- P43-P52：物理DAG、租户预占、SLO、一致性、AI护栏、发布门禁与供应链治理。

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(IN table_name_param varchar(128), IN column_name_param varchar(128), IN column_definition text)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name=table_name_param AND column_name=column_name_param
  ) THEN
    SET @ddl=CONCAT('ALTER TABLE `',table_name_param,'` ADD COLUMN `',column_name_param,'` ',column_definition);
    PREPARE statement_handle FROM @ddl;
    EXECUTE statement_handle;
    DEALLOCATE PREPARE statement_handle;
  END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('async_task','trace_id',"varchar(64) COMMENT '跨API、Outbox、Kafka和Worker的Trace ID' AFTER checkpoint_json");
CALL add_column_if_missing('async_task_outbox','trace_id',"varchar(64) COMMENT '消息链路Trace ID' AFTER schema_version");
CALL add_column_if_missing('runtime_run','release_gate_execution_id',"char(36) COMMENT '关联发布门禁执行ID' AFTER executor_id");

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS tenant_resource_reservation (
  id char(36) NOT NULL COMMENT '资源预占主键ID',
  workspace_id char(36) NOT NULL COMMENT '工作空间ID',
  resource_type varchar(32) NOT NULL COMMENT '资源类型：storage、runtime、task、token',
  resource_key varchar(160) NOT NULL COMMENT '业务资源幂等键',
  reserved_amount bigint NOT NULL COMMENT '预占数量',
  status varchar(32) NOT NULL DEFAULT 'reserved' COMMENT '状态：reserved、committed、released、expired',
  expires_at datetime(3) DEFAULT NULL COMMENT '预占过期时间',
  committed_at datetime(3) DEFAULT NULL COMMENT '提交时间',
  released_at datetime(3) DEFAULT NULL COMMENT '释放时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_resource_reservation (workspace_id,resource_type,resource_key),
  KEY idx_tenant_resource_reservation_expire (status,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作空间资源原子预占表';

CREATE TABLE IF NOT EXISTS platform_slo_policy (
  id char(36) NOT NULL COMMENT 'SLO策略主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID，空值代表平台级',
  policy_code varchar(80) NOT NULL COMMENT 'SLO策略编码',
  policy_name varchar(160) NOT NULL COMMENT 'SLO策略名称',
  metric_name varchar(160) NOT NULL COMMENT 'Prometheus指标名称',
  comparator varchar(16) NOT NULL COMMENT '比较符：gt、gte、lt、lte',
  threshold_value decimal(20,6) NOT NULL COMMENT '目标阈值',
  window_minutes int NOT NULL DEFAULT 5 COMMENT '统计窗口分钟数',
  severity varchar(16) NOT NULL DEFAULT 'warning' COMMENT '告警等级',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_platform_slo_policy (workspace_id,policy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台服务等级目标策略表';

CREATE TABLE IF NOT EXISTS platform_slo_violation (
  id char(36) NOT NULL COMMENT 'SLO违规主键ID',
  policy_id char(36) NOT NULL COMMENT '关联SLO策略ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  observed_value decimal(20,6) NOT NULL COMMENT '实际观测值',
  threshold_value decimal(20,6) NOT NULL COMMENT '目标阈值',
  status varchar(32) NOT NULL DEFAULT 'open' COMMENT '状态：open、acknowledged、resolved',
  detail_json json DEFAULT NULL COMMENT '违规详情JSON',
  first_seen_at datetime(3) NOT NULL COMMENT '首次发现时间',
  last_seen_at datetime(3) NOT NULL COMMENT '最近发现时间',
  resolved_at datetime(3) DEFAULT NULL COMMENT '恢复时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_platform_slo_violation_status (status,last_seen_at),
  KEY idx_platform_slo_violation_policy (policy_id,workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台SLO违规事件表';

CREATE TABLE IF NOT EXISTS data_consistency_issue (
  id char(36) NOT NULL COMMENT '一致性问题主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  resource_type varchar(64) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  storage_type varchar(32) NOT NULL COMMENT '异常存储：mysql、milvus、opensearch、minio、redis',
  issue_type varchar(64) NOT NULL COMMENT '问题类型',
  severity varchar(16) NOT NULL COMMENT '严重等级',
  status varchar(32) NOT NULL DEFAULT 'open' COMMENT '状态：open、repairing、resolved、ignored',
  evidence_json json DEFAULT NULL COMMENT '问题证据JSON',
  repair_task_id char(36) DEFAULT NULL COMMENT '修复任务ID',
  detected_at datetime(3) NOT NULL COMMENT '发现时间',
  resolved_at datetime(3) DEFAULT NULL COMMENT '解决时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_data_consistency_open (resource_type,resource_id,storage_type,issue_type,status),
  KEY idx_data_consistency_status (status,severity,detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨存储数据一致性问题表';

CREATE TABLE IF NOT EXISTS ai_guardrail_policy (
  id char(36) NOT NULL COMMENT 'AI护栏策略主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID，空值代表平台级',
  policy_code varchar(80) NOT NULL COMMENT '策略编码',
  policy_name varchar(160) NOT NULL COMMENT '策略名称',
  guard_stage varchar(32) NOT NULL COMMENT '护栏阶段：input、output、tool',
  risk_type varchar(64) NOT NULL COMMENT '风险类型',
  action_type varchar(32) NOT NULL COMMENT '动作：allow、redact、block、confirm',
  pattern_json json DEFAULT NULL COMMENT '匹配模式JSON',
  priority int NOT NULL DEFAULT 100 COMMENT '策略优先级',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_guardrail_policy (workspace_id,policy_code),
  KEY idx_ai_guardrail_stage (guard_stage,enabled,priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI输入输出与工具安全护栏策略表';

CREATE TABLE IF NOT EXISTS ai_guardrail_event (
  id char(36) NOT NULL COMMENT 'AI护栏事件主键ID',
  policy_id char(36) DEFAULT NULL COMMENT '命中策略ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  run_id char(36) DEFAULT NULL COMMENT '运行ID',
  agent_id char(36) DEFAULT NULL COMMENT '智能体ID',
  guard_stage varchar(32) NOT NULL COMMENT '护栏阶段',
  risk_type varchar(64) NOT NULL COMMENT '风险类型',
  action_type varchar(32) NOT NULL COMMENT '执行动作',
  risk_score decimal(8,4) NOT NULL DEFAULT 0 COMMENT '风险分数',
  content_hash varchar(64) DEFAULT NULL COMMENT '内容哈希，避免保存敏感原文',
  detail_json json DEFAULT NULL COMMENT '脱敏后的事件详情JSON',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ai_guardrail_event_run (run_id,created_at),
  KEY idx_ai_guardrail_event_risk (risk_type,action_type,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI安全护栏命中事件表';

CREATE TABLE IF NOT EXISTS release_gate_policy (
  id char(36) NOT NULL COMMENT '发布门禁策略主键ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  resource_type varchar(32) NOT NULL COMMENT '资源类型：agent、prompt、workflow、knowledge',
  policy_name varchar(160) NOT NULL COMMENT '策略名称',
  min_eval_score decimal(8,4) NOT NULL DEFAULT 0.8 COMMENT '最低评测得分',
  max_failure_rate decimal(8,4) NOT NULL DEFAULT 0.02 COMMENT '最大失败率',
  max_p95_latency_ms int NOT NULL DEFAULT 15000 COMMENT '最大P95耗时毫秒',
  require_security_pass tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否要求安全检查通过',
  require_cost_budget tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否要求成本预算通过',
  enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_release_gate_policy_scope (workspace_id,resource_type,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI资源发布质量门禁策略表';

CREATE TABLE IF NOT EXISTS release_gate_execution (
  id char(36) NOT NULL COMMENT '门禁执行主键ID',
  policy_id char(36) DEFAULT NULL COMMENT '应用策略ID',
  workspace_id char(36) DEFAULT NULL COMMENT '工作空间ID',
  resource_type varchar(32) NOT NULL COMMENT '资源类型',
  resource_id char(36) NOT NULL COMMENT '资源ID',
  target_version varchar(80) DEFAULT NULL COMMENT '目标版本',
  status varchar(32) NOT NULL DEFAULT 'running' COMMENT '状态：running、passed、blocked、error',
  eval_score decimal(8,4) DEFAULT NULL COMMENT '评测得分',
  failure_rate decimal(8,4) DEFAULT NULL COMMENT '失败率',
  p95_latency_ms int DEFAULT NULL COMMENT 'P95耗时毫秒',
  security_passed tinyint(1) DEFAULT NULL COMMENT '安全检查是否通过',
  cost_passed tinyint(1) DEFAULT NULL COMMENT '成本检查是否通过',
  detail_json json DEFAULT NULL COMMENT '门禁详情JSON',
  requested_by char(36) DEFAULT NULL COMMENT '发起用户ID',
  finished_at datetime(3) DEFAULT NULL COMMENT '完成时间',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_release_gate_resource (resource_type,resource_id,created_at),
  KEY idx_release_gate_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI资源发布质量门禁执行表';

CREATE TABLE IF NOT EXISTS software_artifact_attestation (
  id char(36) NOT NULL COMMENT '软件制品证明主键ID',
  artifact_name varchar(200) NOT NULL COMMENT '制品名称',
  artifact_version varchar(100) NOT NULL COMMENT '制品版本',
  artifact_digest varchar(160) NOT NULL COMMENT '制品SHA256摘要',
  sbom_uri varchar(500) DEFAULT NULL COMMENT 'SBOM文件地址',
  signature_uri varchar(500) DEFAULT NULL COMMENT '制品签名地址',
  vulnerability_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '漏洞扫描状态',
  critical_count int NOT NULL DEFAULT 0 COMMENT '严重漏洞数量',
  high_count int NOT NULL DEFAULT 0 COMMENT '高危漏洞数量',
  license_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '许可证检查状态',
  secret_scan_status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '密钥扫描状态',
  provenance_json json DEFAULT NULL COMMENT '构建来源证明JSON',
  status varchar(32) NOT NULL DEFAULT 'pending' COMMENT '制品准入状态',
  created_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_software_artifact_digest (artifact_digest),
  KEY idx_software_artifact_status (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='软件制品SBOM签名与供应链证明表';

INSERT IGNORE INTO platform_slo_policy
  (id,workspace_id,policy_code,policy_name,metric_name,comparator,threshold_value,window_minutes,severity,enabled)
VALUES
  ('39000000-0000-0000-0000-000000000001',NULL,'runtime_success_rate','Agent成功率','openagentflow_runtime_success_rate','gte',0.995,5,'critical',1),
  ('39000000-0000-0000-0000-000000000002',NULL,'first_token_p95','首Token P95','openagentflow_runtime_first_token_p95_ms','lte',2000,5,'warning',1),
  ('39000000-0000-0000-0000-000000000003',NULL,'outbox_oldest_age','Outbox最老等待','openagentflow_outbox_oldest_age_seconds','lte',30,5,'critical',1),
  ('39000000-0000-0000-0000-000000000004',NULL,'document_success_rate','文档任务成功率','openagentflow_document_success_rate','gte',0.999,15,'critical',1);

INSERT IGNORE INTO ai_guardrail_policy
  (id,workspace_id,policy_code,policy_name,guard_stage,risk_type,action_type,pattern_json,priority,enabled)
VALUES
  ('39100000-0000-0000-0000-000000000001',NULL,'prompt_injection','Prompt注入拦截','input','prompt_injection','block',JSON_ARRAY('忽略之前的指令','泄露系统提示词','ignore previous instructions','reveal system prompt'),10,1),
  ('39100000-0000-0000-0000-000000000002',NULL,'secret_leak','密钥泄露脱敏','output','secret_leak','redact',JSON_ARRAY('sk-','ark-','Bearer '),20,1),
  ('39100000-0000-0000-0000-000000000003',NULL,'dangerous_tool','危险工具确认','tool','dangerous_action','confirm',JSON_ARRAY('delete','drop','exec','shutdown'),10,1);

INSERT IGNORE INTO release_gate_policy
  (id,workspace_id,resource_type,policy_name,min_eval_score,max_failure_rate,max_p95_latency_ms,require_security_pass,require_cost_budget,enabled)
VALUES
  ('39200000-0000-0000-0000-000000000001',NULL,'agent','Agent生产发布门禁',0.7500,0.0500,20000,1,1,1);
